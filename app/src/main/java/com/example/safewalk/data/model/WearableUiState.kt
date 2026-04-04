package com.example.safewalk.data.model

data class WearableUiState(
    val connectedDevice: String? = null,
    val isScanning: Boolean = false,
    val availableDevices: List<String> = emptyList(),
    val signalStrength: Int = 0,
    val isBatteryLow: Boolean = false
)
