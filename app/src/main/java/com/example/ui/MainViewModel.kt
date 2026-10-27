package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.R
import com.example.data.AppRelease
import com.example.data.AutoUpdateSettings
import com.example.data.CurrencyRepository
import com.example.data.CurrencySlot
import com.example.premium.Access
import com.example.premium.FREE_COLOR_THEME
import com.example.premium.Monetization
import com.example.premium.PremiumReason
import com.example.update.AppUpdater
import com.example.utils.AppLanguage
import com.example.utils.CalcHistory
import com.example.utils.CountryLanguage
import com.example.utils.DeviceCountry
import com.example.utils.FeatureTourSchedule
import com.example.utils.HistoryEntry
import com.example.utils.applyCalcKey
import com.example.utils.applyKeypadKey
import com.example.utils.completeExpression
import com.example.utils.hasOperation
import com.example.utils.previewExpression
import com.example.utils.toRawAmount
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
    val lastUpdateTimestamp: Long = 0L,
    /** The calculator line while the calculator keys are open ("1250+300"); null = the plain amount keypad. */
    val expression: String? = null,
    /** The calculation behind the amount (after "=" or from the history), shown under it until the next key. */
    val lastExpression: String? = null,
    /** The amount is a result or from the history: the next digit starts a new one. */
    val freshAmount: Boolean = false,
    /** The history list is shown in place of the keypad. */
    val showHistory: Boolean = false,
    val history: List<HistoryEntry> = emptyList(),
) {
    /**
     * The source amount: as typed, or the live result of the calculation, rounded as "=" would leave it (null
     * while it has none: "5/0", or too large).
     */
    val sourceValue: Double?
        get() = if (expression != null) {
            previewExpression(expression)?.let(::toRawAmount)?.toDouble()
        } else {
            sourceAmountRaw.toDoubleOrNull() ?: 0.0
        }

    val sourceAmount: Double
        get() = sourceValue ?: 0.0

    /** The calculator line has something to compute, so the calculator key shows "=". */
    val calculating: Boolean
        get() = expression != null && hasOperation(expression)

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
    private val monetization: Monetization,
) : ViewModel() {
    private val _state = MutableStateFlow(CalculatorState(
        sourceCurrency = repository.getSourceCurrency(),
        targetCurrency = repository.getTargetCurrency(),
        extraTargetCurrency = repository.getExtraTargetCurrency(),
        sourceAmountRaw = repository.getAmount(),
        expression = if (repository.calculatorOpen() && monetization.access.value.unlocked) repository.getAmount() else null,
        history = repository.getHistory(),
    ))
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    val themeMode = repository.themeMode
    val colorTheme = repository.colorTheme
    val access: StateFlow<Access> = monetization.access

    /** The colour set on screen: the chosen one, or Classic while Premium is locked (the choice is kept). */
    val effectiveColorTheme: StateFlow<String> = combine(repository.colorTheme, monetization.access, ::effectiveTheme)
        .stateIn(viewModelScope, SharingStarted.Eagerly, effectiveTheme(repository.colorTheme.value, monetization.access.value))

    private val _premiumSheet = MutableStateFlow<PremiumReason?>(null)
    /** The Premium sheet is open, for this reason; null = closed. */
    val premiumSheet: StateFlow<PremiumReason?> = _premiumSheet.asStateFlow()
    /** A colour set tapped while locked: applied once the user unlocks. */
    private var pendingColorTheme: String? = null
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
        // The Play build always looks (it only tells, Play installs); the GitHub one when the user wants it to.
        if (!BuildConfig.SELF_UPDATE || repository.checkAppUpdates.value) {
            checkForAppUpdate()
        }
        if (repository.languageSuggestionPending()) findLanguageSuggestion()
        viewModelScope.launch {
            var wasUnlocked = monetization.access.value.unlocked
            monetization.access.collect { access ->
                if (!access.unlocked) {
                    // Premium ran out with the calculator open: its result stays as the amount.
                    if (_state.value.expression != null) closeCalculator()
                } else if (!wasUnlocked) {
                    onUnlocked()
                }
                wasUnlocked = access.unlocked
            }
        }
    }

    fun openPremium(reason: PremiumReason) {
        _premiumSheet.value = reason
    }

    fun closePremium() {
        _premiumSheet.value = null
        pendingColorTheme = null
    }

    /** Just unlocked from the sheet: finish what the user was reaching for. */
    private fun onUnlocked() {
        val reason = _premiumSheet.value ?: return
        val theme = pendingColorTheme
        closePremium()
        when (reason) {
            PremiumReason.CALCULATOR -> if (_state.value.expression == null) toggleCalculator()
            PremiumReason.COLORS -> if (theme != null) setColorTheme(theme)
            PremiumReason.WIDGET -> addWidgetToHomeScreen()
            else -> Unit
        }
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
                    bannerDismissed = current.bannerDismissed ||
                        (latest != null && latest.versionName == repository.dismissedUpdateVersion),
                )
            }
        }
    }

    /** Downloads the available release (once) and opens the system installer for it. */
    fun startAppUpdate() {
        val current = _appUpdate.value
        val release = current.available ?: return
        if (current.downloadProgress != null) return
        if (updater.opensStore) return updater.open()

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

    private val _tourOffer = MutableStateFlow<List<FeatureTip>>(emptyList())
    /**
     * Tips offered by a small banner above the calculator; tapping it opens the tour. The tour never pops up
     * by itself: its sheet covers the keypad, and people who open the app to type tapped "Next" by accident.
     */
    val tourOffer: StateFlow<List<FeatureTip>> = _tourOffer.asStateFlow()
    private var tourChecked = false

    /**
     * Once per session: on first launch (or again after a while, see [FeatureTourSchedule]) offer the tour
     * for the features still unused - the widget unless one is placed, converting selected text unless it
     * was tried, the extra currency unless it was used.
     */
    fun maybeShowTour(widgetPlaced: Boolean) {
        if (tourChecked) return
        tourChecked = true
        val selectUnused = !repository.quickConvertUsed()
        val calculatorUnused = !repository.calculatorUsed()
        val tips = buildList {
            if (!widgetPlaced) add(FeatureTip.WIDGET)
            if (selectUnused) add(FeatureTip.SELECT_CONVERT)
            if (calculatorUnused) add(FeatureTip.CALCULATOR)
            if (_state.value.extraTargetCurrency == null && !repository.extraCurrencyEverUsed()) add(FeatureTip.EXTRA_CURRENCY)
        }
        val due = FeatureTourSchedule.isDue(repository.tourTimesShown(), repository.tourLastShownAt(), System.currentTimeMillis())
        // New features: everyone gets their card once, whatever the schedule says.
        val introduce = buildList {
            if (selectUnused && !repository.quickConvertTipSeen()) add(FeatureTip.SELECT_CONVERT)
            if (calculatorUnused && !repository.calculatorTipSeen()) add(FeatureTip.CALCULATOR)
        }
        val shown = if (due) tips else introduce
        if (shown.isEmpty()) return
        // Offered right away, before the user starts typing, so the banner never moves the keys under a finger.
        _tourOffer.value = shown
        // Counted when offered, so another launch (or a second app window) does not offer it again.
        if (due) repository.markTourShown(System.currentTimeMillis())
        if (FeatureTip.SELECT_CONVERT in shown) repository.markQuickConvertTipSeen()
        if (FeatureTip.CALCULATOR in shown) repository.markCalculatorTipSeen()
    }

    fun openOfferedTour() {
        _tour.value = _tourOffer.value
        _tourOffer.value = emptyList()
    }

    fun dismissTourOffer() {
        _tourOffer.value = emptyList()
    }

    /** From Settings: the whole tour, whatever was already used. */
    fun showTour() {
        _state.update { it.copy(showSettings = false) }
        _tourOffer.value = emptyList()
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

    private val _selectGuide = MutableStateFlow(false)
    /** Settings is open on "Convert from any app" with the sample price in a spotlight (from the tour's "show me how"). */
    val selectGuide: StateFlow<Boolean> = _selectGuide.asStateFlow()

    fun startSelectGuide() {
        dismissTour()
        _state.update { it.copy(showSettings = true) }
        _selectGuide.value = true
    }

    fun endSelectGuide() {
        _selectGuide.value = false
    }

    private val _legacyAppInstalled = MutableStateFlow(false)
    /** The old AI Studio build is still installed next to this app (checked on every resume). */
    val legacyAppInstalled: StateFlow<Boolean> = _legacyAppInstalled.asStateFlow()

    fun setLegacyAppInstalled(installed: Boolean) {
        _legacyAppInstalled.value = installed
    }

    fun dismissUpdateBanner() {
        _appUpdate.update { it.copy(bannerDismissed = true) }
        _appUpdate.value.available?.let { repository.dismissedUpdateVersion = it.versionName }
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
        val amount = repository.getAmount()
        _state.update {
            val changed = amount != it.sourceAmountRaw
            it.copy(
                sourceCurrency = repository.getSourceCurrency(),
                targetCurrency = repository.getTargetCurrency(),
                extraTargetCurrency = repository.getExtraTargetCurrency(),
                sourceAmountRaw = amount,
                // An amount typed in the widget (or opened from another app) replaces the calculation.
                expression = if (changed && it.expression != null) amount else it.expression,
                lastExpression = if (changed) null else it.lastExpression,
            )
        }
    }

    /** The app went to the background: the conversion on screen goes to the history. */
    fun onAppHidden() = recordHistory()

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
        if (!monetization.access.value.unlocked && theme != FREE_COLOR_THEME) {
            pendingColorTheme = theme
            openPremium(PremiumReason.COLORS)
            return
        }
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
        if (!monetization.access.value.unlocked) return openPremium(PremiumReason.WIDGET)
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(repository.context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = android.content.ComponentName(repository.context, com.example.widget.LargeCurrencyWidgetReceiver::class.java)
                appWidgetManager.requestPinAppWidget(provider, null, null)
            }
        }
    }

    /** The amount was changed on the keypad since it last went to the history. */
    private var amountEdited = false

    fun onKeypadPress(key: String) {
        val current = _state.value
        if (current.expression != null) return onCalculatorKey(current.expression, key)
        // "C" starts a new amount: the one on screen goes to the history first.
        if (key == "C") recordHistory()
        val startsOver = current.freshAmount && (key == "." || key.all { it.isDigit() })
        val next = applyKeypadKey(if (startsOver) "0" else current.sourceAmountRaw, key)
        _state.update { it.copy(freshAmount = false, lastExpression = null) }
        if (next == current.sourceAmountRaw) return
        if (!amountEdited) monetization.noteConversion()
        amountEdited = true
        setAmount(next)
    }

    private fun onCalculatorKey(expression: String, key: String) {
        if (key == "=") return evaluate()
        if (key == "C") recordHistory()
        val next = applyCalcKey(expression, key, fresh = _state.value.freshAmount)
        _state.update { it.copy(expression = next, freshAmount = false, lastExpression = null) }
        if (next == expression) return
        if (!amountEdited) monetization.noteConversion()
        amountEdited = true
        // The amount follows the live result, so the widget and the next launch see it too.
        previewExpression(next)?.let(::toRawAmount)?.let(::setAmount)
    }

    private fun setAmount(raw: String) {
        if (raw == _state.value.sourceAmountRaw) return
        repository.setAmount(raw)
        _state.update { it.copy(sourceAmountRaw = raw) }
        notifyWidgets()
    }

    /** "=": the result becomes the amount, and the calculation goes to the history. */
    private fun evaluate() {
        val typed = _state.value.expression ?: return
        val expression = completeExpression(typed)
        val raw = previewExpression(expression)?.let(::toRawAmount) ?: return
        setAmount(raw)
        // "1250+" is just 1250 again: nothing to show or keep.
        if (!hasOperation(expression)) {
            _state.update { it.copy(expression = raw, lastExpression = null) }
            return
        }
        _state.update { it.copy(expression = expression) }
        amountEdited = true
        recordHistory()
        _state.update { it.copy(expression = raw, lastExpression = expression, freshAmount = true) }
    }

    /**
     * The calculator key (where "00" was): opens the calculator keys, starting from the amount on screen; with
     * the keys open it closes them (after working out what is left to compute).
     */
    fun toggleCalculator() {
        val current = _state.value
        if (current.expression == null) {
            if (!monetization.access.value.unlocked) return openPremium(PremiumReason.CALCULATOR)
            repository.setCalculatorOpen(true)
            _state.update { it.copy(expression = it.sourceAmountRaw, freshAmount = true, showHistory = false) }
        } else {
            closeCalculator()
        }
    }

    private fun closeCalculator() {
        if (_state.value.calculating) evaluate()
        repository.setCalculatorOpen(false)
        _state.update { it.copy(expression = null) }
    }

    fun openHistory() {
        _state.update { it.copy(showHistory = true) }
    }

    fun closeHistory() {
        _state.update { it.copy(showHistory = false) }
    }

    fun clearHistory() {
        repository.setHistory(emptyList())
        _state.update { it.copy(history = emptyList()) }
    }

    /** Brings a conversion back as it was: its currencies, its amount and the calculation behind it. */
    fun restoreHistory(entry: HistoryEntry) {
        recordHistory()
        val current = _state.value
        val pair = setOf(entry.source, entry.target)
        // The additional currency stays, unless it is now one of the pair: then it takes the one pushed out.
        val extra = current.extraTargetCurrency?.let { extra ->
            if (extra !in pair) extra else listOf(current.sourceCurrency, current.targetCurrency).firstOrNull { it !in pair }
        }
        applySlots(mapOf(CurrencySlot.SOURCE to entry.source, CurrencySlot.TARGET to entry.target, CurrencySlot.EXTRA_TARGET to extra))
        // A finished amount: the next digit starts a new one.
        setAmount(entry.amount)
        amountEdited = false
        _state.update {
            it.copy(
                expression = it.expression?.let { entry.amount },
                freshAmount = true,
                lastExpression = entry.expression,
                showHistory = false
            )
        }
    }

    /** Adds the conversion on screen to the history, once it was typed or calculated (not just reopened). */
    private fun recordHistory() {
        if (!amountEdited) return
        amountEdited = false
        val current = _state.value
        val raw = current.sourceValue?.let(::toRawAmount) ?: return
        val value = raw.toDouble()
        if (value == 0.0) return
        val extra = current.extraTargetCurrency
        val entry = HistoryEntry(
            time = System.currentTimeMillis(),
            source = current.sourceCurrency,
            target = current.targetCurrency,
            extra = extra,
            amount = raw,
            expression = current.expression?.let(::completeExpression)?.takeIf(::hasOperation),
            targetAmount = value * current.rateTo(current.targetCurrency),
            extraAmount = extra?.let { value * current.rateTo(it) },
        )
        val history = CalcHistory.add(current.history, entry)
        repository.setHistory(history)
        _state.update { it.copy(history = history) }
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
        _widgetGuide.value = false
        _selectGuide.value = false
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
        monetization.noteConversion()
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

private fun effectiveTheme(theme: String, access: Access) = if (access.unlocked) theme else FREE_COLOR_THEME
