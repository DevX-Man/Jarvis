package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = CyberBlack,
    primaryContainer = CyberSurfaceVariant,
    onPrimaryContainer = JarvisHoloLight,
    secondary = JarvisBlue,
    onSecondary = Color.White,
    secondaryContainer = CyberBorder,
    onSecondaryContainer = JarvisHoloLight,
    tertiary = JarvisArcGlow,
    onTertiary = CyberBlack,
    background = CyberBlack,
    onBackground = JarvisHoloLight,
    surface = CyberDark,
    onSurface = JarvisHoloLight,
    surfaceVariant = CyberSurface,
    onSurfaceVariant = Color(0xFF90CAF9),
    outline = CyberBorder,
    outlineVariant = Color(0xFF132742),
    error = StatusError,
    onError = Color.White
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Force dark sci-fi HUD theme
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = JarvisColorScheme,
        typography = Typography,
        content = content
    )
}
