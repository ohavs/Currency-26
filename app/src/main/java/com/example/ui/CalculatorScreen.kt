package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.R
import com.example.data.CurrencySlot
import com.example.ui.theme.AmountTextStyle
import com.example.ui.theme.LocalPalette
import com.example.utils.amountSymbol
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatRate
import com.example.utils.formatUpdatedAt
import com.example.utils.currencyName
import com.example.utils.getCurrencyInfo
import com.example.utils.symbolLeads
import kotlin.math.abs

private enum class AppScreen { Calculator, Settings, Picker }

@Composable
fun CalculatorScreen(viewModel: MainViewModel, onRemoveLegacyApp: () -> Unit = {}) {
    val state by viewModel.state.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val colorTheme by viewModel.colorTheme.collectAsState()
    val autoUpdate by viewModel.autoUpdate.collectAsState()
    val appUpdate by viewModel.appUpdate.collectAsState()
    val checkAppUpdates by viewModel.checkAppUpdates.collectAsState()
    val language by viewModel.language.collectAsState()
    val legacyAppInstalled by viewModel.legacyAppInstalled.collectAsState()
    val palette = LocalPalette.current

    val screen = when {
        state.showCurrencySelector -> AppScreen.Picker
        state.showSettings -> AppScreen.Settings
        else -> AppScreen.Calculator
    }

    // Layout direction comes from the app language (RTL for Hebrew, LTR for English/Spanish).
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
                    onSwapSlots = viewModel::swapSlots,
                    onCurrencyClick = viewModel::openCurrencySelector,
                    onKeypad = viewModel::onKeypadPress,
                    onOpenSettings = viewModel::openSettings,
                    onRefresh = viewModel::refreshRates,
                    appUpdate = appUpdate,
                    onStartAppUpdate = viewModel::startAppUpdate,
                    onDismissAppUpdate = viewModel::dismissUpdateBanner,
                    onRemoveExtra = viewModel::removeExtraTarget,
                    legacyAppInstalled = legacyAppInstalled,
                    onRemoveLegacyApp = onRemoveLegacyApp,
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
                    language = language,
                    onLanguageChange = viewModel::setLanguage,
                )
                AppScreen.Picker -> CurrencySelector(
                    state = state,
                    onClose = viewModel::closeCurrencySelector,
                    onSelect = viewModel::selectCurrency,
                    onSearch = viewModel::updateSearchQuery,
                    onRemove = viewModel::removeExtraTarget
                )
            }
        }
    }
}

@Composable
fun CalculatorContent(
    state: CalculatorState,
    onSwap: () -> Unit,
    onCurrencyClick: (CurrencySlot) -> Unit,
    onKeypad: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onRefresh: () -> Unit,
    modifier: Modifier = Modifier,
    appUpdate: AppUpdateState? = null,
    onStartAppUpdate: () -> Unit = {},
    onDismissAppUpdate: () -> Unit = {},
    onSwapSlots: (CurrencySlot, CurrencySlot) -> Unit = { _, _ -> },
    onRemoveExtra: () -> Unit = {},
    legacyAppInstalled: Boolean = false,
    onRemoveLegacyApp: () -> Unit = {},
) {
    val updatedAt = formatUpdatedAt(
        state.lastUpdateTimestamp,
        todayFormat = stringResource(R.string.today_at),
        yesterdayFormat = stringResource(R.string.yesterday_at)
    )
    val subtitle = when {
        state.isRefreshing -> stringResource(R.string.refreshing_rates)
        state.lastRefreshFailed && updatedAt != null -> stringResource(R.string.offline_updated_at, updatedAt)
        state.lastRefreshFailed -> stringResource(R.string.offline)
        updatedAt != null -> stringResource(R.string.updated_at, updatedAt)
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
            navigation = { RoundIconButton(Icons.Rounded.Tune, stringResource(R.string.settings), onOpenSettings) },
            action = {
                RoundIconButton(Icons.Rounded.Refresh, stringResource(R.string.refresh_rates), onRefresh, loading = state.isRefreshing)
            }
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
        if (legacyAppInstalled) {
            LegacyAppBanner(onRemove = onRemoveLegacyApp)
            Spacer(Modifier.height(8.dp))
        }

        Spacer(Modifier.height(4.dp))

        val palette = LocalPalette.current
        val extraCode = state.extraTargetCurrency
        val extraAmount = state.extraTargetAmount
        val entries = buildList {
            add(StackEntry(CurrencySlot.SOURCE, state.sourceCurrency, formatAmountInput(state.sourceAmountRaw), palette.card))
            add(StackEntry(CurrencySlot.TARGET, state.targetCurrency, formatAmount(state.targetAmount), palette.highlight))
            if (extraCode != null && extraAmount != null) {
                add(StackEntry(CurrencySlot.EXTRA_TARGET, extraCode, formatAmount(extraAmount), palette.background, removable = true))
            }
        }
        CurrencyStack(
            entries = entries,
            onSwap = onSwap,
            onCurrencyClick = onCurrencyClick,
            onSwapSlots = onSwapSlots,
            onRemoveExtra = onRemoveExtra
        )

        if (extraCode != null) {
            Spacer(Modifier.height(14.dp))
        } else {
            AddCurrencyButton(onClick = { onCurrencyClick(CurrencySlot.EXTRA_TARGET) })
        }

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
            text = stringResource(R.string.update_available, versionName),
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
                text = if (progress != null) "${(progress * 100).toInt()}%" else stringResource(R.string.update_action),
                style = MaterialTheme.typography.labelLarge,
                color = palette.accent
            )
        }
        Icon(
            Icons.Rounded.Close,
            contentDescription = stringResource(R.string.close),
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

/** The old AI Studio build is still installed (its widget has the old design): offer to uninstall it. */
@Composable
private fun LegacyAppBanner(onRemove: () -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(palette.card)
            .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = palette.ink, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = stringResource(R.string.legacy_app_installed),
            style = MaterialTheme.typography.labelLarge,
            color = palette.ink,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(palette.accent)
                .clickable(role = Role.Button, onClick = onRemove)
                .padding(horizontal = 14.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.legacy_app_remove), style = MaterialTheme.typography.labelLarge, color = palette.onAccent)
        }
    }
}

private val RowHeight = 80.dp
private val RowGap = 6.dp
private val RowShape = RoundedCornerShape(26.dp)

private data class StackEntry(
    val slot: CurrencySlot,
    val code: String,
    val amount: String,
    val cardColor: Color,
    /** The optional extra currency: drawn as a light dashed card with its own remove button. */
    val removable: Boolean = false,
)

/**
 * The currency rows, one per slot, with the swap button in the gap under the source.
 * Long-press a row and drag it onto another one to swap the two currencies. The extra currency can also be
 * dragged onto a trash zone, or long-pressed and released for a small menu (change / remove).
 */
@Composable
private fun CurrencyStack(
    entries: List<StackEntry>,
    onSwap: () -> Unit,
    onCurrencyClick: (CurrencySlot) -> Unit,
    onSwapSlots: (CurrencySlot, CurrencySlot) -> Unit,
    onRemoveExtra: () -> Unit,
) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    var turns by remember { mutableIntStateOf(0) }
    val rotation by animateFloatAsState(turns * 180f, label = "swap")

    // Drag-to-swap: the lifted row, how far it moved, and the row (or trash zone) it currently hovers over.
    var dragging by remember { mutableStateOf<CurrencySlot?>(null) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    var dragDistance by remember { mutableFloatStateOf(0f) }
    var hover by remember { mutableStateOf<CurrencySlot?>(null) }
    var overTrash by remember { mutableStateOf(false) }
    var menuSlot by remember { mutableStateOf<CurrencySlot?>(null) }
    val bounds = remember { mutableStateMapOf<CurrencySlot, Rect>() }
    val visibleSlots by rememberUpdatedState(entries.map { it.slot })
    val currentOnSwapSlots by rememberUpdatedState(onSwapSlots)
    val currentOnRemoveExtra by rememberUpdatedState(onRemoveExtra)
    // The trash zone appears right under the rows (over the keypad) while the extra currency is dragged.
    val trashTop = RowHeight * entries.size + RowGap * (entries.size - 1) + 10.dp
    val currentTrashTop by rememberUpdatedState(trashTop)

    fun endDrag() {
        dragging = null
        dragOffset = 0f
        dragDistance = 0f
        hover = null
        overTrash = false
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (dragging != null) 1f else 0f)
    ) {
        // Drawn before the rows so the lifted row stays on top of it.
        if (dragging == CurrencySlot.EXTRA_TARGET) {
            TrashZone(
                active = overTrash,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = trashTop)
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(RowGap)) {
            entries.forEach { entry ->
                val slot = entry.slot
                val isDragged = dragging == slot
                val isHover = hover == slot
                val scale by animateFloatAsState(
                    targetValue = when {
                        isDragged && overTrash -> 0.92f
                        isDragged -> 1.03f
                        isHover -> 0.97f
                        else -> 1f
                    },
                    label = "rowScale"
                )
                Box(
                    modifier = Modifier
                        .onGloballyPositioned { bounds[slot] = it.boundsInParent() }
                        .zIndex(if (isDragged) 1f else 0f)
                        .graphicsLayer {
                            translationY = if (isDragged) dragOffset else 0f
                            scaleX = scale
                            scaleY = scale
                            alpha = if (isDragged && overTrash) 0.7f else 1f
                            shadowElevation = if (isDragged) 16.dp.toPx() else 0f
                            shape = RowShape
                        }
                        .pointerInput(slot) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    dragging = slot
                                    dragOffset = 0f
                                    dragDistance = 0f
                                    hover = null
                                    overTrash = false
                                    haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDrag = { change, amount ->
                                    change.consume()
                                    dragOffset += amount.y
                                    dragDistance += abs(amount.y)
                                    val centerY = (bounds[slot]?.center?.y ?: 0f) + dragOffset
                                    val trash = slot == CurrencySlot.EXTRA_TARGET && centerY >= currentTrashTop.toPx()
                                    val over = if (trash) null else visibleSlots.firstOrNull { other ->
                                        other != slot && bounds[other]?.let { centerY in it.top..it.bottom } == true
                                    }
                                    if (over != hover || trash != overTrash) {
                                        if (over != null || trash) haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                        hover = over
                                        overTrash = trash
                                    }
                                },
                                onDragEnd = {
                                    val target = hover
                                    val remove = overTrash
                                    val barelyMoved = dragDistance < 12.dp.toPx()
                                    endDrag()
                                    when {
                                        remove -> currentOnRemoveExtra()
                                        target != null -> currentOnSwapSlots(slot, target)
                                        // Long-press and release on the extra currency: offer change / remove.
                                        barelyMoved && slot == CurrencySlot.EXTRA_TARGET -> menuSlot = slot
                                    }
                                },
                                onDragCancel = { endDrag() }
                            )
                        }
                ) {
                    CurrencyRow(
                        code = entry.code,
                        amount = entry.amount,
                        cardColor = entry.cardColor,
                        highlighted = isHover,
                        onClick = { onCurrencyClick(slot) },
                        onRemove = if (entry.removable) onRemoveExtra else null
                    )
                    if (slot == CurrencySlot.EXTRA_TARGET) {
                        DropdownMenu(expanded = menuSlot == slot, onDismissRequest = { menuSlot = null }) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.change_currency)) },
                                leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                                onClick = {
                                    menuSlot = null
                                    onCurrencyClick(slot)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove_currency)) },
                                leadingIcon = { Icon(Icons.Rounded.DeleteOutline, contentDescription = null) },
                                onClick = {
                                    menuSlot = null
                                    onRemoveExtra()
                                }
                            )
                        }
                    }
                }
            }
        }
        // Background-colored ring makes the button look cut into the first two cards.
        if (dragging == null) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .offset(y = RowHeight + RowGap / 2 - 25.dp)
                    .size(50.dp)
                    .clip(RoundedCornerShape(19.dp))
                    .background(palette.background)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(15.dp))
                    .background(palette.accent)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.swap_currencies)) {
                        turns++
                        onSwap()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.SwapVert,
                    contentDescription = stringResource(R.string.swap_currencies),
                    tint = palette.onAccent,
                    modifier = Modifier.rotate(rotation)
                )
            }
        }
    }
}

/** Drop target for removing the extra currency. */
@Composable
private fun TrashZone(active: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    val background by animateColorAsState(if (active) palette.accent else palette.card, label = "trash")
    val content = if (active) palette.onAccent else palette.ink
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RowShape)
            .background(background)
            .border(1.5.dp, palette.accent.copy(alpha = 0.4f), RowShape),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Rounded.DeleteOutline, contentDescription = null, tint = content)
        Spacer(Modifier.width(8.dp))
        Text(
            text = stringResource(if (active) R.string.release_to_remove else R.string.drag_to_remove),
            style = MaterialTheme.typography.labelLarge,
            color = content
        )
    }
}

/** One currency in a single row: flag and currency name (tap to change) on one side, symbol and amount on the other. */
@Composable
private fun CurrencyRow(
    code: String,
    amount: String,
    cardColor: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    /** Set for the optional extra currency: shows a dashed "temporary" outline and a small remove button. */
    onRemove: (() -> Unit)? = null,
) {
    val palette = LocalPalette.current
    val info = getCurrencyInfo(code)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(RowHeight)
            .clip(RowShape)
            .background(cardColor)
            .then(if (onRemove != null) Modifier.dashedBorder(palette.inkMuted.copy(alpha = 0.5f)) else Modifier)
            .then(if (highlighted) Modifier.border(2.dp, palette.accent, RowShape) else Modifier)
            .padding(start = 6.dp, end = if (onRemove != null) 8.dp else 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(role = Role.Button, onClickLabel = stringResource(R.string.change_currency), onClick = onClick)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = info.flag, fontSize = 30.sp)
            Spacer(Modifier.width(10.dp))
            Text(
                text = currencyName(code, LocalAppLocale.current),
                style = MaterialTheme.typography.titleMedium.copy(fontSize = 18.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold),
                color = palette.ink,
                // Long names ("Israeli New Shekel") wrap to a second line instead of being cut.
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 150.dp)
            )
        }
        Spacer(Modifier.width(12.dp))
        AutoSizeText(
            text = amountWithSymbol(amount, amountSymbol(code), palette.accent),
            style = AmountTextStyle.copy(fontSize = 32.sp),
            color = palette.ink,
            // Amount on the far side from the currency (left in Hebrew, right in English/Spanish).
            textAlign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextAlign.Left else TextAlign.Right,
            modifier = Modifier.weight(1f)
        )
        if (onRemove != null) {
            Spacer(Modifier.width(6.dp))
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(palette.cardSoft)
                    .clickable(role = Role.Button, onClickLabel = stringResource(R.string.remove_currency), onClick = onRemove),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.remove_currency),
                    tint = palette.inkMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** The amount with its currency symbol beside it ("$ 1,250", "1,250 Ft"): smaller and in the accent color. */
private fun amountWithSymbol(amount: String, symbol: String, symbolColor: Color): AnnotatedString = buildAnnotatedString {
    val symbolStyle = SpanStyle(color = symbolColor, fontSize = 0.7.em, fontWeight = FontWeight.SemiBold)
    if (symbolLeads(symbol)) {
        withStyle(symbolStyle) { append(symbol); append(' ') }
        append(amount)
    } else {
        append(amount)
        withStyle(symbolStyle) { append(' '); append(symbol) }
    }
}

/** Dashed rounded outline marking the optional (temporary) extra currency. */
private fun Modifier.dashedBorder(color: Color) = drawBehind {
    val stroke = 1.5.dp.toPx()
    drawRoundRect(
        color = color,
        topLeft = Offset(stroke / 2, stroke / 2),
        size = Size(size.width - stroke, size.height - stroke),
        cornerRadius = CornerRadius(26.dp.toPx() - stroke / 2),
        style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 5.dp.toPx())))
    )
}

/** Light "+ add currency" link under the pair; a second target currency is optional. */
@Composable
private fun AddCurrencyButton(onClick: () -> Unit) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.inkMuted, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.add_currency), style = MaterialTheme.typography.labelLarge, color = palette.inkMuted)
        }
    }
}

/** Single-line text that shrinks its font (down to [minFontSize]) instead of cutting long amounts. */
@Composable
private fun AutoSizeText(
    text: AnnotatedString,
    style: TextStyle,
    color: Color,
    textAlign: TextAlign,
    modifier: Modifier = Modifier,
    minFontSize: Float = 12f,
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
        Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
            // Clear and backspace share one slim row, so the digits get the full width.
            Row(
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                KeyButton(
                    color = palette.accent,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { press("C") }
                ) {
                    Text("C", style = MaterialTheme.typography.titleLarge, color = palette.onAccent)
                }
                KeyButton(
                    color = palette.highlight,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    onClick = { press("⌫") }
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = stringResource(R.string.backspace), tint = palette.ink)
                }
            }
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
