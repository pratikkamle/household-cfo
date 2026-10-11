package com.example

import android.app.Application
import android.util.Log
import com.google.firebase.FirebaseApp

class HouseholdCfoApp : Application() {
    override fun onCreate() {
        super.onCreate()
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
