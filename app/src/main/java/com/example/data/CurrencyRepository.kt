package com.example.data

import android.content.Context
import com.example.utils.AppLanguage
import com.example.utils.CalcHistory
import com.example.utils.HistoryEntry
import androidx.room.Room
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class CurrencyRepository(
    val context: Context
) {
    private val database = Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java, "currency_database"
    ).build()

    private val prefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)

    private val api = ExchangeRateApi.create()
    private val dao = database.currencyDao()

    val allRatesFlow: Flow<Map<String, Double>> = dao.getAllRates().map { list ->
        list.associate { it.currencyCode to it.rateRelativeToUSD }
    }

    val lastUpdateTimestampFlow: Flow<Long> = dao.getAllRates().map { list ->
        list.maxOfOrNull { it.timestamp } ?: 0L
    }

    val recentCurrenciesFlow: Flow<List<String>> = dao.getRecentCurrencies().map { list ->
        list.map { it.currencyCode }
    }

    private val _themeMode = MutableStateFlow(prefs.getString("theme_mode", "system") ?: "system")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _colorTheme = MutableStateFlow(prefs.getString("color_theme", "sage") ?: "sage")
    val colorTheme: StateFlow<String> = _colorTheme.asStateFlow()

    private val _autoUpdate = MutableStateFlow(readAutoUpdate())
    val autoUpdate: StateFlow<AutoUpdateSettings> = _autoUpdate.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(AppLanguage.KEY, AppLanguage.DEFAULT) ?: AppLanguage.DEFAULT)
    /** App UI language: "he", "en", "es" or "system". */
    val language: StateFlow<String> = _language.asStateFlow()

    private val _checkAppUpdates = MutableStateFlow(prefs.getBoolean("check_app_updates", true))
    /** Look for a new app version on GitHub every time the app opens. */
    val checkAppUpdates: StateFlow<Boolean> = _checkAppUpdates.asStateFlow()

    fun getSourceCurrency(): String = prefs.getString("source", "USD") ?: "USD"
    fun getTargetCurrency(): String = prefs.getString("target", "ILS") ?: "ILS"
    fun getAmount(): String = prefs.getString("amount", "1") ?: "1"
    /** Optional second target currency; null when only one target is shown (the default). */
    fun getExtraTargetCurrency(): String? = prefs.getString("target2", null)

    fun setSourceCurrency(code: String) { prefs.edit().putString("source", code).apply() }
    fun setTargetCurrency(code: String) { prefs.edit().putString("target", code).apply() }
    fun setAmount(amount: String) { prefs.edit().putString("amount", amount).apply() }
    fun setExtraTargetCurrency(code: String?) {
        val editor = prefs.edit().putString("target2", code)
        // Remembered for good: the feature tour skips a feature the user has already found.
        if (code != null) editor.putBoolean("extra_currency_used", true)
        editor.apply()
    }

    fun extraCurrencyEverUsed(): Boolean = prefs.getBoolean("extra_currency_used", false)

    fun tourTimesShown(): Int = prefs.getInt("tour_times_shown", 0)
    fun tourLastShownAt(): Long = prefs.getLong("tour_last_shown_at", 0L)
    fun markTourShown(now: Long) {
        prefs.edit()
            .putInt("tour_times_shown", tourTimesShown() + 1)
            .putLong("tour_last_shown_at", now)
            .apply()
    }

    fun setThemeMode(mode: String) {
        prefs.edit().putString("theme_mode", mode).apply()
        _themeMode.value = mode
    }

    fun setColorTheme(theme: String) {
        prefs.edit().putString("color_theme", theme).apply()
        _colorTheme.value = theme
    }

    private fun readAutoUpdate(): AutoUpdateSettings {
        val defaults = AutoUpdateSettings()
        return AutoUpdateSettings(
            enabled = prefs.getBoolean("auto_update_enabled", defaults.enabled),
            intervalMinutes = prefs.getLong("auto_update_interval", defaults.intervalMinutes),
            wifiOnly = prefs.getBoolean("auto_update_wifi_only", defaults.wifiOnly),
            refreshOnOpen = prefs.getBoolean("refresh_on_open", defaults.refreshOnOpen),
        )
    }

    fun setAutoUpdate(settings: AutoUpdateSettings) {
        prefs.edit()
            .putBoolean("auto_update_enabled", settings.enabled)
            .putLong("auto_update_interval", settings.intervalMinutes)
            .putBoolean("auto_update_wifi_only", settings.wifiOnly)
            .putBoolean("refresh_on_open", settings.refreshOnOpen)
            .apply()
        _autoUpdate.value = settings
    }

    fun setLanguage(language: String) {
        // commit() so the activity recreated right after reads the new value.
        prefs.edit().putString(AppLanguage.KEY, language).putBoolean(AppLanguage.SUGGEST_KEY, false).commit()
        _language.value = language
    }

    /** A fresh install that has not yet been offered the language of the user's country. */
    fun languageSuggestionPending(): Boolean = prefs.getBoolean(AppLanguage.SUGGEST_KEY, false)

    fun markLanguageSuggestionDone() {
        prefs.edit().putBoolean(AppLanguage.SUGGEST_KEY, false).apply()
    }

    /** "Convert currency" was picked from another app's text selection at least once. */
    fun quickConvertUsed(): Boolean = prefs.getBoolean("quick_convert_used", false)
    fun markQuickConvertUsed() { prefs.edit().putBoolean("quick_convert_used", true).apply() }

    /**
     * The currency last picked in the quick-convert bubble, while the app's target is still the one it was picked
     * against: choosing another target in the app (or "open in app") brings the bubble back to the app's.
     */
    fun getQuickTarget(): String? =
        prefs.getString("quick_target", null)?.takeIf { prefs.getString("quick_target_base", null) == getTargetCurrency() }
    fun setQuickTarget(code: String) {
        val target = getTargetCurrency()
        val editor = prefs.edit()
        if (code == target) editor.remove("quick_target").remove("quick_target_base")
        else editor.putString("quick_target", code).putString("quick_target_base", target)
        editor.apply()
    }

    /** The tour card about converting selected text has been shown (once to everyone, it came in 5.0.15). */
    fun quickConvertTipSeen(): Boolean = prefs.getBoolean("quick_convert_tip_seen", false)
    fun markQuickConvertTipSeen() { prefs.edit().putBoolean("quick_convert_tip_seen", true).apply() }

    /** The calculator keys were left open (they stay open the next time the app starts). */
    fun calculatorOpen(): Boolean = prefs.getBoolean("calculator_open", false)
    fun setCalculatorOpen(open: Boolean) {
        val editor = prefs.edit().putBoolean("calculator_open", open)
        // Remembered for good: the feature tour skips the calculator once it was opened.
        if (open) editor.putBoolean("calculator_used", true)
        editor.apply()
    }
    fun calculatorUsed(): Boolean = prefs.getBoolean("calculator_used", false)

    /** The tour card about the calculator has been shown (once to everyone, it came in 5.0.17). */
    fun calculatorTipSeen(): Boolean = prefs.getBoolean("calculator_tip_seen", false)
    fun markCalculatorTipSeen() { prefs.edit().putBoolean("calculator_tip_seen", true).apply() }

    /** Past conversions and calculations, newest first. */
    fun getHistory(): List<HistoryEntry> = CalcHistory.decode(prefs.getString("history", null))
    fun setHistory(entries: List<HistoryEntry>) { prefs.edit().putString("history", CalcHistory.encode(entries)).apply() }

    /** The version whose "new version" banner was closed: it stays closed for that version. */
    var dismissedUpdateVersion: String?
        get() = prefs.getString("dismissed_update", null)
        set(value) { prefs.edit().putString("dismissed_update", value).apply() }

    fun setCheckAppUpdates(enabled: Boolean) {
        prefs.edit().putBoolean("check_app_updates", enabled).apply()
        _checkAppUpdates.value = enabled
    }

    /** Downloads the latest USD-based rates. Returns true when the local rates were updated. */
    suspend fun refreshRates(): Boolean {
        return try {
            val response = api.getLatestRates("USD")
            if (response.result.equals("success", ignoreCase = true)) {
                val timestamp = System.currentTimeMillis()
                val entities = response.rates.map { (code, rate) ->
                    ExchangeRateEntity(code, rate, timestamp)
                }
                dao.insertRates(entities)
                true
            } else {
                false
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun getRate(code: String): Double? = dao.getRate(code)?.rateRelativeToUSD

    suspend fun getLastUpdateTimestamp(): Long = dao.getLastUpdateTimestamp() ?: 0L

    suspend fun markCurrencyUsed(code: String) {
        dao.updateCurrencyUsage(CurrencyUsageEntity(code, System.currentTimeMillis()))
    }
}
