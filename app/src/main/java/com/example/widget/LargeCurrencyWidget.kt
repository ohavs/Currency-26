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
import com.example.MainActivity
import com.example.R
import com.example.data.CurrencySlot
import com.example.utils.AppLanguage
import com.example.utils.applyKeypadKey
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatRate
import com.example.utils.formatUpdatedAt
import com.example.utils.getCurrencyInfo

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
        val hasExtra = data.extraTargetCurrency != null
        // An extra currency row costs keypad height, so the size tiers kick in earlier.
        val tiny = size.height < (if (hasExtra) 290.dp else 250.dp)
        val compact = size.height < (if (hasExtra) 380.dp else 330.dp)
        // Currencies sit on the side the app language reads from (right for Hebrew, left for English/Spanish).
        // The launcher mirrors rows when the *device* is RTL, so the emitted order accounts for both.
        val appRtl = AppLanguage.isRtl(data.language)
        val launcherRtl = AppLanguage.isLauncherRtl()
        val pillFirst = appRtl == launcherRtl
        val localized = remember(data.language) { AppLanguage.wrap(context, data.language) }
        val updatedLabel = formatUpdatedAt(
            data.updatedAt,
            todayFormat = localized.getString(R.string.today_at),
            yesterdayFormat = localized.getString(R.string.yesterday_at)
        )?.let { localized.getString(R.string.updated_at, it) }

        val gap = if (tiny) 4.dp else 6.dp
        val amount = data.amount.toDoubleOrNull() ?: 0.0
        val amountSize = if (tiny) 16.sp else if (compact) 20.sp else 24.sp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(28.dp)
                .padding(if (tiny) 8.dp else 12.dp)
        ) {
            CurrencyCard(
                code = data.sourceCurrency,
                amount = ltr(formatAmountInput(data.amount)),
                slot = CurrencySlot.SOURCE,
                background = colors.card,
                pillBackground = colors.cardSoft,
                colors = colors,
                tiny = tiny,
                pillFirst = pillFirst,
                amountSize = amountSize
            )
            Spacer(GlanceModifier.height(gap))
            RateRow(data = data, colors = colors, tiny = tiny, pillFirst = pillFirst, appRtl = appRtl, updatedLabel = updatedLabel)
            Spacer(GlanceModifier.height(gap))
            CurrencyCard(
                code = data.targetCurrency,
                amount = ltr(formatAmount(amount * data.rate)),
                slot = CurrencySlot.TARGET,
                background = colors.highlight,
                pillBackground = colors.highlightSoft,
                colors = colors,
                tiny = tiny,
                pillFirst = pillFirst,
                amountSize = amountSize
            )
            val extra = data.extraTargetCurrency
            if (extra != null) {
                Spacer(GlanceModifier.height(gap))
                CurrencyCard(
                    code = extra,
                    amount = ltr(formatAmount(amount * data.extraRate)),
                    slot = CurrencySlot.EXTRA_TARGET,
                    background = colors.highlightSoft,
                    pillBackground = colors.highlight,
                    colors = colors,
                    tiny = tiny,
                    pillFirst = pillFirst,
                    amountSize = amountSize
                )
            }
            Spacer(GlanceModifier.height(gap + 4.dp))
            Keypad(
                colors = colors,
                mirrored = launcherRtl,
                gap = gap,
                fontSize = if (tiny) 15.sp else if (compact) 19.sp else 24.sp,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight()
            )
        }
    }

    /** Card with the amount on the left and a tappable currency pill (opens the picker) on the right. */
    @Composable
    private fun CurrencyCard(
        code: String,
        amount: String,
        slot: CurrencySlot,
        background: ColorProvider,
        pillBackground: ColorProvider,
        colors: WidgetColors,
        tiny: Boolean,
        pillFirst: Boolean,
        amountSize: TextUnit,
    ) {
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(background)
                .cornerRadius(20.dp)
                .padding(if (tiny) 5.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (pillFirst) {
                CurrencyPill(code, slot, pillBackground, colors, tiny)
                Spacer(GlanceModifier.defaultWeight())
                AmountText(amount, colors, amountSize)
            } else {
                AmountText(amount, colors, amountSize)
                Spacer(GlanceModifier.defaultWeight())
                CurrencyPill(code, slot, pillBackground, colors, tiny)
            }
        }
    }

    @Composable
    private fun CurrencyPill(code: String, slot: CurrencySlot, background: ColorProvider, colors: WidgetColors, tiny: Boolean) {
        val context = LocalContext.current
        Row(
            modifier = GlanceModifier
                .background(background)
                .cornerRadius(14.dp)
                .padding(horizontal = 10.dp, vertical = if (tiny) 3.dp else 7.dp)
                .clickable(actionStartActivity(MainActivity.pickCurrencyIntent(context, slot))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(getCurrencyInfo(code).flag, style = TextStyle(fontSize = if (tiny) 14.sp else 18.sp))
            Spacer(GlanceModifier.width(6.dp))
            Text(
                code,
                style = TextStyle(color = colors.ink, fontSize = if (tiny) 13.sp else 16.sp, fontWeight = FontWeight.Bold)
            )
        }
    }

    @Composable
    private fun AmountText(amount: String, colors: WidgetColors, fontSize: TextUnit) {
        Text(
            amount,
            style = TextStyle(color = colors.ink, fontSize = fontSize, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier.padding(horizontal = 8.dp)
        )
    }

    /** Live rate (tap opens the app) with the swap button on the currency-pill side. */
    @Composable
    private fun RateRow(data: WidgetData, colors: WidgetColors, tiny: Boolean, pillFirst: Boolean, appRtl: Boolean, updatedLabel: String?) {
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            if (pillFirst) {
                SwapButton(colors, tiny)
                Spacer(GlanceModifier.width(6.dp))
                RateBox(data, colors, tiny, appRtl, updatedLabel, GlanceModifier.defaultWeight())
            } else {
                RateBox(data, colors, tiny, appRtl, updatedLabel, GlanceModifier.defaultWeight())
                Spacer(GlanceModifier.width(6.dp))
                SwapButton(colors, tiny)
            }
        }
    }

    @Composable
    private fun RateBox(
        data: WidgetData,
        colors: WidgetColors,
        tiny: Boolean,
        appRtl: Boolean,
        updatedLabel: String?,
        modifier: GlanceModifier,
    ) {
        val context = LocalContext.current
        // Text starts on the reading side of the app language, whatever the launcher direction.
        val align = if (appRtl) TextAlign.Right else TextAlign.Left
        Column(
            modifier = modifier
                .height(if (tiny) 34.dp else 44.dp)
                .background(colors.card)
                .cornerRadius(14.dp)
                .padding(horizontal = 12.dp)
                .clickable(actionStartActivity(MainActivity.openAppIntent(context))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                ltr("1 ${data.sourceCurrency} = ${formatRate(data.rate)} ${data.targetCurrency}"),
                style = TextStyle(
                    color = colors.ink,
                    fontSize = if (tiny) 12.sp else 13.sp,
                    fontWeight = FontWeight.Medium,
                    textAlign = align
                ),
                maxLines = 1,
                modifier = GlanceModifier.fillMaxWidth()
            )
            if (!tiny && updatedLabel != null) {
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
    private fun SwapButton(colors: WidgetColors, tiny: Boolean) {
        Box(
            modifier = GlanceModifier
                .size(if (tiny) 34.dp else 44.dp)
                .background(colors.accent)
                .cornerRadius(14.dp)
                .clickable(actionRunCallback<SwapAction>()),
            contentAlignment = Alignment.Center
        ) {
            Text("⇅", style = TextStyle(color = colors.onAccent, fontSize = if (tiny) 16.sp else 20.sp, fontWeight = FontWeight.Bold))
        }
    }

    @Composable
    private fun Keypad(colors: WidgetColors, mirrored: Boolean, gap: Dp, fontSize: TextUnit, modifier: GlanceModifier) {
        val rows = listOf(
            listOf("7", "8", "9"),
            listOf("4", "5", "6"),
            listOf("1", "2", "3"),
            listOf("C", "0", "⌫")
        )
        Column(modifier = modifier) {
            rows.forEachIndexed { rowIndex, row ->
                if (rowIndex > 0) Spacer(GlanceModifier.height(gap))
                Row(modifier = GlanceModifier.fillMaxWidth().defaultWeight()) {
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
