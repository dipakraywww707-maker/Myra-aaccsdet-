package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = MyraCyan,
    onPrimary = Color(0xFF00363D),
    primaryContainer = Color(0xFF004E57),
    onPrimaryContainer = Color(0xFF97F0FF),
    secondary = MyraViolet,
    onSecondary = Color(0xFF420072),
    secondaryContainer = Color(0xFF5F149F),
    onSecondaryContainer = Color(0xFFEEDBFF),
    tertiary = MyraEmerald,
    onTertiary = Color(0xFF003824),
    tertiaryContainer = Color(0xFF005237),
    onTertiaryContainer = Color(0xFF8CF5C3),
    background = MyraBgDark,
    onBackground = MyraTextPrimary,
    surface = MyraSurfaceDark,
    onSurface = MyraTextPrimary,
    surfaceVariant = MyraSurfaceElevated,
    onSurfaceVariant = MyraTextSecondary,
    outline = MyraCardBorder
)

private val LightColorScheme = darkColorScheme( // Keep futuristic dark aesthetic preferred for Myra
    primary = MyraCyan,
    onPrimary = Color.Black,
    secondary = MyraViolet,
    background = MyraBgDark,
    surface = MyraSurfaceDark
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false, // Use Myra custom AI cyber-palette
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
