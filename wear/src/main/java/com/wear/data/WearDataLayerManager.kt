package com.wear.data

import android.content.Context
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataClient.OnDataChangedListener
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
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

@Singleton
class WearDataLayerManager @Inject constructor(
    @ApplicationContext val context: Context,
    private val repository: WearRepository,
) {
    private val dataClient = Wearable.getDataClient(context)

    private val DataClient.dataFlow: Flow<DataEvent>
        get() = callbackFlow {
            val listener = OnDataChangedListener { dataEvents ->
                for (event in dataEvents) {
                    trySend(event.freeze())
                }
            }
            addListener(listener)
            awaitClose { removeListener(listener) }
        }

    fun startListening(scope: CoroutineScope) {
        scope.launch {
            dataClient.dataFlow
                .catch { e -> Log.e("WearDataLayer", "Error listening", e) }
                .collect { event ->
                    if (event.type != DataEvent.TYPE_CHANGED) return@collect
                    when (event.dataItem.uri.path) {
                        "/timer_state" -> {
                            val payload = DataMapItem.fromDataItem(event.dataItem)
                                .dataMap
                                .getByteArray("payload")
                            repository.updateTimerState(payload)
                            Log.d("WearDataLayer", "Timer state synced from phone")
                        }
                        "/contacts" -> {
                            val payload = DataMapItem.fromDataItem(event.dataItem)
                                .dataMap
                                .getByteArray("payload")
                            repository.updateContacts(payload)
                            Log.d("WearDataLayer", "Contacts synced from phone")
                        }
                        "/pairing_confirm" -> {
                            val code = String(event.dataItem.data ?: byteArrayOf())
                            Log.d("WearDataLayer", "Pairing confirm: $code")
                        }
                    }
                }
        }
    }

    suspend fun sendCheckIn(status: String) {
        try {
            val pairedDeviceId = pairedDeviceId() ?: run {
                Log.w("WearDataLayer", "Cannot send check-in: device not paired")
                return
            }
            val request = PutDataMapRequest.create("/check_in").apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("status", status)
                dataMap.putString("device", "wear")
                dataMap.putString("target_device_id", pairedDeviceId)
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            Log.d("WearDataLayer", "Check-in sent: $status")
        } catch (e: Exception) {
            Log.e("WearDataLayer", "Error sending check-in", e)
        }
    }

    /**
     * Asks the phone to start a SafeWalk session with the given duration.
     * The phone is source of truth — it will send back the authoritative timer state.
     */
    suspend fun sendTimerStartRequest(durationMinutes: Int = 30) {
        try {
            val pairedDeviceId = pairedDeviceId() ?: run {
                Log.w("WearDataLayer", "Cannot request timer start: device not paired")
                return
            }
            val request = PutDataMapRequest.create("/timer_start").apply {
                dataMap.putInt("duration_minutes", durationMinutes)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("device", "wear")
                dataMap.putString("target_device_id", pairedDeviceId)
            }.asPutDataRequest().setUrgent()
            dataClient.putDataItem(request).await()
            Log.d("WearDataLayer", "Timer start request sent ($durationMinutes min)")
        } catch (e: Exception) {
            Log.e("WearDataLayer", "Error sending timer start request", e)
        }
    }

    private fun pairedDeviceId(): String? =
        context.getSharedPreferences("wear_pairing", 0)
            .getString("paired_device_id", null)
}
