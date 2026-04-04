package com.example.safewalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "devices")
data class WearableDeviceEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val deviceName: String,
    val bluetoothAddress: String,
    val isConnected: Boolean = false,
    val pairedAt: Long,
    val lastSeen: Long?
)
