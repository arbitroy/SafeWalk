package com.example.safewalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * Check-in status enumeration
 */
enum class CheckInStatus {
    COMPLETED,  // User checked in successfully
    MISSED,     // Timer expired without check-in
    SOS,        // Emergency alert triggered
}




/**
 * Check-in session tracking
 */
@Entity(tableName = "check_ins")
@Serializable
data class CheckIn(
    @PrimaryKey val id: String = System.currentTimeMillis().toString(),
    val timestamp: String = Instant.now().toString(),
    val status: CheckInStatus = CheckInStatus.COMPLETED,
    val duration: Int = 30, // minutes
    val location: String? = null,
    val userId: String = "guest",
)