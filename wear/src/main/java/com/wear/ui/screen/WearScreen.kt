package com.wear.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wear.viewmodel.WearTimerViewModel

@Composable
fun WearTimerScreen(
    viewModel: WearTimerViewModel = hiltViewModel(),
) {
    val session by viewModel.session.collectAsState()
    val remainingSeconds by viewModel.remainingSeconds.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(8.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Timer display (huge, watch-sized)
        Text(
            text = formatTime(remainingSeconds),
            fontSize = 60.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )

        Spacer(Modifier.height(16.dp))

        // Only 2 buttons on watch (no space for more)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { viewModel.checkIn("COMPLETED") }) {
                Text("Safe")
            }
            Button(
                onClick = { viewModel.triggerSOS() },
                colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
            ) {
                Text("SOS")
            }
        }
    }
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}
