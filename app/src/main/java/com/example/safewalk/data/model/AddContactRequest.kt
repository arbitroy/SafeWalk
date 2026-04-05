package com.example.safewalk.data.model

data class AddContactRequest(
    val name: String,
    val phone: String,
    val email: String,
    val notificationPreference: String
)
