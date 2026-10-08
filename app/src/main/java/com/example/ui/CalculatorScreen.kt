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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
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
        AppTopBar(
            title = "המרת מטבע",
            subtitle = subtitle,
            navigation = { RoundIconButton(Icons.Rounded.Tune, "הגדרות", onOpenSettings) },
            action = { RoundIconButton(Icons.Rounded.Refresh, "עדכון שערים", onRefresh, loading = state.isRefreshing) }
        )

        Spacer(Modifier.height(6.dp))

        CurrencyBlock(
            code = state.sourceCurrency,
            caption = "ממטבע",
            amount = formatAmountInput(state.sourceAmountRaw),
            highlighted = false,
            onClick = { onCurrencyClick(true) }
        )

        Spacer(Modifier.height(8.dp))

        SwapRow(
            rateText = "1 ${state.sourceCurrency} = ${formatRate(state.rate)} ${state.targetCurrency}",
            onSwap = onSwap
        )

        Spacer(Modifier.height(8.dp))

        CurrencyBlock(
            code = state.targetCurrency,
            caption = "למטבע",
            amount = formatAmount(state.targetAmount),
            highlighted = true,
            onClick = { onCurrencyClick(false) }
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
                    .heightIn(max = 360.dp)
                    .fillMaxHeight()
            )
        }

        Spacer(Modifier.height(12.dp))
    }
}

/** Currency header card stacked on top of its amount card (reference: "USD" over "1250"). */
@Composable
private fun CurrencyBlock(
    code: String,
    caption: String,
    amount: String,
    highlighted: Boolean,
    onClick: () -> Unit,
) {
    val palette = LocalPalette.current
    val info = getCurrencyInfo(code)
    val headerColor = if (highlighted) palette.highlight else palette.card
    val bodyColor = if (highlighted) palette.highlightSoft else palette.cardSoft

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp, bottomStart = 10.dp, bottomEnd = 10.dp))
                .background(headerColor)
                .clickable(role = Role.Button, onClickLabel = "בחירת מטבע", onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FlagBadge(info.flag, background = bodyColor)
            Spacer(Modifier.width(10.dp))
            Text(code, style = CurrencyCodeTextStyle, color = palette.ink)
            Icon(
                Icons.Rounded.KeyboardArrowDown,
                contentDescription = null,
                tint = palette.inkMuted,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 12.dp)) {
                Text(caption, style = MaterialTheme.typography.labelMedium, color = palette.inkMuted)
                Text(
                    text = info.hebrewName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 10.dp, topEnd = 10.dp, bottomStart = 24.dp, bottomEnd = 24.dp))
                .background(bodyColor)
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = amount,
                style = AmountTextStyle.copy(fontSize = amountFontSize(amount)),
                color = palette.ink,
                // Right edge = reading start in Hebrew, lined up with the currency code above.
                textAlign = TextAlign.Right,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = info.symbol,
                style = MaterialTheme.typography.titleMedium.copy(textDirection = TextDirection.Ltr),
                color = palette.inkMuted,
                maxLines = 1,
                modifier = Modifier.padding(start = 12.dp)
            )
        }
    }
}

private fun amountFontSize(text: String) = when {
    text.length <= 9 -> 34.sp
    text.length <= 12 -> 29.sp
    text.length <= 15 -> 24.sp
    else -> 20.sp
}

/** Small square swap button next to the live rate pill (reference: "⇅  $1=€0.919137"). */
@Composable
private fun SwapRow(rateText: String, onSwap: () -> Unit) {
    val palette = LocalPalette.current
    var turns by remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(turns * 180f, label = "swap")

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(18.dp))
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
        Box(
            modifier = Modifier
                .weight(1f)
                .height(52.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(palette.card)
                .padding(horizontal = 18.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = rateText,
                style = MaterialTheme.typography.titleSmall.copy(textDirection = TextDirection.Ltr),
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
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
