package com.z.financetracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary            = Indigo400,
    onPrimary          = DarkOnBackground,
    primaryContainer   = Color(0xFF312E81),
    onPrimaryContainer = Color(0xFFE0E7FF),
    background         = DarkBackground,
    onBackground       = DarkOnBackground,
    surface            = DarkSurface,
    onSurface          = DarkOnSurface,
    surfaceVariant     = DarkSurfaceVar,
    onSurfaceVariant   = DarkOnSurfaceVar,
    outline            = DarkOutline,
    outlineVariant     = DarkSurfaceVar,
)

private val LightColorScheme = lightColorScheme(
    primary            = Indigo600,
    onPrimary          = LightSurface,
    primaryContainer   = Color(0xFFE0E7FF),
    onPrimaryContainer = Color(0xFF312E81),
    background         = LightBackground,
    onBackground       = LightOnBackground,
    surface            = LightSurface,
    onSurface          = LightOnSurface,
    surfaceVariant     = LightSurfaceVar,
    onSurfaceVariant   = LightOnSurfaceVar,
    outline            = LightOutline,
    outlineVariant     = LightOutline,
)

@Composable
fun FinanceTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography  = Typography,
        content     = content
    )
}
