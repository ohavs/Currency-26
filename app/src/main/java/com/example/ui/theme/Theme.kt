package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

val LocalPalette = staticCompositionLocalOf { Palettes.resolve(Palettes.DEFAULT_THEME, dark = false) }

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

/** Material components (switches, text selection, ripples...) pick their colors from the palette. */
private fun AppPalette.toColorScheme(): ColorScheme {
    val base = if (isDark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = highlight,
        onPrimaryContainer = ink,
        inversePrimary = highlight,
        secondary = inkMuted,
        onSecondary = background,
        secondaryContainer = card,
        onSecondaryContainer = ink,
        tertiary = accent,
        onTertiary = onAccent,
        tertiaryContainer = highlightSoft,
        onTertiaryContainer = ink,
        background = background,
        onBackground = ink,
        surface = background,
        onSurface = ink,
        surfaceVariant = card,
        onSurfaceVariant = inkMuted,
        surfaceTint = Color.Transparent,
        inverseSurface = accent,
        inverseOnSurface = onAccent,
        outline = inkMuted,
        outlineVariant = highlightSoft,
        surfaceBright = cardSoft,
        surfaceDim = card,
        surfaceContainerLowest = cardSoft,
        surfaceContainerLow = cardSoft,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = highlightSoft,
    )
}

fun isDarkTheme(themeMode: String, systemDark: Boolean): Boolean = when (themeMode) {
    "dark" -> true
    "light" -> false
    else -> systemDark
}

@Composable
fun MyApplicationTheme(
    themeMode: String = "system",
    colorThemeStr: String = Palettes.DEFAULT_THEME,
    content: @Composable () -> Unit,
) {
    val palette = Palettes.resolve(colorThemeStr, isDarkTheme(themeMode, isSystemInDarkTheme()))
    CompositionLocalProvider(LocalPalette provides palette) {
        MaterialTheme(
            colorScheme = palette.toColorScheme(),
            typography = Typography,
            shapes = AppShapes,
            content = content,
        )
    }
}
