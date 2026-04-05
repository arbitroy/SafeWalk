@file:OptIn(ExperimentalMaterial3Api::class)
package com.example.safewalk.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothDisabled
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safewalk.presentation.viewmodel.BluetoothViewModel
import com.example.safewalk.presentation.viewmodel.SettingsViewModel

// ============= WEARABLE SCREEN =============

@Composable
fun WearableScreen(
    viewModel: BluetoothViewModel = hiltViewModel()
) {
    val connectedDevice by viewModel.connectedDevice.collectAsState()
    val availableDevices by viewModel.availableDevices.collectAsState()
    val isScanning by viewModel.isScanning.collectAsState()
    val signalStrength by viewModel.signalStrength.collectAsState()

    var showDeviceList by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        TopAppBar(
            title = { Text("Wearable Device") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFEC407A),
                titleContentColor = Color.White
            )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Connection Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Icon(
                        imageVector = if (connectedDevice != null) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        contentDescription = "Bluetooth",
                        modifier = Modifier.size(48.dp),
                        tint = if (connectedDevice != null) Color(0xFF4CAF50) else Color.Gray
                    )

                    Text(
                        text = if (connectedDevice != null) "Connected" else "Not Connected",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (connectedDevice != null) Color(0xFF4CAF50) else Color(0xFFE53935)
                    )

                    if (connectedDevice != null) {
                        Text(
                            text = connectedDevice!!,
                            fontSize = 14.sp,
                            color = Color.Gray
                        )

                        // Signal Strength
                        Text(
                            text = "Signal: $signalStrength%",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }

            // Action Buttons
            Button(
                onClick = { viewModel.scanForDevices() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFEC407A)
                ),
                enabled = !isScanning
            ) {
                if (isScanning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text("${if (isScanning) "Scanning..." else "Scan for Devices"}")
            }

            if (connectedDevice != null) {
                Button(
                    onClick = { viewModel.testDeviceFeedback() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFFFA726)
                    )
                ) {
                    Text("Test Vibration")
                }

                Button(
                    onClick = { viewModel.disconnectDevice() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Disconnect")
                }
            }

            // Available Devices
            if (availableDevices.isNotEmpty()) {
                Text(
                    text = "Available Devices (${availableDevices.size})",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A),
                    modifier = Modifier.padding(top = 8.dp)
                )

                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(availableDevices.size) { index ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFFF5F5F5)
                            )
                        ) {
                            Text(
                                text = availableDevices[index],
                                fontSize = 14.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}