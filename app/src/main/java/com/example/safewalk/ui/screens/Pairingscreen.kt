package com.example.safewalk.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Watch
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safewalk.pairing.PairingState
import com.example.safewalk.ui.viewmodel.PairingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PairingScreen(
    navController: NavController,
    viewModel: PairingViewModel = hiltViewModel(),
) {
    val pairingState by viewModel.pairingState.collectAsState()
    val pairedDevice by viewModel.pairedDevice.collectAsState(initial = null)
    val snackbarHostState = remember { SnackbarHostState() }
    var wearableCode by remember { mutableStateOf("") }

    LaunchedEffect(viewModel.pairingEvent) {
        viewModel.pairingEvent.collect { event ->
            when (event) {
                is PairingViewModel.PairingEvent.PairingSuccess -> {
                    snackbarHostState.showSnackbar("Device paired successfully!")
                }
                is PairingViewModel.PairingEvent.PairingFailed -> {
                    snackbarHostState.showSnackbar("Pairing failed: ${event.reason}")
                }
                is PairingViewModel.PairingEvent.CodeGenerated -> {
                    snackbarHostState.showSnackbar("Share this code with your wearable")
                }

                else -> {}
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Pair Wearable") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            verticalArrangement = Arrangement.Top,
        ) {
            when (pairingState) {
                PairingState.Unpaired -> {
                    UnpairedState(viewModel)
                }

                is PairingState.Pairing -> {
                    PairingState(
                        code = (pairingState as PairingState.Pairing).code,
                        wearableCode = wearableCode,
                        onCodeChange = { wearableCode = it },
                        onConfirm = { viewModel.confirmPairingCode(wearableCode) },
                        onCancel = { viewModel.cancelPairing() },
                    )
                }

                is PairingState.Paired -> {
                    PairedState(
                        device = (pairingState as PairingState.Paired).device,
                        onUnpair = { viewModel.unpair() },
                    )
                }

                is PairingState.Error -> {
                    ErrorState(
                        error = (pairingState as PairingState.Error).message,
                        onRetry = { viewModel.resetPairingState() },
                    )
                }
            }
        }
    }
}

@Composable
private fun UnpairedState(viewModel: PairingViewModel) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.Watch,
                    "Wearable",
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Ready to Pair",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Instructions
        Text(
            "Follow these steps to pair your wearable device:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )

        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StepItem("1", "Tap the button below to generate a pairing code")
                StepItem("2", "Note the 8-character code shown on this screen")
                StepItem("3", "On your wearable, tap Settings (gear icon)")
                StepItem("4", "Enter the code and confirm")
                StepItem("5", "Both devices will sync when paired")
            }
        }

        // Action button
        Button(
            onClick = { viewModel.generatePairingCode() },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
        ) {
            Text("Generate Pairing Code", fontSize = 16.sp)
        }
    }
}

@Composable
private fun PairingState(
    code: String,
    wearableCode: String,
    onCodeChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Display generated code
        Text(
            "Share this code with your wearable:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
            ),
        ) {
            SelectionContainer(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Pairing Code",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        code,
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        letterSpacing = 2.sp,
                    )
                }
            }
        }

        // Wearable code input
        Text(
            "Enter the code from your wearable to confirm:",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = wearableCode,
            onValueChange = { if (it.length <= 8) onCodeChange(it.uppercase()) },
            label = { Text("Wearable Code") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = androidx.compose.material3.LocalTextStyle.current.copy(
                fontSize = 16.sp,
            ),
        )

        // Instructions
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text(
                    "What to do on your wearable:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "1. Tap the Settings icon (gear)\n2. Enter the code shown above\n3. Tap OK to confirm\n4. The code will match this one",
                    fontSize = 12.sp,
                    lineHeight = 18.sp,
                )
            }
        }

        // Action buttons
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Cancel", fontSize = 14.sp)
            }
            Button(
                onClick = onConfirm,
                modifier = Modifier.weight(1f),
                enabled = wearableCode.length == 8,
            ) {
                Text("Confirm", fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun PairedState(
    device: com.example.safewalk.pairing.PairedDevice,
    onUnpair: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        // Success indicator
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(16.dp),
                )
                .padding(32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    Icons.Filled.Check,
                    "Paired",
                    modifier = Modifier.size(64.dp),
                    tint = Color(0xFF10B981),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Device Paired",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        // Device info
        Card {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text("Paired Device", fontWeight = FontWeight.Bold)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Name:", fontSize = 12.sp)
                    Text(device.remoteDeviceName, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Transport:", fontSize = 12.sp)
                    Text(device.transportType.name, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text("Paired:", fontSize = 12.sp)
                    Text(
                        java.text.SimpleDateFormat("MMM dd, yyyy").format(device.pairingTimestamp),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }
        }

        // Info message
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant,
            ),
        ) {
            Text(
                "Your devices are now synced. Timer, contacts, and check-ins will sync automatically.",
                modifier = Modifier.padding(16.dp),
                fontSize = 12.sp,
            )
        }

        // Unpair button
        OutlinedButton(
            onClick = onUnpair,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Unpair Device", fontSize = 14.sp)
        }
    }
}

@Composable
private fun ErrorState(
    error: String,
    onRetry: () -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
            ),
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
            ) {
                Text("Error", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(8.dp))
                Text(error, fontSize = 12.sp)
            }
        }

        Button(
            onClick = onRetry,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
        ) {
            Text("Try Again", fontSize = 14.sp)
        }
    }
}

@Composable
private fun StepItem(step: String, description: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(4.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                step,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
            )
        }
        Text(description, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
    }
}