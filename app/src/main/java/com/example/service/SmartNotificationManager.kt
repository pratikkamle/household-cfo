package com.example.service

import android.app.Notification
import android.content.Context
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.data.model.ParsedTransaction
import com.example.util.SmartTransactionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class CapturedNotification(
    val id: String,
    val packageName: String,
    val title: String,
    val text: String,
    val timestamp: Long,
    val parsedTx: ParsedTransaction?
)

object SmartNotificationManager {
    private const val TAG = "SmartNotifManager"
    private val _capturedList = MutableStateFlow<List<CapturedNotification>>(emptyList())
    val capturedList: StateFlow<List<CapturedNotification>> = _capturedList.asStateFlow()

    private val _latestDetectedTx = MutableStateFlow<ParsedTransaction?>(null)
    val latestDetectedTx: StateFlow<ParsedTransaction?> = _latestDetectedTx.asStateFlow()

    fun onNotificationReceived(context: Context, sbn: StatusBarNotification) {
        try {
            val pkg = sbn.packageName ?: ""
            // Filter out system or noisy packages
            if (pkg.startsWith("com.android.systemui") || pkg.contains("launcher") || pkg.contains("dialer")) {
                return
            }

            val extras: Bundle? = sbn.notification?.extras
            val title = extras?.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: ""
            val text = extras?.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
            val bigText = extras?.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString() ?: ""

            val combined = "$title $text $bigText".trim()
            if (combined.isEmpty()) return

            val parsed = SmartTransactionParser.parse(combined)
            if (parsed != null) {
                Log.d(TAG, "Financial transaction detected from $pkg: ${parsed.amount} at ${parsed.merchant}")
                val notif = CapturedNotification(
                    id = "${sbn.id}_${sbn.postTime}",
                    packageName = pkg,
                    title = title,
                    text = if (text.isNotEmpty()) text else combined,
                    timestamp = sbn.postTime,
                    parsedTx = parsed
                )
                _capturedList.value = listOf(notif) + _capturedList.value.take(19)
                _latestDetectedTx.value = parsed
            }
        } catch (e: Throwable) {
            Log.w(TAG, "Safe handling of incoming notification error: ${e.message}")
        }
    }

    fun clearLatestDetected() {
        _latestDetectedTx.value = null
    }
}

class FinanceNotificationListenerService : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        try {
            SmartNotificationManager.onNotificationReceived(applicationContext, sbn)
        } catch (e: Throwable) {
            Log.w("NotificationService", "Oppo/OEM safe guard caught notification processing exception: ${e.message}")
        }
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i("NotificationService", "Household CFO Notification Listener Connected")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.i("NotificationService", "Household CFO Notification Listener Disconnected")
    }
}
