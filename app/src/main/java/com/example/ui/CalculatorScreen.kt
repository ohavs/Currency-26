package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.AmountTextStyle
import com.example.ui.theme.CurrencyCodeTextStyle
import com.example.ui.theme.LocalPalette
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatRate
import com.example.utils.formatUpdatedAt
import com.example.utils.getCurrencyInfo

private enum class AppScreen { Calculator, Settings, Picker }

@Composable
fun CalculatorScreen(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val colorTheme by viewModel.colorTheme.collectAsState()
    val autoUpdate by viewModel.autoUpdate.collectAsState()
    val appUpdate by viewModel.appUpdate.collectAsState()
    val checkAppUpdates by viewModel.checkAppUpdates.collectAsState()
    val palette = LocalPalette.current

    val screen = when {
        state.showCurrencySelector -> AppScreen.Picker
        state.showSettings -> AppScreen.Settings
        else -> AppScreen.Calculator
    }

    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(palette.background)
        ) {
            AnimatedContent(
                targetState = screen,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(150)) },
                label = "screen"
            ) { target ->
                when (target) {
                    AppScreen.Calculator -> CalculatorContent(
                        state = state,
                        onSwap = viewModel::swapCurrencies,
                        onCurrencyClick = viewModel::openCurrencySelector,
                        onKeypad = viewModel::onKeypadPress,
                        onOpenSettings = viewModel::openSettings,
                        onRefresh = viewModel::refreshRates,
                        appUpdate = appUpdate,
                        onStartAppUpdate = viewModel::startAppUpdate,
                        onDismissAppUpdate = viewModel::dismissUpdateBanner,
                    )
                    AppScreen.Settings -> SettingsScreen(
                        themeMode = themeMode,
                        colorTheme = colorTheme,
                        autoUpdate = autoUpdate,
                        lastUpdateTimestamp = state.lastUpdateTimestamp,
                        isRefreshing = state.isRefreshing,
                        lastRefreshFailed = state.lastRefreshFailed,
                        canPinWidget = viewModel.canPinWidget,
                        onClose = viewModel::closeSettings,
                        onThemeModeChange = viewModel::setThemeMode,
                        onColorThemeChange = viewModel::setColorTheme,
                        onAutoUpdateEnabledChange = viewModel::setAutoUpdateEnabled,
                        onIntervalChange = viewModel::setAutoUpdateInterval,
                        onWifiOnlyChange = viewModel::setAutoUpdateWifiOnly,
                        onRefreshOnOpenChange = viewModel::setRefreshOnOpen,
                        onRefreshNow = viewModel::refreshRates,
                        onAddWidget = viewModel::addWidgetToHomeScreen,
                        appUpdate = appUpdate,
                        checkAppUpdates = checkAppUpdates,
                        onCheckAppUpdatesChange = viewModel::setCheckAppUpdates,
                        onCheckForAppUpdate = viewModel::checkForAppUpdate,
                        onStartAppUpdate = viewModel::startAppUpdate,
                    )
                    AppScreen.Picker -> CurrencySelector(
                        state = state,
                        onClose = viewModel::closeCurrencySelector,
                        onSelect = viewModel::selectCurrency,
                        onSearch = viewModel::updateSearchQuery
                    )
                }
            }
        }
    }
}

@Composable
fun CalculatorContent(
    state: CalculatorState,
    onSwap: () -> Unit,
    onCurrencyClick: (Boolean) -> Unit,
    onKeypad: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    appUpdate: AppUpdateState? = null,
    onStartAppUpdate: () -> Unit = {},
    onDismissAppUpdate: () -> Unit = {},
) {
    val updatedAt = formatUpdatedAt(state.lastUpdateTimestamp)
    val subtitle = when {
        state.isRefreshing -> "מעדכן שערים…"
        state.lastRefreshFailed && updatedAt != null -> "אין חיבור · עודכן $updatedAt"
        state.lastRefreshFailed -> "אין חיבור לאינטרנט"
        updatedAt != null -> "עודכן $updatedAt"
        else -> null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp)
    ) {
        // The header carries the live rate instead of an app title - more useful and saves a row.
        AppTopBar(
            title = "1 ${state.sourceCurrency} = ${formatRate(state.rate)} ${state.targetCurrency}",
            titleStyle = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
            subtitle = subtitle,
            navigation = { RoundIconButton(Icons.Rounded.Tune, "הגדרות", onOpenSettings) },
            action = { RoundIconButton(Icons.Rounded.Refresh, "עדכון שערים", onRefresh, loading = state.isRefreshing) }
        )

        val release = appUpdate?.available
        if (appUpdate != null && release != null && !appUpdate.bannerDismissed) {
            UpdateBanner(
                versionName = release.versionName,
                progress = appUpdate.downloadProgress,
                onUpdate = onStartAppUpdate,
                onDismiss = onDismissAppUpdate
            )
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(4.dp))

        CurrencyPair(
            sourceCode = state.sourceCurrency,
            sourceAmount = formatAmountInput(state.sourceAmountRaw),
            targetCode = state.targetCurrency,
            targetAmount = formatAmount(state.targetAmount),
            onSwap = onSwap,
            onCurrencyClick = onCurrencyClick
        )

        Spacer(Modifier.height(14.dp))

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter
        ) {
            Keypad(
                onKey = onKeypad,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 420.dp)
                    .fillMaxHeight()
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

/** "A new version is available" strip above the calculator. */
@Composable
private fun UpdateBanner(versionName: String, progress: Float?, onUpdate: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.accent)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.SystemUpdate, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = "גרסה $versionName זמינה",
            style = MaterialTheme.typography.labelLarge,
            color = palette.onAccent,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(palette.onAccent)
                .clickable(enabled = progress == null, role = Role.Button, onClick = onUpdate)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (progress != null) "${(progress * 100).toInt()}%" else "עדכון",
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent
            )
        }
        Icon(
            Icons.Rounded.Close,
            contentDescription = "סגירה",
            tint = palette.onAccent,
            modifier = Modifier
                .padding(start = 4.dp)
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onDismiss)
                .padding(8.dp)
                .size(18.dp)
        )
    }
}

/** Source and target cards, one row each, with the swap button sitting in the gap between them. */
@Composable
private fun CurrencyPair(
    sourceCode: String,
    sourceAmount: String,
    targetCode: String,
    targetAmount: String,
    onSwap: () -> Unit,
    onCurrencyClick: (Boolean) -> Unit,
) {
    val palette = LocalPalette.current
    var turns by remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(turns * 180f, label = "swap")

    Box(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            CurrencyRow(
                code = sourceCode,
                amount = sourceAmount,
                cardColor = palette.card,
                chipColor = palette.cardSoft,
                onClick = { onCurrencyClick(true) }
            )
            CurrencyRow(
                code = targetCode,
                amount = targetAmount,
                cardColor = palette.highlight,
                chipColor = palette.highlightSoft,
                onClick = { onCurrencyClick(false) }
            )
        }
        // Background-colored ring makes the button look cut into both cards.
        Box(
            modifier = Modifier
                .align(Alignment.Center)
                .size(50.dp)
                .clip(RoundedCornerShape(19.dp))
                .background(palette.background)
                .padding(4.dp)
                .clip(RoundedCornerShape(15.dp))
                .background(palette.accent)
                .clickable(role = Role.Button, onClickLabel = "החלפת מטבעות") {
                    turns++
                    onSwap()
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Rounded.SwapVert,
                contentDescription = "החלפת מטבעות",
                tint = palette.onAccent,
                modifier = Modifier.rotate(rotation)
            )
        }
    }
}

/** One currency in a single row: tappable currency chip on one side, the amount on the other. */
@Composable
private fun CurrencyRow(
    code: String,
    amount: String,
    cardColor: Color,
    chipColor: Color,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val info = getCurrencyInfo(code)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(84.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(cardColor)
            .padding(start = 10.dp, end = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(chipColor)
                .clickable(role = Role.Button, onClickLabel = "בחירת מטבע", onClick = onClick)
                .padding(start = 6.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagBadge(info.flag, background = cardColor, size = 38.dp)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(code, style = CurrencyCodeTextStyle.copy(fontSize = 19.sp, lineHeight = 22.sp), color = palette.ink)
                Text(
                    text = info.hebrewName,
                    style = MaterialTheme.typography.labelSmall,
                    color = palette.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.widthIn(max = 92.dp)
                )
            }
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = palette.inkMuted,
                modifier = Modifier.size(20.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        AutoSizeText(
            text = amount,
            style = AmountTextStyle.copy(fontSize = 32.sp),
            color = palette.ink,
            // Amount on the far side from the chip, like the original layout.
            textAlign = TextAlign.Left,
            modifier = Modifier.weight(1f)
        )
    }
}

/** Single-line text that shrinks its font (down to [minFontSize]) instead of cutting long amounts. */
@Composable
private fun AutoSizeText(
    text: String,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    minFontSize: Float = 16f,
) {
    BoxWithConstraints(modifier = modifier) {
        val measurer = rememberTextMeasurer()
        val maxWidth = constraints.maxWidth
        val fontSize = remember(text, maxWidth, style) {
            var size = style.fontSize.value
            while (size > minFontSize &&
                measurer.measure(text, style.copy(fontSize = size.sp), maxLines = 1, softWrap = false).size.width > maxWidth
            ) {
                size -= 1f
            }
            size.sp
        }
        Text(
            text = text,
            style = style.copy(fontSize = fontSize),
            color = color,
            textAlign = textAlign,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun Keypad(onKey: (String) -> Unit, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    val press: (String) -> Unit = { key ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        onKey(key)
    }
    val digitRows = listOf(
        listOf("7", "8", "9"),
        listOf("4", "5", "6"),
        listOf("1", "2", "3"),
        listOf(".", "0", "00")
    )

    // Numbers keep the familiar left-to-right keypad order inside the RTL app.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Column(
                modifier = Modifier
                    .weight(3f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                digitRows.forEach { row ->
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        row.forEach { key ->
                            KeyButton(
                                color = palette.cardSoft,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                onClick = { press(key) }
                            ) {
                                Text(key, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp), color = palette.ink)
                            }
                        }
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeyButton(
                    color = palette.card,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onClick = { press("⌫") }
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = "מחיקה", tint = palette.ink)
                }
                KeyButton(
                    color = palette.accent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    onClick = { press("C") }
                ) {
                    Text("C", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp), color = palette.onAccent)
                }
            }
        }
    }
}

@Composable
private fun KeyButton(
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(color)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
