package com.wear.data

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

@Singleton
class WearRepository @Inject constructor() {
    private val _timerState = MutableStateFlow(TimerState(SafeWalkSession.Idle, 0))
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _connectedDevice = MutableStateFlow<ConnectedDevice?>(null)
    val connectedDevice: StateFlow<ConnectedDevice?> = _connectedDevice.asStateFlow()

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContactData>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContactData>> = _emergencyContacts.asStateFlow()

    private val _communicationStatus = MutableStateFlow<CommunicationStatus>(CommunicationStatus.Disconnected)
    val communicationStatus: StateFlow<CommunicationStatus> = _communicationStatus.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    fun updateTimerState(data: ByteArray?) {
        try {
            if (data == null) {
                _timerState.value = TimerState(SafeWalkSession.Idle, 0)
                return
            }

            val jsonString = String(data, Charsets.UTF_8)
            val timerData = json.decodeFromString<TimerStateData>(jsonString)

            val session = if (timerData.isActive) {
                SafeWalkSession.Active(
                    startTime = timerData.startTime,
                    durationMinutes = timerData.durationMinutes,
                    remainingSeconds = timerData.remainingSeconds,
                    startLocation = timerData.startLocation,
                )
            } else {
                SafeWalkSession.Idle
            }

            _timerState.value = TimerState(session, timerData.remainingSeconds)
        } catch (e: Exception) {
            Log.e("WearRepository", "Failed to deserialize timer state", e)
            _timerState.value = TimerState(SafeWalkSession.Idle, 0)
        }
    }

    fun updateContacts(data: ByteArray?) {
        try {
            if (data == null) {
                _emergencyContacts.value = emptyList()
                return
            }

            val jsonString = String(data, Charsets.UTF_8)
            val contactsData = json.decodeFromString<List<EmergencyContactData>>(jsonString)
            _emergencyContacts.value = contactsData
        } catch (e: Exception) {
            Log.e("WearRepository", "Failed to deserialize contacts", e)
            _emergencyContacts.value = emptyList()
        }
    }

    fun updateConnectedDevice(deviceId: String, deviceName: String) {
        _connectedDevice.value = ConnectedDevice(deviceId, deviceName)
        _communicationStatus.value = CommunicationStatus.Connected
    }

    fun setDisconnected() {
        _connectedDevice.value = null
        _communicationStatus.value = CommunicationStatus.Disconnected
    }

    fun setCommunicationError(error: String) {
        _communicationStatus.value = CommunicationStatus.Error(error)
    }

    fun updateSessionState(isActive: Boolean, remaining: Int) {
        val currentSession = _timerState.value.session
        if (isActive && currentSession is SafeWalkSession.Idle) {
            val newSession = SafeWalkSession.Active(
                startTime = System.currentTimeMillis(),
                durationMinutes = 30,
                remainingSeconds = remaining,
            )
            _timerState.value = TimerState(newSession, remaining)
        } else if (!isActive) {
            _timerState.value = TimerState(SafeWalkSession.Idle, 0)
        }
    }

    fun decrementTimer() {
        val current = _timerState.value
        if (current.session is SafeWalkSession.Active && current.remainingSeconds > 0) {
            _timerState.value = TimerState(
                current.session,
                current.remainingSeconds - 1,
            )
        }
    }
}

data class TimerState(
    val session: SafeWalkSession,
    val remainingSeconds: Int,
)

data class ConnectedDevice(
    val deviceId: String,
    val deviceName: String,
)

@kotlinx.serialization.Serializable
data class EmergencyContactData(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String = "",
    val isPrimary: Boolean = false,
)

@kotlinx.serialization.Serializable
data class TimerStateData(
    val isActive: Boolean,
    val startTime: Long,
    val durationMinutes: Int,
    val remainingSeconds: Int,
    val startLocation: String? = null,
)

sealed class CommunicationStatus {
    data object Connected : CommunicationStatus()
    data object Disconnected : CommunicationStatus()
    data class Error(val message: String) : CommunicationStatus()
}