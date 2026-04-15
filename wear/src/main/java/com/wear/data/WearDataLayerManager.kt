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
        Log.d("SW_WEAR_DL", "startListening() called — registering DataClient listener")
        scope.launch {
            dataClient.dataFlow
                .catch { e -> Log.e("SW_WEAR_DL", "Error in dataFlow", e) }
                .collect { event ->
                    val typeStr = when (event.type) {
                        DataEvent.TYPE_CHANGED -> "TYPE_CHANGED"
                        DataEvent.TYPE_DELETED -> "TYPE_DELETED"
                        else                  -> "TYPE_UNKNOWN(${event.type})"
                    }
                    val path = event.dataItem.uri.path
                    val host = event.dataItem.uri.host
                    Log.d("SW_WEAR_DL", "Event received: type=$typeStr  path=$path  host=$host")

                    if (event.type != DataEvent.TYPE_CHANGED) {
                        Log.d("SW_WEAR_DL", "Skipping non-CHANGED event for path=$path")
                        return@collect
                    }

                    when (path) {
                        "/timer_state" -> {
                            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                            val payload = dataMap.getByteArray("payload")
                            Log.d("SW_WEAR_DL", "/timer_state received — payload=${if (payload == null) "NULL" else "${payload.size} bytes: ${String(payload, Charsets.UTF_8)}"}")
                            repository.updateTimerState(payload)
                        }
                        "/contacts" -> {
                            val payload = DataMapItem.fromDataItem(event.dataItem)
                                .dataMap.getByteArray("payload")
                            Log.d("SW_WEAR_DL", "/contacts received — payload=${payload?.size ?: "NULL"} bytes")
                            repository.updateContacts(payload)
                        }
                        "/pairing_confirm" -> {
                            val code = String(event.dataItem.data ?: byteArrayOf())
                            Log.d("SW_WEAR_DL", "/pairing_confirm received: $code")
                        }
                        else -> Log.d("SW_WEAR_DL", "Ignoring unhandled path: $path")
                    }
                }
        }
    }

    suspend fun sendCheckIn(status: String) {
        try {
            val pairedDeviceId = pairedDeviceId()
            Log.d("SW_WEAR_DL", "sendCheckIn($status) — pairedDeviceId from prefs='$pairedDeviceId'")
            // Send regardless of pairedDeviceId; PutDataMapRequest broadcasts to all nodes.
            val request = PutDataMapRequest.create("/check_in").apply {
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("status", status)
                dataMap.putString("device", "wear")
                pairedDeviceId?.let { dataMap.putString("target_device_id", it) }
            }.asPutDataRequest().setUrgent()
            Log.d("SW_WEAR_DL", "Putting /check_in item to DataClient...")
            dataClient.putDataItem(request).await()
            Log.d("SW_WEAR_DL", "/check_in PUT succeeded for status=$status")
        } catch (e: Exception) {
            Log.e("SW_WEAR_DL", "Error sending check-in", e)
        }
    }

    /**
     * Asks the phone to start a SafeWalk session.
     * The phone is source of truth — it will send back the authoritative timer state.
     */
    suspend fun sendTimerStartRequest(durationMinutes: Int = 30) {
        try {
            val pairedDeviceId = pairedDeviceId()
            Log.d("SW_WEAR_DL", "sendTimerStartRequest(${durationMinutes}min) — pairedDeviceId from prefs='$pairedDeviceId'")
            // Send regardless of pairedDeviceId; PutDataMapRequest broadcasts to all nodes.
            val request = PutDataMapRequest.create("/timer_start").apply {
                dataMap.putInt("duration_minutes", durationMinutes)
                dataMap.putLong("timestamp", System.currentTimeMillis())
                dataMap.putString("device", "wear")
                pairedDeviceId?.let { dataMap.putString("target_device_id", it) }
            }.asPutDataRequest().setUrgent()
            Log.d("SW_WEAR_DL", "Putting /timer_start item to DataClient...")
            dataClient.putDataItem(request).await()
            Log.d("SW_WEAR_DL", "/timer_start PUT succeeded (${durationMinutes}min)")
        } catch (e: Exception) {
            Log.e("SW_WEAR_DL", "Error sending timer start request", e)
        }
    }

    private fun pairedDeviceId(): String? {
        val id = context.getSharedPreferences("wear_pairing", 0)
            .getString("paired_device_id", null)
        Log.d("SW_WEAR_DL", "pairedDeviceId() = ${id ?: "NULL (not set in wear_pairing prefs)"}")
        return id
    }
}
