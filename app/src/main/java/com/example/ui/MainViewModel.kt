package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppRelease
import com.example.data.AutoUpdateSettings
import com.example.data.CurrencyRepository
import com.example.data.CurrencySlot
import com.example.update.AppUpdater
import com.example.utils.applyKeypadKey
import com.example.utils.isNewerVersion
import java.io.File
import com.example.widget.triggerWidgetUpdate
import com.example.workers.RateUpdateScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CalculatorState(
    val sourceCurrency: String = "USD",
    val targetCurrency: String = "ILS",
    /** Optional second target; null = single conversion (the default). */
    val extraTargetCurrency: String? = null,
    val sourceAmountRaw: String = "1",
    val rates: Map<String, Double> = emptyMap(),
    val recentCurrencies: List<String> = emptyList(),
    val searchQuery: String = "",
    val pickerSlot: CurrencySlot = CurrencySlot.SOURCE,
    val showCurrencySelector: Boolean = false,
    val showSettings: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastRefreshFailed: Boolean = false,
    val lastUpdateTimestamp: Long = 0L
) {
    val sourceAmount: Double
        get() = sourceAmountRaw.toDoubleOrNull() ?: 0.0

    /** How many [code] units one source unit buys. */
    fun rateTo(code: String): Double = (rates[code] ?: 1.0) / (rates[sourceCurrency] ?: 1.0)

    /** How many target units one source unit buys. */
    val rate: Double
        get() = rateTo(targetCurrency)

    val targetAmount: Double
        get() = sourceAmount * rate

    val extraTargetAmount: Double?
        get() = extraTargetCurrency?.let { sourceAmount * rateTo(it) }

    /** The currency currently assigned to [slot], or null for an empty extra slot. */
    fun currencyIn(slot: CurrencySlot): String? = when (slot) {
        CurrencySlot.SOURCE -> sourceCurrency
        CurrencySlot.TARGET -> targetCurrency
        CurrencySlot.EXTRA_TARGET -> extraTargetCurrency
    }
}

/** In-app update flow: check GitHub Releases -> download the APK -> hand it to the system installer. */
data class AppUpdateState(
    val installedVersion: String,
    val checking: Boolean = false,
    /** Checked at least once this session (to tell "up to date" from "unknown"). */
    val checked: Boolean = false,
    /** A newer release than the installed one, if any. */
    val available: AppRelease? = null,
    /** 0..1 while the APK is downloading. */
    val downloadProgress: Float? = null,
    val downloadedApk: File? = null,
    val failed: Boolean = false,
    /** The user was sent to allow installing updates from this app. */
    val needsInstallPermission: Boolean = false,
    val bannerDismissed: Boolean = false,
)

class MainViewModel(
    private val repository: CurrencyRepository,
    private val updater: AppUpdater,
) : ViewModel() {
    private val _state = MutableStateFlow(CalculatorState(
        sourceCurrency = repository.getSourceCurrency(),
        targetCurrency = repository.getTargetCurrency(),
        extraTargetCurrency = repository.getExtraTargetCurrency(),
        sourceAmountRaw = repository.getAmount()
    ))
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    val themeMode = repository.themeMode
    val colorTheme = repository.colorTheme
    val autoUpdate: StateFlow<AutoUpdateSettings> = repository.autoUpdate
    val checkAppUpdates: StateFlow<Boolean> = repository.checkAppUpdates
    val language: StateFlow<String> = repository.language

    private val _appUpdate = MutableStateFlow(AppUpdateState(installedVersion = BuildConfig.VERSION_NAME))
    val appUpdate: StateFlow<AppUpdateState> = _appUpdate.asStateFlow()

    val canPinWidget: Boolean
        get() {
            if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O) return false
            return android.appwidget.AppWidgetManager.getInstance(repository.context).isRequestPinAppWidgetSupported
        }

    init {
        viewModelScope.launch {
            repository.allRatesFlow.collect { rates ->
                _state.update { it.copy(rates = rates) }
            }
        }
        viewModelScope.launch {
            repository.recentCurrenciesFlow.collect { recent ->
                _state.update { it.copy(recentCurrencies = recent) }
            }
        }
        viewModelScope.launch {
            repository.lastUpdateTimestampFlow.collect { timestamp ->
                _state.update { it.copy(lastUpdateTimestamp = timestamp) }
            }
        }
        viewModelScope.launch {
            // Always fetch when there are no rates yet, otherwise only if the user wants a refresh on open.
            val neverUpdated = repository.getLastUpdateTimestamp() == 0L
            if (neverUpdated || repository.autoUpdate.value.refreshOnOpen) {
                refreshRates()
            }
        }
        if (repository.checkAppUpdates.value) {
            checkForAppUpdate()
        }
    }

    fun checkForAppUpdate() {
        if (_appUpdate.value.checking || _appUpdate.value.downloadProgress != null) return
        _appUpdate.update { it.copy(checking = true, failed = false) }
        viewModelScope.launch {
            val release = runCatching { updater.fetchLatestRelease() }
            _appUpdate.update { current ->
                val latest = release.getOrNull()
                current.copy(
                    checking = false,
                    checked = true,
                    failed = release.isFailure,
                    available = latest?.takeIf { isNewerVersion(it.versionName, current.installedVersion) }
                        ?: current.available.takeIf { release.isFailure },
                )
            }
        }
    }

    /** Downloads the available release (once) and opens the system installer for it. */
    fun startAppUpdate() {
        val current = _appUpdate.value
        val release = current.available ?: return
        if (current.downloadProgress != null) return

        if (!updater.canInstallPackages()) {
            _appUpdate.update { it.copy(needsInstallPermission = true) }
            repository.context.startActivity(updater.installPermissionIntent())
            return
        }

        val downloaded = current.downloadedApk
        if (downloaded != null && downloaded.exists()) {
            repository.context.startActivity(updater.installIntent(downloaded))
            return
        }

        _appUpdate.update { it.copy(downloadProgress = 0f, failed = false, needsInstallPermission = false) }
        viewModelScope.launch {
            val result = runCatching {
                updater.download(release) { progress ->
                    _appUpdate.update { it.copy(downloadProgress = progress) }
                }
            }
            val apk = result.getOrNull()
            _appUpdate.update { it.copy(downloadProgress = null, downloadedApk = apk, failed = apk == null) }
            if (apk != null) {
                repository.context.startActivity(updater.installIntent(apk))
            }
        }
    }

    private val _legacyAppInstalled = MutableStateFlow(false)
    /** The old AI Studio build is still installed next to this app (checked on every resume). */
    val legacyAppInstalled: StateFlow<Boolean> = _legacyAppInstalled.asStateFlow()

    fun setLegacyAppInstalled(installed: Boolean) {
        _legacyAppInstalled.value = installed
    }

    fun dismissUpdateBanner() {
        _appUpdate.update { it.copy(bannerDismissed = true) }
    }

    fun setCheckAppUpdates(enabled: Boolean) {
        repository.setCheckAppUpdates(enabled)
        if (enabled && !_appUpdate.value.checked) checkForAppUpdate()
    }

    private fun notifyWidgets() = triggerWidgetUpdate(repository.context)

    fun refreshRates() {
        if (_state.value.isRefreshing) return
        _state.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            val success = repository.refreshRates()
            if (success) notifyWidgets()
            _state.update { it.copy(isRefreshing = false, lastRefreshFailed = !success) }
        }
    }

    /** Picks up changes made from the widget (amount, swap) while the app was in the background. */
    fun reloadSelection() {
        _state.update {
            it.copy(
                sourceCurrency = repository.getSourceCurrency(),
                targetCurrency = repository.getTargetCurrency(),
                extraTargetCurrency = repository.getExtraTargetCurrency(),
                sourceAmountRaw = repository.getAmount()
            )
        }
    }

    fun setThemeMode(mode: String) {
        repository.setThemeMode(mode)
        notifyWidgets()
    }

    fun setLanguage(language: String) {
        if (language == repository.language.value) return
        repository.setLanguage(language)
        notifyWidgets()
    }

    fun setColorTheme(theme: String) {
        repository.setColorTheme(theme)
        notifyWidgets()
    }

    private fun updateAutoUpdate(transform: (AutoUpdateSettings) -> AutoUpdateSettings) {
        val settings = transform(repository.autoUpdate.value)
        repository.setAutoUpdate(settings)
        RateUpdateScheduler.apply(repository.context, settings, replaceExisting = true)
    }

    fun setAutoUpdateEnabled(enabled: Boolean) = updateAutoUpdate { it.copy(enabled = enabled) }

    fun setAutoUpdateInterval(minutes: Long) = updateAutoUpdate { it.copy(intervalMinutes = minutes) }

    fun setAutoUpdateWifiOnly(wifiOnly: Boolean) = updateAutoUpdate { it.copy(wifiOnly = wifiOnly) }

    fun setRefreshOnOpen(refreshOnOpen: Boolean) = updateAutoUpdate { it.copy(refreshOnOpen = refreshOnOpen) }

    fun addWidgetToHomeScreen() {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(repository.context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = android.content.ComponentName(repository.context, com.example.widget.LargeCurrencyWidgetReceiver::class.java)
                appWidgetManager.requestPinAppWidget(provider, null, null)
            }
        }
    }

    fun onKeypadPress(key: String) {
        val current = _state.value.sourceAmountRaw
        val next = applyKeypadKey(current, key)
        if (next == current) return
        repository.setAmount(next)
        _state.update { it.copy(sourceAmountRaw = next) }
        notifyWidgets()
    }

    fun swapCurrencies() = swapSlots(CurrencySlot.SOURCE, CurrencySlot.TARGET)

    /** Exchanges the currencies shown in two slots (swap button, or dragging one row onto another). */
    fun swapSlots(a: CurrencySlot, b: CurrencySlot) {
        if (a == b) return
        val current = _state.value
        val codeA = current.currencyIn(a) ?: return
        val codeB = current.currencyIn(b) ?: return
        val slots = CurrencySlot.values().associateWith { current.currencyIn(it) }.toMutableMap()
        slots[a] = codeB
        slots[b] = codeA
        applySlots(slots)
    }

    /** Persists the slot assignment, updates the screen and the widgets. */
    private fun applySlots(slots: Map<CurrencySlot, String?>) {
        val newSource = slots.getValue(CurrencySlot.SOURCE)!!
        val newTarget = slots.getValue(CurrencySlot.TARGET)!!
        val newExtra = slots[CurrencySlot.EXTRA_TARGET]
        repository.setSourceCurrency(newSource)
        repository.setTargetCurrency(newTarget)
        repository.setExtraTargetCurrency(newExtra)
        _state.update {
            it.copy(
                sourceCurrency = newSource,
                targetCurrency = newTarget,
                extraTargetCurrency = newExtra,
                showCurrencySelector = false
            )
        }
        notifyWidgets()
    }

    fun openSettings() {
        _state.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _state.update { it.copy(showSettings = false) }
    }

    fun openCurrencySelector(slot: CurrencySlot) {
        _state.update {
            it.copy(
                showCurrencySelector = true,
                showSettings = false,
                pickerSlot = slot,
                searchQuery = ""
            )
        }
    }

    fun closeCurrencySelector() {
        _state.update { it.copy(showCurrencySelector = false) }
    }

    fun selectCurrency(currencyCode: String) {
        val current = _state.value
        val slot = current.pickerSlot
        val slots = CurrencySlot.values().associateWith { current.currencyIn(it) }.toMutableMap()
        val ownCode = slots[slot]
        // Picking a currency that is already shown elsewhere swaps the two instead of showing it twice.
        val clash = slots.entries.firstOrNull { it.key != slot && it.value == currencyCode }?.key
        if (clash != null) {
            if (ownCode == null) {
                closeCurrencySelector()
                return
            }
            slots[clash] = ownCode
        }
        slots[slot] = currencyCode

        viewModelScope.launch {
            repository.markCurrencyUsed(currencyCode)
        }
        applySlots(slots)
    }

    fun removeExtraTarget() {
        repository.setExtraTargetCurrency(null)
        _state.update { it.copy(extraTargetCurrency = null, showCurrencySelector = false) }
        notifyWidgets()
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}
