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
        if (pairingManager.pairedDevice.value == null) {
            pairingManager.checkWearableConnection()
        }
    }

    fun startListening(scope: CoroutineScope) {
        scope.launch {
            dataClient.dataFlow
                .catch { e -> Log.e("DataLayer", "Error listening", e) }
                .collect { event ->
                    if (event.type != DataEvent.TYPE_CHANGED &&
                        event.type != DataEvent.TYPE_DELETED) return@collect

                    val path = event.dataItem.uri.path

                    // Only process paths that the watch sends to us.
                    // The Wearable Data Layer is authenticated at the OS level, so
                    // strict source validation is a secondary defence — we do it when
                    // we have a cached pairing, but we never silently drop events just
                    // because the cache is empty (e.g. fresh install, cleared prefs).
                    val knownWatchPaths = setOf("/check_in", "/timer_start", "/timer_sync", "/error")
                    if (path !in knownWatchPaths) return@collect

                    val pairedDevice = pairingManager.pairedDevice.value
                    if (pairedDevice != null) {
                        val source = event.dataItem.uri.host
                        if (!validateMessageSource(source, pairedDevice.remoteDeviceId)) {
                            Log.w("DataLayer", "Ignoring data from unpaired source: $source")
                            return@collect
                        }
                    } else {
                        // No pairing cached yet — accept the event and opportunistically
                        // discover the node so future sends work immediately.
                        Log.d("DataLayer", "No pairing cached; accepting event from watch and auto-discovering")
                        pairingManager.checkWearableConnection()
                    }

                    when (path) {
                        "/check_in" -> {
                            val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                            val status = dataMap.getString("status")
                                ?: String(event.dataItem.data ?: byteArrayOf())
                            _wearableEvents.emit(WearableEvent.CheckInReceived(status))
                        }

                        "/timer_start" -> {
                            // Emit without the duration — the phone will use its own
                            // configured default so the watch always reflects the right value.
                            _wearableEvents.emit(WearableEvent.TimerStartRequest)
                        }

                        "/timer_sync" -> {
                            _wearableEvents.emit(WearableEvent.TimerSyncRequest)
                        }

                        "/error" -> {
                            val errorMsg = String(event.dataItem.data ?: byteArrayOf())
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
        return try {
            // Discover the watch node if we don't have it cached yet.
            ensureConnected()

            val timerData = TimerSyncData(
                isActive = isActive,
                startTime = System.currentTimeMillis(),
                durationMinutes = durationMinutes,
                remainingSeconds = remainingSeconds,
                startLocation = startLocation,
            )

            val jsonString = json.encodeToString(timerData)
            val request = PutDataMapRequest.create("/timer_state").apply {
                dataMap.putByteArray("payload", jsonString.toByteArray())
                dataMap.putString("source_device_id", pairingManager.deviceId)
                // target_device_id is optional metadata; PutDataMapRequest broadcasts
                // to all connected nodes automatically.
                pairingManager.pairedDevice.value?.let {
                    dataMap.putString("target_device_id", it.remoteDeviceId)
                }
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            Log.d("DataLayer", "Timer state sent: active=$isActive remaining=${remainingSeconds}s")
            _wearableEvents.emit(WearableEvent.TimerSent)
            true
        } catch (e: Exception) {
            Log.e("DataLayer", "Error sending timer update", e)
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