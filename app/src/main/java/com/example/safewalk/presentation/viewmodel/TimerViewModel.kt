package com.example.safewalk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.model.AlertType
import com.example.safewalk.data.model.TimerState
import com.example.safewalk.data.service.BluetoothService
import com.example.safewalk.data.service.LocationManager
import com.example.safewalk.domain.repository.AlertRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TimerViewModel @Inject constructor(
    private val alertRepository: AlertRepository,
    private val locationManager: LocationManager,
    private val bluetoothService: BluetoothService
) : ViewModel() {

    private val _timerState = MutableStateFlow(TimerState.Idle)
    val timerState: StateFlow<TimerState> = _timerState.asStateFlow()

    private var timerJob: Job? = null

    fun startTimer(durationSeconds: Int) {
        _timerState.value = TimerState(
            isRunning = true,
            totalSeconds = durationSeconds,
            remainingSeconds = durationSeconds
        )
        startCountdown(durationSeconds)
    }

    fun pauseTimer() {
        timerJob?.cancel()
        _timerState.value = _timerState.value.copy(isPaused = true, isRunning = false)
    }

    fun resumeTimer() {
        if (_timerState.value.isPaused) {
            _timerState.value = _timerState.value.copy(isPaused = false, isRunning = true)
            startCountdown(_timerState.value.remainingSeconds)
        }
    }

    fun cancelTimer() {
        timerJob?.cancel()
        _timerState.value = TimerState.Idle
    }

    fun triggerPanicAlert() {
        viewModelScope.launch {
            try {
                locationManager.getCurrentLocation().onSuccess { location ->
                    alertRepository.triggerAlert(AlertType.MANUAL, "Panic alert triggered")
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    private fun startCountdown(seconds: Int) {
        timerJob?.cancel()
        timerJob = viewModelScope.launch {
            var remaining = seconds
            while (remaining > 0) {
                delay(1000)
                remaining--
                _timerState.value = _timerState.value.copy(remainingSeconds = remaining)
            }
            handleTimerExpired()
        }
    }

    private fun handleTimerExpired() {
        viewModelScope.launch {
            bluetoothService.sendVibrationFeedback(longArrayOf(0, 200, 100, 200))
            _timerState.value = _timerState.value.copy(isRunning = false)
            // Trigger auto-alert after timeout
            alertRepository.triggerAlert(AlertType.AUTO, "Timer expired - auto alert")
        }
    }
}
