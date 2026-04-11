package com.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataClient.OnDataChangedListener
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

// WearDataLayerManager.kt
@Singleton
class WearDataLayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: WearRepository,
) {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)

    private val DataClient.dataFlow: Flow<DataEvent>
        get() = callbackFlow {
            val listener = OnDataChangedListener { dataEvents ->
                for (event in dataEvents) {
                    trySend(event.freeze())
                }
            }
            addListener(listener)
            awaitClose {
                removeListener(listener)
            }
        }

    // Listen for changes from phone
    fun startListening(scope: CoroutineScope) {
        scope.launch {
            dataClient.dataFlow
                .catch { e -> Log.e("DataLayer", "Error listening", e) }
                .collect { event ->
                    when (val uri = event.dataItem.uri.path) {
                        "/timer_state" -> {
                            val timerData = event.dataItem.data
                            repository.updateTimerState(timerData)
                        }
                        "/contacts" -> {
                            val contacts = event.dataItem.data
                            repository.updateContacts(contacts)
                        }
                    }
                }
        }
    }

    // Send data to phone
    suspend fun sendCheckIn(status: String) {
        try {
            val request = PutDataMapRequest.create("/check_in").apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("status", status)
                dataMap.putString("device", "wear")
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
        } catch (e: Exception) {
            Log.e("DataLayer", "Error sending check-in", e)
        }
    }
}