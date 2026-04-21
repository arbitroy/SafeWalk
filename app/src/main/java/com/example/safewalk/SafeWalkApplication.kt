package com.example.safewalk

import android.app.Application
import android.util.Log
import com.example.safewalk.data.firebase.PhoneFirebaseSyncManager
import com.google.firebase.database.FirebaseDatabase
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class SafeWalkApplication : Application() {

    @Inject lateinit var phoneFirebaseSyncManager: PhoneFirebaseSyncManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        FirebaseDatabase.getInstance().setPersistenceEnabled(true)
        Log.d("SW_PHONE_APP", "SafeWalkApplication.onCreate() — starting Firebase sync listener")
        phoneFirebaseSyncManager.startListening(appScope)
    }
}
