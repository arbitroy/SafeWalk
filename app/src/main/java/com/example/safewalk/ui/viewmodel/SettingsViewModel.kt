package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.AppSettings
import com.example.safewalk.data.model.EmergencyContact
import com.example.safewalk.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
) : ViewModel() {

    val settings: Flow<AppSettings> = repository.getSettings()
        .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())

    val totalWalks: Flow<Int> = repository.getTotalCheckInCount()

    val totalContacts: Flow<Int> = repository.getAllContacts()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
        .stateIn(viewModelScope, SharingStarted.Lazily, 0)
        .run {
            repository.getAllContacts()
                .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())
                .run {
                    val allContacts: Flow<List<EmergencyContact>> = repository.getAllContacts()
                    flow {
                        allContacts.collect { contacts ->
                            emit(contacts.size)
                        }
                    }
                }
        }

    fun updateDefaultDuration(duration: Int) {
        viewModelScope.launch {
            val current = repository.getSettings()
                .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())
                .value
            repository.saveSettings(current.copy(defaultDuration = duration))
        }
    }

    fun toggleNotificationSound() {
        viewModelScope.launch {
            val current = repository.getSettings()
                .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())
                .value
            repository.saveSettings(current.copy(notificationSound = !current.notificationSound))
        }
    }

    fun toggleAutoLocation() {
        viewModelScope.launch {
            val current = repository.getSettings()
                .stateIn(viewModelScope, SharingStarted.Lazily, AppSettings())
                .value
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