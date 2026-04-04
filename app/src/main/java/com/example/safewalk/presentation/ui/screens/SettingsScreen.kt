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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val timerDuration by viewModel.timerDuration.collectAsState()
    val enableSMS by viewModel.enableSMS.collectAsState()
    val enableNotifications by viewModel.enableNotifications.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        TopAppBar(
            title = { Text("Settings") },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFEC407A),
                titleContentColor = Color.White
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SettingCategory(title = "Timer Settings")
            }

            item {
                SettingCard(
                    title = "Default Timer Duration",
                    subtitle = "${timerDuration / 60} minutes"
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        listOf(300, 600, 900, 1200).forEach { duration ->
                            Button(
                                onClick = { viewModel.setTimerDuration(duration) },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (timerDuration == duration) Color(0xFFEC407A) else Color.LightGray
                                )
                            ) {
                                Text(
                                    text = "${duration / 60}m",
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            item {
                SettingCategory(title = "Notification Settings")
            }

            item {
                SettingToggle(
                    title = "SMS Notifications",
                    subtitle = "Send alerts via SMS",
                    isEnabled = enableSMS,
                    onToggle = { viewModel.toggleSMS(it) }
                )
            }

            item {
                SettingToggle(
                    title = "Push Notifications",
                    subtitle = "Send alerts via app notifications",
                    isEnabled = enableNotifications,
                    onToggle = { viewModel.toggleNotifications(it) }
                )
            }

            item {
                SettingCategory(title = "About")
            }

            item {
                SettingCard(
                    title = "App Version",
                    subtitle = "Safe Walk v1.0"
                )
            }

            item {
                SettingCard(
                    title = "Privacy Policy",
                    subtitle = "Tap to view"
                )
            }

            item {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun SettingCategory(title: String) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = Color(0xFFEC407A),
        modifier = Modifier.padding(top = 8.dp, bottom = 8.dp)
    )
}

@Composable
fun SettingCard(
    title: String,
    subtitle: String,
    content: (@Composable () -> Unit)? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A)
            )

            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = Color.Gray,
                modifier = Modifier.padding(top = 4.dp)
            )

            if (content != null) {
                content()
            }
        }
    }
}

@Composable
fun SettingToggle(
    title: String,
    subtitle: String,
    isEnabled: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )

                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Switch(
                checked = isEnabled,
                onCheckedChange = onToggle,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
    }
}
