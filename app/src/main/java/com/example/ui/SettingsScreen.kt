package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.text.style.TextOverflow
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
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.HighlightAlt
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.ui.platform.LocalUriHandler
import com.example.premium.Access
import com.example.premium.FREE_COLOR_THEME
import com.example.premium.PremiumRules
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.TipsAndUpdates
import androidx.compose.material.icons.rounded.Update
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import kotlinx.coroutines.delay
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.toSize
import kotlin.math.roundToInt
import androidx.compose.ui.geometry.Rect
import androidx.compose.runtime.remember
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.AutoUpdateSettings
import com.example.ui.theme.ColorThemeOption
import com.example.ui.theme.LocalPalette
import com.example.ui.theme.Palettes
import com.example.utils.AppLanguage
import com.example.utils.ConvertedPrice
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
    onShowTour: () -> Unit = {},
    /** Walks the user to "add widget": the widget section opens, scrolls into view and gets a spotlight. */
    highlightAddWidget: Boolean = false,
    showAppUpdates: Boolean = true,
    /** The Play build's Premium row; null in the GitHub build. */
    premium: PremiumSummary? = null,
    onOpenPremium: () -> Unit = {},
    /** Colour sets other than Classic carry a lock and open Premium. */
    colorsLocked: Boolean = false,
    adPrivacyOptions: Boolean = false,
    onAdPrivacyOptions: () -> Unit = {},
    onWidgetGuideDone: () -> Unit = {},
    /** [SAMPLE_PRICE] in the user's currency, for the "Convert from any app" demo (null before there are rates). */
    quickSample: ConvertedPrice? = null,
    /** Walks the user to the sample price: "Convert from any app" opens, scrolls into view and the price gets a spotlight. */
    highlightSelectMenu: Boolean = false,
    onSelectGuideDone: () -> Unit = {},
) {
    BackHandler(onBack = onClose)
    BackHandler(enabled = highlightAddWidget, onBack = onWidgetGuideDone)
    BackHandler(enabled = highlightSelectMenu, onBack = onSelectGuideDone)
    val palette = LocalPalette.current
    val scroll = rememberScrollState()
    var addWidgetBounds by remember { mutableStateOf<Rect?>(null) }
    var samplePriceBounds by remember { mutableStateOf<Rect?>(null) }
    var viewportBounds by remember { mutableStateOf<Rect?>(null) }
    var spotlightReady by remember { mutableStateOf(false) }
    LaunchedEffect(highlightAddWidget, highlightSelectMenu) {
        spotlightReady = false
        if (highlightAddWidget) {
            // Let the widget section finish opening, then bring it (the last section) into view.
            delay(380)
            scroll.animateScrollTo(scroll.maxValue)
            spotlightReady = true
        } else if (highlightSelectMenu) {
            // The price comes up a little above the middle, with room for the tooltip over it.
            delay(380)
            val price = samplePriceBounds
            val viewport = viewportBounds
            if (price != null && viewport != null) {
                val goal = scroll.value + price.top - viewport.top - viewport.height * 0.4f
                scroll.animateScrollTo(goal.roundToInt().coerceIn(0, scroll.maxValue))
            }
            spotlightReady = true
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                    .onGloballyPositioned { viewportBounds = it.boundsInWindow() }
                    .verticalScroll(scroll)
                    .padding(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                if (premium != null) PremiumRow(premium, onOpenPremium)

                // Every section starts closed: its title and a one-line summary of the current values.
                val languageSummary = if (language == AppLanguage.SYSTEM) {
                    stringResource(R.string.language_system)
                } else {
                    AppLanguage.OPTIONS.firstOrNull { it.first == language }?.second
                }
                SettingsSection(title = stringResource(R.string.language), icon = Icons.Rounded.Language, summary = languageSummary) {
                    LanguageGrid(
                        options = listOf(AppLanguage.SYSTEM to stringResource(R.string.language_system)) + AppLanguage.OPTIONS,
                        selected = language,
                        onSelect = onLanguageChange
                    )
                }

                val modeLabel = when (themeMode) {
                    "light" -> stringResource(R.string.mode_light)
                    "dark" -> stringResource(R.string.mode_dark)
                    else -> stringResource(R.string.mode_system)
                }
                val themeLabel = stringResource(Palettes.option(colorTheme).labelRes)
                SettingsSection(title = stringResource(R.string.appearance), icon = Icons.Rounded.Palette, summary = "$modeLabel · $themeLabel") {
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
                    val selectedKey = Palettes.normalize(colorTheme)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Palettes.options.chunked(3).forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                row.forEach { option ->
                                    ThemeIconTile(
                                        option = option,
                                        selected = option.key == selectedKey,
                                        locked = colorsLocked && option.key != FREE_COLOR_THEME,
                                        onClick = { onColorThemeChange(option.key) },
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }
                }

                val updatesSummary = appUpdate.available?.let { stringResource(R.string.update_available, it.versionName) }
                    ?: stringResource(R.string.installed_version, appUpdate.installedVersion)
                // Google Play updates the app by itself; only the GitHub build has its own updater.
                if (showAppUpdates) SettingsSection(title = stringResource(R.string.app_updates), icon = Icons.Rounded.SystemUpdate, summary = updatesSummary) {
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

                val ratesSummary = if (autoUpdate.enabled) {
                    stringResource(R.string.background_updates_every, intervalLabel(autoUpdate.intervalMinutes))
                } else {
                    stringResource(R.string.background_updates_off)
                }
                SettingsSection(title = stringResource(R.string.rate_updates), icon = Icons.Rounded.Update, summary = ratesSummary) {
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

                val menu = rememberSelectionMenuInfo()
                SettingsSection(
                    title = stringResource(R.string.tip_select_title),
                    icon = Icons.Rounded.HighlightAlt,
                    summary = stringResource(R.string.select_menu_summary, menu.menuLabel),
                    initiallyExpanded = highlightSelectMenu
                ) {
                    Text(
                        text = stringResource(R.string.select_menu_body, menu.menuLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkMuted
                    )
                    if (menu.samsung) {
                        Text(
                            text = stringResource(R.string.select_menu_samsung),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.ink
                        )
                    }
                    // On Samsung the demo turns the item on first (it ends with the conversion too, so one is enough).
                    SelectionMenuDemo(menu, SAMPLE_PRICE, quickSample, setup = menu.samsung)
                    // A real selection menu, right here: the quickest way to Samsung's "Manage apps", and a try-out.
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .background(palette.cardSoft)
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // The spotlight's tooltip says the same, and its edge would cut through this line.
                        Text(
                            text = stringResource(R.string.select_menu_try),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.inkMuted,
                            modifier = Modifier.alpha(if (highlightSelectMenu && spotlightReady) 0f else 1f)
                        )
                        SelectablePrice(
                            text = SAMPLE_PRICE,
                            modifier = Modifier
                                .fillMaxWidth()
                                // Not clipped to the screen: the guide measures it before scrolling it into view.
                                .onGloballyPositioned { samplePriceBounds = Rect(it.positionInWindow(), it.size.toSize()) },
                            onLongPress = { if (highlightSelectMenu) onSelectGuideDone() }
                        )
                    }
                    if (menu.samsung) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            listOf(
                                stringResource(R.string.select_menu_step1),
                                stringResource(R.string.select_menu_step2),
                                stringResource(R.string.select_menu_step3, menu.appLabel)
                            ).forEachIndexed { index, step -> StepRow(index + 1, step) }
                        }
                    }
                    Text(
                        text = stringResource(R.string.select_menu_share, menu.menuLabel),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                }

                SettingsSection(
                    title = stringResource(R.string.widget),
                    icon = Icons.Rounded.Widgets,
                    summary = null,
                    initiallyExpanded = highlightAddWidget
                ) {
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .onGloballyPositioned { addWidgetBounds = it.boundsInWindow() }
                    )
                    if (!canPinWidget) {
                        Text(
                            text = stringResource(R.string.add_widget_manual),
                            style = MaterialTheme.typography.bodySmall,
                            color = palette.inkMuted
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(18.dp))
                            .clickable(role = Role.Button, onClick = onShowTour)
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.TipsAndUpdates, contentDescription = null, tint = palette.ink, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(stringResource(R.string.show_tips), style = MaterialTheme.typography.labelLarge, color = palette.ink)
                    }
                }

                val uriHandler = LocalUriHandler.current
                SettingsSection(
                    title = stringResource(R.string.about),
                    icon = Icons.Rounded.Info,
                    summary = stringResource(R.string.installed_version, appUpdate.installedVersion)
                ) {
                    // The free rates API asks for this credit wherever its rates are shown.
                    LinkRow(stringResource(R.string.rates_source)) { runCatching { uriHandler.openUri(AppLinks.RATES_SOURCE) } }
                    Text(
                        text = stringResource(R.string.rates_disclaimer),
                        style = MaterialTheme.typography.bodySmall,
                        color = palette.inkMuted
                    )
                    LinkRow(stringResource(R.string.privacy_policy)) { runCatching { uriHandler.openUri(AppLinks.PRIVACY_POLICY) } }
                    if (adPrivacyOptions) LinkRow(stringResource(R.string.ad_privacy), onClick = onAdPrivacyOptions)
                    Text(
                        text = stringResource(R.string.installed_version, appUpdate.installedVersion),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkMuted
                    )
                }
            }
        }

        val target = addWidgetBounds
        if (highlightAddWidget && spotlightReady && canPinWidget && target != null) {
            Spotlight(
                target = target,
                message = stringResource(R.string.guide_add_widget),
                onTargetTap = {
                    onWidgetGuideDone()
                    onAddWidget()
                },
                onDismiss = onWidgetGuideDone
            )
        }
        val price = samplePriceBounds
        if (highlightSelectMenu && spotlightReady && price != null) {
            // The price stays live under the spotlight: a long press selects it and the guide steps aside.
            Spotlight(
                target = price,
                message = stringResource(R.string.select_menu_try),
                onTargetTap = null,
                onDismiss = onSelectGuideDone
            )
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

/** The price Settings offers to long-press, and that its demo converts. */
internal const val SAMPLE_PRICE = "€120"

/**
 * A settings card that opens and closes by tapping its title row. Closed, it shows only the title and
 * [summary] (the current values), so the screen stays short and calm.
 */
@Composable
private fun SettingsSection(
    title: String,
    icon: ImageVector,
    summary: String?,
    initiallyExpanded: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalPalette.current
    var expanded by rememberSaveable { mutableStateOf(initiallyExpanded) }
    // Opened by a guide while the screen is already showing.
    LaunchedEffect(initiallyExpanded) { if (initiallyExpanded) expanded = true }
    val chevron by animateFloatAsState(if (expanded) 180f else 0f, label = "chevron")
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(palette.card)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(if (expanded) R.string.collapse_section else R.string.expand_section)
                ) { expanded = !expanded }
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
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
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = palette.ink)
                AnimatedVisibility(visible = !expanded && summary != null) {
                    Text(
                        text = summary.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        color = palette.inkMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            Icon(
                Icons.Rounded.ExpandMore,
                contentDescription = null,
                tint = palette.inkMuted,
                modifier = Modifier.rotate(chevron)
            )
        }
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(
                modifier = Modifier.padding(start = 18.dp, end = 18.dp, bottom = 18.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                content = content
            )
        }
    }
}

/** Two columns of language pills, each name in its own language; the chosen one is filled. */
@Composable
private fun LanguageGrid(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
    val palette = LocalPalette.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEach { (code, name) ->
                    val isSelected = code == selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(44.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (isSelected) palette.accent else palette.cardSoft)
                            .clickable(role = Role.RadioButton) { onSelect(code) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.labelLarge,
                            color = if (isSelected) palette.onAccent else palette.ink,
                            maxLines = 1
                        )
                    }
                }
                // Keeps the last pill half-width when the count is odd.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
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

/** A colour set as the app icon drawn in its colours and motif, named after it; the chosen one is framed. */
@Composable
private fun ThemeIconTile(option: ColorThemeOption, selected: Boolean, locked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Box(modifier = modifier) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) palette.cardSoft else Color.Transparent)
                .border(2.dp, if (selected) palette.accent else Color.Transparent, RoundedCornerShape(16.dp))
                .clickable(role = Role.RadioButton, onClick = onClick)
                .padding(vertical = 8.dp, horizontal = 2.dp)
        ) {
            Image(
                painter = painterResource(option.iconRes),
                contentDescription = null,
                modifier = Modifier.size(56.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = stringResource(option.labelRes),
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) palette.ink else palette.inkMuted,
                maxLines = 1
            )
        }
        if (locked) LockBadge()
    }
}

/** Premium as the Settings row shows it: what is unlocked now and until when. */
data class PremiumSummary(val access: Access, val purchasePending: Boolean)

/** The first row of Settings in the Play build: Premium's state; tapping it opens the Premium sheet. */
@Composable
private fun PremiumRow(premium: PremiumSummary, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val summary = when {
        premium.access.purchased -> stringResource(R.string.premium_owned)
        else -> premiumStatus(premium.access, premium.purchasePending)
            ?: stringResource(R.string.premium_settings_free, PremiumRules.VIDEOS_PER_UNLOCK, PremiumRules.REWARD_HOURS)
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(palette.highlight)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(palette.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.WorkspacePremium, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(stringResource(R.string.premium), style = MaterialTheme.typography.titleMedium, color = palette.ink)
            Text(summary, style = MaterialTheme.typography.labelMedium, color = palette.inkMuted, maxLines = 2, overflow = TextOverflow.Ellipsis)
        }
        Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = palette.inkMuted)
    }
}

/** A numbered step: the number in an accent circle, then the text. */
@Composable
private fun StepRow(number: Int, text: String) {
    val palette = LocalPalette.current
    Row(verticalAlignment = Alignment.Top) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(palette.accent),
            contentAlignment = Alignment.Center
        ) {
            Text("$number", style = MaterialTheme.typography.labelMedium, color = palette.onAccent)
        }
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium, color = palette.ink, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun LinkRow(text: String, onClick: () -> Unit) {
    val palette = LocalPalette.current
    Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
        color = palette.ink,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(palette.cardSoft)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    )
}
