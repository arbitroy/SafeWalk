package com.example.safewalk.data.model

data class Alert(
    val id: String,
    val userId: String,
    val alertType: AlertType,
    val status: AlertStatus,
    val triggeredAt: Long,
    val resolvedAt: Long?,
    val notes: String?
)
