package com.example.safewalk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor() : ViewModel() {

    private val _timerDuration = MutableStateFlow(600) // 10 minutes in seconds
    val timerDuration: StateFlow<Int> = _timerDuration.asStateFlow()

    private val _enableSMS = MutableStateFlow(true)
    val enableSMS: StateFlow<Boolean> = _enableSMS.asStateFlow()

    private val _enableNotifications = MutableStateFlow(true)
    val enableNotifications: StateFlow<Boolean> = _enableNotifications.asStateFlow()

    fun setTimerDuration(seconds: Int) {
        _timerDuration.value = seconds
    }

    fun toggleSMS(enabled: Boolean) {
        _enableSMS.value = enabled
    }

    fun toggleNotifications(enabled: Boolean) {
        _enableNotifications.value = enabled
    }
}
