package com.example.safewalk.data.model

import com.google.gson.annotations.SerializedName

@SerializedName("triggerAlertRequest")
data class TriggerAlertRequest(
    val alertType: String,
    val notes: String?
)
