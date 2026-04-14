package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.User
import com.example.safewalk.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
) : ViewModel() {

    val currentUser: StateFlow<User?> = repository.getCurrentUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isGuest: StateFlow<Boolean> = repository.isGuestUser()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    private val _editState = MutableStateFlow(ProfileEditState())
    val editState: StateFlow<ProfileEditState> = _editState.asStateFlow()

    private val _saveEvent = MutableSharedFlow<SaveResult>()
    val saveEvent = _saveEvent.asSharedFlow()

    init {
        // Populate editable fields from the persisted user + profile
        viewModelScope.launch {
            combine(
                repository.getCurrentUser(),
                repository.getUserProfile(),
            ) { user, profile ->
                ProfileEditState(
                    name = user?.name ?: "",
                    emergencyMessage = profile?.emergencyMessage ?: "",
                    medicalInfo = profile?.medicalInfo ?: "",
                    shareLocationAlways = profile?.shareLocationAlways ?: false,
                )
            }.collect { state ->
                _editState.value = state
            }
        }
    }

    fun onNameChange(value: String) {
        _editState.value = _editState.value.copy(name = value)
    }

    fun onEmergencyMessageChange(value: String) {
        _editState.value = _editState.value.copy(emergencyMessage = value)
    }

    fun onMedicalInfoChange(value: String) {
        _editState.value = _editState.value.copy(medicalInfo = value)
    }

    fun onLocationSharingChange(enabled: Boolean) {
        _editState.value = _editState.value.copy(shareLocationAlways = enabled)
    }

    fun saveProfile() {
        viewModelScope.launch {
            val user = repository.getCurrentUser().first()
            if (user == null) {
                _saveEvent.emit(SaveResult.Error("No user session found"))
                return@launch
            }

            val state = _editState.value

            // Update name on the User record if it changed
            if (state.name.isNotBlank() && state.name != user.name) {
                val updatedUser = user.copy(name = state.name.trim())
                repository.updateUser(updatedUser)
            }

            // Save the full profile to DataStore
            repository.saveProfile(
                UserProfile(
                    userId = user.id,
                    shareLocationAlways = state.shareLocationAlways,
                    emergencyMessage = state.emergencyMessage,
                    medicalInfo = state.medicalInfo,
                )
            )

            _saveEvent.emit(SaveResult.Success)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}

data class ProfileEditState(
    val name: String = "",
    val emergencyMessage: String = "",
    val medicalInfo: String = "",
    val shareLocationAlways: Boolean = false,
)

sealed class SaveResult {
    data object Success : SaveResult()
    data class Error(val message: String) : SaveResult()
}
