package com.wear.data

sealed class SafeWalkSession {
    data object Idle : SafeWalkSession()

    data class Active(
        val startTime: Long,
        val durationMinutes: Int,
        val remainingSeconds: Int,
        val startLocation: String? = null,
    ) : SafeWalkSession()
}
