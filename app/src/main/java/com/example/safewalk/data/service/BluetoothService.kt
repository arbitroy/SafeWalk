package com.example.safewalk.data.service

import android.bluetooth.BluetoothDevice
import kotlinx.coroutines.flow.Flow
import java.util.UUID
import java.util.concurrent.Flow

data class WearableAlertEvent(
    val eventId: String = UUID.randomUUID().toString(),
    val eventType: String,
    val timestamp: Long = System.currentTimeMillis()
)

interface BluetoothService {
    fun startDeviceScan(): Flow<BluetoothDevice>
    suspend fun connectToDevice(device: BluetoothDevice): Result<Boolean>
    suspend fun disconnectDevice(): Result<Unit>
    fun listenForAlertTriggers(): Flow<WearableAlertEvent>
    suspend fun sendVibrationFeedback(pattern: LongArray): Result<Unit>
    fun isDeviceConnected(): Boolean
    fun getConnectedDeviceAddress(): String?
}
