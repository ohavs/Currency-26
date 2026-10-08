package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.CurrencyRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CalculatorState(
    val sourceCurrency: String = "USD",
    val targetCurrency: String = "ILS",
    val sourceAmountRaw: String = "1",
    val rates: Map<String, Double> = emptyMap(),
    val recentCurrencies: List<String> = emptyList(),
    val isSearching: Boolean = false,
    val searchQuery: String = "",
    val selectingForSource: Boolean = true, // true if selecting source currency, false if target
    val showCurrencySelector: Boolean = false,
    val lastUpdateTimestamp: Long = 0L
) {
    val sourceAmount: Double
        get() = sourceAmountRaw.toDoubleOrNull() ?: 0.0

    val targetAmount: Double
        get() {
            val sourceRate = rates[sourceCurrency] ?: 1.0
            val targetRate = rates[targetCurrency] ?: 1.0
            // Source amount * (TargetRate / SourceRate) = Target amount
            return sourceAmount * (targetRate / sourceRate)
        }
}

class MainViewModel(private val repository: CurrencyRepository) : ViewModel() {
    private val _state = MutableStateFlow(CalculatorState(
        sourceCurrency = repository.getSourceCurrency(),
        targetCurrency = repository.getTargetCurrency(),
        sourceAmountRaw = repository.getAmount()
    ))
    val state: StateFlow<CalculatorState> = _state.asStateFlow()

    val themeMode = repository.themeMode
    val colorTheme = repository.colorTheme

    fun setThemeMode(mode: String) {
        repository.setThemeMode(mode)
        com.example.widget.triggerWidgetUpdate(repository.context)
    }

    fun setColorTheme(theme: String) {
        repository.setColorTheme(theme)
        com.example.widget.triggerWidgetUpdate(repository.context)
    }

    fun addWidgetToHomeScreen() {
        val appWidgetManager = android.appwidget.AppWidgetManager.getInstance(repository.context)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (appWidgetManager.isRequestPinAppWidgetSupported) {
                val provider = android.content.ComponentName(repository.context, com.example.widget.LargeCurrencyWidgetReceiver::class.java)
                appWidgetManager.requestPinAppWidget(provider, null, null)
            }
        }
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
            repository.refreshRates()
        }
    }

    fun onKeypadPress(char: Char) {
        _state.update { current ->
            val currentAmount = if (current.sourceAmountRaw == "0") "" else current.sourceAmountRaw
            val newAmountRaw = if (char == 'C') {
                "0"
            } else if (char == '⌫') {
                if (currentAmount.length <= 1) "0" else currentAmount.dropLast(1)
            } else if (char == '.') {
                if (currentAmount.contains(".")) currentAmount else "$currentAmount."
            } else {
                currentAmount + char
            }
            repository.setAmount(newAmountRaw)
            com.example.widget.triggerWidgetUpdate(repository.context)
            current.copy(sourceAmountRaw = newAmountRaw)
        }
    }

    fun swapCurrencies() {
        _state.update {
            val newSource = it.targetCurrency
            val newTarget = it.sourceCurrency
            repository.setSourceCurrency(newSource)
            repository.setTargetCurrency(newTarget)
            com.example.widget.triggerWidgetUpdate(repository.context)
            it.copy(
                sourceCurrency = newSource,
                targetCurrency = newTarget
            )
        }
    }

    fun openCurrencySelector(isSource: Boolean) {
        _state.update {
            it.copy(
                showCurrencySelector = true,
                selectingForSource = isSource,
                searchQuery = ""
            )
        }
    }

    fun closeCurrencySelector() {
        _state.update { it.copy(showCurrencySelector = false) }
    }

    fun selectCurrency(currencyCode: String) {
        _state.update { current ->
            viewModelScope.launch {
                repository.markCurrencyUsed(currencyCode)
            }
            if (current.selectingForSource) {
                repository.setSourceCurrency(currencyCode)
                com.example.widget.triggerWidgetUpdate(repository.context)
                current.copy(sourceCurrency = currencyCode, showCurrencySelector = false)
            } else {
                repository.setTargetCurrency(currencyCode)
                com.example.widget.triggerWidgetUpdate(repository.context)
                current.copy(targetCurrency = currencyCode, showCurrencySelector = false)
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _state.update { it.copy(searchQuery = query) }
    }
}
