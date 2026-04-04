package com.example.safewalk.data.model

import com.google.gson.annotations.SerializedName

@SerializedName("loginRequest")
data class LoginRequest(
    val email: String,
    val password: String
)
