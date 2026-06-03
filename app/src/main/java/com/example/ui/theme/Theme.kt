package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = RadiantCyan,
    secondary = SafetyGreen,
    tertiary = TextLightGrey,
    background = MidnightNavy,
    surface = StealthSlate,
    error = AlarmCrimson,
    onPrimary = Color(0xFF00315B),
    onSecondary = Color(0xFF00315B),
    onBackground = SoftLavender,
    onSurface = SoftLavender,
    onError = Color.White
)

// Simple fallback light scheme representing a "clean tech chalk" mode
private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007A8A),
    secondary = Color(0xFF008953),
    tertiary = Color(0xFF5F518A),
    background = Color(0xFFF2F4F7),
    surface = Color.White,
    error = Color(0xFFBA1A1A),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFF191C20),
    onSurface = Color(0xFF191C20),
    onError = Color.White
)

@Composable
fun CyberHideoutTheme(
    darkTheme: Boolean = true, // Force dark theme by default to preserve theater immersion
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
