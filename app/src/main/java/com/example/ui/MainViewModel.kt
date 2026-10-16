package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.R
import com.example.data.AppRelease
import com.example.data.AutoUpdateSettings
import com.example.data.CurrencyRepository
import com.example.data.CurrencySlot
import com.example.update.AppUpdater
import com.example.utils.AppLanguage
import com.example.utils.CountryLanguage
import com.example.utils.DeviceCountry
import com.example.utils.FeatureTourSchedule
import com.example.utils.applyKeypadKey
import com.example.utils.isNewerVersion
import java.io.File
import com.example.widget.triggerWidgetUpdate
import com.example.workers.RateUpdateScheduler
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
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

/** The language of the user's country, offered once on a fresh install; [title] and [action] are in it. */
data class LanguageSuggestion(val language: String, val title: String, val action: String)

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

    // Declared before init, which fills it.
    private val _languageSuggestion = MutableStateFlow<LanguageSuggestion?>(null)
    val languageSuggestion: StateFlow<LanguageSuggestion?> = _languageSuggestion.asStateFlow()

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
        if (repository.languageSuggestionPending()) findLanguageSuggestion()
    }

    private fun findLanguageSuggestion() {
        val context = repository.context
        val code = CountryLanguage.suggestion(DeviceCountry.home(context), AppLanguage.effective(language.value))
        if (code == null) {
            repository.markLanguageSuggestionDone()
            return
        }
        val localized = AppLanguage.wrap(context, code)
        _languageSuggestion.value = LanguageSuggestion(
            language = code,
            title = localized.getString(R.string.language_suggestion),
            action = localized.getString(R.string.language_suggestion_switch)
        )
    }

    fun acceptLanguageSuggestion() {
        val suggestion = _languageSuggestion.value ?: return
        setLanguage(suggestion.language)
    }

    fun dismissLanguageSuggestion() {
        _languageSuggestion.value = null
        repository.markLanguageSuggestionDone()
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

    private val _tour = MutableStateFlow<List<FeatureTip>>(emptyList())
    /** Feature tour cards to show over the calculator; empty = hidden. */
    val tour: StateFlow<List<FeatureTip>> = _tour.asStateFlow()
    private var tourChecked = false

    /**
     * Once per session: on first launch (or again after a while, see [FeatureTourSchedule]) show the tour
     * for the features still unused - the widget unless one is placed, the extra currency unless it was used.
     */
    fun maybeShowTour(widgetPlaced: Boolean) {
        if (tourChecked) return
        tourChecked = true
        val selectUnused = !repository.quickConvertUsed()
        val tips = buildList {
            if (!widgetPlaced) add(FeatureTip.WIDGET)
            if (selectUnused) add(FeatureTip.SELECT_CONVERT)
            if (_state.value.extraTargetCurrency == null && !repository.extraCurrencyEverUsed()) add(FeatureTip.EXTRA_CURRENCY)
        }
        val due = FeatureTourSchedule.isDue(repository.tourTimesShown(), repository.tourLastShownAt(), System.currentTimeMillis())
        // Converting selected text is new: everyone gets its card once, whatever the schedule says.
        val introduceSelect = selectUnused && !repository.quickConvertTipSeen()
        val shown = when {
            due -> tips
            introduceSelect -> listOf(FeatureTip.SELECT_CONVERT)
            else -> emptyList()
        }
        if (shown.isEmpty()) return
        viewModelScope.launch {
            // Let the calculator appear first, and never pop up under a typing finger: wait for a quiet
            // moment on the calculator screen itself.
            delay(1_200)
            while (
                System.currentTimeMillis() - lastInteractionAt < TOUR_QUIET_MS ||
                _state.value.showSettings || _state.value.showCurrencySelector
            ) {
                delay(400)
            }
            _tour.value = shown
            // Counted when it appears, so another launch (or a second app window) does not show it again.
            if (due) repository.markTourShown(System.currentTimeMillis())
            if (FeatureTip.SELECT_CONVERT in shown) repository.markQuickConvertTipSeen()
        }
    }

    /** Last time the user typed or swapped on the calculator; the tour waits for a quiet moment after it. */
    @Volatile
    private var lastInteractionAt = 0L

    /** From Settings: the whole tour, whatever was already used. */
    fun showTour() {
        _state.update { it.copy(showSettings = false) }
        _tour.value = FeatureTip.entries.toList()
    }

    fun dismissTour() {
        _tour.value = emptyList()
    }

    private val _widgetGuide = MutableStateFlow(false)
    /** Settings is open with the "add widget" button in a spotlight (from the tour's "show me how"). */
    val widgetGuide: StateFlow<Boolean> = _widgetGuide.asStateFlow()

    fun startWidgetGuide() {
        dismissTour()
        _state.update { it.copy(showSettings = true) }
        _widgetGuide.value = true
    }

    fun endWidgetGuide() {
        _widgetGuide.value = false
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

    private fun notifyWidgets(settle: Boolean = false) = triggerWidgetUpdate(repository.context, settle)

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
        notifyWidgets(settle = true)
    }

    fun setLanguage(language: String) {
        if (_languageSuggestion.value != null) dismissLanguageSuggestion()
        if (language == repository.language.value) return
        repository.setLanguage(language)
        notifyWidgets(settle = true)
    }

    /** The widgets switch to the new colors right away, like the app. */
    fun setColorTheme(theme: String) {
        repository.setColorTheme(theme)
        notifyWidgets(settle = true)
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
        lastInteractionAt = System.currentTimeMillis()
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
        lastInteractionAt = System.currentTimeMillis()
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
        _widgetGuide.value = false
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

    private companion object {
        /** No keypad activity for this long before the tour may appear. */
        const val TOUR_QUIET_MS = 2_500L
    }
}
