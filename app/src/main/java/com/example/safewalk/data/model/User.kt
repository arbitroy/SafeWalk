package com.example.safewalk.data.model

import kotlinx.serialization.Serializable
import java.time.Instant

/**
 * User account representation
 */
@Serializable
data class User(
    val id: String,
    val email: String,
    val name: String,
    val createdAt: String = Instant.now().toString(),
)