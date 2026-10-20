package com.example.widget

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
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
import com.example.premium.FREE_COLOR_THEME
import com.example.utils.AppLanguage
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    /** App language ("he", "en", "es" or "system"): widget texts and which side the currencies sit on. */
    val language: String,
    /** Premium is open (always in the GitHub build); locked, the widget shows only the way to unlock it. */
    val unlocked: Boolean = true,
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
    val language = stringPreferencesKey("language")
    val unlocked = booleanPreferencesKey("premium_unlocked")

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
    language = this[WidgetStateKeys.language] ?: AppLanguage.DEFAULT,
    unlocked = this[WidgetStateKeys.unlocked] ?: true,
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
                this[WidgetStateKeys.language] = data.language
                this[WidgetStateKeys.unlocked] = data.unlocked
            }
        }
    }
}

/**
 * One update at a time, each reading the settings when it runs: an older, slower update (say, one waiting on
 * the rate database) can never finish last and write a stale theme or pair over a newer one.
 */
private val updateLock = Mutex()

suspend fun updateWidgets(context: Context) = updateLock.withLock { updateWidgetsNow(context) }

private suspend fun updateWidgetsNow(context: Context) {
    try {
        val appContext = context.applicationContext
        val repository = (appContext as CurrencyApp).repository
        val unlocked = appContext.monetization.access.value.unlocked

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
            // Locked, the widget wears Classic like the app (the chosen set comes back with Premium).
            colorTheme = if (unlocked) repository.colorTheme.value else FREE_COLOR_THEME,
            updatedAt = repository.getLastUpdateTimestamp(),
            language = repository.language.value,
            unlocked = unlocked,
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

/**
 * Refreshes the widgets in the background. [settle] adds a second pass a moment later, for changes that must
 * not be missed (theme, colors, language): a widget whose Glance session is just closing can drop an update.
 */
@OptIn(DelicateCoroutinesApi::class)
fun triggerWidgetUpdate(context: Context, settle: Boolean = false) {
    GlobalScope.launch {
        updateWidgets(context)
        if (settle) {
            delay(1_500)
            updateWidgets(context)
        }
    }
}
