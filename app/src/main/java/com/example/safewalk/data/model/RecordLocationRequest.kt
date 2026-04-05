package com.example.safewalk.data.model

data class RecordLocationRequest(
    val alertId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)
