package com.reverb.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF355C7D),
    onPrimary = Color(0xFFF7F4EF),
    secondary = Color(0xFFDD6E42),
    onSecondary = Color(0xFFFDF7F2),
    tertiary = Color(0xFF6C8E7B),
    background = Color(0xFFF5F1EA),
    onBackground = Color(0xFF1A1F24),
    surface = Color(0xFFFFFBF6),
    onSurface = Color(0xFF1A1F24),
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF90B4D8),
    onPrimary = Color(0xFF102132),
    secondary = Color(0xFFF2A074),
    onSecondary = Color(0xFF35170A),
    tertiary = Color(0xFF9DD0B7),
    background = Color(0xFF101417),
    onBackground = Color(0xFFF4EEE7),
    surface = Color(0xFF172026),
    onSurface = Color(0xFFF4EEE7),
)

@Composable
fun ReverbTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
