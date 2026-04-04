package com.example.safewalk.data.model

data class User(
    val id: String,
    val email: String,
    val firstName: String,
    val lastName: String,
    val phone: String?,
    val createdAt: Long
)
