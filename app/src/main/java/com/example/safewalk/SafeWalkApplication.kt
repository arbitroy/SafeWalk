package com.example.safewalk

import android.app.Application
import android.util.Log
import com.example.safewalk.data.wearable.WearDataLayerManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Inject

@HiltAndroidApp
class SafeWalkApplication : Application() {

    @Inject lateinit var wearDataLayerManager: WearDataLayerManager

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("SW_PHONE_APP", "SafeWalkApplication.onCreate() — starting DataClient listener")
        wearDataLayerManager.startListening(appScope)
    }
}
