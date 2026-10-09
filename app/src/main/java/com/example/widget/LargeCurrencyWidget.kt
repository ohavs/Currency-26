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
import com.example.utils.applyKeypadKey
import com.example.utils.currencyName
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatRate
import com.example.utils.formatUpdatedAt
import com.example.utils.getCurrencyInfo
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
        val updatedLabel = formatUpdatedAt(
            data.updatedAt,
            todayFormat = localized.getString(R.string.today_at),
            yesterdayFormat = localized.getString(R.string.yesterday_at)
        )?.let { localized.getString(R.string.updated_at, it) }

        // The keypad keeps a normal, bounded size; all remaining height goes to the amounts.
        val padding = 12.dp
        val gap = 6.dp
        val keyRow = (size.height.value * 0.085f).coerceIn(28f, 50f).dp
        val keypadHeight = keyRow * 4 + gap * 3
        val showRate = size.height >= 260.dp
        val rateHeight = 36.dp
        val cardCount = if (extra != null) 3 else 2
        val displayHeight = size.height - padding * 2 - keypadHeight - 8.dp
        val fixedInDisplay = (if (showRate) rateHeight + gap * 2 else gap) + (if (extra != null) gap else 0.dp)
        val cardHeight = (displayHeight - fixedInDisplay) / cardCount
        val maxAmountSp = (cardHeight.value * 0.5f).coerceIn(16f, 46f)
        val showNames = cardHeight >= 54.dp
        // Width left for an amount next to the currency pill (~92dp) inside the card padding.
        val amountWidth = size.width.value - padding.value * 2 - 28f - 92f

        val amount = data.amount.toDoubleOrNull() ?: 0.0
        val sourceText = formatAmountInput(data.amount)
        val targetText = formatAmount(amount * data.rate)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(28.dp)
                .padding(padding)
        ) {
            Column(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
                CurrencyCard(
                    code = data.sourceCurrency,
                    amount = sourceText,
                    fontSize = fitAmountSp(sourceText, amountWidth, maxAmountSp).sp,
                    slot = CurrencySlot.SOURCE,
                    background = colors.card,
                    pillBackground = colors.cardSoft,
                    colors = colors,
                    locale = locale,
                    showName = showNames,
                    pillFirst = pillFirst,
                    removable = false,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )
                Spacer(GlanceModifier.height(gap))
                if (showRate) {
                    RateRow(data, colors, rateHeight, pillFirst, appRtl, updatedLabel)
                    Spacer(GlanceModifier.height(gap))
                }
                CurrencyCard(
                    code = data.targetCurrency,
                    amount = targetText,
                    fontSize = fitAmountSp(targetText, amountWidth, maxAmountSp).sp,
                    slot = CurrencySlot.TARGET,
                    background = colors.highlight,
                    pillBackground = colors.highlightSoft,
                    colors = colors,
                    locale = locale,
                    showName = showNames,
                    pillFirst = pillFirst,
                    removable = false,
                    modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                )
                if (extra != null) {
                    val extraText = formatAmount(amount * data.extraRate)
                    Spacer(GlanceModifier.height(gap))
                    // The optional currency: lighter card plus a remove button, matching the app.
                    CurrencyCard(
                        code = extra,
                        amount = extraText,
                        fontSize = fitAmountSp(extraText, amountWidth - 30f, maxAmountSp).sp,
                        slot = CurrencySlot.EXTRA_TARGET,
                        background = colors.cardSoft,
                        pillBackground = colors.card,
                        colors = colors,
                        locale = locale,
                        showName = showNames,
                        pillFirst = pillFirst,
                        removable = true,
                        modifier = GlanceModifier.fillMaxWidth().defaultWeight()
                    )
                }
            }
            Spacer(GlanceModifier.height(8.dp))
            Keypad(
                colors = colors,
                mirrored = launcherRtl,
                gap = gap,
                rowHeight = keyRow,
                fontSize = (keyRow.value * 0.42f).coerceIn(15f, 21f).sp,
                modifier = GlanceModifier.fillMaxWidth()
            )
        }
    }

    /** Amount (large) on one side, the currency pill on the reading side; tapping the pill opens the picker. */
    @Composable
    private fun CurrencyCard(
        code: String,
        amount: String,
        fontSize: TextUnit,
        slot: CurrencySlot,
        background: ColorProvider,
        pillBackground: ColorProvider,
        colors: WidgetColors,
        locale: Locale,
        showName: Boolean,
        pillFirst: Boolean,
        removable: Boolean,
        modifier: GlanceModifier,
    ) {
        Row(
            modifier = modifier
                .background(background)
                .cornerRadius(20.dp)
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Row children are mirrored by RTL launchers, so the whole order flips with pillFirst.
            if (pillFirst) {
                CurrencyPill(code, slot, pillBackground, colors, locale, showName, flagFirst = true)
                Spacer(GlanceModifier.defaultWeight())
                AmountText(amount, colors, fontSize)
                if (removable) {
                    Spacer(GlanceModifier.width(6.dp))
                    RemoveButton(colors)
                }
            } else {
                if (removable) {
                    RemoveButton(colors)
                    Spacer(GlanceModifier.width(6.dp))
                }
                AmountText(amount, colors, fontSize)
                Spacer(GlanceModifier.defaultWeight())
                CurrencyPill(code, slot, pillBackground, colors, locale, showName, flagFirst = false)
            }
        }
    }

    /** Flag and symbol, with the currency name underneath when there is room. */
    @Composable
    private fun CurrencyPill(
        code: String,
        slot: CurrencySlot,
        background: ColorProvider,
        colors: WidgetColors,
        locale: Locale,
        showName: Boolean,
        flagFirst: Boolean,
    ) {
        val context = LocalContext.current
        val info = getCurrencyInfo(code)
        val symbol = info.symbol.ifEmpty { code }
        Column(
            modifier = GlanceModifier
                .background(background)
                .cornerRadius(14.dp)
                .padding(horizontal = 10.dp, vertical = 5.dp)
                .clickable(actionStartActivity(MainActivity.pickCurrencyIntent(context, slot))),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (flagFirst) {
                    Text(info.flag, style = TextStyle(fontSize = 18.sp))
                    Spacer(GlanceModifier.width(5.dp))
                    Text(symbol, style = TextStyle(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                } else {
                    Text(symbol, style = TextStyle(color = colors.ink, fontSize = 16.sp, fontWeight = FontWeight.Bold))
                    Spacer(GlanceModifier.width(5.dp))
                    Text(info.flag, style = TextStyle(fontSize = 18.sp))
                }
            }
            if (showName) {
                Text(
                    shortName(currencyName(code, locale)),
                    style = TextStyle(color = colors.inkMuted, fontSize = 11.sp, textAlign = TextAlign.Center),
                    maxLines = 1
                )
            }
        }
    }

    @Composable
    private fun AmountText(amount: String, colors: WidgetColors, fontSize: TextUnit) {
        Text(
            ltr(amount),
            style = TextStyle(color = colors.ink, fontSize = fontSize, fontWeight = FontWeight.Bold),
            maxLines = 1
        )
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

    /** Live rate (tap opens the app) with the swap button on the currency-pill side. */
    @Composable
    private fun RateRow(
        data: WidgetData,
        colors: WidgetColors,
        height: Dp,
        pillFirst: Boolean,
        appRtl: Boolean,
        updatedLabel: String?,
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (pillFirst) {
                SwapButton(colors, height)
                Spacer(GlanceModifier.width(6.dp))
                RateBox(data, colors, height, appRtl, updatedLabel, GlanceModifier.defaultWeight())
            } else {
                RateBox(data, colors, height, appRtl, updatedLabel, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(6.dp))
                SwapButton(colors, height)
            }
        }
    }

    @Composable
    private fun RateBox(
        data: WidgetData,
        colors: WidgetColors,
        height: Dp,
        appRtl: Boolean,
        updatedLabel: String?,
        modifier: GlanceModifier,
    ) {
        val context = LocalContext.current
        // Text starts on the reading side of the app language, whatever the launcher direction.
        val align = if (appRtl) TextAlign.Right else TextAlign.Left
        Column(
            modifier = modifier
                .height(height)
                .background(colors.cardSoft)
                .cornerRadius(12.dp)
                .padding(horizontal = 12.dp)
                .clickable(actionStartActivity(MainActivity.openAppIntent(context))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                ltr("1 ${data.sourceCurrency} = ${formatRate(data.rate)} ${data.targetCurrency}"),
                style = TextStyle(color = colors.ink, fontSize = 12.sp, fontWeight = FontWeight.Medium, textAlign = align),
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth()
            )
            if (updatedLabel != null) {
                Text(
                    updatedLabel,
                    style = TextStyle(color = colors.inkMuted, fontSize = 10.sp, textAlign = align),
                    maxLines = 1,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun SwapButton(colors: WidgetColors, size: Dp) {
        Box(
            modifier = GlanceModifier
                .size(size)
                .background(colors.accent)
                .cornerRadius(12.dp)
                .clickable(actionRunCallback<SwapAction>()),
            contentAlignment = Alignment.Center
        ) {
            Text("⇅", style = TextStyle(color = colors.onAccent, fontSize = 18.sp, fontWeight = FontWeight.Bold))
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
                .cornerRadius(14.dp)
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
