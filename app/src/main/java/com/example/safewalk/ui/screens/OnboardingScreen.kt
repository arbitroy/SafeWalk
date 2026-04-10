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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController

@Composable
fun OnboardingScreen(
    navController: NavController,
) {
    val steps = listOf(
        OnboardingStep(
            title = "Stay Safe",
            description = "Set timed check-ins during walks, runs, or any solo activity",
            icon = Icons.Filled.Shield,
            color = Color(0xFF3B82F6),
        ),
        OnboardingStep(
            title = "Emergency Contacts",
            description = "Add trusted contacts who will be notified if you miss a check-in",
            icon = Icons.Filled.Person,
            color = Color(0xFFA855F7),
        ),
        OnboardingStep(
            title = "Quick Check-Ins",
            description = "One tap to let your contacts know you're safe",
            icon = Icons.Filled.Check,
            color = Color(0xFFF87171),
        ),
    )

    var currentStep by remember { mutableStateOf(0) }
    val step = steps[currentStep]

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Progress Indicators
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                repeat(steps.size) { index ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(
                                if (index == currentStep) step.color else Color.LightGray
                            ),
                    )
                }
            }

            Spacer(Modifier.height(48.dp))

            // Icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape)
                    .background(step.color),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    step.icon,
                    step.title,
                    modifier = Modifier.size(48.dp),
                    tint = Color.White,
                )
            }

            Spacer(Modifier.height(32.dp))

            // Content
            Text(
                step.title,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(16.dp))
            Text(
                step.description,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(48.dp))

            // Buttons
            Button(
                onClick = {
                    if (currentStep < steps.size - 1) {
                        currentStep++
                    } else {
                        navController.navigate("dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            ) {
                Text(
                    if (currentStep < steps.size - 1) "Next" else "Get Started",
                    fontSize = 16.sp,
                )
            }

            if (currentStep < steps.size - 1) {
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = {
                        navController.navigate("dashboard") {
                            popUpTo("onboarding") { inclusive = true }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                ) {
                    Text("Skip Tutorial")
                }
            }
        }
    }
}

data class OnboardingStep(
    val title: String,
    val description: String,
    val icon: ImageVector,
    val color: Color,
)
