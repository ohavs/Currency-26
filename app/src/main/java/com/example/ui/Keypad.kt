package com.example.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Backspace
import androidx.compose.material.icons.rounded.Calculate
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmountTextStyle
import com.example.ui.theme.LocalPalette
import com.example.utils.formatAmountInput
import kotlinx.coroutines.delay

private val KeyGap = 10.dp
private const val CALCULATOR_KEY = "calc"
private val DigitRows = listOf(
    listOf("7", "8", "9"),
    listOf("4", "5", "6"),
    listOf("1", "2", "3"),
    listOf(".", "0", CALCULATOR_KEY),
)
/** The operator beside each digit row, top to bottom; the keys send ASCII, the labels are real signs. */
private val Operators = listOf("/" to "÷", "*" to "×", "-" to "−", "+" to "+")

/**
 * The keypad. Where "00" used to be is the calculator key: it opens a column of operators beside the digits,
 * with brackets and percent between clear and backspace. While there is something to compute it shows "=",
 * otherwise it closes the calculator again. The digits never move, they only get narrower.
 * While the amount is zero, clear and backspace have nothing to do: their row offers the history and a few
 * round amounts instead ([quickAmounts], null when the row should keep its keys).
 */
@Composable
internal fun Keypad(
    calculatorOpen: Boolean,
    canEvaluate: Boolean,
    quickAmounts: List<Long>?,
    currencySymbol: String,
    onKey: (String) -> Unit,
    onToggleCalculator: () -> Unit,
    onQuickAmount: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    val press: (String) -> Unit = { key ->
        haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (key == CALCULATOR_KEY) onToggleCalculator() else onKey(key)
    }
    // 0 = plain amount keypad, 1 = calculator keys fully open.
    val open by animateFloatAsState(if (calculatorOpen) 1f else 0f, tween(260), label = "calculator")
    val showQuick = quickAmounts != null && !calculatorOpen

    // Numbers keep the familiar left-to-right keypad order inside the RTL app.
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        Column(modifier = modifier) {
            AnimatedContent(
                targetState = showQuick,
                transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
                label = "topRow",
                modifier = Modifier
                    .weight(0.62f)
                    .fillMaxWidth()
            ) { quick ->
                if (quick && quickAmounts != null) {
                    QuickAmountRow(quickAmounts, currencySymbol, onQuickAmount, onOpenHistory, Modifier.fillMaxSize())
                } else {
                    // Clear and backspace share one slim row, so the digits get the full width; brackets and
                    // percent open up between them.
                    Row(Modifier.fillMaxSize()) {
                        KeyButton(palette.accent, Modifier.weight(1.5f - 0.5f * open).fillMaxHeight(), onClick = { press("C") }) {
                            Text("C", style = MaterialTheme.typography.titleLarge, color = palette.onAccent)
                        }
                        if (open > 0f) {
                            CalculatorKey("()", "( )", open, Modifier.weight(open), press)
                            CalculatorKey("%", "%", open, Modifier.weight(open), press)
                        }
                        Spacer(Modifier.width(KeyGap))
                        KeyButton(palette.highlight, Modifier.weight(1.5f - 0.5f * open).fillMaxHeight(), onClick = { press("⌫") }) {
                            Icon(Icons.AutoMirrored.Rounded.Backspace, contentDescription = stringResource(R.string.backspace), tint = palette.ink)
                        }
                    }
                }
            }
            DigitRows.forEachIndexed { row, keys ->
                Spacer(Modifier.height(KeyGap))
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                ) {
                    keys.forEachIndexed { index, key ->
                        if (index > 0) Spacer(Modifier.width(KeyGap))
                        if (key == CALCULATOR_KEY) {
                            CalculatorToggle(calculatorOpen, canEvaluate, Modifier.weight(1f).fillMaxHeight(), press)
                        } else {
                            KeyButton(palette.cardSoft, Modifier.weight(1f).fillMaxHeight(), onClick = { press(key) }) {
                                Text(key, style = MaterialTheme.typography.headlineSmall.copy(fontSize = 26.sp), color = palette.ink)
                            }
                        }
                    }
                    if (open > 0f) {
                        val (key, label) = Operators[row]
                        CalculatorKey(key, label, open, Modifier.weight(open), press)
                    }
                }
            }
        }
    }
}

/** An operator, bracket or percent key: slides in with the calculator ([open] 0..1), gap included. */
@Composable
private fun RowScope.CalculatorKey(key: String, label: String, open: Float, modifier: Modifier, press: (String) -> Unit) {
    val palette = LocalPalette.current
    Spacer(Modifier.width(KeyGap * open))
    KeyButton(
        color = palette.highlight,
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer { alpha = open },
        onClick = { press(key) }
    ) {
        Text(
            label,
            style = MaterialTheme.typography.headlineSmall.copy(fontSize = if (label.length > 1) 22.sp else 28.sp),
            color = palette.ink,
            maxLines = 1,
            softWrap = false
        )
    }
}

/** The key in place of "00": a calculator that opens the operators, "=" while there is something to compute. */
@Composable
private fun CalculatorToggle(open: Boolean, canEvaluate: Boolean, modifier: Modifier, press: (String) -> Unit) {
    val palette = LocalPalette.current
    val background by animateColorAsState(
        when {
            open && canEvaluate -> palette.accent
            open -> palette.highlight
            else -> palette.cardSoft
        },
        label = "calculatorKey"
    )
    val description = stringResource(
        when {
            open && canEvaluate -> R.string.equals
            open -> R.string.close_calculator
            else -> R.string.calculator
        }
    )
    KeyButton(
        color = background,
        modifier = modifier.semantics { contentDescription = description },
        onClick = { press(if (open && canEvaluate) "=" else CALCULATOR_KEY) }
    ) {
        AnimatedContent(
            targetState = open && canEvaluate,
            transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
            label = "equals"
        ) { equals ->
            if (equals) {
                Text("=", style = MaterialTheme.typography.headlineSmall.copy(fontSize = 32.sp), color = palette.onAccent)
            } else {
                Icon(
                    Icons.Rounded.Calculate,
                    contentDescription = null,
                    tint = if (open) palette.ink else palette.accent,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}

/**
 * The clear/backspace row while the amount is zero: the history, then round amounts in the source currency.
 * Taps right after it appears are ignored: they were meant for the backspace or clear key that was there.
 */
@Composable
private fun QuickAmountRow(
    amounts: List<Long>,
    currencySymbol: String,
    onQuickAmount: (Long) -> Unit,
    onOpenHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    val haptics = LocalHapticFeedback.current
    var armed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(450)
        armed = true
    }
    fun guarded(action: () -> Unit): () -> Unit = {
        if (armed) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            action()
        }
    }
    Row(modifier) {
        KeyButton(palette.highlight, Modifier.weight(0.8f).fillMaxHeight(), onClick = guarded(onOpenHistory)) {
            Icon(Icons.Rounded.History, contentDescription = stringResource(R.string.history), tint = palette.ink)
        }
        amounts.forEach { amount ->
            Spacer(Modifier.width(KeyGap))
            KeyButton(palette.card, Modifier.weight(1f).fillMaxHeight(), onClick = guarded { onQuickAmount(amount) }) {
                AutoSizeText(
                    text = amountWithSymbol(formatAmountInput(amount.toString()), currencySymbol, palette.accent),
                    style = AmountTextStyle.copy(fontSize = 17.sp),
                    color = palette.ink,
                    textAlign = TextAlign.Center,
                    minFontSize = 10f,
                    modifier = Modifier.padding(horizontal = 6.dp)
                )
            }
        }
    }
}

@Composable
internal fun KeyButton(
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
