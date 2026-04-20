package com.example.safewalk.service

import android.content.Intent
import android.os.IBinder
import android.util.Log
import com.example.safewalk.data.wearable.WearDataLayerManager
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class PhoneWearableListenerService : WearableListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface PhoneListenerEntryPoint {
        fun wearDataLayerManager(): WearDataLayerManager
    }

    private val wearDataLayerManager: WearDataLayerManager by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            PhoneListenerEntryPoint::class.java,
        ).wearDataLayerManager()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        Log.d("SW_PHONE_SVC", "onCreate() — service started by GMS")
    }

    override fun onBind(intent: Intent?): IBinder? {
        Log.d("SW_PHONE_SVC", "onBind() intent=$intent")
        return super.onBind(intent)
    }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("SW_PHONE_SVC", "onDataChanged: ${dataEvents.count} event(s)")
        dataEvents.forEach { event ->
            scope.launch {
                wearDataLayerManager.processDataEvent(event.freeze())
            }
        }
    }

    override fun onDestroy() {
        Log.d("SW_PHONE_SVC", "onDestroy()")
        super.onDestroy()
        scope.cancel()
    }
}
