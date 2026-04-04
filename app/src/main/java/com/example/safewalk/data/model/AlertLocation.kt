package com.example.safewalk.data.model

data class AlertLocation(
    val id: String,
    val alertId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)
