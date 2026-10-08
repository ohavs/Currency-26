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

    private val _colorTheme = MutableStateFlow(prefs.getString("color_theme", "standard") ?: "standard")
    val colorTheme: StateFlow<String> = _colorTheme.asStateFlow()

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

    suspend fun refreshRates() {
        try {
            // Fetch rates base on USD
            val response = api.getLatestRates("USD")
            if (response.result.equals("success", ignoreCase = true)) {
                val timestamp = System.currentTimeMillis()
                val entities = response.rates.map { (code, rate) ->
                    ExchangeRateEntity(code, rate, timestamp)
                }
                dao.insertRates(entities)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun markCurrencyUsed(code: String) {
        dao.updateCurrencyUsage(CurrencyUsageEntity(code, System.currentTimeMillis()))
    }
}
