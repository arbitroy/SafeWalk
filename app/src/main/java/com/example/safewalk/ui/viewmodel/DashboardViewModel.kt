package com.example.safewalk.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.local.SafeWalkRepository
import com.example.safewalk.data.model.AlertType
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

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

    fun startSafeWalk(durationMinutes: Int? = null) {
        viewModelScope.launch {
            val duration = durationMinutes
                ?: repository.getSettings().first().defaultDuration
            _session.value = SafeWalkSession.Active(
                durationMinutes = duration,
                remainingSeconds = duration * 60,
            )
            _remainingSeconds.value = duration * 60
        }
    }

    fun stopSafeWalk() {
        handleCheckIn(CheckInStatus.COMPLETED)
    }

    fun triggerSOS() {
        // Capture session before resetting so duration is preserved in the DB record
        handleCheckIn(CheckInStatus.SOS)
    }

    private fun handleCheckIn(status: CheckInStatus) {
        // Capture session state before resetting
        val capturedSession = _session.value
        _session.value = SafeWalkSession.Idle
        _remainingSeconds.value = 0

        viewModelScope.launch {
            val duration = if (capturedSession is SafeWalkSession.Active) {
                capturedSession.durationMinutes
            } else {
                repository.getSettings().first().defaultDuration
            }

            repository.addCheckIn(
                CheckIn(
                    status = status,
                    duration = duration,
                )
            )

            when (status) {
                CheckInStatus.COMPLETED -> _uiEvent.emit(DashboardEvent.CheckInSuccessful)
                CheckInStatus.MISSED -> _uiEvent.emit(DashboardEvent.NavigateToAlert(AlertType.MISSED))
                CheckInStatus.SOS -> _uiEvent.emit(DashboardEvent.NavigateToAlert(AlertType.SOS))
            }
        }
    }

    private fun startTimerUpdates() {
        viewModelScope.launch {
            while (true) {
                if (_session.value is SafeWalkSession.Active) {
                    delay(1000)
                    val newValue = _remainingSeconds.value - 1
                    when {
                        newValue <= 0 -> {
                            _remainingSeconds.value = 0
                            handleCheckIn(CheckInStatus.MISSED)
                        }
                        newValue == 300 -> {
                            _remainingSeconds.value = newValue
                            _uiEvent.emit(DashboardEvent.TimeWarning)
                        }
                        else -> _remainingSeconds.value = newValue
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
    data object TimeWarning : DashboardEvent()
    data class NavigateToAlert(val alertType: AlertType) : DashboardEvent()
}
