package com.example.data

import android.content.Context
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

    fun getSourceCurrency(): String = prefs.getString("source", "USD") ?: "USD"
    fun getTargetCurrency(): String = prefs.getString("target", "ILS") ?: "ILS"
    fun getAmount(): String = prefs.getString("amount", "1") ?: "1"

    fun setSourceCurrency(code: String) { prefs.edit().putString("source", code).apply() }
    fun setTargetCurrency(code: String) { prefs.edit().putString("target", code).apply() }
    fun setAmount(amount: String) { prefs.edit().putString("amount", amount).apply() }

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
