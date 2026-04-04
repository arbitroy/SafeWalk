package com.example.safewalk.data.model

import com.google.gson.annotations.SerializedName

@SerializedName("registerRequest")
data class RegisterRequest(
    val email: String,
    val password: String,
    val firstName: String,
    val lastName: String,
    val phone: String?
)
