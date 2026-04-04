package com.example.safewalk.data.model

data class TimerState(
    val isRunning: Boolean = false,
    val totalSeconds: Int = 600,
    val remainingSeconds: Int = 600,
    val isPaused: Boolean = false
) {
    companion object {
        val Idle = TimerState()
    }
}
