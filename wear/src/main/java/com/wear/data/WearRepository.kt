package com.wear.data

import jakarta.inject.Inject
import jakarta.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

@Singleton
class WearRepository @Inject constructor() {
    private val _timerState = MutableStateFlow(TimerState(SafeWalkSession.Idle, 0))
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    fun updateTimerState(data: ByteArray?) {
        // TODO: Implement actual deserialization from ByteArray
    }

    fun updateContacts(data: ByteArray?) {
        // TODO: Implement actual deserialization from ByteArray
    }
}

data class TimerState(
    val session: SafeWalkSession,
    val remainingSeconds: Int
)
