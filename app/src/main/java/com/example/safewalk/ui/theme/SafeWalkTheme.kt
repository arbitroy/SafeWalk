package com.example.safewalk.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Define custom colors matching React app's gradient palette
private val Blue600 = Color(0xFF2563EB)
private val Purple600 = Color(0xFF7C3AED)
private val Green500 = Color(0xFF10B981)
private val Red500 = Color(0xFFEF4444)

private val LightColorScheme = lightColorScheme(
    primary = Blue600,
    secondary = Purple600,
    tertiary = Green500,
    error = Red500,
)

private val DarkColorScheme = darkColorScheme(
    primary = Blue600,
    secondary = Purple600,
    tertiary = Green500,
    error = Red500,
)

@Composable
fun SafeWalkTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}