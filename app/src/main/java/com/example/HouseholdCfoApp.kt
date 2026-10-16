package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class HouseholdCfoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("HouseholdCfoCrash", "Caught uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            val msg = throwable.message?.lowercase() ?: ""
            val isOemKnownBug = throwable is android.os.BadParcelableException ||
                    throwable is android.os.DeadSystemException ||
                    throwable is android.content.res.Resources.NotFoundException ||
                    msg.contains("coloros") ||
                    msg.contains("oppo") ||
                    msg.contains("notificationlistener") ||
                    msg.contains("badtokenexception") ||
                    msg.contains("badparcelable")
            if (!isOemKnownBug) {
                defaultHandler?.uncaughtException(thread, throwable)
            } else {
                Log.w("HouseholdCfoCrash", "Suppressed OEM/ColorOS background crash to keep app alive: ${throwable.message}")
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
