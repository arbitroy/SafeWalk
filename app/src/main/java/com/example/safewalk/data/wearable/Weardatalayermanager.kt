package com.example.safewalk.data.wearable

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataClient.OnDataChangedListener
import com.google.android.gms.wearable.DataEvent
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

    fun startListening(scope: CoroutineScope) {
        scope.launch {
            dataClient.dataFlow
                .catch { e -> Log.e("DataLayer", "Error listening", e) }
                .collect { event ->
                    val pairedDevice = pairingManager.pairedDevice.value
                    if (pairedDevice == null) {
                        Log.w("DataLayer", "Received data but no paired device")
                        return@collect
                    }

                    when (event.type) {
                        DataEvent.TYPE_CHANGED, DataEvent.TYPE_DELETED -> {
                            val source = event.dataItem.uri.host
                            // Validate message is from paired wearable
                            if (!validateMessageSource(source, pairedDevice.remoteDeviceId)) {
                                Log.w("DataLayer", "Ignoring data from unpaired source: $source")
                                return@collect
                            }

                            when (event.dataItem.uri.path) {
                                "/check_in" -> {
                                    val dataMap = event.dataItem.data
                                    val status = String(dataMap ?: byteArrayOf())
                                    _wearableEvents.emit(WearableEvent.CheckInReceived(status))
                                }

                                "/timer_sync" -> {
                                    val dataMap = event.dataItem.data
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
        }
    }

    suspend fun sendTimerUpdate(
        isActive: Boolean,
        durationMinutes: Int,
        remainingSeconds: Int,
        startLocation: String? = null,
    ): Boolean {
        try {
            val pairedDevice = pairingManager.pairedDevice.value
            if (pairedDevice == null) {
                Log.w("DataLayer", "Cannot send timer update: no paired device")
                return false
            }

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
                dataMap.putString("target_device_id", pairedDevice.remoteDeviceId)
                dataMap.putLong("timestamp", System.currentTimeMillis())
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            _wearableEvents.emit(WearableEvent.TimerSent)
            return true
        } catch (e: Exception) {
            Log.e("DataLayer", "Error sending timer update", e)
            _wearableEvents.emit(WearableEvent.SendError(e.message ?: "Unknown error"))
            return false
        }
    }

    suspend fun sendContactsList(contacts: List<ContactData>): Boolean {
        try {
            val pairedDevice = pairingManager.pairedDevice.value
            if (pairedDevice == null) {
                Log.w("DataLayer", "Cannot send contacts: no paired device")
                return false
            }

            val jsonString = json.encodeToString(contacts)
            val request = PutDataMapRequest.create("/contacts").apply {
                dataMap.putByteArray("payload", jsonString.toByteArray())
                dataMap.putString("source_device_id", pairingManager.deviceId)
                dataMap.putString("target_device_id", pairedDevice.remoteDeviceId)
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            return true
        } catch (e: Exception) {
            Log.e("DataLayer", "Error sending contacts", e)
            return false
        }
    }

    suspend fun sendCheckInResponse(status: String, timestamp: Long): Boolean {
        try {
            val pairedDevice = pairingManager.pairedDevice.value
            if (pairedDevice == null) {
                Log.w("DataLayer", "Cannot send check-in response: no paired device")
                return false
            }

            val request = PutDataMapRequest.create("/check_in_response").apply {
                dataMap.putString("status", status)
                dataMap.putLong("timestamp", timestamp)
                dataMap.putString("source_device_id", pairingManager.deviceId)
                dataMap.putString("target_device_id", pairedDevice.remoteDeviceId)
            }.asPutDataRequest().setUrgent()

            dataClient.putDataItem(request).await()
            return true
        } catch (e: Exception) {
            Log.e("DataLayer", "Error sending check-in response", e)
            return false
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