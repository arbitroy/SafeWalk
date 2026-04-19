package com.example.safewalk.service

import android.util.Log
import com.example.safewalk.data.wearable.WearDataLayerManager
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class PhoneWearableListenerService : WearableListenerService() {

    @Inject lateinit var wearDataLayerManager: WearDataLayerManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        Log.d("SW_PHONE_SVC", "onDataChanged: ${dataEvents.count} event(s)")
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
