package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.*
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.CurrencyApp
import com.example.MainActivity
import com.example.R
import com.example.data.CurrencySlot
import com.example.utils.AppLanguage
import com.example.utils.amountSymbol
import com.example.utils.applyKeypadKey
import com.example.utils.currencyName
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatRate
import com.example.utils.formatUpdatedAt
import com.example.utils.getCurrencyInfo
import com.example.utils.symbolLeads
import java.util.Locale

class LargeCurrencyWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            LargeWidgetContent(currentState<Preferences>().toWidgetData())
        }
    }

    @Composable
    private fun LargeWidgetContent(data: WidgetData) {
        val context = LocalContext.current
        val size = LocalSize.current
        val colors = WidgetColors(data.themeMode, data.colorTheme)
        val extra = data.extraTargetCurrency
        // Currencies sit on the side the app language reads from (right for Hebrew, left for English/Spanish).
        // The launcher mirrors rows when the *device* is RTL, so the emitted order accounts for both.
        val appRtl = AppLanguage.isRtl(data.language)
        val launcherRtl = AppLanguage.isLauncherRtl()
        val pillFirst = appRtl == launcherRtl
        val locale = AppLanguage.locale(data.language)
        val localized = remember(data.language) { AppLanguage.wrap(context, data.language) }
        val updatedTime = formatUpdatedAt(
            data.updatedAt,
            todayFormat = localized.getString(R.string.today_at),
            yesterdayFormat = localized.getString(R.string.yesterday_at)
        )

        val layout = largeWidgetLayout(size.width.value, size.height.value, hasExtra = extra != null)
        val padding = LargeWidgetLayout.PADDING.dp
        val gap = LargeWidgetLayout.GAP.dp

        val amount = data.amount.toDoubleOrNull() ?: 0.0
        val sourceText = formatAmountInput(data.amount)
        val targetText = formatAmount(amount * data.rate)

        @Composable
        fun card(code: String, text: String, slot: CurrencySlot, background: ColorProvider, removable: Boolean, modifier: GlanceModifier) {
            val symbol = amountSymbol(code)
            val width = if (removable) layout.amountWidthDp - 32f else layout.amountWidthDp
            CurrencyCard(
                code = code,
                symbol = symbol,
                amount = text,
                fontSize = fitAmountSp(text, width, layout.maxAmountSp, symbol = symbol),
                layout = layout,
                slot = slot,
                background = background,
                colors = colors,
                locale = locale,
                pillFirst = pillFirst,
                launcherRtl = launcherRtl,
                removable = removable,
                modifier = modifier
            )
        }

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(28.dp)
                .padding(padding)
        ) {
            // Source and target with the swap button alone in the middle, cut into both cards (as in the app).
            Box(modifier = GlanceModifier.fillMaxWidth().defaultWeight(), contentAlignment = Alignment.Center) {
                Column(modifier = GlanceModifier.fillMaxSize()) {
                    card(data.sourceCurrency, sourceText, CurrencySlot.SOURCE, colors.card, false, GlanceModifier.fillMaxWidth().defaultWeight())
                    Spacer(GlanceModifier.height(gap))
                    card(data.targetCurrency, targetText, CurrencySlot.TARGET, colors.highlight, false, GlanceModifier.fillMaxWidth().defaultWeight())
                }
                SwapButton(colors, layout.swapWidthDp, layout.swapHeightDp)
            }
            if (extra != null) {
                Spacer(GlanceModifier.height(gap))
                // The optional currency: lighter card plus a remove button, matching the app.
                card(extra, formatAmount(amount * data.extraRate), CurrencySlot.EXTRA_TARGET, colors.cardSoft, true, GlanceModifier.fillMaxWidth().height(layout.cardHeightDp.dp))
            }
            if (layout.showKeypad) {
                Spacer(GlanceModifier.height(LargeWidgetLayout.KEYPAD_SPACING.dp))
                Keypad(
                    colors = colors,
                    mirrored = launcherRtl,
                    gap = gap,
                    rowHeight = layout.keyRowDp.dp,
                    fontSize = layout.keyFontSp.sp,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
            if (layout.showRate) {
                RateLine(
                    data = data,
                    colors = colors,
                    updatedTime = updatedTime,
                    updatedLabel = localized.getString(R.string.updated_at, updatedTime.orEmpty()),
                    pillFirst = pillFirst,
                    height = LargeWidgetLayout.RATE_HEIGHT.dp,
                    widthDp = size.width.value - LargeWidgetLayout.PADDING * 2
                )
            }
        }
    }

    /** Flag and name (on the reading side) and the amount with its symbol; tapping the card opens the picker. */
    @Composable
    private fun CurrencyCard(
        code: String,
        symbol: String,
        amount: String,
        fontSize: Float,
        layout: LargeWidgetLayout,
        slot: CurrencySlot,
        background: ColorProvider,
        colors: WidgetColors,
        locale: Locale,
        pillFirst: Boolean,
        launcherRtl: Boolean,
        removable: Boolean,
        modifier: GlanceModifier,
    ) {
        val context = LocalContext.current
        Row(
            modifier = modifier
                .background(background)
                .cornerRadius(20.dp)
                .padding(horizontal = LargeWidgetLayout.CARD_PADDING.dp)
                .clickable(actionStartActivity(MainActivity.pickCurrencyIntent(context, slot))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val label: @Composable () -> Unit = { CurrencyLabel(code, layout, colors, locale) }
            val symbolText: @Composable () -> Unit = {
                Text(
                    ltr(symbol),
                    style = TextStyle(color = colors.accent, fontSize = (fontSize * SymbolScale).sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
            }
            val amountText: @Composable () -> Unit = {
                Text(
                    ltr(amount),
                    style = TextStyle(color = colors.ink, fontSize = fontSize.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
            val small: @Composable () -> Unit = { Spacer(GlanceModifier.width(4.dp)) }
            val flexible: @Composable () -> Unit = { Spacer(GlanceModifier.defaultWeight()) }
            val remove: @Composable () -> Unit = { RemoveButton(colors) }
            // Symbol and amount always read left to right on screen ("$ 1,250", "1,250 Ft"); RTL launchers mirror rows.
            val onScreen = if (symbolLeads(symbol)) listOf(symbolText, small, amountText) else listOf(amountText, small, symbolText)
            val amountGroup = if (launcherRtl) onScreen.reversed() else onScreen
            val tail = if (removable) listOf(small, remove) else emptyList()
            // Row children are mirrored by RTL launchers, so the whole order flips with pillFirst.
            val items = if (pillFirst) listOf(label, flexible) + amountGroup + tail else tail.reversed() + amountGroup + listOf(flexible, label)
            items.forEach { it() }
        }
    }

    /** A big flag on its own (no background), with the currency name underneath when there is room. */
    @Composable
    private fun CurrencyLabel(code: String, layout: LargeWidgetLayout, colors: WidgetColors, locale: Locale) {
        Column(
            modifier = GlanceModifier.width(layout.labelWidthDp.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(getCurrencyInfo(code).flag, style = TextStyle(fontSize = layout.flagSp.sp))
            if (layout.showNames) {
                val name = currencyName(code, locale)
                Text(
                    if (layout.nameLines > 1) shortName(name, max = 24) else shortName(name, max = 13),
                    style = TextStyle(color = colors.ink, fontSize = layout.nameSp.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center),
                    maxLines = layout.nameLines,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun RemoveButton(colors: WidgetColors) {
        Box(
            modifier = GlanceModifier
                .size(26.dp)
                .background(colors.card)
                .cornerRadius(13.dp)
                .clickable(actionRunCallback<RemoveExtraAction>()),
            contentAlignment = Alignment.Center
        ) {
            Text("✕", style = TextStyle(color = colors.inkMuted, fontSize = 12.sp, fontWeight = FontWeight.Bold))
        }
    }

    /** Small line at the very bottom: the live rate and when it was updated; tapping it opens the app. */
    @Composable
    private fun RateLine(
        data: WidgetData,
        colors: WidgetColors,
        updatedTime: String?,
        updatedLabel: String,
        pillFirst: Boolean,
        height: Dp,
        widthDp: Float,
    ) {
        val context = LocalContext.current
        val rate = "1 ${data.sourceCurrency} = ${formatRate(data.rate)} ${data.targetCurrency}"
        // Drop the "updated" wording, then the time, when the line would not fit on one row (~0.55em per char at 11sp).
        val fits = { text: String -> (rate.length + 3 + text.length) * 11f * 0.55f <= widthDp }
        val updated = when {
            updatedTime == null -> null
            fits(updatedLabel) -> updatedLabel
            fits(updatedTime) -> updatedTime
            else -> null
        }
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .height(height)
                .clickable(actionStartActivity(MainActivity.openAppIntent(context))),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalAlignment = Alignment.Bottom
        ) {
            val rateText: @Composable () -> Unit = {
                Text(ltr(rate), style = TextStyle(color = colors.inkMuted, fontSize = 11.sp, fontWeight = FontWeight.Medium), maxLines = 1)
            }
            // The rate comes first in reading order, the update time after it.
            if (updated == null) {
                rateText()
            } else {
                val updatedText: @Composable () -> Unit = {
                    Text(updated, style = TextStyle(color = colors.inkMuted, fontSize = 11.sp), maxLines = 1)
                }
                if (pillFirst) rateText() else updatedText()
                Text("  ·  ", style = TextStyle(color = colors.inkMuted, fontSize = 11.sp), maxLines = 1)
                if (pillFirst) updatedText() else rateText()
            }
        }
    }

    /** Accent button in a background-colored ring, so it looks cut into the two cards around it. */
    @Composable
    private fun SwapButton(colors: WidgetColors, width: Float, height: Float) {
        Box(
            modifier = GlanceModifier
                .size(width.dp, height.dp)
                .background(colors.background)
                .cornerRadius((height * 0.375f).dp)
                .clickable(actionRunCallback<SwapAction>())
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(colors.accent)
                    .cornerRadius((height * 0.3f).dp),
                contentAlignment = Alignment.Center
            ) {
                Text("⇅", style = TextStyle(color = colors.onAccent, fontSize = (height * 0.5f).sp, fontWeight = FontWeight.Bold))
            }
        }
    }

    @Composable
    private fun Keypad(
        colors: WidgetColors,
        mirrored: Boolean,
        gap: Dp,
        rowHeight: Dp,
        fontSize: TextUnit,
        modifier: GlanceModifier,
    ) {
        val rows = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("C", "0", "⌫")
        )
        Column(modifier = modifier) {
            rows.forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(GlanceModifier.height(gap))
                Row(modifier = GlanceModifier.fillMaxWidth().height(rowHeight)) {
                    // Keep 7-8-9 left-to-right even when the launcher mirrors rows.
                    val ordered = if (mirrored) row.reversed() else row
                    ordered.forEachIndexed { index, key ->
                        if (index > 0) Spacer(GlanceModifier.width(gap))
                        KeyButton(key, colors, fontSize)
                    }
                }
            }
        }
    }

    @Composable
    private fun RowScope.KeyButton(key: String, colors: WidgetColors, fontSize: TextUnit) {
        val background = when (key) {
            "C" -> colors.accent
            "⌫" -> colors.highlight
            else -> colors.cardSoft
        }
        Box(
            modifier = GlanceModifier
                .defaultWeight()
                .fillMaxHeight()
                .background(background)
                .cornerRadius(16.dp)
                .clickable(actionRunCallback<KeypadAction>(actionParametersOf(KeypadAction.KeyParam to key))),
            contentAlignment = Alignment.Center
        ) {
            Text(
                key,
                style = TextStyle(
                    color = if (key == "C") colors.onAccent else colors.ink,
                    fontSize = fontSize,
                    fontWeight = FontWeight.Medium
                )
            )
        }
    }
}

class SwapAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
        val currentSource = prefs.getString("source", "USD") ?: "USD"
        val currentTarget = prefs.getString("target", "ILS") ?: "ILS"
        prefs.edit()
            .putString("source", currentTarget)
            .putString("target", currentSource)
            .apply()

        updateWidgets(context)
    }
}

class KeypadAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val key = parameters[KeyParam] ?: return
        val prefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
        val currentAmount = prefs.getString("amount", "1") ?: "1"
        val newAmount = applyKeypadKey(currentAmount, key)
        if (newAmount == currentAmount) return
        prefs.edit().putString("amount", newAmount).apply()

        updateWidgets(context)
    }

    companion object {
        val KeyParam = ActionParameters.Key<String>("key")
    }
}

/** The ✕ on the widget's extra currency. */
class RemoveExtraAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        (context.applicationContext as CurrencyApp).repository.setExtraTargetCurrency(null)
        updateWidgets(context)
    }
}
