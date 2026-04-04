package com.example.safewalk.data.model

import java.time.Instant

data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val altitude: Double,
    val speed: Float,
    val timestamp: Long = Instant.now().toEpochMilli()
)
