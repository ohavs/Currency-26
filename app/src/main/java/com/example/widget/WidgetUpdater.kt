package com.example.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.glance.GlanceId
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.appwidget.updateAll
import androidx.glance.state.PreferencesGlanceStateDefinition
import com.example.CurrencyApp
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

/** Everything a widget needs to draw itself, stored in its Glance state. */
data class WidgetData(
    val amount: String,
    val sourceCurrency: String,
    val targetCurrency: String,
    val rate: Double,
    /** Optional second target and its rate from the source currency. */
    val extraTargetCurrency: String?,
    val extraRate: Double,
    val themeMode: String,
    val colorTheme: String,
    val updatedAt: Long,
)

object WidgetStateKeys {
    val amount = stringPreferencesKey("amount")
    val source = stringPreferencesKey("source")
    val target = stringPreferencesKey("target")
    val rate = doublePreferencesKey("conversion_rate")
    val themeMode = stringPreferencesKey("theme_mode")
    val colorTheme = stringPreferencesKey("color_theme")
    val updatedAt = longPreferencesKey("updated_at")
    /** Empty string = no extra currency. */
    val extraTarget = stringPreferencesKey("target2")
    val extraRate = doublePreferencesKey("extra_rate")

    /** Float rate written by older versions; read until the widget is refreshed once. */
    val legacyRate = floatPreferencesKey("rate")
}

fun Preferences.toWidgetData() = WidgetData(
    amount = this[WidgetStateKeys.amount] ?: "1",
    sourceCurrency = this[WidgetStateKeys.source] ?: "USD",
    targetCurrency = this[WidgetStateKeys.target] ?: "ILS",
    rate = this[WidgetStateKeys.rate] ?: this[WidgetStateKeys.legacyRate]?.toDouble() ?: 1.0,
    extraTargetCurrency = this[WidgetStateKeys.extraTarget]?.takeIf { it.isNotEmpty() },
    extraRate = this[WidgetStateKeys.extraRate] ?: 1.0,
    themeMode = this[WidgetStateKeys.themeMode] ?: "system",
    colorTheme = this[WidgetStateKeys.colorTheme] ?: "sage",
    updatedAt = this[WidgetStateKeys.updatedAt] ?: 0L,
)

private suspend fun writeWidgetState(context: Context, ids: List<GlanceId>, data: WidgetData) {
    ids.forEach { id ->
        updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
            prefs.toMutablePreferences().apply {
                this[WidgetStateKeys.amount] = data.amount
                this[WidgetStateKeys.source] = data.sourceCurrency
                this[WidgetStateKeys.target] = data.targetCurrency
                this[WidgetStateKeys.rate] = data.rate
                this[WidgetStateKeys.extraTarget] = data.extraTargetCurrency.orEmpty()
                this[WidgetStateKeys.extraRate] = data.extraRate
                this[WidgetStateKeys.themeMode] = data.themeMode
                this[WidgetStateKeys.colorTheme] = data.colorTheme
                this[WidgetStateKeys.updatedAt] = data.updatedAt
            }
        }
    }
}

suspend fun updateWidgets(context: Context) {
    try {
        val appContext = context.applicationContext
        val repository = (appContext as CurrencyApp).repository

        val sourceId = repository.getSourceCurrency()
        val targetId = repository.getTargetCurrency()
        val extraId = repository.getExtraTargetCurrency()
        val sourceRate = repository.getRate(sourceId) ?: 1.0
        val targetRate = repository.getRate(targetId) ?: 1.0
        val extraRate = extraId?.let { repository.getRate(it) } ?: 1.0

        val data = WidgetData(
            amount = repository.getAmount(),
            sourceCurrency = sourceId,
            targetCurrency = targetId,
            rate = targetRate / sourceRate,
            extraTargetCurrency = extraId,
            extraRate = extraRate / sourceRate,
            themeMode = repository.themeMode.value,
            colorTheme = repository.colorTheme.value,
            updatedAt = repository.getLastUpdateTimestamp(),
        )

        val manager = GlanceAppWidgetManager(appContext)
        writeWidgetState(appContext, manager.getGlanceIds(LargeCurrencyWidget::class.java), data)
        writeWidgetState(appContext, manager.getGlanceIds(SmallCurrencyWidget::class.java), data)

        SmallCurrencyWidget().updateAll(appContext)
        LargeCurrencyWidget().updateAll(appContext)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun triggerWidgetUpdate(context: Context) {
    GlobalScope.launch {
        updateWidgets(context)
    }
}
