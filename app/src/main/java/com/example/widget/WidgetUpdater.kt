package com.example.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.state.PreferencesGlanceStateDefinition
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

suspend fun updateWidgets(context: Context) {
    try {
        val sharedPrefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
        val amount = sharedPrefs.getString("amount", "1") ?: "1"
        val sourceId = sharedPrefs.getString("source", "USD") ?: "USD"
        val targetId = sharedPrefs.getString("target", "ILS") ?: "ILS"
        val themeMode = sharedPrefs.getString("theme_mode", "system") ?: "system"
        val colorThemeStr = sharedPrefs.getString("color_theme", "standard") ?: "standard"

        val dao = androidx.room.Room.databaseBuilder(context, com.example.data.AppDatabase::class.java, "currency_database").build().currencyDao()
        val sourceRate = dao.getRate(sourceId)?.rateRelativeToUSD ?: 1.0
        val targetRate = dao.getRate(targetId)?.rateRelativeToUSD ?: 1.0
        val conversionRate = targetRate / sourceRate

        val manager = GlanceAppWidgetManager(context)
        
        manager.getGlanceIds(LargeCurrencyWidget::class.java).forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                val mutablePrefs = prefs.toMutablePreferences()
                mutablePrefs[LargeCurrencyWidget.amountKey] = amount
                mutablePrefs[LargeCurrencyWidget.sourceKey] = sourceId
                mutablePrefs[LargeCurrencyWidget.targetKey] = targetId
                mutablePrefs[LargeCurrencyWidget.rateKey] = conversionRate.toFloat()
                mutablePrefs[LargeCurrencyWidget.themeModeKey] = themeMode
                mutablePrefs[LargeCurrencyWidget.colorThemeKey] = colorThemeStr
                mutablePrefs
            }
        }
        
        manager.getGlanceIds(SmallCurrencyWidget::class.java).forEach { id ->
            updateAppWidgetState(context, PreferencesGlanceStateDefinition, id) { prefs ->
                val mutablePrefs = prefs.toMutablePreferences()
                mutablePrefs[LargeCurrencyWidget.amountKey] = amount
                mutablePrefs[LargeCurrencyWidget.sourceKey] = sourceId
                mutablePrefs[LargeCurrencyWidget.targetKey] = targetId
                mutablePrefs[LargeCurrencyWidget.rateKey] = conversionRate.toFloat()
                mutablePrefs[LargeCurrencyWidget.themeModeKey] = themeMode
                mutablePrefs[LargeCurrencyWidget.colorThemeKey] = colorThemeStr
                mutablePrefs
            }
        }

        SmallCurrencyWidget().updateAll(context)
        LargeCurrencyWidget().updateAll(context)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

@OptIn(DelicateCoroutinesApi::class)
fun triggerWidgetUpdate(context: Context) {
    GlobalScope.launch {
        updateWidgets(context)
    }
}
