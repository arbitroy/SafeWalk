package com.example.safewalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

@Entity(tableName = "emergency_contacts")
@Serializable
data class EmergencyContact(
    @PrimaryKey val id: String = System.currentTimeMillis().toString(),
    val name: String,
    val phone: String,
    val relationship: String = "",
    val isPrimary: Boolean = false,
)