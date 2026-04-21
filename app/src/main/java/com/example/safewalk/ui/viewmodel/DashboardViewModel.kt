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
import com.example.safewalk.data.firebase.PhoneFirebaseSyncManager
import com.example.safewalk.data.firebase.WearableEvent
import android.util.Log
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
    private val syncManager: PhoneFirebaseSyncManager,
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
        Log.d("SW_PHONE_VM", "DashboardViewModel init")
        startTimerUpdates()
        listenForWearableEvents()
    }

    fun startSafeWalk(durationMinutes: Int? = null) {
        Log.d("SW_PHONE_VM", "startSafeWalk(durationMinutes=$durationMinutes) called")
        viewModelScope.launch {
            val duration = durationMinutes
                ?: repository.getSettings().first().defaultDuration
            Log.d("SW_PHONE_VM", "Resolved duration=${duration}min — creating Active session")
            _session.value = SafeWalkSession.Active(
                durationMinutes = duration,
                remainingSeconds = duration * 60,
            )
            _remainingSeconds.value = duration * 60
            Log.d("SW_PHONE_VM", "Session set to Active, syncing to watch now")
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
        Log.d("SW_PHONE_VM", "listenForWearableEvents() — subscribing to wearable events")
        viewModelScope.launch {
            syncManager.wearableEvents.collect { event ->
                Log.d("SW_PHONE_VM", "WearableEvent received: ${event::class.simpleName}")
                when (event) {
                    is WearableEvent.CheckInReceived -> {
                        Log.d("SW_PHONE_VM", "CheckInReceived status='${event.status}'")
                        when (event.status) {
                            "COMPLETED" -> stopSafeWalk()
                            "SOS"       -> triggerSOS()
                            else        -> Log.w("SW_PHONE_VM", "Unknown check-in status: ${event.status}")
                        }
                    }
                    is WearableEvent.TimerStartRequest -> {
                        Log.d("SW_PHONE_VM", "TimerStartRequest from watch — session=${_session.value::class.simpleName}")
                        if (_session.value !is SafeWalkSession.Active) startSafeWalk(null)
                    }
                    else -> Log.d("SW_PHONE_VM", "Unhandled event: ${event::class.simpleName}")
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
        Log.d("SW_PHONE_VM", "syncTimerToWatch: active=$active  duration=${dur}min  remaining=${rem}s")
        viewModelScope.launch {
            syncManager.sendTimerUpdate(
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
