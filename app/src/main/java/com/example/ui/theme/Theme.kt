package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CyberDarkColorScheme = darkColorScheme(
    primary = CyberNeonCyan,
    onPrimary = CyberBgDark,
    primaryContainer = CyberBgSurfaceElevated,
    onPrimaryContainer = CyberNeonCyan,
    secondary = CyberElectricEmerald,
    onSecondary = CyberBgDark,
    secondaryContainer = CyberBgSurfaceElevated,
    onSecondaryContainer = CyberElectricEmerald,
    tertiary = CyberAmber,
    onTertiary = CyberBgDark,
    background = CyberBgDark,
    onBackground = CyberTextPrimary,
    surface = CyberBgSurface,
    onSurface = CyberTextPrimary,
    surfaceVariant = CyberBgCard,
    onSurfaceVariant = CyberTextSecondary,
    outline = CyberBorderSubtle,
    outlineVariant = CyberBorderGlow,
    error = CyberCrimson,
    onError = CyberTextPrimary
)

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true, // Cyber-tech is dark-first
    content: @Composable () -> Unit
) {
    val colorScheme = CyberDarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = CyberBgDark.toArgb()
                window.navigationBarColor = CyberBgDark.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = false
                controller.isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
