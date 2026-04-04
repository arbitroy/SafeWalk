package com.example.safewalk.data.model

import com.google.gson.annotations.SerializedName

@SerializedName("recordLocationRequest")
data class RecordLocationRequest(
    val alertId: String,
    val latitude: Double,
    val longitude: Double,
    val accuracy: Float,
    val timestamp: Long
)
