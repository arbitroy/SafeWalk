package com.example.safewalk.communication

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BluetoothCommunicationManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    private val bluetoothAdapter = bluetoothManager.adapter ?: BluetoothAdapter.getDefaultAdapter()

    private var bluetoothSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null

    private val _communicationEvents = MutableSharedFlow<CommunicationEvent>()
    val communicationEvents: Flow<CommunicationEvent> = _communicationEvents.asSharedFlow()

    private val SAFEWALK_UUID = UUID.fromString("a1e96650-7a6f-11ec-90d6-0242ac120003")
    private val SAFEWALK_SERVICE_NAME = "SafeWalk"
    private val MESSAGE_DELIMITER = "\n".toByteArray()

    suspend fun connectToRemoteDevice(remoteDeviceAddress: String, localDeviceId: String, authToken: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val remoteDevice = bluetoothAdapter?.getRemoteDevice(remoteDeviceAddress) ?: return@withContext false

                bluetoothSocket = remoteDevice.createRfcommSocketToServiceRecord(SAFEWALK_UUID)
                bluetoothSocket?.connect()

                inputStream = bluetoothSocket?.inputStream
                outputStream = bluetoothSocket?.outputStream

                // Send authentication handshake
                val handshake = "AUTH:$localDeviceId:$authToken"
                sendMessage(handshake)

                _communicationEvents.emit(CommunicationEvent.Connected)
                startListening()

                true
            } catch (e: IOException) {
                Log.e("BluetoothComm", "Connection failed", e)
                _communicationEvents.emit(CommunicationEvent.ConnectionError(e.message ?: "Unknown error"))
                false
            }
        }
    }

    suspend fun sendMessage(message: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                outputStream?.write(message.toByteArray())
                outputStream?.write(MESSAGE_DELIMITER)
                outputStream?.flush()
                _communicationEvents.emit(CommunicationEvent.MessageSent(message))
                true
            } catch (e: IOException) {
                Log.e("BluetoothComm", "Send failed", e)
                _communicationEvents.emit(CommunicationEvent.SendError(e.message ?: "Unknown error"))
                false
            }
        }
    }

    private suspend fun startListening() {
        withContext(Dispatchers.IO) {
            try {
                val buffer = ByteArray(1024)
                var bytesRead: Int

                while (bluetoothSocket?.isConnected == true) {
                    bytesRead = inputStream?.read(buffer) ?: -1
                    if (bytesRead > 0) {
                        val message = String(buffer, 0, bytesRead).trim()
                        _communicationEvents.emit(CommunicationEvent.MessageReceived(message))
                    }
                }
            } catch (e: IOException) {
                if (bluetoothSocket?.isConnected == true) {
                    Log.e("BluetoothComm", "Listening error", e)
                    _communicationEvents.emit(CommunicationEvent.ConnectionError(e.message ?: "Connection lost"))
                }
            }
        }
    }

    suspend fun disconnect() {
        withContext(Dispatchers.IO) {
            try {
                inputStream?.close()
                outputStream?.close()
                bluetoothSocket?.close()
                _communicationEvents.emit(CommunicationEvent.Disconnected)
            } catch (e: IOException) {
                Log.e("BluetoothComm", "Disconnect failed", e)
            }
        }
    }

    fun isConnected(): Boolean = bluetoothSocket?.isConnected == true
}

sealed class CommunicationEvent {
    data object Connected : CommunicationEvent()
    data object Disconnected : CommunicationEvent()
    data class MessageReceived(val message: String) : CommunicationEvent()
    data class MessageSent(val message: String) : CommunicationEvent()
    data class ConnectionError(val error: String) : CommunicationEvent()
    data class SendError(val error: String) : CommunicationEvent()
}