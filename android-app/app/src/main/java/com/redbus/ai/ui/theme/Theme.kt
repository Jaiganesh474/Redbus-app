package com.redbus.ai.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val LightColorScheme = lightColorScheme(
    primary = RedbusCrimson,
    onPrimary = Color.White,
    primaryContainer = RedbusSoftTint,
    onPrimaryContainer = RedbusCrimsonDark,
    secondary = AiViolet,
    onSecondary = Color.White,
    secondaryContainer = AiSoftPurple,
    onSecondaryContainer = AiIndigo,
    tertiary = RatingEmerald,
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightBorder
)

private val DarkColorScheme = darkColorScheme(
    primary = RedbusCrimson,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF4C1D24),
    onPrimaryContainer = Color(0xFFFFCDD2),
    secondary = Color(0xFFA78BFA),
    onSecondary = Color(0xFF1E1B4B),
    secondaryContainer = Color(0xFF2E1065),
    onSecondaryContainer = Color(0xFFDDD6FE),
    tertiary = Color(0xFF34D399),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkBorder
)

@Composable
fun RedBusAITheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = RedbusCrimson.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RedbusTypography,
        content = content
    )
}

