package com.example.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationManagerCompat
import com.example.util.ParsedTransaction
import com.example.util.SmartTransactionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class CapturedNotification(
    val id: String = UUID.randomUUID().toString(),
    val appName: String,
    val packageName: String,
    val title: String,
    val body: String,
    val timestamp: Long = System.currentTimeMillis(),
    val parsedTransaction: ParsedTransaction? = null,
    val isSaved: Boolean = false
)

object SmartNotificationManager {

    private val _capturedNotifications = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val capturedNotifications: StateFlow<List<CapturedNotification>> = _capturedNotifications.asStateFlow()

    private val _isServiceConnected = MutableStateFlow(false)
    val isServiceConnected: StateFlow<Boolean> = _isServiceConnected.asStateFlow()

    fun updateServiceConnected(connected: Boolean) {
        _isServiceConnected.value = connected
    }

    fun isNotificationAccessGranted(context: Context): Boolean {
        return try {
            val enabledPackages = NotificationManagerCompat.getEnabledListenerPackages(context)
            enabledPackages.contains(context.packageName)
        } catch (e: Exception) {
            false
        }
    }

    fun openNotificationListenerSettings(context: Context) {
        try {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (ignored: Exception) {}
        }
    }

    fun onNotificationReceived(context: Context, sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return
        val extras = notification.extras ?: return
        val packageName = sbn.packageName ?: ""

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

        val fullBody = when {
            bigText.isNotBlank() -> bigText
            text.isNotBlank() -> text
            else -> ""
        }

        if (fullBody.isBlank() && title.isBlank()) return

        val combinedContent = "$title $fullBody".trim()

        // Filter financial transaction keywords or financial packages
        if (!isTransactionRelated(combinedContent, packageName)) return

        val parsed = SmartTransactionParser.parse(combinedContent)

        val appName = inferAppName(packageName, title)

        val item = CapturedNotification(
            appName = appName,
            packageName = packageName,
            title = title,
            body = fullBody,
            timestamp = sbn.postTime.takeIf { it > 0 } ?: System.currentTimeMillis(),
            parsedTransaction = parsed
        )

        // Avoid exact duplicates in list within recent items
        val current = _capturedNotifications.value
        val isDuplicate = current.any { it.body == item.body && Math.abs(it.timestamp - item.timestamp) < 5000 }
        if (!isDuplicate) {
            _capturedNotifications.value = listOf(item) + current.take(29)
        }
    }

    fun simulateNotification(
        appName: String,
        sampleText: String,
        packageName: String = "com.google.android.apps.nbu.paisa.user"
    ) {
        val parsed = SmartTransactionParser.parse(sampleText)
        val item = CapturedNotification(
            appName = appName,
            packageName = packageName,
            title = "Transaction Alert",
            body = sampleText,
            timestamp = System.currentTimeMillis(),
            parsedTransaction = parsed
        )
        _capturedNotifications.value = listOf(item) + _capturedNotifications.value.take(29)
    }

    fun markNotificationSaved(id: String) {
        _capturedNotifications.value = _capturedNotifications.value.map {
            if (it.id == id) it.copy(isSaved = true) else it
        }
    }

    fun removeNotification(id: String) {
        _capturedNotifications.value = _capturedNotifications.value.filter { it.id != id }
    }

    fun clearAll() {
        _capturedNotifications.value = emptyList()
    }

    private fun isTransactionRelated(text: String, pkg: String): Boolean {
        val lower = text.lowercase()
        val hasMoneyIndicator = lower.contains("rs.") || lower.contains("rs ") || lower.contains("inr") ||
                lower.contains("₹") || lower.contains("debited") || lower.contains("spent") ||
                lower.contains("paid") || lower.contains("credited") || lower.contains("vpa") ||
                lower.contains("txn") || lower.contains("transaction")

        val isFinancePackage = pkg.contains("paisa") || pkg.contains("phonepe") ||
                pkg.contains("paytm") || pkg.contains("cred") || pkg.contains("bank") ||
                pkg.contains("bhim") || pkg.contains("mms") || pkg.contains("messaging")

        return hasMoneyIndicator || isFinancePackage
    }

    private fun inferAppName(pkg: String, title: String): String {
        val p = pkg.lowercase()
        return when {
            p.contains("paisa") || title.contains("Google Pay", true) -> "Google Pay"
            p.contains("phonepe") || title.contains("PhonePe", true) -> "PhonePe"
            p.contains("paytm") || title.contains("Paytm", true) -> "Paytm"
            p.contains("cred") || title.contains("CRED", true) -> "CRED"
            p.contains("hdfc") || title.contains("HDFC", true) -> "HDFC Bank"
            p.contains("icici") || title.contains("ICICI", true) -> "ICICI Bank"
            p.contains("sbi") || title.contains("SBI", true) -> "SBI"
            p.contains("axis") || title.contains("Axis", true) -> "Axis Bank"
            p.contains("messaging") || p.contains("mms") -> "SMS Alerts"
            else -> title.ifBlank { "Bank Alert" }
        }
    }
}
