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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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

    when {
        pairingState == WearPairingViewModel.PairingState.Unpaired -> CodeEntryScreen(viewModel)
        showPairingMenu -> WearPairingMenuScreen(viewModel)
        else -> WearMainScreen(viewModel)
    }
}

@Composable
private fun CodeEntryScreen(viewModel: WearPairingViewModel) {
    var code by remember { mutableStateOf("") }
    val pairingError by viewModel.pairingError.collectAsState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                "SAFEWALK",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF10B981),
            )
            Text(
                "Enter code from\nthe phone app",
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                color = Color(0xFFAAAAAA),
            )

            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) code = it.filter { c -> c.isDigit() } },
                placeholder = { Text("123456", color = Color(0xFF555555), fontSize = 20.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Number,
                    imeAction = ImeAction.Done,
                ),
                keyboardActions = KeyboardActions(
                    onDone = { if (code.length == 6) viewModel.pairWithCode(code) }
                ),
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    letterSpacing = 4.sp,
                ),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF10B981),
                    unfocusedBorderColor = Color(0xFF444444),
                    cursorColor = Color(0xFF10B981),
                ),
            )

            if (pairingError != null) {
                Text(
                    pairingError!!,
                    fontSize = 9.sp,
                    color = Color(0xFFDC2626),
                    textAlign = TextAlign.Center,
                )
            }

            Button(
                onClick = { if (code.length == 6) viewModel.pairWithCode(code) },
                enabled = code.length == 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WearUIConstants.BUTTON_HEIGHT),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981),
                    disabledContainerColor = Color(0xFF1A3D2E),
                ),
            ) {
                Text("Connect", fontSize = WearUIConstants.BUTTON_FONT_SIZE, color = Color.White)
            }
        }
    }
}

@Composable
private fun WearMainScreen(viewModel: WearPairingViewModel) {
    val session by viewModel.session.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val timerFontSize = computeTimerFontSize()
    val isActive = session is SafeWalkSession.Active

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(color = Color(0xFF10B981), shape = CircleShape)
                .clickable { viewModel.togglePairingMenu() },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Connected",
                modifier = Modifier.size(14.dp),
                tint = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = formatTime(remainingSeconds),
            fontSize = timerFontSize,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = WearUIConstants.TIMER_LETTER_SPACING,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        Column(
            modifier = Modifier.fillMaxWidth(0.75f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            if (isActive) {
                OutlinedButton(
                    onClick = { viewModel.checkIn("COMPLETED") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WearUIConstants.BUTTON_HEIGHT),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                ) {
                    Text("OK", fontSize = WearUIConstants.BUTTON_FONT_SIZE, fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = { viewModel.requestStart() },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(WearUIConstants.BUTTON_HEIGHT),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Text("Start", fontSize = WearUIConstants.BUTTON_FONT_SIZE, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }

            Button(
                onClick = { viewModel.triggerSOS() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WearUIConstants.BUTTON_HEIGHT),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            ) {
                Text("SOS", fontSize = WearUIConstants.BUTTON_FONT_SIZE, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

@Composable
private fun WearPairingMenuScreen(viewModel: WearPairingViewModel) {
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text("CONNECTED", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
            Text("Phone synced via Firebase", fontSize = 10.sp, color = Color(0xFFAAAAAA))

            Spacer(Modifier.height(4.dp))

            OutlinedButton(
                onClick = { viewModel.togglePairingMenu() },
                modifier = Modifier.fillMaxWidth().height(WearUIConstants.BUTTON_HEIGHT),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
            ) {
                Text("Back", fontSize = 11.sp)
            }
            Button(
                onClick = { viewModel.unpair() },
                modifier = Modifier.fillMaxWidth().height(WearUIConstants.BUTTON_HEIGHT),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            ) {
                Text("Disconnect", fontSize = 11.sp)
            }
        }
    }
}
