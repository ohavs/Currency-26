package com.example.widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.appwidget.cornerRadius
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.state.PreferencesGlanceStateDefinition
import androidx.glance.layout.*
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.text.FontWeight
import androidx.compose.ui.graphics.Color
import androidx.room.Room
import com.example.data.AppDatabase
import com.example.utils.getCurrencyInfo

class LargeCurrencyWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition
    override val sizeMode = androidx.glance.appwidget.SizeMode.Exact

    companion object {
        val amountKey = androidx.datastore.preferences.core.stringPreferencesKey("amount")
        val sourceKey = androidx.datastore.preferences.core.stringPreferencesKey("source")
        val targetKey = androidx.datastore.preferences.core.stringPreferencesKey("target")
        val rateKey = androidx.datastore.preferences.core.floatPreferencesKey("rate")
        val themeModeKey = androidx.datastore.preferences.core.stringPreferencesKey("theme_mode")
        val colorThemeKey = androidx.datastore.preferences.core.stringPreferencesKey("color_theme")
    }

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val amount = prefs[amountKey] ?: "1"
            val sourceId = prefs[sourceKey] ?: "USD"
            val targetId = prefs[targetKey] ?: "ILS"
            val conversionRate = prefs[rateKey]?.toDouble() ?: 1.0
            val themeMode = prefs[themeModeKey] ?: "system"
            val colorThemeStr = prefs[colorThemeKey] ?: "standard"

            LargeWidgetContent(amount, sourceId, targetId, conversionRate, themeMode, colorThemeStr)
        }
    }

    @Composable
    private fun LargeWidgetContent(amount: String, sourceId: String, targetId: String, conversionRate: Double, themeMode: String, colorThemeStr: String) {
        val size = androidx.glance.LocalSize.current
        val isVeryCompactHeight = size.height < 300.dp
        val isCompactHeight = size.height < 400.dp
        
        val converted = (amount.toDoubleOrNull() ?: 0.0) * conversionRate
        val sourceInfo = getCurrencyInfo(sourceId)
        val targetInfo = getCurrencyInfo(targetId)

        val headerFontSize = if (isVeryCompactHeight) 12.sp else if (isCompactHeight) 18.sp else 24.sp
        val amountFontSize = if (isVeryCompactHeight) 16.sp else if (isCompactHeight) 22.sp else 32.sp
        val swapFontSize = if (isVeryCompactHeight) 18.sp else if (isCompactHeight) 28.sp else 42.sp
        val keypadFontSize = if (isVeryCompactHeight) 14.sp else if (isCompactHeight) 20.sp else 28.sp
        
        val spacerHeight = if (isVeryCompactHeight) 2.dp else if (isCompactHeight) 6.dp else 12.dp
        val rowPaddingVal = if (isVeryCompactHeight) 2.dp else if (isCompactHeight) 6.dp else 16.dp

        val context = androidx.glance.LocalContext.current
        val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val isDark = when(themeMode) {
            "dark" -> true
            "light" -> false
            else -> isSystemDark
        }

        val primaryColor = when(colorThemeStr) {
            "ocean" -> if (isDark) Color(0xFF63DBB6) else Color(0xFF006C52)
            "forest" -> if (isDark) Color(0xFF9CD67E) else Color(0xFF376A20)
            "sunset" -> if (isDark) Color(0xFFFFB68F) else Color(0xFF9E4200)
            "rose" -> if (isDark) Color(0xFFFFB3B4) else Color(0xFF904A4C)
            "lavender" -> if (isDark) Color(0xFFB9C3FF) else Color(0xFF4758A9)
            else -> if (isDark) Color(0xFFD0BCFF) else Color(0xFF6650a4)
        }
        
        val bgColor = if(isDark) Color(0xFF1E1E1E) else Color(0xFFF8F9FA)
        val surfaceColor = if(isDark) Color(0xFF2C2C2C) else Color.White
        val secondarySurface = if (isDark) Color(0xFF3A3A3A) else Color(0xFFE9ECEF)
        val textColor = if(isDark) Color.White else Color.Black
        val keyBgColor = if(isDark) Color(0xFF383838) else Color.White
        val keyTextColor = if(isDark) Color.White else Color.Black
        val clearKeyColor = if (isDark) Color(0xFFFFB4AB) else Color(0xFFBA1A1A)

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(16.dp)
                .padding(if (isVeryCompactHeight) 6.dp else 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(4.dp).background(surfaceColor).cornerRadius(16.dp).padding(rowPaddingVal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(amount, style = TextStyle(fontWeight = FontWeight.Bold, fontSize = amountFontSize, color = androidx.glance.unit.ColorProvider(textColor)))
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text("${sourceInfo.symbol} $sourceId ${sourceInfo.flag}", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = headerFontSize, color = androidx.glance.unit.ColorProvider(textColor)))
            }
            Spacer(modifier = GlanceModifier.height(spacerHeight))
            
            Box(
                modifier = GlanceModifier
                    .fillMaxWidth()
                    .clickable(actionRunCallback<SwapAction>()),
                contentAlignment = Alignment.Center
            ) {
                Text("⇅", style = TextStyle(fontSize = swapFontSize, color = androidx.glance.unit.ColorProvider(primaryColor)), modifier = GlanceModifier.padding(vertical = 4.dp, horizontal = 32.dp))
            }
            
            Spacer(modifier = GlanceModifier.height(spacerHeight))
            Row(
                modifier = GlanceModifier.fillMaxWidth().padding(4.dp).background(secondarySurface).cornerRadius(16.dp).padding(rowPaddingVal),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(String.format("%.2f", converted), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = amountFontSize, color = androidx.glance.unit.ColorProvider(primaryColor)))
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text("${targetInfo.symbol} $targetId ${targetInfo.flag}", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = headerFontSize, color = androidx.glance.unit.ColorProvider(textColor)))
            }
            
            Spacer(modifier = GlanceModifier.defaultWeight())
            
            val keys = listOf(
                listOf("7", "8", "9"),
                listOf("4", "5", "6"),
                listOf("1", "2", "3"),
                listOf("C", "0", "⌫")
            )
            
            Column(modifier = GlanceModifier.fillMaxWidth()) {
                keys.forEach { row ->
                    Row(modifier = GlanceModifier.fillMaxWidth()) {
                        row.forEach { key ->
                            val currentBgColor = if (key == "C") (if(isDark) Color(0xFF93000A) else Color(0xFFFFDAD6)) else if (key == "⌫") primaryColor.copy(alpha = 0.7f) else primaryColor.copy(alpha = if (isDark) 0.2f else 0.1f)
                            val currentTextColor = if (key == "C") clearKeyColor else if (key == "⌫") (if(isDark) Color.Black else Color.White) else keyTextColor
                            Box(
                                modifier = GlanceModifier
                                    .defaultWeight()
                                    .padding(4.dp)
                                    .background(currentBgColor)
                                    .cornerRadius(16.dp)
                                    .clickable(actionRunCallback<KeypadAction>(actionParametersOf(ActionParameters.Key<String>("key") to key))),
                                contentAlignment = Alignment.Center
                            ) {
                                val keyPadVerticalPadding = if (isVeryCompactHeight) 2.dp else if (isCompactHeight) 6.dp else 12.dp
                                Text(key, style = TextStyle(fontSize = keypadFontSize, fontWeight = FontWeight.Medium, color = androidx.glance.unit.ColorProvider(currentTextColor)), modifier = GlanceModifier.padding(vertical = keyPadVerticalPadding))
                            }
                        }
                    }
                }
            }
        }
    }
}

class SwapAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val prefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
        val currentSource = prefs.getString("source", "USD") ?: "USD"
        val currentTarget = prefs.getString("target", "ILS") ?: "ILS"
        prefs.edit()
            .putString("source", currentTarget)
            .putString("target", currentSource)
            .apply()
            
        updateWidgets(context)
    }
}

class KeypadAction : ActionCallback {
    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        val key = parameters[ActionParameters.Key<String>("key")] ?: return
        val prefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)
        val currentAmount = prefs.getString("amount", "0") ?: "0"
        val newAmount = when (key) {
            "C" -> "0"
            "⌫" -> if (currentAmount.length <= 1) "0" else currentAmount.dropLast(1)
            else -> if (currentAmount == "0") key else currentAmount + key
        }
        prefs.edit().putString("amount", newAmount).apply()
        
        updateWidgets(context)
    }
}
