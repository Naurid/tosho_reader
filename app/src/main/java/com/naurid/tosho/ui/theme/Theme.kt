package com.naurid.tosho.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = DarkTextPrimary,
    onPrimary = DarkBackground,
    secondary = ExpressiveSecondary,
    onSecondary = DarkBackground,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    outline = DarkGlassBorder,
    outlineVariant = DarkDivider
)

private val LightColorScheme = lightColorScheme(
    primary = LightTextPrimary,
    onPrimary = LightBackground,
    secondary = ExpressivePrimary,
    onSecondary = LightBackground,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    background = LightBackground,
    onBackground = LightTextPrimary,
    outline = LightGlassBorder,
    outlineVariant = LightDivider
)

@Composable
fun ToshoTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
