package com.example.widget

/** Currency symbols beside widget amounts are drawn at this fraction of the amount's size. */
internal const val SymbolScale = 0.62f

/** Approximate width of [text] in em for bold digits: digits ~0.6em, separators ~0.3em, letters/signs ~0.65em. */
internal fun textEm(text: String): Float = text.sumOf {
    when {
        it.isDigit() -> 0.6
        it == ',' || it == '.' || it == ' ' -> 0.3
        else -> 0.65
    }
}.toFloat()

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
    val symbolEm = if (symbol.isEmpty()) 0f else (textEm(symbol) + 0.3f) * SymbolScale
    // A few percent of slack for fonts with wider digits than Roboto.
    val byWidth = availableWidthDp / ((textEm(text).coerceAtLeast(0.6f) + symbolEm) * 1.05f)
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
    val flagSp: Float,
    /** The swap button between the source and target cards. */
    val swapSizeDp: Float,
    val showNames: Boolean,
    val nameSp: Float,
    val nameLines: Int,
    /** Width of the flag + name column on the reading side of each card. */
    val labelWidthDp: Float,
    /** Width for an amount and its symbol, next to the flag/name column. */
    val amountWidthDp: Float,
) {
    companion object {
        const val PADDING = 12f
        const val GAP = 6f
        const val CARD_PADDING = 12f
        const val RATE_HEIGHT = 22f
        const val KEYPAD_SPACING = 8f
    }
}

/**
 * The keypad takes the larger share of the height and its keys grow with the widget; the cards keep what
 * their content needs (never under ~40dp) and their flag, name and amount scale with them. A small rate
 * line sits at the very bottom. Short widgets drop the rate line, and then the keypad.
 */
internal fun largeWidgetLayout(widthDp: Float, heightDp: Float, hasExtra: Boolean): LargeWidgetLayout {
    val padding = LargeWidgetLayout.PADDING
    val gap = LargeWidgetLayout.GAP
    val cardCount = if (hasExtra) 3 else 2
    val showKeypad = heightDp >= 230f
    val showRate = heightDp >= 280f
    val available = heightDp - padding * 2 - if (showRate) LargeWidgetLayout.RATE_HEIGHT else 0f
    val keypadChrome = LargeWidgetLayout.KEYPAD_SPACING + gap * 3
    val minCards = 40f * cardCount + gap * (cardCount - 1)
    val keypadShare = if (hasExtra) 0.5f else 0.58f
    val keyRow = if (!showKeypad) 0f else ((available * keypadShare - keypadChrome) / 4)
        .coerceAtMost((available - keypadChrome - minCards) / 4)
        .coerceIn(22f, 92f)
    val keypad = if (showKeypad) keyRow * 4 + keypadChrome else 0f
    val cardHeight = (available - keypad - gap * (cardCount - 1)) / cardCount
    val flagSp = (cardHeight * 0.34f).coerceIn(18f, 46f)
    val showNames = cardHeight >= 52f
    val nameSp = (cardHeight * 0.12f).coerceIn(11f, 16f)
    val labelWidth = if (showNames) maxOf(flagSp * 1.35f, nameSp * 6f) else flagSp * 1.35f
    return LargeWidgetLayout(
        showKeypad = showKeypad,
        keyRowDp = keyRow,
        keyFontSp = (keyRow * 0.4f).coerceIn(14f, 34f),
        showRate = showRate,
        cardHeightDp = cardHeight,
        maxAmountSp = (cardHeight * 0.55f).coerceIn(16f, 64f),
        flagSp = flagSp,
        swapSizeDp = (cardHeight * 0.36f).coerceIn(40f, 52f),
        showNames = showNames,
        nameSp = nameSp,
        nameLines = if (cardHeight >= 100f) 2 else 1,
        labelWidthDp = labelWidth,
        amountWidthDp = widthDp - padding * 2 - LargeWidgetLayout.CARD_PADDING * 2 - labelWidth - 12f,
    )
}
