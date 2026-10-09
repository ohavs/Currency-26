package com.example.ui.theme

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import com.example.R

/**
 * Design tokens shared by the app and the home-screen widgets.
 *
 * Light palettes are soft pastel tints with a deep "ink" accent, dark palettes are deep slates
 * with a pastel accent - the same language as the sage/slate exchange-app reference.
 */
@Immutable
data class AppPalette(
    val isDark: Boolean,
    /** Screen background. */
    val background: Color,
    /** Primary cards (source currency, settings sections). */
    val card: Color,
    /** Softer fill: amount fields, keypad digits, chip tracks. */
    val cardSoft: Color,
    /** Emphasised cards (target currency, selected items). */
    val highlight: Color,
    val highlightSoft: Color,
    /** Main text. */
    val ink: Color,
    /** Labels and secondary text. */
    val inkMuted: Color,
    /** Solid call-to-action fill (buttons, selected chips). */
    val accent: Color,
    val onAccent: Color,
)

@Immutable
data class ColorThemeOption(
    val key: String,
    @param:StringRes val labelRes: Int,
    val light: AppPalette,
    val dark: AppPalette,
)

object Palettes {
    const val DEFAULT_THEME = "sage"

    private val sage = ColorThemeOption(
        key = "sage",
        labelRes = R.string.theme_sage,
        light = AppPalette(
            isDark = false,
            background = Color(0xFFD9EBD7),
            card = Color(0xFFBFDCC0),
            cardSoft = Color(0xFFE8F3E6),
            highlight = Color(0xFFA3CBAD),
            highlightSoft = Color(0xFFB6D6BD),
            ink = Color(0xFF26343F),
            inkMuted = Color(0xFF465F54),
            accent = Color(0xFF34495A),
            onAccent = Color(0xFFE4F2E2),
        ),
        dark = AppPalette(
            isDark = true,
            background = Color(0xFF24313D),
            card = Color(0xFF2F3F4E),
            cardSoft = Color(0xFF2A3846),
            highlight = Color(0xFF45596E),
            highlightSoft = Color(0xFF3D5063),
            ink = Color(0xFFE5F1E3),
            inkMuted = Color(0xFFA8BAC6),
            accent = Color(0xFFD3E8D1),
            onAccent = Color(0xFF24313D),
        ),
    )

    private val ocean = ColorThemeOption(
        key = "ocean",
        labelRes = R.string.theme_ocean,
        light = AppPalette(
            isDark = false,
            background = Color(0xFFD8E7EF),
            card = Color(0xFFC0D8E6),
            cardSoft = Color(0xFFE9F2F7),
            highlight = Color(0xFFA5C5D9),
            highlightSoft = Color(0xFFB9D3E3),
            ink = Color(0xFF22344A),
            inkMuted = Color(0xFF435C72),
            accent = Color(0xFF2C4660),
            onAccent = Color(0xFFE4EFF6),
        ),
        dark = AppPalette(
            isDark = true,
            background = Color(0xFF1E2B39),
            card = Color(0xFF283A4C),
            cardSoft = Color(0xFF243444),
            highlight = Color(0xFF37536E),
            highlightSoft = Color(0xFF314A62),
            ink = Color(0xFFDFEBF4),
            inkMuted = Color(0xFFA3B8CA),
            accent = Color(0xFFCBE0EE),
            onAccent = Color(0xFF1E2B39),
        ),
    )

    private val sunset = ColorThemeOption(
        key = "sunset",
        labelRes = R.string.theme_sunset,
        light = AppPalette(
            isDark = false,
            background = Color(0xFFF2E4D8),
            card = Color(0xFFE8D0BD),
            cardSoft = Color(0xFFF8EEE6),
            highlight = Color(0xFFDDB89E),
            highlightSoft = Color(0xFFE5C7B1),
            ink = Color(0xFF3E2F2A),
            inkMuted = Color(0xFF6E5346),
            accent = Color(0xFF4B3630),
            onAccent = Color(0xFFF8ECE2),
        ),
        dark = AppPalette(
            isDark = true,
            background = Color(0xFF2C2321),
            card = Color(0xFF3A2F2B),
            cardSoft = Color(0xFF342A27),
            highlight = Color(0xFF57423A),
            highlightSoft = Color(0xFF4D3B34),
            ink = Color(0xFFF5E8DE),
            inkMuted = Color(0xFFBBA597),
            accent = Color(0xFFF0DAC9),
            onAccent = Color(0xFF2C2321),
        ),
    )

    private val rose = ColorThemeOption(
        key = "rose",
        labelRes = R.string.theme_rose,
        light = AppPalette(
            isDark = false,
            background = Color(0xFFF2E0E2),
            card = Color(0xFFE7C9CD),
            cardSoft = Color(0xFFF8EDEE),
            highlight = Color(0xFFD9AFB6),
            highlightSoft = Color(0xFFE2C0C5),
            ink = Color(0xFF3C2A31),
            inkMuted = Color(0xFF6E4E58),
            accent = Color(0xFF4B303A),
            onAccent = Color(0xFFF8E8EA),
        ),
        dark = AppPalette(
            isDark = true,
            background = Color(0xFF2A2126),
            card = Color(0xFF382B32),
            cardSoft = Color(0xFF32262D),
            highlight = Color(0xFF553F4A),
            highlightSoft = Color(0xFF4B3741),
            ink = Color(0xFFF4E4E8),
            inkMuted = Color(0xFFBA9FA9),
            accent = Color(0xFFF0D3D9),
            onAccent = Color(0xFF2A2126),
        ),
    )

    private val lavender = ColorThemeOption(
        key = "lavender",
        labelRes = R.string.theme_lavender,
        light = AppPalette(
            isDark = false,
            background = Color(0xFFE3E1F1),
            card = Color(0xFFCECAE7),
            cardSoft = Color(0xFFF0EFF8),
            highlight = Color(0xFFB8B2DB),
            highlightSoft = Color(0xFFC6C1E2),
            ink = Color(0xFF2C2A46),
            inkMuted = Color(0xFF524E78),
            accent = Color(0xFF37355B),
            onAccent = Color(0xFFEDEBF8),
        ),
        dark = AppPalette(
            isDark = true,
            background = Color(0xFF22212E),
            card = Color(0xFF2E2D3F),
            cardSoft = Color(0xFF29283A),
            highlight = Color(0xFF46446A),
            highlightSoft = Color(0xFF3E3C5E),
            ink = Color(0xFFE8E6F6),
            inkMuted = Color(0xFFAEABC9),
            accent = Color(0xFFD9D5F1),
            onAccent = Color(0xFF22212E),
        ),
    )

    val options: List<ColorThemeOption> = listOf(sage, ocean, sunset, rose, lavender)

    /** Maps keys stored by older versions ("standard", "forest") onto the current themes. */
    fun normalize(key: String?): String = when (key) {
        null, "standard", "forest" -> DEFAULT_THEME
        else -> if (options.any { it.key == key }) key else DEFAULT_THEME
    }

    fun option(key: String?): ColorThemeOption {
        val normalized = normalize(key)
        return options.first { it.key == normalized }
    }

    fun resolve(key: String?, dark: Boolean): AppPalette = option(key).let { if (dark) it.dark else it.light }
}
