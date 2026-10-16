package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AccountEntity
import java.util.Calendar

object CreditCardBillReminderHelper {

    private const val CHANNEL_ID = "cc_bill_reminders_channel"
    private const val CHANNEL_NAME = "Credit Card Bill Reminders"

    fun checkAndNotifyBillReminders(context: Context, accounts: List<AccountEntity>) {
        try {
            createNotificationChannel(context)

            val creditCards = accounts.filter { it.isActive && it.accountType == "CREDIT_CARD" }
            if (creditCards.isEmpty()) return

            val now = Calendar.getInstance()
            val currentDay = now.get(Calendar.DAY_OF_MONTH)
            val daysInMonth = now.getActualMaximum(Calendar.DAY_OF_MONTH)

            creditCards.forEach { card ->
                val dueDay = card.paymentDueDateDay ?: return@forEach
                val dueAmount = if (card.statementDueAmount > 0) {
                    card.statementDueAmount
                } else if (card.balance < 0) {
                    -card.balance
                } else {
                    card.balance
                }

                // Calculate days remaining until due date
                val daysUntilDue = if (dueDay >= currentDay) {
                    dueDay - currentDay
                } else {
                    (daysInMonth - currentDay) + dueDay
                }

                // If due in the next 5 days (or due today / within 3 days past due)
                if (daysUntilDue in 0..5 || (currentDay - dueDay in 1..2)) {
                    val formattedAmt = CurrencyFormatter.formatInr(dueAmount)
                    val title = if (daysUntilDue == 0) {
                        "🚨 ${card.name}: Bill Payment Due Today!"
                    } else if (daysUntilDue < 0) {
                        "⚠️ ${card.name}: Bill Payment Overdue!"
                    } else {
                        "💳 ${card.name}: Bill Due in $daysUntilDue days (${CurrencyFormatter.formatDayOfMonth(dueDay)})"
                    }

                    val message = "Payment of $formattedAmt is due on ${CurrencyFormatter.formatDayOfMonth(dueDay)} for your ${card.institution.ifBlank { "Credit Card" }} (${card.holder}). Tally recorded in monthly cashflow."

                    postNotification(context, card.id.toInt(), title, message)
                }
            }
        } catch (t: Throwable) {
            Log.e("CCBillReminder", "Failed to process bill reminders: ${t.message}")
        }
    }

    private fun postNotification(context: Context, notificationId: Int, title: String, content: String) {
        try {
            val intent = Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            }
            val pendingIntent = PendingIntent.getActivity(
                context,
                notificationId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
            )

            val builder = NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(title)
                .setContentText(content)
                .setStyle(NotificationCompat.BigTextStyle().bigText(content))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)

            val manager = NotificationManagerCompat.from(context)
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                androidx.core.content.ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == android.content.pm.PackageManager.PERMISSION_GRANTED
            } else {
                manager.areNotificationsEnabled()
            }

            if (hasPermission && manager.areNotificationsEnabled()) {
                manager.notify(1000 + notificationId, builder.build())
            }
        } catch (t: Throwable) {
            Log.w("CCBillReminder", "Notification post skipped: ${t.message}")
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts for credit card statement billing cycles and payment due dates"
                enableVibration(true)
            }
            manager?.createNotificationChannel(channel)
        }
    }
}
