package com.example.safewalk.data.model

data class UpdateAlertRequest(
    val status: String,
    val resolvedAt: Long?
)
