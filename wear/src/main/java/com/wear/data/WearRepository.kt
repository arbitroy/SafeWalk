package com.wear.data

import android.util.Log
import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class WearRepository @Inject constructor() {
    private val _timerState = MutableStateFlow(TimerState(SafeWalkSession.Idle, 0))
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private val _emergencyContacts = MutableStateFlow<List<EmergencyContactData>>(emptyList())
    val emergencyContacts: StateFlow<List<EmergencyContactData>> = _emergencyContacts.asStateFlow()

    fun updateTimerState(data: TimerStateData) {
        Log.d("SW_WEAR_REPO", "updateTimerState: isActive=${data.isActive} remaining=${data.remainingSeconds}s")
        val session = if (data.isActive) {
            SafeWalkSession.Active(
                startTime = data.startTime,
                durationMinutes = data.durationMinutes,
                remainingSeconds = data.remainingSeconds,
                startLocation = data.startLocation,
            )
        } else {
            SafeWalkSession.Idle
        }
        _timerState.value = TimerState(session, data.remainingSeconds)
    }

    fun updateContacts(contacts: List<EmergencyContactData>) {
        _emergencyContacts.value = contacts
    }
}

data class TimerState(
    val session: SafeWalkSession,
    val remainingSeconds: Int,
)

data class TimerStateData(
    val isActive: Boolean,
    val startTime: Long,
    val durationMinutes: Int,
    val remainingSeconds: Int,
    val startLocation: String? = null,
)

data class EmergencyContactData(
    val id: String,
    val name: String,
    val phone: String,
    val relationship: String = "",
    val isPrimary: Boolean = false,
)
