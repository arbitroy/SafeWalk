package com.wear.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
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
import com.wear.data.SafeWalkSession
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
    val session by viewModel.session.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val timerFontSize = computeTimerFontSize()
    val isPaired = pairingState == WearPairingViewModel.PairingState.Paired
    val isActive = session is SafeWalkSession.Active

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Status indicator — centred, safe on a circular screen
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

        // Action buttons — 75 % width keeps them away from the circular edge
        Column(
            modifier = Modifier
                .fillMaxWidth(0.75f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isActive) {
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
            } else {
                Button(
                    onClick = { viewModel.requestStart() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WearUIConstants.BUTTON_HEIGHT),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF10B981),
                    ),
                ) {
                    Icon(
                        Icons.Filled.PlayArrow,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        "Start",
                        fontSize = WearUIConstants.BUTTON_FONT_SIZE,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                    )
                }
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

/**
 * Pairing menu — displayed when the user taps the status indicator.
 *
 * Uses verticalScroll so content never clips on small round screens.
 * Horizontal width capped at 78 % to stay inside the inscribed safe zone.
 */
@Composable
private fun WearPairingMenuScreen(
    viewModel: WearPairingViewModel,
) {
    val pairingState by viewModel.pairingState.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.78f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            when (pairingState) {
                WearPairingViewModel.PairingState.Unpaired -> UnpairedMenuState(viewModel)
                WearPairingViewModel.PairingState.Paired   -> PairedMenuState(viewModel)
            }
        }
    }
}

@Composable
private fun UnpairedMenuState(viewModel: WearPairingViewModel) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "NOT PAIRED",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
        )
        Text(
            "Open SafeWalk on\nyour phone",
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            color = Color(0xFFAAAAAA),
        )

        Spacer(Modifier.height(12.dp))

        OutlinedButton(
            onClick = { viewModel.togglePairingMenu() },
            modifier = Modifier
                .fillMaxWidth()
                .height(WearUIConstants.BUTTON_HEIGHT),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color.White,
            ),
        ) {
            Text("Back", fontSize = 11.sp)
        }
    }
}

@Composable
private fun PairedMenuState(viewModel: WearPairingViewModel) {
    // Keep the total height inside the inscribed safe zone of a round watch face.
    // The large icon was removed — the green dot on the main screen already signals
    // paired status before the user taps into this menu.
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            "PAIRED",
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF10B981),
        )
        Text(
            "Phone connected",
            fontSize = 11.sp,
            color = Color(0xFFAAAAAA),
        )

        Spacer(Modifier.height(4.dp))

        OutlinedButton(
            onClick = { viewModel.togglePairingMenu() },
            modifier = Modifier
                .fillMaxWidth()
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
                .fillMaxWidth()
                .height(WearUIConstants.BUTTON_HEIGHT),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFDC2626),
            ),
        ) {
            Text("Disconnect", fontSize = 11.sp)
        }
    }
}
