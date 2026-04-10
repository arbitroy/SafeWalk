package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AuthEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun login(email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            delay(800) // Simulate API call

            val user = repository.findUserByEmail(email)
            if (user != null) {
                repository.setCurrentUser(user)
                _authState.value = AuthState.Authenticated(user)
                _uiEvent.emit(AuthEvent.LoginSuccess)
            } else {
                _authState.value = AuthState.Error("Invalid email or password")
                _uiEvent.emit(AuthEvent.LoginFailed)
            }
        }
    }

    fun signup(name: String, email: String, password: String) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            // Validation
            if (password.length < 6) {
                _authState.value = AuthState.Error("Password must be at least 6 characters")
                return@launch
            }

            val existing = repository.findUserByEmail(email)
            if (existing != null) {
                _authState.value = AuthState.Error("Email already in use")
                return@launch
            }

            delay(800) // Simulate API call

            val newUser = User(
                id = System.currentTimeMillis().toString(),
                email = email,
                name = name,
            )

            repository.addUser(newUser)
            repository.setCurrentUser(newUser)
            _authState.value = AuthState.Authenticated(newUser)
            _uiEvent.emit(AuthEvent.SignupSuccess)
        }
    }

    fun loginAsGuest() {
        viewModelScope.launch {
            val guestUser = repository.createGuestUser()
            _authState.value = AuthState.Authenticated(guestUser)
            _uiEvent.emit(AuthEvent.GuestLoginSuccess)
        }
    }
}

sealed class AuthState {
    data object Idle : AuthState()
    data object Loading : AuthState()
    data class Authenticated(val user: User) : AuthState()
    data class Error(val message: String) : AuthState()
}

sealed class AuthEvent {
    data object LoginSuccess : AuthEvent()
    data object LoginFailed : AuthEvent()
    data object SignupSuccess : AuthEvent()
    data object GuestLoginSuccess : AuthEvent()
}
