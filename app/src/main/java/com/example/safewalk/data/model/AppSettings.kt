package com.example.safewalk.data.model

import kotlinx.serialization.Serializable

/**
 * App settings
 */
@Serializable
data class AppSettings(
    val defaultDuration: Int = 30,
    val notificationSound: Boolean = true,
    val autoStartLocation: Boolean = false,
)
