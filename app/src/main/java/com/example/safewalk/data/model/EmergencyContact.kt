package com.example.safewalk.data.model

data class EmergencyContact(
    val id: String,
    val userId: String,
    val name: String,
    val phone: String,
    val email: String,
    val notificationPreference: NotificationPreference,
    val isActive: Boolean = true
)
