package com.example.widget

/**
 * Glance cannot auto-size text, so amounts get a font size computed from the space they have:
 * as large as [maxSp] allows, but small enough for [text] to fit [availableWidthDp] in bold digits.
 */
internal fun fitAmountSp(text: String, availableWidthDp: Float, maxSp: Float, minSp: Float = 14f): Float {
    // Bold Roboto digits are ~0.6em wide; separators are narrower, so this errs on the safe side.
    val byWidth = availableWidthDp / (text.length.coerceAtLeast(1) * 0.62f)
    return byWidth.coerceAtMost(maxSp).coerceAtLeast(minSp)
}

/** Keeps long currency names ("דירהם (איחוד האמירויות)") from crowding the widget. */
internal fun shortName(name: String, max: Int = 14): String =
    if (name.length <= max) name else name.take(max - 1).trimEnd() + "…"
