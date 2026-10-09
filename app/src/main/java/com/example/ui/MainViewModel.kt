package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.AppRelease
import com.example.data.AutoUpdateSettings
import com.example.data.CurrencyRepository
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
    val sourceAmountRaw: String = "1",
    val rates: Map<String, Double> = emptyMap(),
    val recentCurrencies: List<String> = emptyList(),
    val searchQuery: String = "",
    val selectingForSource: Boolean = true, // true if selecting source currency, false if target
    val showCurrencySelector: Boolean = false,
    val showSettings: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastRefreshFailed: Boolean = false,
    val lastUpdateTimestamp: Long = 0L
) {
    val sourceAmount: Double
        get() = sourceAmountRaw.toDoubleOrNull() ?: 0.0

    /** How many target units one source unit buys. */
    val rate: Double
        get() {
            val sourceRate = rates[sourceCurrency] ?: 1.0
            val targetRate = rates[targetCurrency] ?: 1.0
            return targetRate / sourceRate
        }

    val targetAmount: Double
        get() = sourceAmount * rate
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
        sourceAmountRaw = repository.getAmount()
    ))
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    val themeMode = repository.themeMode
    val colorTheme = repository.colorTheme
    val autoUpdate: StateFlow<AutoUpdateSettings> = repository.autoUpdate
    val checkAppUpdates: StateFlow<Boolean> = repository.checkAppUpdates

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
                sourceAmountRaw = repository.getAmount()
            )
        }
    }

    fun setThemeMode(mode: String) {
        repository.setThemeMode(mode)
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

    fun swapCurrencies() {
        val current = _state.value
        repository.setSourceCurrency(current.targetCurrency)
        repository.setTargetCurrency(current.sourceCurrency)
        _state.update {
            it.copy(sourceCurrency = current.targetCurrency, targetCurrency = current.sourceCurrency)
        }
        notifyWidgets()
    }

    fun openSettings() {
        _state.update { it.copy(showSettings = true) }
    }

    fun closeSettings() {
        _state.update { it.copy(showSettings = false) }
    }

    fun openCurrencySelector(isSource: Boolean) {
        _state.update {
            it.copy(
                showCurrencySelector = true,
                showSettings = false,
                selectingForSource = isSource,
                searchQuery = ""
            )
        }
    }

    fun closeCurrencySelector() {
        _state.update { it.copy(showCurrencySelector = false) }
    }

    fun selectCurrency(currencyCode: String) {
        val current = _state.value
        viewModelScope.launch {
            repository.markCurrencyUsed(currencyCode)
        }
        // Picking the currency that is already on the other side swaps the pair instead of showing X -> X.
        val otherSide = if (current.selectingForSource) current.targetCurrency else current.sourceCurrency
        val ownSide = if (current.selectingForSource) current.sourceCurrency else current.targetCurrency
        val newSource: String
        val newTarget: String
        if (current.selectingForSource) {
            newSource = currencyCode
            newTarget = if (currencyCode == otherSide) ownSide else current.targetCurrency
        } else {
            newTarget = currencyCode
            newSource = if (currencyCode == otherSide) ownSide else current.sourceCurrency
        }
        repository.setSourceCurrency(newSource)
        repository.setTargetCurrency(newTarget)
        _state.update {
            it.copy(sourceCurrency = newSource, targetCurrency = newTarget, showCurrencySelector = false)
        }
        notifyWidgets()
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}
