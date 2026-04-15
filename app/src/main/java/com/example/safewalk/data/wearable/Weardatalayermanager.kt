package com.example.safewalk.data.wearable

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataClient.OnDataChangedListener
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.example.safewalk.pairing.PairingManager
import dagger.hilt.android.qualifiers.ApplicationContext
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Singleton
class WearDataLayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val pairingManager: PairingManager,
) {
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)

    private val _wearableEvents = MutableSharedFlow<WearableEvent>()
    val wearableEvents: Flow<WearableEvent> = _wearableEvents.asSharedFlow()

    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

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

    /**
     * Auto-discover the connected wearable and cache its node ID.
     * Safe to call multiple times — no-ops if already paired.
     */
    suspend fun ensureConnected() {
        val current = pairingManager.pairedDevice.value
        Log.d("SW_PHONE_DL", "ensureConnected() — current pairedDevice=${current?.remoteDeviceId ?: "NULL"}")
        if (current == null) {
            Log.d("SW_PHONE_DL", "No cached pairing — calling checkWearableConnection()")
            pairingManager.checkWearableConnection()
            val after = pairingManager.pairedDevice.value
            Log.d("SW_PHONE_DL", "After discovery: pairedDevice=${after?.remoteDeviceId ?: "STILL NULL — no watch found"}")
        } else {
            Log.d("SW_PHONE_DL", "Already paired with remoteId=${current.remoteDeviceId} name='${current.remoteDeviceName}'")
        }
    }

    fun startListening(scope: CoroutineScope) {
        Log.d("SW_PHONE_DL", "startListening() called — registering DataClient listener on phone")
        scope.launch {
            dataClient.dataFlow
                .catch { e -> Log.e("SW_PHONE_DL", "Error in dataFlow", e) }
                .collect { event ->
                    val typeStr = when (event.type) {
                        DataEvent.TYPE_CHANGED -> "TYPE_CHANGED"
                        DataEvent.TYPE_DELETED -> "TYPE_DELETED"
                        else                  -> "TYPE_UNKNOWN(${event.type})"
                    }
                    val path = event.dataItem.uri.path
                    val host = event.dataItem.uri.host
                    Log.d("SW_PHONE_DL", "Event: type=$typeStr  path=$path  host=$host")

                    if (event.type != DataEvent.TYPE_CHANGED &&
                        event.type != DataEvent.TYPE_DELETED) {
                        Log.d("SW_PHONE_DL", "Skipping non-CHANGED/DELETED event")
                        return@collect
                    }

                    val knownWatchPaths = setOf("/check_in", "/timer_start", "/timer_sync", "/error")
                    if (path !in knownWatchPaths) {
                        Log.d("SW_PHONE_DL", "Path '$path' not in watch paths — ignoring (this is normal for /timer_state sent by phone itself)")
                        return@collect
                    }

                    val pairedDevice = pairingManager.pairedDevice.value
                    Log.d("SW_PHONE_DL", "Processing path=$path  pairedDevice=${pairedDevice?.remoteDeviceId ?: "NULL"}  eventHost=$host")

                    if (pairedDevice != null) {
                        if (!validateMessageSource(host, pairedDevice.remoteDeviceId)) {
                            Log.w("SW_PHONE_DL", "Source mismatch — host=$host expected=${pairedDevice.remoteDeviceId} — IGNORING")
                            return@collect
                        }
                        Log.d("SW_PHONE_DL", "Source validated OK: $host == ${pairedDevice.remoteDeviceId}")
                    } else {
                        Log.w("SW_PHONE_DL", "No pairing cached — accepting event and auto-discovering")
                        pairingManager.checkWearableConnection()
                    }

                    Log.d("SW_PHONE_DL", "Dispatching event for path=$path")
                    when (path) {
                        "/check_in" -> {
                            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                            val status = dataMap.getString("status")
                                ?: String(event.dataItem.data ?: byteArrayOf())
                            Log.d("SW_PHONE_DL", "/check_in received — status='$status' — emitting CheckInReceived")
                            _wearableEvents.emit(WearableEvent.CheckInReceived(status))
                        }

                        "/timer_start" -> {
                            Log.d("SW_PHONE_DL", "/timer_start received from watch — emitting TimerStartRequest (phone will use own settings)")
                            _wearableEvents.emit(WearableEvent.TimerStartRequest)
                        }

                        "/timer_sync" -> {
                            Log.d("SW_PHONE_DL", "/timer_sync received — emitting TimerSyncRequest")
                            _wearableEvents.emit(WearableEvent.TimerSyncRequest)
                        }

                        "/error" -> {
                            val errorMsg = String(event.dataItem.data ?: byteArrayOf())
                            Log.d("SW_PHONE_DL", "/error received: $errorMsg")
                            _wearableEvents.emit(WearableEvent.WearableError(errorMsg))
                        }
                    }
                }
        }
    }

    suspend fun sendTimerUpdate(
        isActive: Boolean,
        durationMinutes: Int,
        remainingSeconds: Int,
        startLocation: String? = null,
    ): Boolean {
        Log.d("SW_PHONE_DL", "sendTimerUpdate() called: isActive=$isActive  duration=${durationMinutes}min  remaining=${remainingSeconds}s")
        return try {
            ensureConnected()
            val pairedAfter = pairingManager.pairedDevice.value
            Log.d("SW_PHONE_DL", "sendTimerUpdate: pairedDevice after ensureConnected=${pairedAfter?.remoteDeviceId ?: "NULL"}")

            val timerData = TimerSyncData(
                isActive = isActive,
                startTime = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                remainingSeconds = remainingSeconds,
                startLocation = startLocation,
            )

            val jsonString = json.encodeToString(timerData)
            Log.d("SW_PHONE_DL", "Sending /timer_state JSON: $jsonString")

            val request = PutDataMapRequest.create("/timer_state").apply {
                dataMap.putByteArray("payload", jsonString.toByteArray())
                dataMap.putString("source_device_id", pairingManager.deviceId)
                pairedAfter?.let { dataMap.putString("target_device_id", it.remoteDeviceId) }
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d("SW_PHONE_DL", "/timer_state PUT succeeded — watch should receive DataEvent now")
            _wearableEvents.emit(WearableEvent.TimerSent)
            true
        } catch (e: Exception) {
            Log.e("SW_PHONE_DL", "Error sending timer update", e)
            _wearableEvents.emit(WearableEvent.SendError(e.message ?: "Unknown error"))
            false
        }
    }

    suspend fun sendContactsList(contacts: List<ContactData>): Boolean {
        return try {
            ensureConnected()

            val jsonString = json.encodeToString(contacts)
            val request = PutDataMapRequest.create("/contacts").apply {
                dataMap.putByteArray("payload", jsonString.toByteArray())
                dataMap.putString("source_device_id", pairingManager.deviceId)
                pairingManager.pairedDevice.value?.let {
                    dataMap.putString("target_device_id", it.remoteDeviceId)
                }
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            true
        } catch (e: Exception) {
            Log.e("DataLayer", "Error sending contacts", e)
            false
        }
    }

    suspend fun sendCheckInResponse(status: String, timestamp: Long): Boolean {
        return try {
            ensureConnected()

            val request = PutDataMapRequest.create("/check_in_response").apply {
                dataMap.putString("status", status)
                dataMap.putLong("timestamp", timestamp)
                dataMap.putString("source_device_id", pairingManager.deviceId)
                pairingManager.pairedDevice.value?.let {
                    dataMap.putString("target_device_id", it.remoteDeviceId)
                }
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            true
        } catch (e: Exception) {
            Log.e("DataLayer", "Error sending check-in response", e)
            false
        }
    }

    private fun validateMessageSource(source: String?, expectedRemoteId: String): Boolean {
        return source == expectedRemoteId
    }

    suspend fun disconnect() {
        try {
            dataClient.deleteDataItems(Uri.parse("")).await()
        } catch (e: Exception) {
            Log.e("DataLayer", "Error on disconnect", e)
        }
    }
}

sealed class WearableEvent {
    data object TimerSyncRequest : WearableEvent()
    data object TimerSent : WearableEvent()
    // Watch asks the phone to start a walk; duration comes from the phone's own settings.
    data object TimerStartRequest : WearableEvent()
    data class CheckInReceived(val status: String) : WearableEvent()
    data class WearableError(val error: String) : WearableEvent()
    data class SendError(val error: String) : WearableEvent()
}

@kotlinx.serialization.Serializable
data class TimerSyncData(
    val isActive: Boolean,
    val startTime: Long,
    val durationMinutes: Int,
    val remainingSeconds: Int,
    val startLocation: String? = null,
)

@kotlinx.serialization.Serializable
data class ContactData(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String = "",
    val isPrimary: Boolean = false,
)