package com.example.safewalk.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safewalk.data.model.AlertType
import com.example.safewalk.data.model.EmergencyContact
import com.example.safewalk.ui.viewmodel.AlertUiState
import com.example.safewalk.ui.viewmodel.AlertViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlertScreen(
    navController: NavController,
    viewModel: AlertViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val pendingCount = state.contacts.count { contact ->
        contact.id in state.selectedContactIds && contact.id !in state.alertedContactIds
    }
    val allAlerted = state.selectedContactIds.isNotEmpty() && pendingCount == 0

    val accentColor = if (state.alertType == AlertType.SOS) {
        MaterialTheme.colorScheme.error
    } else {
        Color(0xFFFF6F00)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Icon(
                            if (state.alertType == AlertType.SOS) Icons.Filled.Dangerous else Icons.Filled.Warning,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(24.dp),
                        )
                        Text(
                            "Emergency Alert",
                            fontWeight = FontWeight.Bold,
                            color = accentColor,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Filled.Close, "Close")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                ),
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            // Alert type banner
            item {
                AlertBanner(alertType = state.alertType, accentColor = accentColor)
            }

            // Location card
            item {
                LocationCard(state = state)
            }

            // SMS preview card
            item {
                MessagePreviewCard(state = state, viewModel = viewModel)
            }

            // Contacts header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text("Select contacts to alert", fontWeight = FontWeight.SemiBold)
                    if (state.contacts.isNotEmpty()) {
                        Text(
                            "${state.alertedContactIds.size}/${state.contacts.size} alerted",
                            fontSize = 12.sp,
                            color = if (allAlerted) Color(0xFF16A34A) else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (state.contacts.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text("No emergency contacts", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(
                                "Add emergency contacts to send alerts",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(Modifier.height(12.dp))
                            OutlinedButton(onClick = { navController.navigate("contacts") }) {
                                Text("Add Contacts")
                            }
                        }
                    }
                }
            } else {
                items(state.contacts) { contact ->
                    ContactAlertRow(
                        contact = contact,
                        isSelected = contact.id in state.selectedContactIds,
                        isAlerted = contact.id in state.alertedContactIds,
                        isAutoAlerted = contact.id in state.autoAlertedContactIds,
                        onToggle = { viewModel.toggleContact(contact.id) },
                        onSend = { viewModel.launchSmsForContact(context, contact) },
                    )
                }
            }

            // Bottom action buttons
            item {
                Spacer(Modifier.height(4.dp))
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (state.contacts.isNotEmpty()) {
                        Button(
                            onClick = { viewModel.sendToNextPending(context) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            enabled = pendingCount > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = accentColor,
                            ),
                        ) {
                            Icon(Icons.Filled.Send, null, Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                if (allAlerted) "All contacts alerted" else "Send to Next ($pendingCount remaining)",
                                fontSize = 16.sp,
                            )
                        }
                    }
                    TextButton(
                        onClick = { navController.popBackStack() },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Dismiss alert", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun AlertBanner(alertType: AlertType, accentColor: Color) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = accentColor.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(accentColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (alertType == AlertType.SOS) Icons.Filled.Dangerous else Icons.Filled.Timer,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column {
                Text(
                    if (alertType == AlertType.SOS) "SOS Triggered" else "Missed Check-in",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = accentColor,
                )
                Text(
                    if (alertType == AlertType.SOS) {
                        "Emergency SOS was activated"
                    } else {
                        "Your safety timer expired without a check-in"
                    },
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun LocationCard(state: AlertUiState) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(Icons.Filled.LocationOn, null, modifier = Modifier.size(18.dp))
                Text("Location", fontWeight = FontWeight.SemiBold)
            }
            HorizontalDivider()
            if (state.isLoadingLocation) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    Text("Getting current location...", fontSize = 13.sp)
                }
            } else if (state.location != null) {
                val loc = state.location
                if (loc.address.isNotEmpty()) {
                    Text(loc.address, fontWeight = FontWeight.Medium)
                }
                Text(
                    "GPS: ${loc.formattedGps}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    loc.googleMapsUrl,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                Text(
                    "Location unavailable — check permissions",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun MessagePreviewCard(state: AlertUiState, viewModel: AlertViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("SMS Message Preview", fontWeight = FontWeight.SemiBold)
                TextButton(onClick = { expanded = !expanded }) {
                    Text(if (expanded) "Hide" else "Show")
                }
            }
            AnimatedVisibility(visible = expanded) {
                Column {
                    HorizontalDivider()
                    Spacer(Modifier.height(8.dp))
                    Text(
                        viewModel.buildSmsMessage(),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ContactAlertRow(
    contact: EmergencyContact,
    isSelected: Boolean,
    isAlerted: Boolean,
    isAutoAlerted: Boolean,
    onToggle: () -> Unit,
    onSend: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                isAlerted -> Color(0xFF16A34A).copy(alpha = 0.1f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f)
                else -> MaterialTheme.colorScheme.surface
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onToggle() },
                enabled = !isAlerted,
            )
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    contact.name.first().toString(),
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(contact.name, fontWeight = FontWeight.Medium)
                    if (isAlerted) {
                        Icon(
                            Icons.Filled.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF16A34A),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    if (isAutoAlerted) {
                        Text(
                            "Auto-sent",
                            fontSize = 10.sp,
                            color = Color(0xFF16A34A),
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
                Text(
                    contact.phone,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (contact.relationship.isNotEmpty()) {
                    Text(
                        contact.relationship,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            IconButton(
                onClick = onSend,
                enabled = isSelected,
            ) {
                Icon(
                    if (isAlerted) Icons.Filled.CheckCircle else Icons.Filled.Send,
                    contentDescription = if (isAlerted) "Already alerted" else "Send SMS",
                    tint = when {
                        isAlerted -> Color(0xFF16A34A)
                        isSelected -> MaterialTheme.colorScheme.primary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
        }
    }
}
