package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.AppSettings
import com.example.safewalk.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val totalWalks: Flow<Int> = repository.getTotalCheckInCount()

    val totalContacts: Flow<Int> = repository.getAllContacts().map { it.size }

    fun updateDefaultDuration(duration: Int) {
        viewModelScope.launch {
            val current = repository.getSettings().first()
            repository.saveSettings(current.copy(defaultDuration = duration))
        }
    }

    fun toggleNotificationSound() {
        viewModelScope.launch {
            val current = repository.getSettings().first()
            repository.saveSettings(current.copy(notificationSound = !current.notificationSound))
        }
    }

    fun toggleAutoLocation() {
        viewModelScope.launch {
            val current = repository.getSettings().first()
            repository.saveSettings(current.copy(autoStartLocation = !current.autoStartLocation))
        }
    }

    fun resetAllData(currentUser: User?) {
        viewModelScope.launch {
            repository.clearCheckInHistory()
            repository.clearContacts()
            if (currentUser != null) {
                repository.setCurrentUser(currentUser)
            }
        }
    }
}
