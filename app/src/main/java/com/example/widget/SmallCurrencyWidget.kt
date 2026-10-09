package com.example.widget

import android.content.Context
import android.view.View
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
        // RemoteViews mirrors rows in RTL locales; used to keep the currency pills on the right.
        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL
        val amount = data.amount.toDoubleOrNull() ?: 0.0
        // A third line only when the widget is tall enough to stay readable.
        val extra = data.extraTargetCurrency?.takeIf { size.height >= 110.dp }
        val gap = if (compact) 3.dp else 6.dp

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(colors.background)
                .cornerRadius(24.dp)
                .padding(if (compact) 6.dp else 10.dp)
                .clickable(actionStartActivity(MainActivity.openAppIntent(context))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CurrencyLine(
                code = data.sourceCurrency,
                amount = ltr(formatAmountInput(data.amount)),
                slot = CurrencySlot.SOURCE,
                background = colors.card,
                pillBackground = colors.cardSoft,
                colors = colors,
                compact = compact,
                isRtl = isRtl
            )
            Spacer(GlanceModifier.height(gap))
            CurrencyLine(
                code = data.targetCurrency,
                amount = ltr(formatAmount(amount * data.rate)),
                slot = CurrencySlot.TARGET,
                background = colors.highlight,
                pillBackground = colors.highlightSoft,
                colors = colors,
                compact = compact,
                isRtl = isRtl
            )
            if (extra != null) {
                Spacer(GlanceModifier.height(gap))
                CurrencyLine(
                    code = extra,
                    amount = ltr(formatAmount(amount * data.extraRate)),
                    slot = CurrencySlot.EXTRA_TARGET,
                    background = colors.highlightSoft,
                    pillBackground = colors.highlight,
                    colors = colors,
                    compact = compact,
                    isRtl = isRtl
                )
            }
            if (size.height >= (if (extra != null) 190.dp else 150.dp)) {
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

    /** One currency row: amount on the left, flag/code pill (opens the currency picker) on the right. */
    @Composable
    private fun CurrencyLine(
        code: String,
        amount: String,
        slot: CurrencySlot,
        background: ColorProvider,
        pillBackground: ColorProvider,
        colors: WidgetColors,
        compact: Boolean,
        isRtl: Boolean,
    ) {
        // In the smallest size the cards would not fit, so only the pills keep a background.
        val rowModifier = if (compact) {
            GlanceModifier.fillMaxWidth()
        } else {
            GlanceModifier.fillMaxWidth().background(background).cornerRadius(16.dp).padding(5.dp)
        }
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            if (isRtl) {
                CurrencyPill(code, slot, pillBackground, colors, compact)
                Spacer(GlanceModifier.defaultWeight())
                AmountText(amount, colors, compact)
            } else {
                AmountText(amount, colors, compact)
                Spacer(GlanceModifier.defaultWeight())
                CurrencyPill(code, slot, pillBackground, colors, compact)
            }
        }
    }

    @Composable
    private fun CurrencyPill(code: String, slot: CurrencySlot, background: ColorProvider, colors: WidgetColors, compact: Boolean) {
        val context = LocalContext.current
        Row(
            modifier = GlanceModifier
                .background(background)
                .cornerRadius(12.dp)
                .padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 5.dp)
                .clickable(actionStartActivity(MainActivity.pickCurrencyIntent(context, slot))),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(getCurrencyInfo(code).flag, style = TextStyle(fontSize = if (compact) 12.sp else 15.sp))
            Spacer(GlanceModifier.width(4.dp))
            Text(
                code,
                style = TextStyle(color = colors.ink, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold)
            )
        }
    }

    @Composable
    private fun AmountText(amount: String, colors: WidgetColors, compact: Boolean) {
        Text(
            amount,
            style = TextStyle(color = colors.ink, fontSize = if (compact) 13.sp else 17.sp, fontWeight = FontWeight.Bold),
            maxLines = 1,
            modifier = GlanceModifier.padding(horizontal = 6.dp)
        )
    }
}
