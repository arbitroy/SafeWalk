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
import com.example.safewalk.data.wearable.WearDataLayerManager
import com.example.safewalk.data.wearable.WearableEvent
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
    private val wearDataLayerManager: WearDataLayerManager,
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
        // Eagerly discover the paired watch so sends and receives work immediately,
        // without requiring the user to manually visit the Pairing screen first.
        viewModelScope.launch { wearDataLayerManager.ensureConnected() }
        startTimerUpdates()
        listenForWearableEvents()
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
            syncTimerToWatch(isActive = true, duration = duration, remaining = duration * 60)
        }
    }

    fun stopSafeWalk() {
        handleCheckIn(CheckInStatus.COMPLETED)
    }

    fun triggerSOS() {
        handleCheckIn(CheckInStatus.SOS)
    }

    private fun handleCheckIn(status: CheckInStatus) {
        val capturedSession = _session.value
        _session.value = SafeWalkSession.Idle
        _remainingSeconds.value = 0

        viewModelScope.launch {
            val duration = if (capturedSession is SafeWalkSession.Active) {
                capturedSession.durationMinutes
            } else {
                repository.getSettings().first().defaultDuration
            }

            repository.addCheckIn(CheckIn(status = status, duration = duration))

            // Notify watch that session ended
            syncTimerToWatch(isActive = false, duration = duration, remaining = 0)

            when (status) {
                CheckInStatus.COMPLETED -> _uiEvent.emit(DashboardEvent.CheckInSuccessful)
                CheckInStatus.MISSED -> _uiEvent.emit(DashboardEvent.NavigateToAlert(AlertType.MISSED))
                CheckInStatus.SOS -> _uiEvent.emit(DashboardEvent.NavigateToAlert(AlertType.SOS))
            }
        }
    }

    private fun startTimerUpdates() {
        viewModelScope.launch {
            var ticksSinceSync = 0
            while (true) {
                if (_session.value is SafeWalkSession.Active) {
                    delay(1000)
                    // Re-check after the delay — user may have checked in during the sleep,
                    // which resets _remainingSeconds to 0. Without this guard, 0 - 1 = -1
                    // fires a spurious MISSED alert.
                    if (_session.value !is SafeWalkSession.Active) {
                        ticksSinceSync = 0
                        continue
                    }
                    val newValue = _remainingSeconds.value - 1
                    ticksSinceSync++
                    when {
                        newValue <= 0 -> {
                            _remainingSeconds.value = 0
                            ticksSinceSync = 0
                            handleCheckIn(CheckInStatus.MISSED)
                        }
                        newValue == 300 -> {
                            _remainingSeconds.value = newValue
                            _uiEvent.emit(DashboardEvent.TimeWarning)
                            syncTimerToWatch(remaining = newValue)
                            ticksSinceSync = 0
                        }
                        ticksSinceSync >= 30 -> {
                            // Resync watch every 30 seconds to correct any drift
                            _remainingSeconds.value = newValue
                            syncTimerToWatch(remaining = newValue)
                            ticksSinceSync = 0
                        }
                        else -> _remainingSeconds.value = newValue
                    }
                } else {
                    ticksSinceSync = 0
                    delay(100)
                }
            }
        }
    }

    private fun listenForWearableEvents() {
        wearDataLayerManager.startListening(viewModelScope)
        viewModelScope.launch {
            wearDataLayerManager.wearableEvents.collect { event ->
                when (event) {
                    is WearableEvent.CheckInReceived -> when (event.status) {
                        "COMPLETED" -> stopSafeWalk()
                        "SOS" -> triggerSOS()
                    }
                    // Watch-initiated start: use the phone's own configured default duration,
                    // not whatever the watch hardcoded. This ensures the watch always
                    // reflects the duration the user set in Settings.
                    is WearableEvent.TimerStartRequest -> startSafeWalk(null)
                    is WearableEvent.TimerSyncRequest -> {
                        val s = _session.value
                        if (s is SafeWalkSession.Active) {
                            syncTimerToWatch(
                                isActive = true,
                                duration = s.durationMinutes,
                                remaining = _remainingSeconds.value,
                            )
                        }
                    }
                    else -> Unit
                }
            }
        }
    }

    private fun syncTimerToWatch(
        isActive: Boolean? = null,
        duration: Int? = null,
        remaining: Int? = null,
    ) {
        val s = _session.value
        val active = isActive ?: (s is SafeWalkSession.Active)
        val dur = duration ?: if (s is SafeWalkSession.Active) s.durationMinutes else 0
        val rem = remaining ?: _remainingSeconds.value
        viewModelScope.launch {
            wearDataLayerManager.sendTimerUpdate(
                isActive = active,
                durationMinutes = dur,
                remainingSeconds = rem,
            )
        }
    }
}

sealed class DashboardEvent {
    data object CheckInSuccessful : DashboardEvent()
    data object CheckInMissed : DashboardEvent()
    data object TimeWarning : DashboardEvent()
    data class NavigateToAlert(val alertType: AlertType) : DashboardEvent()
}
