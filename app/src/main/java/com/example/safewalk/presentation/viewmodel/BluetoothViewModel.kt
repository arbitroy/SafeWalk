package com.example.safewalk.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.safewalk.data.service.BluetoothService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BluetoothViewModel @Inject constructor(
    private val bluetoothService: BluetoothService
) : ViewModel() {

    private val _connectedDevice = MutableStateFlow<String?>(null)
    val connectedDevice: StateFlow<String?> = _connectedDevice.asStateFlow()

    private val _availableDevices = MutableStateFlow<List<String>>(emptyList())
    val availableDevices: StateFlow<List<String>> = _availableDevices.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _signalStrength = MutableStateFlow(0)
    val signalStrength: StateFlow<Int> = _signalStrength.asStateFlow()

    init {
        _connectedDevice.value = bluetoothService.getConnectedDeviceAddress()
    }

    fun scanForDevices() {
        viewModelScope.launch {
            _isScanning.value = true
            try {
                bluetoothService.startDeviceScan().collect { device ->
                    _availableDevices.value += device.name ?: "Unknown Device"
                }
            } catch (e: Exception) {
                _isScanning.value = false
            }
        }
    }

    fun connectToDevice(deviceAddress: String) {
        viewModelScope.launch {
            // Implementation would find device by address and connect
            _connectedDevice.value = deviceAddress
        }
    }

    fun disconnectDevice() {
        viewModelScope.launch {
            try {
                bluetoothService.disconnectDevice()
                _connectedDevice.value = null
            } catch (e: Exception) {
                // Handle error
            }
        }
    }

    fun testDeviceFeedback() {
        viewModelScope.launch {
            try {
                bluetoothService.sendVibrationFeedback(longArrayOf(0, 100, 50, 100, 50, 200))
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
}
