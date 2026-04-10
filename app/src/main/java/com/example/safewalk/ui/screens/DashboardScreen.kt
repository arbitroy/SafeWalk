package com.example.safewalk.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Start
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safewalk.data.model.SafeWalkSession
import com.example.safewalk.ui.viewmodel.DashboardViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    navController: NavController,
    viewModel: DashboardViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState(initial = null)
    val contacts by viewModel.contacts.collectAsState(initial = emptyList())
    val completedCount by viewModel.completedCount.collectAsState(initial = 0)
    val totalCount by viewModel.totalCount.collectAsState(initial = 0)

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel.uiEvent) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is DashboardEvent.CheckInSuccessful -> {
                    snackbarHostState.showSnackbar(
                        "Check-in successful! You're safe.",
                        duration = SnackbarDuration.Short,
                    )
                }
                is DashboardEvent.CheckInMissed -> {
                    snackbarHostState.showSnackbar(
                        "Missed check-in! Contacts may be notified.",
                        duration = SnackbarDuration.Long,
                    )
                }
                is DashboardEvent.SosTriggered -> {
                    snackbarHostState.showSnackbar(
                        "SOS Alert sent!",
                        duration = SnackbarDuration.Long,
                    )
                }
                is DashboardEvent.TimeWarning -> {
                    snackbarHostState.showSnackbar(
                        "5 minutes remaining! Check in soon.",
                        duration = SnackbarDuration.Short,
                    )
                }

                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(Icons.Filled.Shield, "SafeWalk", Modifier.size(24.dp))
                        Column {
                            Text("SafeWalk", fontWeight = FontWeight.Bold)
                            Text(
                                "Good ${if (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) < 12) "Morning" else if (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY) < 18) "Afternoon" else "Evening"}, ${currentUser?.name?.split(" ")?.get(0) ?: "User"}",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("profile") }) {
                        Icon(Icons.Filled.Person, "Profile")
                    }
                    IconButton(onClick = { navController.navigate("history") }) {
                        Icon(Icons.Filled.History, "History")
                    }
                    IconButton(onClick = { navController.navigate("settings") }) {
                        Icon(Icons.Filled.Settings, "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // Status Card
            item {
                StatusCard(
                    isActive = session is SafeWalkSession.Active,
                    remainingSeconds = remainingSeconds,
                    onStart = { viewModel.startSafeWalk() },
                    onStop = { viewModel.stopSafeWalk() },
                    onCheckIn = { viewModel.stopSafeWalk() },
                )
            }

            // Quick Actions
            if (session is SafeWalkSession.Idle) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { navController.navigate("contacts") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(Icons.Filled.Phone, "Contacts", Modifier.size(24.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("Contacts", fontWeight = FontWeight.Bold)
                                Text("${contacts.size} saved", fontSize = 12.sp)
                            }
                        }

                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .clickable { navController.navigate("history") },
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            ),
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Icon(Icons.Filled.History, "History", Modifier.size(24.dp))
                                Spacer(Modifier.height(8.dp))
                                Text("History", fontWeight = FontWeight.Bold)
                                Text("View all", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // SOS Button
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                    ),
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Button(
                            onClick = { viewModel.triggerSOS() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                            ),
                        ) {
                            Icon(Icons.Filled.Dangerous, "SOS", Modifier.size(24.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Emergency SOS", fontSize = 16.sp)
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "⚡ Instantly alerts all emergency contacts",
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            // Contacts Preview
            item {
                Card {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Icon(Icons.Filled.Phone, "Contacts")
                                Text("Emergency Contacts", fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = { navController.navigate("contacts") },
                                modifier = Modifier.height(32.dp),
                            ) {
                                Text("Manage")
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        if (contacts.isEmpty()) {
                            Text("No emergency contacts added yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        } else {
                            contacts.take(3).forEach { contact ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            contact.name.first().toString(),
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            fontWeight = FontWeight.Bold,
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(contact.name, fontWeight = FontWeight.Bold)
                                        Text(contact.phone, fontSize = 12.sp)
                                    }
                                    if (contact.isPrimary) {
                                        Icon(Icons.Filled.Star, "Primary", tint = Color.Yellow)
                                    }
                                }
                            }
                            if (contacts.size > 3) {
                                Text("+${contacts.size - 3} more contacts", fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // Stats
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StatCard("$completedCount", "Safe")
                    StatCard("$totalCount", "Total")
                    StatCard("${contacts.size}", "Contacts")
                }
            }
        }
    }
}

@Composable
fun RowScope.StatCard(value: String, label: String) {
    Card(
        modifier = Modifier.weight(1f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            Text(label, fontSize = 12.sp)
        }
    }
}

@Composable
private fun StatusCard(
    isActive: Boolean,
    remainingSeconds: Int,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onCheckIn: () -> Unit,
) {
    Card {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Shield Icon
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (isActive)
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF10B981), Color(0xFF059669)),
                            )
                        else
                            Brush.linearGradient(
                                colors = listOf(Color(0xFFE5E7EB), Color(0xFFD1D5DB)),
                            ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    Icons.Filled.Shield,
                    "Status",
                    modifier = Modifier.size(60.dp),
                    tint = if (isActive) Color.White else Color.Gray,
                )
            }

            Spacer(Modifier.height(16.dp))

            Text(
                if (isActive) "SafeWalk Active" else "Ready to Start",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
            )

            Text(
                if (isActive) "Check in within ${formatTime(remainingSeconds)}" else "Begin a timed safety check-in session",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            if (isActive) {
                Spacer(Modifier.height(16.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    formatTime(remainingSeconds),
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Spacer(Modifier.height(24.dp))

            if (isActive) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedButton(
                        onClick = onStop,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.Pause, "Stop", Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Stop")
                    }
                    Button(
                        onClick = onCheckIn,
                        modifier = Modifier.weight(1f),
                    ) {
                        Icon(Icons.Filled.CheckCircle, "Safe", Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("I'm Safe")
                    }
                }
            } else {
                Button(
                    onClick = onStart,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                ) {
                    Icon(Icons.Filled.Start, "Start", Modifier.size(24.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Start SafeWalk", fontSize = 16.sp)
                }
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}

// Placeholder events - import from viewmodel in real app
sealed class DashboardEvent {
    data object CheckInSuccessful : DashboardEvent()
    data object CheckInMissed : DashboardEvent()
    data object SosTriggered : DashboardEvent()
    data object TimeWarning : DashboardEvent()
}
