package com.example.safewalk.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safewalk.ui.viewmodel.SettingsViewModel
import kotlin.math.roundToInt

// Duration options in minutes
private val DURATION_OPTIONS = listOf(5, 10, 15, 20, 30, 45, 60, 90, 120)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsState()
    val totalWalks by viewModel.totalWalks.collectAsState(initial = 0)
    val totalContacts by viewModel.totalContacts.collectAsState(initial = 0)

    // Index of the current duration in DURATION_OPTIONS (clamp to nearest if not found)
    val durationIndex = DURATION_OPTIONS.indexOf(settings.defaultDuration)
        .takeIf { it >= 0 }
        ?: DURATION_OPTIONS.indexOfFirst { it >= settings.defaultDuration }.coerceAtLeast(0)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {

            // ── Check-in Timer ────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Timer, null, modifier = Modifier.size(20.dp))
                            Text("Check-in Timer", fontWeight = FontWeight.SemiBold)
                        }

                        HorizontalDivider()

                        // Current duration display
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "Default duration",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp,
                            )
                            Text(
                                "${settings.defaultDuration} min",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }

                        // Stepper: − value +
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            FilledIconButton(
                                onClick = {
                                    if (durationIndex > 0) {
                                        viewModel.updateDefaultDuration(
                                            DURATION_OPTIONS[durationIndex - 1]
                                        )
                                    }
                                },
                                enabled = durationIndex > 0,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ) {
                                Icon(Icons.Filled.Remove, "Decrease duration")
                            }

                            Spacer(Modifier.width(24.dp))

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    "${settings.defaultDuration}",
                                    fontSize = 32.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                                Text("minutes", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            Spacer(Modifier.width(24.dp))

                            FilledIconButton(
                                onClick = {
                                    if (durationIndex < DURATION_OPTIONS.lastIndex) {
                                        viewModel.updateDefaultDuration(
                                            DURATION_OPTIONS[durationIndex + 1]
                                        )
                                    }
                                },
                                enabled = durationIndex < DURATION_OPTIONS.lastIndex,
                                colors = IconButtonDefaults.filledIconButtonColors(
                                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                ),
                            ) {
                                Icon(Icons.Filled.Add, "Increase duration")
                            }
                        }

                        // Slider for quick scrubbing
                        Slider(
                            value = durationIndex.toFloat(),
                            onValueChange = { raw ->
                                viewModel.updateDefaultDuration(
                                    DURATION_OPTIONS[raw.roundToInt().coerceIn(0, DURATION_OPTIONS.lastIndex)]
                                )
                            },
                            valueRange = 0f..(DURATION_OPTIONS.lastIndex.toFloat()),
                            steps = DURATION_OPTIONS.size - 2,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        // Range labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text("${DURATION_OPTIONS.first()} min", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${DURATION_OPTIONS.last()} min", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // ── Notifications ─────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.Notifications, null, modifier = Modifier.size(20.dp))
                            Text("Notifications", fontWeight = FontWeight.SemiBold)
                        }

                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Sound on timer expiry", fontSize = 14.sp)
                                Text(
                                    "Play alert sound when check-in is missed",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = settings.notificationSound,
                                onCheckedChange = { viewModel.toggleNotificationSound() },
                            )
                        }
                    }
                }
            }

            // ── Location ──────────────────────────────────────────────────────
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(20.dp))
                            Text("Location", fontWeight = FontWeight.SemiBold)
                        }

                        HorizontalDivider()
                        Spacer(Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Auto-start location on walk", fontSize = 14.sp)
                                Text(
                                    "Fetch GPS as soon as SafeWalk begins",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = settings.autoStartLocation,
                                onCheckedChange = { viewModel.toggleAutoLocation() },
                            )
                        }
                    }
                }
            }

            // ── Stats ─────────────────────────────────────────────────────────
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalWalks", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Total Walks", fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("$totalContacts", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Contacts", fontSize = 11.sp)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("${settings.defaultDuration}m", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text("Default Timer", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}
