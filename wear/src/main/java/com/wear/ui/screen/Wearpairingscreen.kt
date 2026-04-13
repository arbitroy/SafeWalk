package com.wear.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
        WearMainScreen(viewModel, pairingState)
    }
}

@Composable
private fun WearMainScreen(
    viewModel: WearPairingViewModel,
    pairingState: WearPairingViewModel.PairingState,
) {
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val timerFontSize = computeTimerFontSize()
    val isPaired = pairingState == WearPairingViewModel.PairingState.Paired

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Status indicator
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = if (isPaired) Color(0xFF10B981) else Color(0xFFDC2626),
                    shape = CircleShape,
                )
                .clickable { viewModel.togglePairingMenu() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPaired) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = if (isPaired) "Paired" else "Not Paired",
                modifier = Modifier.size(14.dp),
                tint = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer display
        Text(
            text = formatTime(remainingSeconds),
            fontSize = timerFontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = WearUIConstants.TIMER_LETTER_SPACING,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons
        Column(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            OutlinedButton(
                onClick = { viewModel.checkIn("COMPLETED") },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WearUIConstants.BUTTON_HEIGHT),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                ),
            ) {
                Text(
                    "OK",
                    fontSize = WearUIConstants.BUTTON_FONT_SIZE,
                    fontWeight = FontWeight.Bold,
                )
            }

            Button(
                onClick = { viewModel.triggerSOS() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WearUIConstants.BUTTON_HEIGHT),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                ),
            ) {
                Text(
                    "SOS",
                    fontSize = WearUIConstants.BUTTON_FONT_SIZE,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                )
            }
        }
    }
}

@Composable
private fun WearPairingMenuScreen(
    viewModel: WearPairingViewModel,
) {
    val pairingState by viewModel.pairingState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (pairingState) {
            WearPairingViewModel.PairingState.Unpaired -> {
                UnpairedMenuState(viewModel)
            }

            WearPairingViewModel.PairingState.Paired -> {
                PairedMenuState(viewModel)
            }
        }
    }
}

@Composable
private fun UnpairedMenuState(viewModel: WearPairingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "NOT PAIRED",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text(
            "Waiting for phone",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = Color.White,
        )

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WearUIConstants.BUTTON_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(WearUIConstants.BUTTON_GAP),
        ) {
            OutlinedButton(
                onClick = { viewModel.togglePairingMenu() },
                modifier = Modifier
                    .weight(1f)
                    .height(WearUIConstants.BUTTON_HEIGHT),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                ),
            ) {
                Text("Back", fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun PairedMenuState(viewModel: WearPairingViewModel) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Icon(
            Icons.Filled.Check,
            "Paired",
            modifier = Modifier.size(40.dp),
            tint = Color(0xFF10B981),
        )
        Text(
            "PAIRED",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
        Text("Device synced", fontSize = 11.sp, color = Color.White)

        Spacer(Modifier.height(12.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(WearUIConstants.BUTTON_HEIGHT),
            horizontalArrangement = Arrangement.spacedBy(WearUIConstants.BUTTON_GAP),
        ) {
            OutlinedButton(
                onClick = { viewModel.togglePairingMenu() },
                modifier = Modifier
                    .weight(1f)
                    .height(WearUIConstants.BUTTON_HEIGHT),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White,
                ),
            ) {
                Text("Back", fontSize = 11.sp)
            }
            Button(
                onClick = { viewModel.unpair() },
                modifier = Modifier
                    .weight(1f)
                    .height(WearUIConstants.BUTTON_HEIGHT),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFDC2626),
                ),
            ) {
                Text("Remove", fontSize = 11.sp)
            }
        }
    }
}