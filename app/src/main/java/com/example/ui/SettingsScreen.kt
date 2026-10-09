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
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AutoUpdateSettings
import com.example.ui.theme.ColorThemeOption
import com.example.ui.theme.LocalPalette
import com.example.ui.theme.Palettes
import com.example.utils.AppLanguage
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
    appUpdate: AppUpdateState,
    checkAppUpdates: Boolean,
    onCheckAppUpdatesChange: (Boolean) -> Unit,
    onCheckForAppUpdate: () -> Unit,
    onStartAppUpdate: () -> Unit,
    language: String = AppLanguage.DEFAULT,
    onLanguageChange: (String) -> Unit = {},
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
            title = stringResource(R.string.settings),
            navigation = { RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onClose) }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            SettingsSection(title = stringResource(R.string.language), icon = Icons.Rounded.Language) {
                SegmentedControl(
                    options = AppLanguage.OPTIONS + (AppLanguage.SYSTEM to stringResource(R.string.language_system)),
                    selected = language,
                    onSelect = onLanguageChange,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            SettingsSection(title = stringResource(R.string.appearance), icon = Icons.Rounded.Palette) {
                SettingLabel(stringResource(R.string.display_mode))
                SegmentedControl(
                    options = listOf(
                        "light" to stringResource(R.string.mode_light),
                        "dark" to stringResource(R.string.mode_dark),
                        "system" to stringResource(R.string.mode_system)
                    ),
                    selected = themeMode,
                    onSelect = onThemeModeChange,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(4.dp))
                SettingLabel(stringResource(R.string.color_theme))
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

            SettingsSection(title = stringResource(R.string.app_updates), icon = Icons.Rounded.SystemUpdate) {
                SwitchRow(
                    title = stringResource(R.string.auto_check_updates),
                    subtitle = stringResource(R.string.auto_check_updates_desc),
                    checked = checkAppUpdates,
                    onCheckedChange = onCheckAppUpdatesChange
                )
                val available = appUpdate.available
                val downloading = appUpdate.downloadProgress
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(palette.cardSoft)
                        .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(stringResource(R.string.installed_version, appUpdate.installedVersion), style = MaterialTheme.typography.labelMedium, color = palette.inkMuted)
                        Text(
                            text = when {
                                downloading != null -> stringResource(R.string.downloading, (downloading * 100).toInt())
                                appUpdate.checking -> stringResource(R.string.checking_updates)
                                available != null -> stringResource(R.string.update_available, available.versionName)
                                appUpdate.failed -> stringResource(R.string.update_check_failed)
                                appUpdate.checked -> stringResource(R.string.up_to_date)
                                else -> stringResource(R.string.not_checked)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.ink
                        )
                    }
                    if (available != null) {
                        PillButton(
                            text = stringResource(R.string.update_action),
                            icon = Icons.Rounded.SystemUpdate,
                            loading = downloading != null,
                            onClick = onStartAppUpdate
                        )
                    } else {
                        PillButton(
                            text = stringResource(R.string.check_action),
                            icon = Icons.Rounded.Refresh,
                            loading = appUpdate.checking,
                            onClick = onCheckForAppUpdate
                        )
                    }
                }
                if (appUpdate.needsInstallPermission) {
                    Text(
                        text = stringResource(R.string.install_permission_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.rate_updates), icon = Icons.Rounded.Update) {
                SwitchRow(
                    title = stringResource(R.string.background_updates),
                    subtitle = stringResource(R.string.background_updates_desc),
                    checked = autoUpdate.enabled,
                    onCheckedChange = onAutoUpdateEnabledChange
                )
                AnimatedVisibility(visible = autoUpdate.enabled) {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        SettingLabel(stringResource(R.string.update_frequency))
                        ChipSelector(
                            options = AutoUpdateSettings.INTERVAL_OPTIONS.map { it to intervalLabel(it) },
                            selected = autoUpdate.intervalMinutes,
                            onSelect = onIntervalChange,
                            modifier = Modifier.fillMaxWidth()
                        )
                        SwitchRow(
                            title = stringResource(R.string.wifi_only),
                            subtitle = stringResource(R.string.wifi_only_desc),
                            checked = autoUpdate.wifiOnly,
                            onCheckedChange = onWifiOnlyChange
                        )
                    }
                }
                SwitchRow(
                    title = stringResource(R.string.refresh_on_open),
                    subtitle = stringResource(R.string.refresh_on_open_desc),
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
                        Text(stringResource(R.string.last_updated), style = MaterialTheme.typography.labelMedium, color = palette.inkMuted)
                        Text(
                            text = when {
                                isRefreshing -> stringResource(R.string.updating)
                                lastRefreshFailed -> stringResource(R.string.update_failed)
                                else -> formatUpdatedAt(
                                    lastUpdateTimestamp,
                                    todayFormat = stringResource(R.string.today_at),
                                    yesterdayFormat = stringResource(R.string.yesterday_at)
                                ) ?: stringResource(R.string.never_updated)
                            },
                            style = MaterialTheme.typography.bodyLarge,
                            color = palette.ink
                        )
                    }
                    PillButton(
                        text = stringResource(R.string.update_now),
                        icon = Icons.Rounded.Refresh,
                        loading = isRefreshing,
                        onClick = onRefreshNow
                    )
                }
            }

            SettingsSection(title = stringResource(R.string.widget), icon = Icons.Rounded.Widgets) {
                Text(
                    text = stringResource(R.string.widget_desc),
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.inkMuted
                )
                PillButton(
                    text = stringResource(R.string.add_widget),
                    icon = Icons.Rounded.Widgets,
                    enabled = canPinWidget,
                    onClick = onAddWidget,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!canPinWidget) {
                    Text(
                        text = stringResource(R.string.add_widget_manual),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                }
            }
        }
    }
}

@Composable
private fun intervalLabel(minutes: Long): String = when {
    minutes == 60L -> stringResource(R.string.interval_hour)
    minutes == 1440L -> stringResource(R.string.interval_day)
    minutes % 60L == 0L -> stringResource(R.string.interval_hours, (minutes / 60).toInt())
    else -> stringResource(R.string.interval_minutes, minutes.toInt())
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
            text = stringResource(option.labelRes),
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) palette.ink else palette.inkMuted
        )
    }
}

