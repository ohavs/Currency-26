package com.example.widget

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
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
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.example.MainActivity
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
        val tiny = size.height < 250.dp
        val compact = size.height < 330.dp
        // RemoteViews mirrors rows in RTL locales; used to keep the keypad in 7-8-9 order.
        val isRtl = context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL

        val gap = if (tiny) 4.dp else 6.dp
        val converted = (data.amount.toDoubleOrNull() ?: 0.0) * data.rate
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
                forSource = true,
                background = colors.card,
                pillBackground = colors.cardSoft,
                colors = colors,
                tiny = tiny,
                amountSize = amountSize
            )
            Spacer(GlanceModifier.height(gap))
            RateRow(data = data, colors = colors, tiny = tiny)
            Spacer(GlanceModifier.height(gap))
            CurrencyCard(
                code = data.targetCurrency,
                amount = ltr(formatAmount(converted)),
                forSource = false,
                background = colors.highlight,
                pillBackground = colors.highlightSoft,
                colors = colors,
                tiny = tiny,
                amountSize = amountSize
            )
            Spacer(GlanceModifier.height(gap + 4.dp))
            Keypad(
                colors = colors,
                isRtl = isRtl,
                gap = gap,
                fontSize = if (tiny) 15.sp else if (compact) 19.sp else 24.sp,
                modifier = GlanceModifier.fillMaxWidth().defaultWeight()
            )
        }
    }

    /** Card with a tappable currency pill (opens the picker in the app) and the amount. */
    @Composable
    private fun CurrencyCard(
        code: String,
        amount: String,
        forSource: Boolean,
        background: ColorProvider,
        pillBackground: ColorProvider,
        colors: WidgetColors,
        tiny: Boolean,
        amountSize: TextUnit,
    ) {
        val context = LocalContext.current
        val info = getCurrencyInfo(code)
        Row(
            modifier = GlanceModifier
                .fillMaxWidth()
                .background(background)
                .cornerRadius(20.dp)
                .padding(if (tiny) 5.dp else 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = GlanceModifier
                    .background(pillBackground)
                    .cornerRadius(14.dp)
                    .padding(horizontal = 10.dp, vertical = if (tiny) 3.dp else 7.dp)
                    .clickable(actionStartActivity(MainActivity.pickCurrencyIntent(context, forSource))),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(info.flag, style = TextStyle(fontSize = if (tiny) 14.sp else 18.sp))
                Spacer(GlanceModifier.width(6.dp))
                Text(
                    code,
                    style = TextStyle(color = colors.ink, fontSize = if (tiny) 13.sp else 16.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(GlanceModifier.width(4.dp))
                Text("▾", style = TextStyle(color = colors.inkMuted, fontSize = 12.sp))
            }
            Spacer(GlanceModifier.defaultWeight())
            Text(
                amount,
                style = TextStyle(color = colors.ink, fontSize = amountSize, fontWeight = FontWeight.Bold),
                maxLines = 1,
                modifier = GlanceModifier.padding(horizontal = 8.dp)
            )
        }
    }

    @Composable
    private fun RateRow(data: WidgetData, colors: WidgetColors, tiny: Boolean) {
        val height = if (tiny) 34.dp else 44.dp
        val updatedAt = formatUpdatedAt(data.updatedAt)
        Row(modifier = GlanceModifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = GlanceModifier
                    .size(height)
                    .background(colors.accent)
                    .cornerRadius(14.dp)
                    .clickable(actionRunCallback<SwapAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text("⇅", style = TextStyle(color = colors.onAccent, fontSize = if (tiny) 16.sp else 20.sp, fontWeight = FontWeight.Bold))
            }
            Spacer(GlanceModifier.width(6.dp))
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .height(height)
                    .background(colors.card)
                    .cornerRadius(14.dp)
                    .padding(horizontal = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    ltr("1 ${data.sourceCurrency} = ${formatRate(data.rate)} ${data.targetCurrency}"),
                    style = TextStyle(color = colors.ink, fontSize = if (tiny) 12.sp else 13.sp, fontWeight = FontWeight.Medium),
                    maxLines = 1
                )
                if (!tiny && updatedAt != null) {
                    Text(
                        "עודכן $updatedAt",
                        style = TextStyle(color = colors.inkMuted, fontSize = 10.sp),
                        maxLines = 1
                    )
                }
            }
        }
    }

    @Composable
    private fun Keypad(colors: WidgetColors, isRtl: Boolean, gap: Dp, fontSize: TextUnit, modifier: GlanceModifier) {
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
                    val ordered = if (isRtl) row.reversed() else row
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
