package com.example.safewalk.ui.screens


import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dangerous
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.example.safewalk.data.model.CheckIn
import com.example.safewalk.data.model.CheckInStatus
import com.example.safewalk.ui.viewmodel.HistoryViewModel
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    navController: NavController,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val history by viewModel.history.collectAsState(initial = emptyList())
    val completedCount by viewModel.completedCount.collectAsState(initial = 0)
    val totalCount by viewModel.totalCount.collectAsState(initial = 0)

    val successRate = if (totalCount > 0) (completedCount * 100) / totalCount else 0

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Check-in History") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (totalCount > 0) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = Color(0xFF10B981),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column {
                                Text("Success Rate", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                                Text("$successRate%", fontSize = 32.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                Spacer(Modifier.height(8.dp))
                                Text("$completedCount successful check-ins out of $totalCount", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                            }
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text("🏆", fontSize = 32.sp)
                            }
                        }
                    }
                }

                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        StatCard("$completedCount", "Safe")
                        StatCard("${totalCount - completedCount}", "Missed")
                        StatCard("0", "SOS")
                    }
                }
            }

            items(history) { checkIn ->
                CheckInListItem(checkIn)
            }
        }
    }
}

@Composable
private fun CheckInListItem(checkIn: CheckIn) {
    Card {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                when (checkIn.status) {
                    CheckInStatus.COMPLETED -> Icons.Filled.CheckCircle
                    CheckInStatus.MISSED -> Icons.Filled.Close
                    CheckInStatus.SOS -> Icons.Filled.Dangerous
                },
                contentDescription = null,
                tint = when (checkIn.status) {
                    CheckInStatus.COMPLETED -> Color(0xFF10B981)
                    CheckInStatus.MISSED -> Color(0xFFEA580C)
                    CheckInStatus.SOS -> Color(0xFFDC2626)
                },
            )

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        when (checkIn.status) {
                            CheckInStatus.COMPLETED -> "✓ Safe"
                            CheckInStatus.MISSED -> "⏰ Missed"
                            CheckInStatus.SOS -> "⚠️ SOS"
                        },
                        fontSize = 12.sp,
                    )
                    Text(formatRelativeDate(checkIn.timestamp), fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(4.dp))
                Text("${parseTimestamp(checkIn.timestamp).second} • ${checkIn.duration} min session", fontSize = 12.sp)
                if (checkIn.location != null) {
                    Text("📍 ${checkIn.location}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

private fun formatRelativeDate(timestamp: String): String {
    return try {
        val instant = Instant.parse(timestamp)
        val now = Instant.now()
        val diffMillis = now.toEpochMilli() - instant.toEpochMilli()
        val diffHours = diffMillis / (1000 * 60 * 60)
        val diffDays = diffHours / 24

        when {
            diffHours < 1 -> "Just now"
            diffHours < 24 -> "${diffHours}h ago"
            diffDays < 7 -> "${diffDays}d ago"
            else -> instant.atZone(ZoneId.systemDefault()).toLocalDate().toString()
        }
    } catch (e: Exception) {
        "Recently"
    }
}

private fun parseTimestamp(timestamp: String): Pair<String, String> {
    return try {
        val instant = Instant.parse(timestamp)
        val ldt = LocalDateTime.ofInstant(instant, ZoneId.systemDefault())
        val time = "%02d:%02d".format(ldt.hour, ldt.minute)
        val date = ldt.toLocalDate().toString()
        date to time
    } catch (e: Exception) {
        "" to ""
    }
}
