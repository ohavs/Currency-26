package com.example.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.layout.ContentScale
import androidx.glance.layout.fillMaxSize
import androidx.glance.unit.ColorProvider
import com.example.ui.theme.AppPalette
import com.example.ui.theme.Palettes
import androidx.glance.color.ColorProvider as DayNightColorProvider

/**
 * The app palette for Glance. A forced light/dark choice in the app is honoured; in "system" mode the
 * colors switch with the launcher's day/night mode without needing a widget refresh.
 */
class WidgetColors(themeMode: String, colorTheme: String) {
    private val option = Palettes.option(colorTheme)
    private val forced: AppPalette? = when (themeMode) {
        "dark" -> option.dark
        "light" -> option.light
        else -> null
    }

    private fun pick(select: (AppPalette) -> Color): ColorProvider =
        forced?.let { ColorProvider(select(it)) }
            ?: DayNightColorProvider(day = select(option.light), night = select(option.dark))

    val background = pick { it.background }
    val card = pick { it.card }
    val cardSoft = pick { it.cardSoft }
    val highlight = pick { it.highlight }
    val highlightSoft = pick { it.highlightSoft }
    val ink = pick { it.ink }
    val inkMuted = pick { it.inkMuted }
    val accent = pick { it.accent }
    val onAccent = pick { it.onAccent }
    /** Opaque, so the texture's own alpha sets its strength with either tint mode (API 31+ or older). */
    val texture = pick { it.textureInk }
    /** The colour set's texture drawable (see [WidgetTexture]). */
    val textureRes = option.widgetTexture
}

/**
 * The colour set's background texture, as in the app: a tile repeated over the widget (or the sun rays
 * stretched over it), tinted with the accent. Goes first in a Box, under the content.
 */
@Composable
internal fun WidgetTexture(colors: WidgetColors) {
    Image(
        provider = ImageProvider(colors.textureRes),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        colorFilter = ColorFilter.tint(colors.texture),
        modifier = GlanceModifier.fillMaxSize()
    )
}

/** Forces left-to-right display for numbers/codes in RTL launchers (Glance has no text-direction option). */
internal fun ltr(text: String) = "\u200E$text"
