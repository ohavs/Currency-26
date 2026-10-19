package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf

/** Signature-compatible stand-in for the Room/Retrofit backed repository. */
class CurrencyRepository(val context: android.content.Context) {
    val allRatesFlow: Flow<Map<String, Double>> = flowOf(emptyMap())
    val lastUpdateTimestampFlow: Flow<Long> = flowOf(0L)
    val recentCurrenciesFlow: Flow<List<String>> = flowOf(emptyList())
    val themeMode: StateFlow<String> = MutableStateFlow("system")
    val colorTheme: StateFlow<String> = MutableStateFlow("sage")
    val autoUpdate: StateFlow<AutoUpdateSettings> = MutableStateFlow(AutoUpdateSettings())
    val checkAppUpdates: StateFlow<Boolean> = MutableStateFlow(true)
    val language: StateFlow<String> = MutableStateFlow("he")
    fun setLanguage(language: String) {}
    fun languageSuggestionPending(): Boolean = false
    fun markLanguageSuggestionDone() {}
    fun quickConvertUsed(): Boolean = false
    fun markQuickConvertUsed() {}
    fun quickConvertTipSeen(): Boolean = false
    fun markQuickConvertTipSeen() {}
    fun setCheckAppUpdates(enabled: Boolean) {}
    fun getSourceCurrency(): String = "USD"
    fun getTargetCurrency(): String = "ILS"
    fun getAmount(): String = "1"
    fun getExtraTargetCurrency(): String? = null
    fun setExtraTargetCurrency(code: String?) {}
    fun extraCurrencyEverUsed(): Boolean = false
    fun tourTimesShown(): Int = 0
    fun tourLastShownAt(): Long = 0L
    fun markTourShown(now: Long) {}
    fun setSourceCurrency(code: String) {}
    fun setTargetCurrency(code: String) {}
    fun setAmount(amount: String) {}
    fun setThemeMode(mode: String) {}
    fun setColorTheme(theme: String) {}
    fun setAutoUpdate(settings: AutoUpdateSettings) {}
    suspend fun refreshRates(): Boolean = true
    suspend fun getRate(code: String): Double? = null
    suspend fun getLastUpdateTimestamp(): Long = 0L
    suspend fun markCurrencyUsed(code: String) {}
}
