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
import androidx.compose.foundation.shape.CircleShape
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
import com.wear.viewmodel.WearTimerViewModel

@Composable
fun WearTimerScreen(
    viewModel: WearTimerViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()
    val isActive = session is SafeWalkSession.Active

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Status indicator - centered above timer
        Box(
            modifier = Modifier
                .size(24.dp)
                .background(
                    color = Color(0xFF10B981),
                    shape = CircleShape,
                )
                .clickable { },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                Icons.Filled.Check,
                contentDescription = "Paired",
                modifier = Modifier.size(14.dp),
                tint = Color.White,
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Timer display - centered
        Text(
            text = formatTime(remainingSeconds),
            fontSize = computeTimerFontSize(),
            fontWeight = FontWeight.Bold,
            color = Color.White,
            letterSpacing = WearUIConstants.TIMER_LETTER_SPACING,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Action buttons - centered, stacked vertically
        Column(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .padding(horizontal = 16.dp),
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