package com.example.safewalk.presentation.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safewalk.data.model.AlertStatus
import com.example.safewalk.navigation.NavigationScreen
import com.example.safewalk.presentation.viewmodel.AlertViewModel
import com.example.safewalk.presentation.viewmodel.BluetoothViewModel
import com.example.safewalk.presentation.viewmodel.ContactViewModel

@Composable
fun HomeScreen(
    navController: NavController,
    alertViewModel: AlertViewModel = hiltViewModel(),
    bluetoothViewModel: BluetoothViewModel = hiltViewModel(),
    contactViewModel: ContactViewModel = hiltViewModel()
) {
    val activeAlert by alertViewModel.activeAlert.collectAsState()
    val connectedDevice by bluetoothViewModel.connectedDevice.collectAsState()
    val contacts by contactViewModel.contacts.collectAsState()

    val statusColor by animateColorAsState(
        targetValue = when (activeAlert?.status) {
            AlertStatus.ACTIVE -> Color(0xFFE53935)
            AlertStatus.RESPONDING -> Color(0xFFFFA726)
            AlertStatus.RESOLVED -> Color(0xFF4CAF50)
            else -> Color(0xFF4CAF50)
        },
        label = "statusColor"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
            .verticalScroll(rememberScrollState())
    ) {
        // App Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = statusColor,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(
                        bottomStart = 24.dp,
                        bottomEnd = 24.dp
                    )
                )
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Safe Walk",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (activeAlert == null) "Protected & Ready" else "Alert Active",
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.9f)
                )
            }
        }

        // Status Cards
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Active Alert Status
            if (activeAlert != null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFEBEE)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "🚨 ALERT ACTIVE",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFE53935)
                            )
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = "Alert",
                                tint = Color(0xFFE53935),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Text(
                            text = "Type: ${activeAlert!!.alertType.name}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                        Text(
                            text = "Status: ${activeAlert!!.status.name}",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFE8F5E9)
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
                        Text(
                            text = "✓ No Active Alerts",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Safe",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            // Device Status Card
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
                            text = "Wearable Device",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = if (connectedDevice != null) "Connected: $connectedDevice" else "Not Connected",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = if (connectedDevice != null) Icons.Default.Bluetooth else Icons.Default.BluetoothDisabled,
                        contentDescription = "Bluetooth",
                        tint = if (connectedDevice != null) Color(0xFF4CAF50) else Color(0xFFE53935),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Contacts Status Card
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
                            text = "Emergency Contacts",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1A1A1A)
                        )
                        Text(
                            text = "${contacts.size} contact${if (contacts.size != 1) "s" else ""} configured",
                            fontSize = 12.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.Contacts,
                        contentDescription = "Contacts",
                        tint = Color(0xFFEC407A),
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Quick Action Buttons
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Quick Actions",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1A1A1A),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            QuickActionButton(
                icon = Icons.Default.Schedule,
                title = "Safety Timer",
                subtitle = "Set countdown with auto-alert",
                onClick = { navController.navigate(NavigationScreen.Timer.route) },
                color = Color(0xFFEC407A)
            )

            QuickActionButton(
                icon = Icons.Default.History,
                title = "Alert History",
                subtitle = "View past emergencies",
                onClick = { navController.navigate(NavigationScreen.AlertHistory.route) },
                color = Color(0xFFFFA726)
            )

            QuickActionButton(
                icon = Icons.Default.Contacts,
                title = "Emergency Contacts",
                subtitle = "Manage alert recipients",
                onClick = { navController.navigate(NavigationScreen.Contacts.route) },
                color = Color(0xFF4CAF50)
            )

            QuickActionButton(
                icon = Icons.Default.Bluetooth,
                title = "Wearable Setup",
                subtitle = "Pair & configure device",
                onClick = { navController.navigate(NavigationScreen.Wearable.route) },
                color = Color(0xFF2196F3)
            )

            QuickActionButton(
                icon = Icons.Default.Settings,
                title = "Settings",
                subtitle = "App preferences",
                onClick = {  navController.navigate(NavigationScreen.Settings.route) },
                color = Color(0xFF78909C)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
fun QuickActionButton(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    color: Color
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = androidx.compose.foundation.shape.CircleShape,
                color = color.copy(alpha = 0.1f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = icon,
                        contentDescription = title,
                        tint = color,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1A1A1A)
                )
                Text(
                    text = subtitle,
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = Color.Gray,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
