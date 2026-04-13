package com.wear.ui.screen

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.wear.viewmodel.WearPairingViewModel

@Composable
fun WearPairingScreen(
    viewModel: WearPairingViewModel = hiltViewModel(),
) {
    val pairingState by viewModel.pairingState.collectAsState()
    val showPairingMenu by viewModel.showPairingMenu.collectAsState()

    if (showPairingMenu) {
        WearPairingMenuScreen(viewModel)
    } else {
        WearTimerDisplay(viewModel, pairingState)
    }
}

@Composable
fun WearTimerDisplay(
    viewModel: WearPairingViewModel,
    pairingState: WearPairingViewModel.PairingState,
) {
    val session by viewModel.session.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Top: Pairing status + Settings button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                when (pairingState) {
                    WearPairingViewModel.PairingState.Paired -> "PAIRED"
                    WearPairingViewModel.PairingState.Unpaired -> "NO PAIR"
                    else -> "..."
                },
                fontSize = 10.sp,
                color = when (pairingState) {
                    WearPairingViewModel.PairingState.Paired -> Color.Green
                    else -> Color.Red
                },
            )

            OutlinedButton(
                onClick = { viewModel.togglePairingMenu() },
                modifier = Modifier
                    .size(width = 48.dp, height = 28.dp),
                colors = ButtonDefaults.outlinedButtonColors(),
            ) {
                Icon(Icons.Filled.Settings, "", modifier = Modifier.size(12.dp))
            }
        }

        // Middle: Timer
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = formatTime(remainingSeconds),
                fontSize = 56.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }

        // Bottom: Action buttons (Safe and SOS)
        Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            OutlinedButton(
                onClick = { viewModel.checkIn("COMPLETED") },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                ),
            ) {
                Text("OK", fontSize = 11.sp, fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = { viewModel.triggerSOS() },
                modifier = Modifier
                    .weight(1f)
                    .height(40.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                ),
            ) {
                Text("SOS", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
fun WearPairingMenuScreen(
    viewModel: WearPairingViewModel,
) {
    val pairingState by viewModel.pairingState.collectAsState()
    var userInputCode by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(10.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (pairingState) {
            WearPairingViewModel.PairingState.Unpaired -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("PAIR", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Enter code from phone",
                        fontSize = 9.sp,
                        textAlign = TextAlign.Center,
                        color = Color.White,
                    )

                    OutlinedTextField(
                        value = userInputCode,
                        onValueChange = { if (it.length <= 8) userInputCode = it.uppercase() },
                        label = { Text("Code", fontSize = 8.sp) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(45.dp),
                        singleLine = true,
                        textStyle = androidx.compose.material3.LocalTextStyle.current.copy(fontSize = 11.sp),
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.togglePairingMenu() },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Back", fontSize = 10.sp)
                        }
                        Button(
                            onClick = {
                                if (userInputCode.length == 8) {
                                    viewModel.confirmPairing(userInputCode)
                                    userInputCode = ""
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            enabled = userInputCode.length == 8,
                        ) {
                            Text("OK", fontSize = 10.sp)
                        }
                    }
                }
            }

            WearPairingViewModel.PairingState.Paired -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Icon(
                        Icons.Filled.Check,
                        "Paired",
                        modifier = Modifier.size(36.dp),
                        tint = Color.Green,
                    )
                    Text("PAIRED", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("Device synced", fontSize = 9.sp, color = Color.White)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        OutlinedButton(
                            onClick = { viewModel.togglePairingMenu() },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White,
                            ),
                        ) {
                            Text("Back", fontSize = 10.sp)
                        }
                        Button(
                            onClick = { viewModel.unpair() },
                            modifier = Modifier
                                .weight(1f)
                                .height(42.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFDC2626),
                            ),
                        ) {
                            Text("Remove", fontSize = 10.sp)
                        }
                    }
                }
            }

            WearPairingViewModel.PairingState.Pairing -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Pairing...", fontSize = 12.sp, color = Color.White)
                    Spacer(Modifier.height(4.dp))
                    OutlinedButton(
                        onClick = { viewModel.cancelPairing() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color.White,
                        ),
                    ) {
                        Text("Cancel", fontSize = 10.sp)
                    }
                }
            }

            WearPairingViewModel.PairingState.Error -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Error", fontSize = 12.sp, color = Color(0xFFDC2626))
                    Text("Pairing failed", fontSize = 9.sp, color = Color.White)
                    Button(
                        onClick = { viewModel.togglePairingMenu() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(42.dp),
                    ) {
                        Text("Back", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}

