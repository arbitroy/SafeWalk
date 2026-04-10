package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.CheckIn
import com.example.safewalk.data.model.CheckInStatus
import com.example.safewalk.data.model.EmergencyContact
import com.example.safewalk.data.model.SafeWalkSession
import com.example.safewalk.data.model.User
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// ============================================================================
// Dashboard ViewModel
// ============================================================================

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val repository: SafeWalkRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<SafeWalkSession>(SafeWalkSession.Idle)
    val session: StateFlow<SafeWalkSession> = _session.asStateFlow()

    private val _remainingSeconds = MutableStateFlow(0)
    val remainingSeconds: StateFlow<Int> = _remainingSeconds.asStateFlow()

    val currentUser: Flow<User?> = repository.getCurrentUser()

    val contacts: Flow<List<EmergencyContact>> = repository.getAllContacts()

    val completedCount: Flow<Int> = repository.getCompletedCheckInCount()

    val totalCount: Flow<Int> = repository.getTotalCheckInCount()

    private val _uiEvent = MutableSharedFlow<DashboardEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        startTimerUpdates()
    }

    fun startSafeWalk(durationMinutes: Int = 30) {
        _session.value = SafeWalkSession.Active(
            durationMinutes = durationMinutes,
            remainingSeconds = durationMinutes * 60,
        )
        _remainingSeconds.value = durationMinutes * 60
    }

    fun stopSafeWalk() {
        handleCheckIn(CheckInStatus.COMPLETED)
    }

    fun triggerSOS() {
        handleCheckIn(CheckInStatus.SOS)
        viewModelScope.launch {
            _uiEvent.emit(DashboardEvent.SosTriggered)
        }
    }

    private fun handleCheckIn(status: CheckInStatus) {
        _session.value = SafeWalkSession.Idle
        _remainingSeconds.value = 0

        viewModelScope.launch {
            val session = _session.value
            val duration = if (session is SafeWalkSession.Active) {
                session.durationMinutes
            } else {
                30
            }

            repository.addCheckIn(
                CheckIn(
                    status = status,
                    duration = duration,
                )
            )

            when (status) {
                CheckInStatus.COMPLETED -> {
                    _uiEvent.emit(DashboardEvent.CheckInSuccessful)
                }
                CheckInStatus.MISSED -> {
                    _uiEvent.emit(DashboardEvent.CheckInMissed)
                }
                CheckInStatus.SOS -> {
                    _uiEvent.emit(DashboardEvent.SosTriggered)
                }
            }
        }
    }

    private fun startTimerUpdates() {
        viewModelScope.launch {
            while (true) {
                if (_session.value is SafeWalkSession.Active) {
                    delay(1000)
                    _remainingSeconds.update { current ->
                        val newValue = current - 1
                        if (newValue <= 0) {
                            handleCheckIn(CheckInStatus.MISSED)
                            0
                        } else {
                            // 5-minute warning
                            if (newValue == 300) {
                                viewModelScope.launch {
                                    _uiEvent.emit(DashboardEvent.TimeWarning)
                                }
                            }
                            newValue
                        }
                    }
                } else {
                    delay(100)
                }
            }
        }
    }
}

sealed class DashboardEvent {
    data object CheckInSuccessful : DashboardEvent()
    data object CheckInMissed : DashboardEvent()
    data object SosTriggered : DashboardEvent()
    data object TimeWarning : DashboardEvent()
}