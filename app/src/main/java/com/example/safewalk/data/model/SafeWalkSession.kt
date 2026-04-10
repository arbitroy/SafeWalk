package com.example.safewalk.data.model

/**
 * Active SafeWalk session state
 */
sealed class SafeWalkSession {
    data object Idle : SafeWalkSession()

    data class Active(
        val startTime: Long = System.currentTimeMillis(),
        val durationMinutes: Int = 30,
        val remainingSeconds: Int = durationMinutes * 60,
        val startLocation: String? = null,
    ) : SafeWalkSession()
}