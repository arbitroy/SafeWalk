package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.AppSettings
import com.example.safewalk.data.model.EmergencyContact
import com.example.safewalk.data.model.User
import com.example.safewalk.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
) : ViewModel() {

    val currentUser: Flow<User?> = repository.getCurrentUser()

    val isGuest: Flow<Boolean> = repository.isGuestUser()

    val profile: Flow<UserProfile?> = repository.getUserProfile()

    fun saveProfile(profile: UserProfile) {
        viewModelScope.launch {
            repository.saveProfile(profile)
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }
}
