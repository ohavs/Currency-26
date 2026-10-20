package com.example.widget

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import com.example.MainActivity
import com.example.R
import com.example.premium.PremiumRules
import com.example.utils.AppLanguage

/**
 * Premium is locked (the free trial or a video unlock ran out): the widget keeps its place on the home screen
 * and shows the way back in. A tap anywhere opens the app on the Premium sheet.
 */
@Composable
internal fun LockedWidgetContent(data: WidgetData) {
    val context = LocalContext.current
    val size = LocalSize.current
    val colors = WidgetColors(data.themeMode, data.colorTheme)
    val localized = remember(data.language) { AppLanguage.wrap(context, data.language) }
    val roomy = size.height >= 110.dp
    Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.background)
            .cornerRadius(24.dp)
            .clickable(actionStartActivity(MainActivity.openPremiumIntent(context))),
        contentAlignment = Alignment.Center
    ) {
        WidgetTexture(colors)
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (roomy) {
                Text("🔒", style = TextStyle(fontSize = 24.sp))
                Spacer(GlanceModifier.height(6.dp))
                AppFontText(
                    text = localized.getString(R.string.widget_locked_title),
                    color = colors.ink,
                    sizeSp = 15f,
                    widthDp = size.width.value - 28f,
                    maxLines = 2,
                    minSp = 12f
                )
                Spacer(GlanceModifier.height(10.dp))
            }
            Box(
                modifier = GlanceModifier
                    .background(colors.accent)
                    .cornerRadius(18.dp)
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    (if (roomy) "" else "🔒 ") + localized.getString(R.string.widget_locked_action),
                    style = TextStyle(color = colors.onAccent, fontSize = 14.sp, fontWeight = FontWeight.Bold),
                    maxLines = 1
                )
            }
            if (size.height >= 200.dp) {
                // The free way back in, for the larger sizes.
                Spacer(GlanceModifier.height(10.dp))
                Text(
                    localized.getString(R.string.premium_settings_free, PremiumRules.VIDEOS_PER_UNLOCK, PremiumRules.REWARD_HOURS),
                    style = TextStyle(color = colors.inkMuted, fontSize = 12.sp, textAlign = TextAlign.Center),
                    maxLines = 2
                )
            }
        }
    }
}
