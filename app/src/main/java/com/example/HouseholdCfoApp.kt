package com.example

import android.app.Application
import com.example.service.SmartNotificationManager
import com.google.firebase.FirebaseApp

class HouseholdCfoApp : Application() {
    override fun onCreate() {
        super.onCreate()
        runCatching {
            FirebaseApp.initializeApp(this)
        }
        SmartNotificationManager.initialize(this)
    }
}
