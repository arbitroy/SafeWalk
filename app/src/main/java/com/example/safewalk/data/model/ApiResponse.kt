package com.example.safewalk.data.model

/**
 * Network response wrapper for future API integration
 */
data class ApiResponse<T>(
    val success: Boolean,
    val data: T? = null,
    val error: String? = null,
)