package com.example.safewalk.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val SafeWalkLightColorScheme = lightColorScheme(
    primary = PrimaryPink,
    onPrimary = Color.White,
    primaryContainer = AccentPink,
    onPrimaryContainer = TextDark,

    secondary = SecondaryBlue,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3F2FD),
    onSecondaryContainer = TextDark,

    tertiary = TertiaryPurple,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFF3E5F5),
    onTertiaryContainer = TextDark,

    error = DangerRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFEBEE),
    onErrorContainer = DangerRed,

    background = BackgroundLight,
    onBackground = TextDark,

    surface = SurfaceWhite,
    onSurface = TextDark,
    surfaceVariant = Color(0xFFEEEEEE),
    onSurfaceVariant = TextGray,

    outline = TextLight
)

@Composable
fun SafeWalkTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SafeWalkLightColorScheme,
        typography = SafeWalkTypography,
        content = content
    )
}