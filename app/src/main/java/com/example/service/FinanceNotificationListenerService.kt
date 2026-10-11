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
        super.onListenerConnected()
        SmartNotificationManager.updateServiceConnected(true)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        SmartNotificationManager.updateServiceConnected(false)
    }
}
