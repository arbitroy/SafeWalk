package com.example.safewalk.data.model

import com.google.gson.annotations.SerializedName

@SerializedName("authResponse")
data class AuthResponse(
    val user: User,
    val token: String
)
