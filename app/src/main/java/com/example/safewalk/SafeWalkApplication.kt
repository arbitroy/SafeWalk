package com.example.safewalk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class SafeWalkApplication  : Application() {
    override fun onCreate() {
        super.onCreate()
        // Initialize app-level dependencies if needed
    }
}