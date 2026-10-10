package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.Preferences
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.SizeMode
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
import com.example.data.CurrencySlot
import com.example.utils.AppLanguage
import com.example.utils.amountSymbol
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatRate
import com.example.utils.getCurrencyInfo
import com.example.utils.symbolLeads

class SmallCurrencyWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = SizeMode.Exact

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            SmallWidgetContent(currentState<Preferences>().toWidgetData())
        }
    }

    @Composable
    private fun SmallWidgetContent(data: WidgetData) {
        val context = LocalContext.current
        val size = LocalSize.current
        val colors = WidgetColors(data.themeMode, data.colorTheme)
        val compact = size.height < 90.dp
        // Flags on the reading side of the app language; the launcher mirrors rows when the device is RTL.
        val launcherRtl = AppLanguage.isLauncherRtl()
        val pillFirst = AppLanguage.isRtl(data.language) == launcherRtl
        val amount = data.amount.toDoubleOrNull() ?: 0.0
        // A third line only when the widget is tall enough to stay readable.
        val extra = data.extraTargetCurrency?.takeIf { size.height >= 110.dp }
        val gap = if (compact) 3.dp else 6.dp
        val showRate = size.height >= (if (extra != null) 190.dp else 150.dp)
        // Amounts get the space: as large as the line height allows and their width fits next to the flag.
        val lines = if (extra != null) 3 else 2
        val padding = if (compact) 6.dp else 10.dp
        val lineHeight = (size.height - padding * 2 - gap * (lines - 1) - (if (showRate) 24.dp else 0.dp)) / lines
        val maxAmountSp = (lineHeight.value * 0.55f).coerceIn(13f, 34f)
        val flagSp = if (compact) 15f else (lineHeight.value * 0.45f).coerceIn(17f, 30f)
        val amountWidth = size.width.value - padding.value * 2 - (if (compact) 0f else 10f) - (flagSp * 1.3f + 8f) - 12f
        val sourceText = formatAmountInput(data.amount)
        val targetText = formatAmount(amount * data.rate)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(24.dp)
                .padding(padding)
                .clickable(actionStartActivity(MainActivity.openAppIntent(context))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyLine(
                code = data.sourceCurrency,
                amount = sourceText,
                fontSize = fitAmountSp(sourceText, amountWidth, maxAmountSp, minSp = 12f, symbol = amountSymbol(data.sourceCurrency)),
                slot = CurrencySlot.SOURCE,
                background = colors.card,
                colors = colors,
                compact = compact,
                flagSp = flagSp,
                pillFirst = pillFirst,
                launcherRtl = launcherRtl
            )
            Spacer(GlanceModifier.height(gap))
            CurrencyLine(
                code = data.targetCurrency,
                amount = targetText,
                fontSize = fitAmountSp(targetText, amountWidth, maxAmountSp, minSp = 12f, symbol = amountSymbol(data.targetCurrency)),
                slot = CurrencySlot.TARGET,
                background = colors.highlight,
                colors = colors,
                compact = compact,
                flagSp = flagSp,
                pillFirst = pillFirst,
                launcherRtl = launcherRtl
            )
            if (extra != null) {
                val extraText = formatAmount(amount * data.extraRate)
                Spacer(GlanceModifier.height(gap))
                // The optional currency gets the lighter card, like in the app.
                CurrencyLine(
                    code = extra,
                    amount = extraText,
                    fontSize = fitAmountSp(extraText, amountWidth, maxAmountSp, minSp = 12f, symbol = amountSymbol(extra)),
                    slot = CurrencySlot.EXTRA_TARGET,
                    background = colors.cardSoft,
                    colors = colors,
                    compact = compact,
                    flagSp = flagSp,
                    pillFirst = pillFirst,
                    launcherRtl = launcherRtl
                )
            }
            if (showRate) {
                Spacer(GlanceModifier.height(8.dp))
                Text(
                    ltr("1 ${data.sourceCurrency} = ${formatRate(data.rate)} ${data.targetCurrency}"),
                    style = TextStyle(color = colors.inkMuted, fontSize = 11.sp, textAlign = TextAlign.Center),
                    maxLines = 1,
                    modifier = GlanceModifier.fillMaxWidth()
                )
            }
        }
    }

    /** One currency row: the flag (opens the currency picker) on the reading side, symbol and amount on the other. */
    @Composable
    private fun CurrencyLine(
        code: String,
        amount: String,
        fontSize: Float,
        slot: CurrencySlot,
        background: ColorProvider,
        colors: WidgetColors,
        compact: Boolean,
        flagSp: Float,
        pillFirst: Boolean,
        launcherRtl: Boolean,
    ) {
        val context = LocalContext.current
        val symbol = amountSymbol(code)
        // In the smallest size the cards would not fit, so the lines go without a background.
        val rowModifier = if (compact) {
            GlanceModifier.fillMaxWidth()
        } else {
            GlanceModifier.fillMaxWidth().background(background).cornerRadius(16.dp).padding(5.dp)
        }
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            val flag: @Composable () -> Unit = {
                Text(
                    getCurrencyInfo(code).flag,
                    style = TextStyle(fontSize = flagSp.sp),
                    modifier = GlanceModifier
                        .clickable(actionStartActivity(MainActivity.pickCurrencyIntent(context, slot)))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
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
            val small: @Composable () -> Unit = { Spacer(GlanceModifier.width(3.dp)) }
            val edge: @Composable () -> Unit = { Spacer(GlanceModifier.width(6.dp)) }
            val flexible: @Composable () -> Unit = { Spacer(GlanceModifier.defaultWeight()) }
            // Symbol and amount always read left to right on screen ("$ 1,250", "1,250 Ft"); RTL launchers mirror rows.
            val onScreen = if (symbolLeads(symbol)) listOf(symbolText, small, amountText) else listOf(amountText, small, symbolText)
            val amountGroup = if (launcherRtl) onScreen.reversed() else onScreen
            val items = if (pillFirst) listOf(flag, flexible) + amountGroup + edge else listOf(edge) + amountGroup + listOf(flexible, flag)
            items.forEach { it() }
        }
    }
}
