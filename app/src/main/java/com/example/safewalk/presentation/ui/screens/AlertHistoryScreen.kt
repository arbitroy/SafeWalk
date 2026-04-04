package com.example.safewalk.presentation.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.safewalk.data.model.Alert
import com.example.safewalk.data.model.AlertStatus
import com.example.safewalk.data.model.AlertType
import com.example.safewalk.presentation.viewmodel.AlertViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun AlertHistoryScreen(
    viewModel: AlertViewModel = hiltViewModel(),
    onBack: () -> Unit = {},
    userId: String = ""
) {
    LaunchedEffect(Unit) {
        if (userId.isNotEmpty()) {
            viewModel.loadAlertHistory(userId)
        }
    }

    val alertHistory by viewModel.alertHistory.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFAFAFA))
    ) {
        // Header
        TopAppBar(
            title = { Text("Alert History") },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color(0xFFEC407A),
                titleContentColor = Color.White,
                navigationIconContentColor = Color.White
            )
        )

        // Alert List
        if (alertHistory.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No alerts yet",
                    fontSize = 16.sp,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(alertHistory) { alert ->
                    AlertHistoryCard(alert)
                }
            }
        }
    }
}

@Composable
fun AlertHistoryCard(alert: Alert) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(4.dp)
            .clickable { },
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = alert.alertType.name,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEC407A)
                    )
                    Text(
                        text = formatAlertTime(alert.triggeredAt),
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Badge(
                    status = alert.status,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }

            if (!alert.notes.isNullOrEmpty()) {
                Text(
                    text = alert.notes,
                    fontSize = 13.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            Text(
                text = "Duration: ${calculateDuration(alert.triggeredAt, alert.resolvedAt)}",
                fontSize = 11.sp,
                color = Color.LightGray,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun Badge(status: AlertStatus, modifier: Modifier = Modifier) {
    val (backgroundColor, textColor) = when (status) {
        AlertStatus.ACTIVE -> Color(0xFFE53935) to Color.White
        AlertStatus.RESPONDING -> Color(0xFFFFA726) to Color.White
        AlertStatus.RESOLVED -> Color(0xFF4CAF50) to Color.White
    }

    Surface(
        modifier = modifier
            .background(backgroundColor)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        color = backgroundColor
    ) {
        Text(
            text = status.name,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = textColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun formatAlertTime(millis: Long): String {
    val formatter = SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault())
    return formatter.format(Date(millis))
}

private fun calculateDuration(triggeredAt: Long, resolvedAt: Long?): String {
    return if (resolvedAt != null) {
        val durationMillis = resolvedAt - triggeredAt
        val seconds = durationMillis / 1000
        val minutes = seconds / 60
        val secs = seconds % 60
        String.format("%dm %ds", minutes, secs)
    } else {
        "Ongoing"
    }
}