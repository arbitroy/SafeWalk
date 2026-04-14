package com.example.safewalk.ui.viewmodel

import android.util.Patterns
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.User
import com.example.safewalk.data.model.UserProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.security.MessageDigest
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
) : ViewModel() {

    private val _authState = MutableStateFlow<AuthState>(AuthState.CheckingSession)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<AuthEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        // Restore session from DataStore on startup
        viewModelScope.launch {
            val user = repository.getCurrentUser().first()
            _authState.value = if (user != null) {
                AuthState.Authenticated(user)
            } else {
                AuthState.Idle
            }
        }
    }

    fun login(email: String, password: String) {
        if (email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("Email and password are required")
            return
        }
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val user = repository.findUserByEmail(email.trim())
            if (user != null && user.passwordHash == hashPassword(password)) {
                repository.setCurrentUser(user)
                _authState.value = AuthState.Authenticated(user)
                _uiEvent.emit(AuthEvent.LoginSuccess)
            } else {
                _authState.value = AuthState.Error("Invalid email or password")
                _uiEvent.emit(AuthEvent.LoginFailed)
            }
        }
    }

    fun signup(name: String, email: String, password: String, confirmPassword: String) {
        if (name.isBlank() || email.isBlank() || password.isBlank()) {
            _authState.value = AuthState.Error("All fields are required")
            return
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()) {
            _authState.value = AuthState.Error("Please enter a valid email address")
            return
        }
        if (password.length < 6) {
            _authState.value = AuthState.Error("Password must be at least 6 characters")
            return
        }
        if (password != confirmPassword) {
            _authState.value = AuthState.Error("Passwords do not match")
            return
        }

        _authState.value = AuthState.Loading
        viewModelScope.launch {
            val existing = repository.findUserByEmail(email.trim())
            if (existing != null) {
                _authState.value = AuthState.Error("An account with this email already exists")
                return@launch
            }

            val newUser = User(
                id = System.currentTimeMillis().toString(),
                email = email.trim(),
                name = name.trim(),
                passwordHash = hashPassword(password),
            )

            repository.addUser(newUser)
            repository.setCurrentUser(newUser)
            // Create a default profile record for the new user
            repository.saveProfile(UserProfile(userId = newUser.id))

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

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            _authState.value = AuthState.Idle
            _uiEvent.emit(AuthEvent.LoggedOut)
        }
    }

    private fun hashPassword(password: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val bytes = digest.digest(password.toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

sealed class AuthState {
    data object CheckingSession : AuthState()
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
    data object LoggedOut : AuthEvent()
}
