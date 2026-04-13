package com.wear.ui.screen

import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object WearUIConstants {
    val SAFE_ZONE_PADDING = 12.dp
    val SAFE_ZONE_PADDING_BOTTOM = 16.dp
    val BUTTON_HEIGHT = 40.dp
    val BUTTON_GAP = 8.dp
    val BUTTON_PADDING_HORIZONTAL = 12.dp
    val BUTTON_FONT_SIZE = 11.sp
    val STATUS_FONT_SIZE = 12.sp
    val SETTINGS_BUTTON_SIZE = 40.dp
    val STATUS_DOT_SIZE = 8.dp
    val STATUS_BAR_HEIGHT = 40.dp
    val TIMER_LETTER_SPACING = 1.sp
}

@androidx.compose.runtime.Composable
fun computeTimerFontSize(): androidx.compose.ui.unit.TextUnit {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return when {
        screenWidthDp < 240 -> 44.sp
        screenWidthDp < 280 -> 48.sp
        screenWidthDp < 300 -> 52.sp
        else -> 56.sp
    }
}

@androidx.compose.runtime.Composable
fun shouldStackButtonsVertically(): Boolean {
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    return screenWidthDp < 260
}

fun formatTime(seconds: Int): String {
    val mins = seconds / 60
    val secs = seconds % 60
    return "%d:%02d".format(mins, secs)
}