package com.example.safewalk.data.model

data class AlertUiState(
    val currentAlert: Alert? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val alertSent: Boolean = false
)
