package com.wear.service

import android.util.Log
import com.wear.data.WearDataLayerManager
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

class WearWearableListenerService : WearableListenerService() {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WearListenerEntryPoint {
        fun wearDataLayerManager(): WearDataLayerManager
    }

    private val wearDataLayerManager: WearDataLayerManager by lazy {
        EntryPointAccessors.fromApplication(
            applicationContext,
            WearListenerEntryPoint::class.java,
        ).wearDataLayerManager()
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("SW_WEAR_SVC", "onDataChanged: ${dataEvents.count} event(s)")
        dataEvents.forEach { event ->
            scope.launch {
                wearDataLayerManager.processDataEvent(event.freeze())
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }
}
