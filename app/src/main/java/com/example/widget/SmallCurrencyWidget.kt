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
        val converted = (data.amount.toDoubleOrNull() ?: 0.0) * data.rate

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
                forSource = true,
                background = colors.card,
                pillBackground = colors.cardSoft,
                colors = colors,
                compact = compact
            )
            Spacer(GlanceModifier.height(if (compact) 3.dp else 6.dp))
            CurrencyLine(
                code = data.targetCurrency,
                amount = ltr(formatAmount(converted)),
                forSource = false,
                background = colors.highlight,
                pillBackground = colors.highlightSoft,
                colors = colors,
                compact = compact
            )
            if (size.height >= 150.dp) {
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

    /** One currency row; tapping the flag/code pill opens the currency picker for that side. */
    @Composable
    private fun CurrencyLine(
        code: String,
        amount: String,
        forSource: Boolean,
        background: ColorProvider,
        pillBackground: ColorProvider,
        colors: WidgetColors,
        compact: Boolean,
    ) {
        val context = LocalContext.current
        val info = getCurrencyInfo(code)
        // In the smallest size the cards would not fit, so only the pills keep a background.
        val rowModifier = if (compact) {
            GlanceModifier.fillMaxWidth()
        } else {
            GlanceModifier.fillMaxWidth().background(background).cornerRadius(16.dp).padding(5.dp)
        }
        Row(modifier = rowModifier, verticalAlignment = Alignment.CenterVertically) {
            Row(
                modifier = GlanceModifier
                    .background(pillBackground)
                    .cornerRadius(12.dp)
                    .padding(horizontal = 8.dp, vertical = if (compact) 2.dp else 5.dp)
                    .clickable(actionStartActivity(MainActivity.pickCurrencyIntent(context, forSource))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(info.flag, style = TextStyle(fontSize = if (compact) 12.sp else 15.sp))
                Spacer(GlanceModifier.width(4.dp))
                Text(
                    code,
                    style = TextStyle(color = colors.ink, fontSize = if (compact) 12.sp else 14.sp, fontWeight = FontWeight.Bold)
                )
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(
                amount,
                style = TextStyle(color = colors.ink, fontSize = if (compact) 13.sp else 17.sp, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.padding(horizontal = 6.dp)
            )
        }
    }
}
