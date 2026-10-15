package com.example.service

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class FinanceNotificationListenerService : NotificationListenerService() {

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return
        try {
            SmartNotificationManager.onNotificationReceived(applicationContext, sbn)
        } catch (e: Exception) {
            Log.e("FinanceNotifListener", "Failed to process inbound notification", e)
        }
    }

    override fun onListenerConnected() {
        try {
            super.onListenerConnected()
            SmartNotificationManager.updateServiceConnected(true)
        } catch (t: Throwable) {
            Log.w("FinanceNotifListener", "onListenerConnected safety catch: ${t.message}")
        }
    }

    override fun onListenerDisconnected() {
        try {
            super.onListenerDisconnected()
            SmartNotificationManager.updateServiceConnected(false)
        } catch (t: Throwable) {
            Log.w("FinanceNotifListener", "onListenerDisconnected safety catch: ${t.message}")
        }
    }
}
