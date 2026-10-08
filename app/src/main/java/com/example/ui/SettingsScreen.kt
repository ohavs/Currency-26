package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.data.AutoUpdateSettings
import com.example.ui.theme.ColorThemeOption
import com.example.ui.theme.LocalPalette
import com.example.ui.theme.Palettes
import com.example.utils.formatUpdatedAt

@Composable
fun SettingsScreen(
    themeMode: String,
    colorTheme: String,
    autoUpdate: AutoUpdateSettings,
    lastUpdateTimestamp: Long,
    isRefreshing: Boolean,
    lastRefreshFailed: Boolean,
    canPinWidget: Boolean,
    onClose: () -> Unit,
    onThemeModeChange: (String) -> Unit,
    onColorThemeChange: (String) -> Unit,
    onAutoUpdateEnabledChange: (Boolean) -> Unit,
    onIntervalChange: (Long) -> Unit,
    onWifiOnlyChange: (Boolean) -> Unit,
    onRefreshOnOpenChange: (Boolean) -> Unit,
    onRefreshNow: () -> Unit,
    onAddWidget: () -> Unit,
) {
    BackHandler(onBack = onClose)
    val palette = LocalPalette.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp)
    ) {
        AppTopBar(
            title = "הגדרות",
            navigation = { RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "חזרה", onClose) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsSection(title = "מראה", icon = Icons.Rounded.Palette) {
                SettingLabel("מצב תצוגה")
                SegmentedControl(
                    options = listOf("light" to "בהיר", "dark" to "כהה", "system" to "לפי המערכת"),
                    selected = themeMode,
                    onSelect = onThemeModeChange,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                SettingLabel("ערכת צבעים")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    val selectedKey = Palettes.normalize(colorTheme)
                    Palettes.options.forEach { option ->
                        ThemeSwatch(
                            option = option,
                            dark = palette.isDark,
                            selected = option.key == selectedKey,
                            onClick = { onColorThemeChange(option.key) }
                        )
                    }
                }
            }

            SettingsSection(title = "עדכונים אוטומטיים", icon = Icons.Rounded.Update) {
                SwitchRow(
                    title = "עדכון שערים ברקע",
                    subtitle = "השערים והווידג'ט מתעדכנים אוטומטית גם כשהאפליקציה סגורה",
                    checked = autoUpdate.enabled,
                    onCheckedChange = onAutoUpdateEnabledChange
                )
                AnimatedVisibility(visible = autoUpdate.enabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingLabel("תדירות עדכון")
                        ChipSelector(
                            options = AutoUpdateSettings.INTERVAL_OPTIONS.map { it to intervalLabel(it) },
                            selected = autoUpdate.intervalMinutes,
                            onSelect = onIntervalChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SwitchRow(
                            title = "רק ב-Wi-Fi",
                            subtitle = "חוסך בחבילת הגלישה",
                            checked = autoUpdate.wifiOnly,
                            onCheckedChange = onWifiOnlyChange
                        )
                    }
                }
                SwitchRow(
                    title = "עדכון בפתיחת האפליקציה",
                    subtitle = "משיכת שערים עדכניים בכל כניסה",
                    checked = autoUpdate.refreshOnOpen,
                    onCheckedChange = onRefreshOnOpenChange
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.cardSoft)
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("עודכן לאחרונה", style = MaterialTheme.typography.labelMedium, color = palette.inkMuted)
                        Text(
                            text = when {
                                isRefreshing -> "מעדכן…"
                                lastRefreshFailed -> "העדכון נכשל – בדקו את החיבור"
                                else -> formatUpdatedAt(lastUpdateTimestamp) ?: "עדיין לא עודכן"
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.ink
                        )
                    }
                    PillButton(
                        text = "עדכן עכשיו",
                        icon = Icons.Rounded.Refresh,
                        loading = isRefreshing,
                        onClick = onRefreshNow
                    )
                }
            }

            SettingsSection(title = "וידג'ט", icon = Icons.Rounded.Widgets) {
                Text(
                    text = "לחיצה על מטבע בווידג'ט פותחת ישירות את בחירת המטבע באפליקציה.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted
                )
                PillButton(
                    text = "הוספת וידג'ט למסך הבית",
                    icon = Icons.Rounded.Widgets,
                    enabled = canPinWidget,
                    onClick = onAddWidget,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!canPinWidget) {
                    Text(
                        text = "אפשר להוסיף את הווידג'ט ידנית: לחיצה ארוכה על מסך הבית ← ווידג'טים.",
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                }
            }
        }
    }
}

private fun intervalLabel(minutes: Long): String = when (minutes) {
    15L -> "15 דק'"
    60L -> "שעה"
    1440L -> "יום"
    else -> if (minutes % 60L == 0L) "${minutes / 60} שע'" else "$minutes דק'"
}

@Composable
private fun SettingsSection(title: String, icon: ImageVector, content: @Composable ColumnScope.() -> Unit) {
    val palette = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(palette.card)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
        }
        content()
    }
}

@Composable
private fun SettingLabel(text: String) {
    Text(text, style = MaterialTheme.typography.labelLarge, color = LocalPalette.current.inkMuted)
}

@Composable
private fun SwitchRow(title: String, subtitle: String?, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .toggleable(value = checked, role = Role.Switch, onValueChange = onCheckedChange)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, color = palette.ink)
            if (subtitle != null) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = palette.inkMuted)
            }
        }
        Switch(
            checked = checked,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedThumbColor = palette.onAccent,
                checkedTrackColor = palette.accent,
                checkedBorderColor = palette.accent,
                uncheckedThumbColor = palette.inkMuted,
                uncheckedTrackColor = palette.cardSoft,
                uncheckedBorderColor = palette.inkMuted,
            )
        )
    }
}

/** Mini preview of a color theme: its card color with the accent dot, in the current light/dark mode. */
@Composable
private fun ThemeSwatch(option: ColorThemeOption, dark: Boolean, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val preview = if (dark) option.dark else option.light
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(preview.background)
                .border(
                    width = if (selected) 2.5.dp else 1.dp,
                    color = if (selected) palette.accent else palette.ink.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(18.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(preview.highlight),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape)
                        .background(preview.accent),
                    contentAlignment = Alignment.Center
                ) {
                    if (selected) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = preview.onAccent, modifier = Modifier.size(12.dp))
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = option.label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) palette.ink else palette.inkMuted
        )
    }
}

