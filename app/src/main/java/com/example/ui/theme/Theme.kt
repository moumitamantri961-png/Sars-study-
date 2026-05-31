package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val CommonDarkColorScheme = darkColorScheme(
    primary = EnglishPrimary,
    secondary = EnglishSecondary,
    tertiary = EnglishTertiary,
    background = Color(0xFF0F172A),
    surface = Color(0xFF1E293B),
    onBackground = Color(0xFFF8FAFC),
    onSurface = Color(0xFFF8FAFC)
)

private val CommonLightColorScheme = lightColorScheme(
    primary = EnglishPrimary,
    secondary = EnglishSecondary,
    tertiary = EnglishTertiary,
    background = EnglishBackground,
    surface = EnglishSurface,
    onBackground = EnglishOnBackground,
    onSurface = EnglishOnSurface,
    onPrimary = Color.White,
    onSecondary = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // We force our hand-crafted, high-contrast, modern blue/white theme
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) {
        CommonDarkColorScheme
    } else {
        CommonLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
