package com.example.widget

import android.content.Context
import android.content.Intent
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.appwidget.cornerRadius
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
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
import com.example.MainActivity
import com.example.data.AppDatabase
import com.example.utils.getCurrencyInfo

class SmallCurrencyWidget : GlanceAppWidget() {
    override val stateDefinition: GlanceStateDefinition<*> = PreferencesGlanceStateDefinition

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            val prefs = currentState<androidx.datastore.preferences.core.Preferences>()
            val amount = prefs[LargeCurrencyWidget.amountKey] ?: "1"
            val sourceId = prefs[LargeCurrencyWidget.sourceKey] ?: "USD"
            val targetId = prefs[LargeCurrencyWidget.targetKey] ?: "ILS"
            val conversionRate = prefs[LargeCurrencyWidget.rateKey]?.toDouble() ?: 1.0
            val themeMode = prefs[LargeCurrencyWidget.themeModeKey] ?: "system"
            val colorThemeStr = prefs[LargeCurrencyWidget.colorThemeKey] ?: "standard"

            SmallWidgetContent(amount, sourceId, targetId, conversionRate, themeMode, colorThemeStr)
        }
    }

    @Composable
    private fun SmallWidgetContent(amount: String, sourceId: String, targetId: String, conversionRate: Double, themeMode: String, colorThemeStr: String) {
        val context = LocalContext.current
        val converted = (amount.toDoubleOrNull() ?: 0.0) * conversionRate
        val sourceInfo = getCurrencyInfo(sourceId)
        val targetInfo = getCurrencyInfo(targetId)

        val isSystemDark = (context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        val isDark = when(themeMode) {
            "dark" -> true
            "light" -> false
            else -> isSystemDark
        }

        val size = androidx.glance.LocalSize.current
        val isCompactHeight = size.height < 150.dp

        val fontSize1 = if (isCompactHeight) 14.sp else 20.sp
        val fontSize2 = if (isCompactHeight) 18.sp else 24.sp
        val spacerHeight = if (isCompactHeight) 2.dp else 12.dp

        val primaryColor = when(colorThemeStr) {
            "ocean" -> if (isDark) Color(0xFF63DBB6) else Color(0xFF006C52)
            "forest" -> if (isDark) Color(0xFF9CD67E) else Color(0xFF376A20)
            "sunset" -> if (isDark) Color(0xFFFFB68F) else Color(0xFF9E4200)
            "rose" -> if (isDark) Color(0xFFFFB3B4) else Color(0xFF904A4C)
            "lavender" -> if (isDark) Color(0xFFB9C3FF) else Color(0xFF4758A9)
            else -> if (isDark) Color(0xFFD0BCFF) else Color(0xFF6650a4)
        }
        
        val bgColor = if(isDark) Color(0xFF1E1E1E) else Color(0xFFF8F9FA)
        val textColor = if(isDark) Color.White else Color.Black
        val secondaryTextColor = if (isDark) Color(0xFFB0B0B0) else Color.Gray

        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .background(bgColor)
                .cornerRadius(16.dp)
                .padding(if (isCompactHeight) 8.dp else 16.dp)
                .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(amount, style = TextStyle(fontWeight = FontWeight.Medium, fontSize = fontSize1, color = androidx.glance.unit.ColorProvider(textColor)))
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text("${sourceInfo.symbol} $sourceId ${sourceInfo.flag}", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = fontSize1, color = androidx.glance.unit.ColorProvider(textColor)))
            }
            Spacer(modifier = GlanceModifier.height(spacerHeight))
            Row(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(String.format("%.2f", converted), style = TextStyle(fontWeight = FontWeight.Bold, fontSize = fontSize2, color = androidx.glance.unit.ColorProvider(primaryColor)))
                Spacer(modifier = GlanceModifier.defaultWeight())
                Text("${targetInfo.symbol} $targetId ${targetInfo.flag}", style = TextStyle(fontWeight = FontWeight.Bold, fontSize = fontSize1, color = androidx.glance.unit.ColorProvider(textColor)))
            }
            Spacer(modifier = GlanceModifier.height(spacerHeight))
            if (!isCompactHeight) {
                Text("הקש כדי לפתוח", style = TextStyle(fontSize = 12.sp, color = androidx.glance.unit.ColorProvider(secondaryTextColor)))
            }
        }
    }
}
