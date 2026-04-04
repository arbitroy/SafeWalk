package com.example.safewalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alerts")
data class AlertEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val alertType: String,
    val status: String,
    val triggeredAt: Long,
    val resolvedAt: Long?,
    val notes: String?
)
