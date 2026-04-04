package com.example.safewalk.data.model

data class EmergencyNotificationPayload(
    val userId: String,
    val alertId: String,
    val latitude: Double,
    val longitude: Double,
    val contactName: String,
    val timestamp: Long
)
