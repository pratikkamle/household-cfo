package com.example

import android.app.Application
import android.content.res.Resources
import android.os.BadParcelableException
import android.os.DeadSystemException
import android.util.Log
import com.google.firebase.FirebaseApp
import java.util.Locale

class HouseholdCfoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            val threadName = thread.name
            val message = throwable.message ?: ""
            Log.e("HouseholdCfoCrash", "Caught uncaught exception on thread $threadName: $message", throwable)
            val lowerMsg = message.lowercase(Locale.ROOT)
            val isOemIssue = throwable is BadParcelableException ||
                    throwable is DeadSystemException ||
                    throwable is Resources.NotFoundException ||
                    lowerMsg.contains("coloros") ||
                    lowerMsg.contains("oppo") ||
                    lowerMsg.contains("notificationlistener") ||
                    lowerMsg.contains("badtokenexception") ||
                    lowerMsg.contains("badparcelable")

            if (isOemIssue) {
                Log.w("HouseholdCfoCrash", "Suppressed OEM/ColorOS background crash to keep app alive: ${throwable.message}")
            } else {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }

        try {
            if (FirebaseApp.getApps(this).isEmpty()) {
                FirebaseApp.initializeApp(this)
                Log.d("HouseholdCfoApp", "FirebaseApp initialized successfully")
            }
        } catch (e: Exception) {
            Log.e("HouseholdCfoApp", "Failed to initialize FirebaseApp", e)
        }
    }
}
