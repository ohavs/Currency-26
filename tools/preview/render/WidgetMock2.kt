package com.example.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AppPalette
import com.example.ui.theme.paletteTexture
import com.example.utils.amountSymbol
import com.example.utils.currencyName
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatRate
import com.example.utils.getCurrencyInfo
import com.example.utils.symbolLeads
import java.util.Locale

/**
 * Desktop mirror of LargeCurrencyWidget (same size math and emitted order). [launcherRtl] lays rows out
 * right-to-left like a mirrored RemoteViews LinearLayout; [appRtl] = Hebrew app vs English/Spanish.
 */
@Composable
fun LargeWidgetMock2(
    p: AppPalette, widthDp: Float, heightDp: Float, appRtl: Boolean, locale: Locale,
    amountRaw: String, src: String, tgt: String, rate: Double, extra: String?, extraRate: Double,
    updatedTime: String?, updatedLabel: String, launcherRtl: Boolean = false, theme: String = "sage",
) {
    val pillFirst = appRtl == launcherRtl
    val layout = largeWidgetLayout(widthDp, heightDp, extra != null)
    val padding = LargeWidgetLayout.PADDING.dp
    val gap = LargeWidgetLayout.GAP.dp
    val keyRow = layout.keyRowDp.dp
    val showRate = layout.showRate
    val rateHeight = LargeWidgetLayout.RATE_HEIGHT.dp
    val cardHeight = layout.cardHeightDp.dp
    val maxAmountSp = layout.maxAmountSp
    val showNames = layout.showNames
    val flagSp = layout.flagSp
    val amountWidth = layout.amountWidthDp
    val amount = amountRaw.toDoubleOrNull() ?: 0.0
    val sourceText = formatAmountInput(amountRaw)
    val targetText = formatAmount(amount * rate)

    @Composable
    fun card(code: String, text: String, bg: Color, removable: Boolean, modifier: Modifier) {
        val symbol = amountSymbol(code)
        val width = if (removable) amountWidth - 32f else amountWidth
        Card(p, code, symbol, text, fitAmountSp(text, width, maxAmountSp, symbol = symbol), layout, bg, locale, pillFirst, launcherRtl, removable, modifier)
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (launcherRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Column(Modifier.size(widthDp.dp, heightDp.dp).clip(RoundedCornerShape(28.dp)).background(p.background).paletteTexture(theme, p).padding(padding)) {
            val swap = @Composable {
                val sh = layout.swapHeightDp
                Box(Modifier.size(layout.swapWidthDp.dp, sh.dp).clip(RoundedCornerShape((sh * 0.375f).dp)).background(p.background).padding(4.dp)) {
                    Box(Modifier.fillMaxSize().clip(RoundedCornerShape((sh * 0.3f).dp)).background(p.accent), contentAlignment = Alignment.Center) {
                        Text("⇅", style = TextStyle(color = p.onAccent, fontSize = (sh * 0.5f).sp, fontWeight = FontWeight.Bold))
                    }
                }
            }
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = if (extra != null) Alignment.TopCenter else Alignment.Center) {
                Column(Modifier.fillMaxSize()) {
                    card(src, sourceText, p.card, false, Modifier.fillMaxWidth().weight(1f))
                    Spacer(Modifier.height(gap))
                    card(tgt, targetText, p.highlight, false, Modifier.fillMaxWidth().weight(1f))
                    if (extra != null) {
                        Spacer(Modifier.height(gap))
                        card(extra, formatAmount(amount * extraRate), p.cardSoft, true, Modifier.fillMaxWidth().weight(1f))
                    }
                }
                if (extra == null) swap() else {
                    val ch = layout.cardHeightDp; val bh = layout.swapHeightDp
                    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(Modifier.height((ch + LargeWidgetLayout.GAP / 2 - bh / 2).dp)); swap()
                        Spacer(Modifier.height((ch + LargeWidgetLayout.GAP - bh).dp)); swap()
                    }
                }
            }
            if (layout.showKeypad) Spacer(Modifier.height(8.dp))
            val keyFont = layout.keyFontSp
            if (layout.showKeypad) Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(gap)) {
                listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"), listOf("C", "0", "⌫")).forEach { row ->
                    Row(Modifier.fillMaxWidth().height(keyRow), horizontalArrangement = Arrangement.spacedBy(gap)) {
                        (if (launcherRtl) row.reversed() else row).forEach { key ->
                            val bg = when (key) { "C" -> p.accent; "⌫" -> p.highlight; else -> p.cardSoft }
                            Box(Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(16.dp)).background(bg), contentAlignment = Alignment.Center) {
                                Text(key, style = TextStyle(color = if (key == "C") p.onAccent else p.ink, fontSize = keyFont.sp, fontWeight = FontWeight.Medium))
                            }
                        }
                    }
                }
            }
            if (showRate) {
                val rateStr = "1 $src = ${formatRate(rate)} $tgt"
                val w = widthDp - padding.value * 2
                val fits = { t: String -> (rateStr.length + 3 + t.length) * 11f * 0.55f <= w }
                val updated = when { updatedTime == null -> null; fits(updatedLabel) -> updatedLabel; fits(updatedTime) -> updatedTime; else -> null }
                Row(Modifier.fillMaxWidth().height(rateHeight), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.Bottom) {
                    val rateText = @Composable { Text("\u200E$rateStr", style = TextStyle(color = p.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1) }
                    if (updated == null) rateText() else {
                        val updatedText = @Composable { Text(updated, style = TextStyle(color = p.inkMuted, fontSize = 11.sp), maxLines = 1) }
                        if (pillFirst) rateText() else updatedText()
                        Text("  ·  ", style = TextStyle(color = p.inkMuted, fontSize = 11.sp), maxLines = 1)
                        if (pillFirst) updatedText() else rateText()
                    }
                }
            }
        }
    }
}

@Composable
private fun Card(
    p: AppPalette, code: String, symbol: String, amount: String, fontSp: Float, layout: LargeWidgetLayout, bg: Color,
    locale: Locale, pillFirst: Boolean, launcherRtl: Boolean, removable: Boolean, modifier: Modifier,
) {
    val info = getCurrencyInfo(code)
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(bg).padding(horizontal = LargeWidgetLayout.CARD_PADDING.dp), verticalAlignment = Alignment.CenterVertically) {
        val label: @Composable () -> Unit = {
            Column(Modifier.width(layout.labelWidthDp.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(info.flag, fontSize = layout.flagSp.sp)
                if (layout.showNames) {
                    val name = currencyName(code, locale)
                    Text(if (layout.nameLines > 1) shortName(name, max = 24) else shortName(name, max = 13),
                        style = TextStyle(color = p.ink, fontSize = layout.nameSp.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                        maxLines = layout.nameLines, modifier = Modifier.fillMaxWidth())
                }
            }
        }
        val symbolText: @Composable () -> Unit = { Text("\u200E$symbol", style = TextStyle(color = p.accent, fontSize = (fontSp * SymbolScale).sp, fontWeight = FontWeight.Medium), maxLines = 1) }
        val amountText: @Composable () -> Unit = { Text("\u200E$amount", style = TextStyle(color = p.ink, fontSize = fontSp.sp, fontWeight = FontWeight.Bold), maxLines = 1) }
        val small: @Composable () -> Unit = { Spacer(Modifier.width(4.dp)) }
        val flexible: @Composable () -> Unit = { Spacer(Modifier.weight(1f)) }
        val remove: @Composable () -> Unit = {
            Box(Modifier.size(26.dp).clip(RoundedCornerShape(13.dp)).background(p.card), contentAlignment = Alignment.Center) {
                Text("✕", style = TextStyle(color = p.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
            }
        }
        val onScreen = if (symbolLeads(symbol)) listOf(symbolText, small, amountText) else listOf(amountText, small, symbolText)
        val amountGroup = if (launcherRtl) onScreen.reversed() else onScreen
        val tail = if (removable) listOf(small, remove) else emptyList()
        val items = if (pillFirst) listOf(label, flexible) + amountGroup + tail else tail.reversed() + amountGroup + listOf(flexible, label)
        items.forEach { it() }
    }
}

/** Desktop mirror of SmallCurrencyWidget. */
@Composable
fun SmallWidgetMock2(
    p: AppPalette, widthDp: Float, heightDp: Float, appRtl: Boolean,
    amountRaw: String, src: String, tgt: String, rate: Double, extra: String?, extraRate: Double, launcherRtl: Boolean = false,
    theme: String = "sage",
) {
    val compact = heightDp < 90f
    val pillFirst = appRtl == launcherRtl
    val amount = amountRaw.toDoubleOrNull() ?: 0.0
    val extraShown = extra?.takeIf { heightDp >= 110f }
    val gap = if (compact) 3.dp else 6.dp
    val showRate = heightDp >= (if (extraShown != null) 190f else 150f)
    val lines = if (extraShown != null) 3 else 2
    val padding = if (compact) 6.dp else 10.dp
    val lineHeight = (heightDp.dp - padding * 2 - gap * (lines - 1) - (if (showRate) 24.dp else 0.dp)) / lines
    val maxAmountSp = (lineHeight.value * 0.55f).coerceIn(13f, 34f)
    val flagSp = if (compact) 15f else (lineHeight.value * 0.45f).coerceIn(17f, 30f)
    val amountWidth = widthDp - padding.value * 2 - (if (compact) 0f else 10f) - (flagSp * 1.3f + 8f) - 12f

    @Composable
    fun line(code: String, text: String, bg: Color) {
        val symbol = amountSymbol(code)
        val fontSp = fitAmountSp(text, amountWidth, maxAmountSp, minSp = 12f, symbol = symbol)
        val rowModifier = if (compact) Modifier.fillMaxWidth() else Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(bg).padding(5.dp)
        Row(rowModifier, verticalAlignment = Alignment.CenterVertically) {
            val flag: @Composable () -> Unit = { Text(getCurrencyInfo(code).flag, fontSize = flagSp.sp, modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)) }
            val symbolText: @Composable () -> Unit = { Text("\u200E$symbol", style = TextStyle(color = p.accent, fontSize = (fontSp * SymbolScale).sp, fontWeight = FontWeight.Medium), maxLines = 1) }
            val amountText: @Composable () -> Unit = { Text("\u200E$text", style = TextStyle(color = p.ink, fontSize = fontSp.sp, fontWeight = FontWeight.Bold), maxLines = 1) }
            val small: @Composable () -> Unit = { Spacer(Modifier.width(3.dp)) }
            val edge: @Composable () -> Unit = { Spacer(Modifier.width(6.dp)) }
            val flexible: @Composable () -> Unit = { Spacer(Modifier.weight(1f)) }
            val onScreen = if (symbolLeads(symbol)) listOf(symbolText, small, amountText) else listOf(amountText, small, symbolText)
            val amountGroup = if (launcherRtl) onScreen.reversed() else onScreen
            val items = if (pillFirst) listOf(flag, flexible) + amountGroup + edge else listOf(edge) + amountGroup + listOf(flexible, flag)
            items.forEach { it() }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides if (launcherRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
        Column(Modifier.size(widthDp.dp, heightDp.dp).clip(RoundedCornerShape(24.dp)).background(p.background).paletteTexture(theme, p).padding(padding), verticalArrangement = Arrangement.Center) {
            line(src, formatAmountInput(amountRaw), p.card)
            Spacer(Modifier.height(gap))
            line(tgt, formatAmount(amount * rate), p.highlight)
            if (extraShown != null) {
                Spacer(Modifier.height(gap))
                line(extraShown, formatAmount(amount * extraRate), p.cardSoft)
            }
            if (showRate) {
                Spacer(Modifier.height(8.dp))
                Text("\u200E1 $src = ${formatRate(rate)} $tgt", style = TextStyle(color = p.inkMuted, fontSize = 11.sp, textAlign = TextAlign.Center), maxLines = 1, modifier = Modifier.fillMaxWidth())
            }
        }
    }
}
