package com.example.safewalk.data.model

import kotlinx.serialization.Serializable

/**
 * User profile with safety preferences
 */
@Serializable
data class UserProfile(
    val userId: String,
    val shareLocationAlways: Boolean = false,
    val emergencyMessage: String = "",
    val medicalInfo: String = "",
)
