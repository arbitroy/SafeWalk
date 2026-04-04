package com.example.safewalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "alert_locations")
data class AlertLocationEntity(
    @PrimaryKey val id: String,
    val alertId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)
