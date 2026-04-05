package com.example.safewalk.presentation.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safewalk.presentation.viewmodel.TimerViewModel

@Composable
fun TimerScreen(
    viewModel: TimerViewModel = hiltViewModel()
) {
    val timerState by viewModel.timerState.collectAsState()

    val backgroundColor by animateColorAsState(
        targetValue = when {
            timerState.remainingSeconds < 30 && timerState.isRunning -> Color(0xFFE53935)
            timerState.remainingSeconds < 60 && timerState.isRunning -> Color(0xFFFFA726)
            else -> Color(0xFFF8BBD0)
        },
        label = "backgroundColor"
    )

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.triggerPanicAlert() },
                modifier = Modifier
                    .padding(16.dp)
                    .size(60.dp),
                containerColor = Color(0xFFE53935),
                contentColor = Color.White
            ) {
                Text(
                    "SOS",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(backgroundColor)
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            // Status Text
            Text(
                text = if (timerState.isRunning) "Protected - Timer Active" else "Protected - Ready",
                fontSize = 16.sp,
                color = Color.DarkGray,
                modifier = Modifier.padding(top = 24.dp)
            )

            // Timer Ring
            Box(
                modifier = Modifier
                    .size(280.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = formatTime(timerState.remainingSeconds),
                        fontSize = 72.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEC407A)
                    )
                    Text(
                        text = "minutes remaining",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            }

            // Control Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                if (timerState.isRunning && !timerState.isPaused) {
                    Button(
                        onClick = { viewModel.pauseTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pause,
                            contentDescription = "Pause",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Pause")
                    }
                }

                if (timerState.isPaused) {
                    Button(
                        onClick = { viewModel.resumeTimer() },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Resume",
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Resume")
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                Button(
                    onClick = { viewModel.cancelTimer() },
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFE53935)
                    )
                ) {
                    Text("Cancel", color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

private fun formatTime(seconds: Int): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}