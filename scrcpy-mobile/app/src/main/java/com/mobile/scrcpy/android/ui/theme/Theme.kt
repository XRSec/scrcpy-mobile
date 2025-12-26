package com.mobile.scrcpy.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF4A90E2),
    secondary = Color(0xFFE85D75),
    background = Color(0xFFF5F5F5),
    surface = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4A90E2),
    secondary = Color(0xFFE85D75),
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E)
)

@Composable
fun ScreenRemoteTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
