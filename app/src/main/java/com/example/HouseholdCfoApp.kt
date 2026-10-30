package com.example

import android.app.Application
import com.example.service.SmartNotificationManager
import com.google.firebase.FirebaseApp

class HouseholdCfoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
            Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
                android.util.Log.e("HouseholdCfoApp", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
                com.example.ui.components.CrashDiagnostics.recordFatalError(throwable)
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
        runCatching {
            FirebaseApp.initializeApp(this)
        }.onFailure {
            android.util.Log.w("HouseholdCfoApp", "FirebaseApp init warning: ${it.message}")
            com.example.ui.components.CrashDiagnostics.recordLog("INIT", "Firebase initialization warning: ${it.message}", it)
        }
        runCatching {
            SmartNotificationManager.initialize(this)
        }.onFailure {
            android.util.Log.w("HouseholdCfoApp", "SmartNotificationManager init warning: ${it.message}")
        }
    }
}
