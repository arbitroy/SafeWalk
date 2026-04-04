package com.example.safewalk.data.model

import com.google.gson.annotations.SerializedName

@SerializedName("addContactRequest")
data class AddContactRequest(
    val name: String,
    val phone: String,
    val email: String,
    val notificationPreference: String
)
