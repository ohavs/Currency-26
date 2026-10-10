package com.example.widget

/** Currency symbols beside widget amounts are drawn at this fraction of the amount's size. */
internal const val SymbolScale = 0.62f

/**
 * Glance cannot auto-size text, so amounts get a font size computed from the space they have:
 * as large as [maxSp] allows, but small enough for [text] to fit [availableWidthDp] in bold digits,
 * together with its [symbol] (drawn at [SymbolScale] of the amount size) when there is one.
 */
internal fun fitAmountSp(
    text: String,
    availableWidthDp: Float,
    maxSp: Float,
    minSp: Float = 14f,
    symbol: String = "",
): Float {
    // Bold Roboto digits are ~0.6em wide; separators are narrower, so this errs on the safe side.
    val symbolEm = if (symbol.isEmpty()) 0f else (symbol.length * 0.62f + 0.3f) * SymbolScale
    val byWidth = availableWidthDp / (text.length.coerceAtLeast(1) * 0.62f + symbolEm)
    return byWidth.coerceAtMost(maxSp).coerceAtLeast(minSp)
}

/** Keeps long currency names ("דירהם (איחוד האמירויות)") from crowding the widget. */
internal fun shortName(name: String, max: Int = 14): String =
    if (name.length <= max) name else name.take(max - 1).trimEnd() + "…"

/** Sizes for the large widget, in dp / sp values (Glance cannot size text by itself). */
internal data class LargeWidgetLayout(
    val showKeypad: Boolean,
    val keyRowDp: Float,
    val keyFontSp: Float,
    val showRate: Boolean,
    val cardHeightDp: Float,
    val maxAmountSp: Float,
    val showNames: Boolean,
    val flagSp: Float,
    /** Width for an amount and its symbol, next to the flag/name column. */
    val amountWidthDp: Float,
) {
    companion object {
        const val PADDING = 12f
        const val GAP = 6f
        const val RATE_HEIGHT = 22f
        const val KEYPAD_SPACING = 8f
    }
}

/**
 * A roomy keypad first (but never squeezing the cards below ~40dp), a small rate line at the very bottom,
 * and the cards share what is left. Short widgets drop the rate line, and then the keypad.
 */
internal fun largeWidgetLayout(widthDp: Float, heightDp: Float, hasExtra: Boolean): LargeWidgetLayout {
    val padding = LargeWidgetLayout.PADDING
    val gap = LargeWidgetLayout.GAP
    val cardCount = if (hasExtra) 3 else 2
    val showKeypad = heightDp >= 230f
    val showRate = heightDp >= 280f
    val chrome = padding * 2 + if (showRate) LargeWidgetLayout.RATE_HEIGHT else 0f
    val minCards = 40f * cardCount + gap * (cardCount - 1)
    val keyRow = if (!showKeypad) 0f else minOf(
        heightDp * (if (hasExtra) 0.10f else 0.115f),
        (heightDp - chrome - LargeWidgetLayout.KEYPAD_SPACING - minCards - gap * 3) / 4
    ).coerceIn(22f, 58f)
    val keypad = if (showKeypad) keyRow * 4 + gap * 3 + LargeWidgetLayout.KEYPAD_SPACING else 0f
    val cardHeight = (heightDp - chrome - keypad - gap * (cardCount - 1)) / cardCount
    val showNames = cardHeight >= 50f
    val flagSp = (cardHeight * 0.34f).coerceIn(16f, 26f)
    val currencyWidth = if (showNames) 72f else flagSp * 1.4f
    return LargeWidgetLayout(
        showKeypad = showKeypad,
        keyRowDp = keyRow,
        keyFontSp = (keyRow * 0.5f).coerceIn(14f, 26f),
        showRate = showRate,
        cardHeightDp = cardHeight,
        maxAmountSp = (cardHeight * 0.5f).coerceIn(16f, 40f),
        showNames = showNames,
        flagSp = flagSp,
        amountWidthDp = widthDp - padding * 2 - 24f - currencyWidth - 10f,
    )
}
