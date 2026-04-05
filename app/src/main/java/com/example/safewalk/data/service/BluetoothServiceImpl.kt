package com.example.safewalk.data.service

import android.Manifest
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothServiceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : BluetoothService {

    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter
    private val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
        vibratorManager.defaultVibrator
    } else {
        context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    }

    private val _alertTriggers = MutableSharedFlow<WearableAlertEvent>()
    private var connectedDeviceAddress: String? = null

    override fun startDeviceScan(): Flow<BluetoothDevice> {
        return MutableSharedFlow()
    }

    override suspend fun connectToDevice(device: BluetoothDevice): Result<Boolean> {
        return try {
            connectedDeviceAddress = device.address
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun disconnectDevice(): Result<Unit> {
        return try {
            connectedDeviceAddress = null
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun listenForAlertTriggers(): Flow<WearableAlertEvent> {
        return _alertTriggers.asSharedFlow()
    }

    override suspend fun sendVibrationFeedback(pattern: LongArray): Result<Unit> {
        return try {
            if (ContextCompat.checkSelfPermission(
                    context, Manifest.permission.VIBRATE
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                val effect = VibrationEffect.createWaveform(pattern, -1)
                vibrator.vibrate(effect)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun isDeviceConnected(): Boolean = connectedDeviceAddress != null

    override fun getConnectedDeviceAddress(): String? = connectedDeviceAddress
}