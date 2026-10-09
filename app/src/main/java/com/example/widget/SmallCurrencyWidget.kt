package com.example.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.TextUnit
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
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatRate
import com.example.utils.getCurrencyInfo

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
        // Pills on the reading side of the app language; the launcher mirrors rows when the device is RTL.
        val pillFirst = AppLanguage.isRtl(data.language) == AppLanguage.isLauncherRtl()
        val amount = data.amount.toDoubleOrNull() ?: 0.0
        // A third line only when the widget is tall enough to stay readable.
        val extra = data.extraTargetCurrency?.takeIf { size.height >= 110.dp }
        val gap = if (compact) 3.dp else 6.dp
        val showRate = size.height >= (if (extra != null) 190.dp else 150.dp)
        // Amounts get the space: as large as the line height allows and their width fits (next to a ~64dp pill).
        val lines = if (extra != null) 3 else 2
        val padding = if (compact) 6.dp else 10.dp
        val lineHeight = (size.height - padding * 2 - gap * (lines - 1) - (if (showRate) 24.dp else 0.dp)) / lines
        val maxAmountSp = (lineHeight.value * 0.55f).coerceIn(13f, 34f)
        val amountWidth = size.width.value - padding.value * 2 - (if (compact) 0f else 10f) - 64f
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
                fontSize = fitAmountSp(sourceText, amountWidth, maxAmountSp, minSp = 12f).sp,
                slot = CurrencySlot.SOURCE,
                background = colors.card,
                pillBackground = colors.cardSoft,
                colors = colors,
                compact = compact,
                pillFirst = pillFirst
            )
            Spacer(GlanceModifier.height(gap))
            CurrencyLine(
                code = data.targetCurrency,
                amount = targetText,
                fontSize = fitAmountSp(targetText, amountWidth, maxAmountSp, minSp = 12f).sp,
                slot = CurrencySlot.TARGET,
                background = colors.highlight,
                pillBackground = colors.highlightSoft,
                colors = colors,
                compact = compact,
                pillFirst = pillFirst
            )
            if (extra != null) {
                val extraText = formatAmount(amount * data.extraRate)
                Spacer(GlanceModifier.height(gap))
                // The optional currency gets the lighter card, like in the app.
                CurrencyLine(
                    code = extra,
                    amount = extraText,
                    fontSize = fitAmountSp(extraText, amountWidth, maxAmountSp, minSp = 12f).sp,
                    slot = CurrencySlot.EXTRA_TARGET,
                    background = colors.cardSoft,
                    pillBackground = colors.card,
                    colors = colors,
                    compact = compact,
                    pillFirst = pillFirst
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

    /** One currency row: amount on one side, flag/code pill (opens the currency picker) on the reading side. */
    @Composable
    private fun CurrencyLine(
        code: String,
        amount: String,
        fontSize: TextUnit,
        slot: CurrencySlot,
        background: ColorProvider,
        pillBackground: ColorProvider,
        colors: WidgetColors,
        compact: Boolean,
        pillFirst: Boolean,
    ) {
        // In the smallest size the cards would not fit, so only the pills keep a background.
        val rowModifier = if (compact) {
            GlanceModifier.fillMaxWidth()
        } else {
            GlanceModifier.fillMaxWidth().background(background).cornerRadius(16.dp).padding(5.dp)
        }
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            if (pillFirst) {
                CurrencyPill(code, slot, pillBackground, colors, compact, flagFirst = true)
                Spacer(GlanceModifier.defaultWeight())
                AmountText(amount, colors, fontSize)
            } else {
                AmountText(amount, colors, fontSize)
                Spacer(GlanceModifier.defaultWeight())
                CurrencyPill(code, slot, pillBackground, colors, compact, flagFirst = false)
            }
        }
    }

    /** Flag and currency symbol (the code when the currency has no symbol). */
    @Composable
    private fun CurrencyPill(
        code: String,
        slot: CurrencySlot,
        background: ColorProvider,
        colors: WidgetColors,
        compact: Boolean,
        flagFirst: Boolean,
    ) {
        val context = LocalContext.current
        val info = getCurrencyInfo(code)
        val symbol = info.symbol.ifEmpty { code }
        Row(
            modifier = GlanceModifier
                .background(background)
                .cornerRadius(12.dp)
                .padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 5.dp)
                .clickable(actionStartActivity(MainActivity.pickCurrencyIntent(context, slot))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val flagStyle = TextStyle(fontSize = if (compact) 12.sp else 15.sp)
            val symbolStyle = TextStyle(color = colors.ink, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold)
            Text(if (flagFirst) info.flag else symbol, style = if (flagFirst) flagStyle else symbolStyle)
            Spacer(GlanceModifier.width(4.dp))
            Text(if (flagFirst) symbol else info.flag, style = if (flagFirst) symbolStyle else flagStyle)
        }
    }

    @Composable
    private fun AmountText(amount: String, colors: WidgetColors, fontSize: TextUnit) {
        Text(
            ltr(amount),
            style = TextStyle(color = colors.ink, fontSize = fontSize, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier.padding(horizontal = 6.dp)
        )
    }
}
