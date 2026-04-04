package com.example.safewalk.data.model

import com.google.gson.annotations.SerializedName

@SerializedName("updateAlertRequest")
data class UpdateAlertRequest(
    val status: String,
    val resolvedAt: Long?
)
