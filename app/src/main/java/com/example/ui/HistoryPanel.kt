package com.example.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Dialpad
import androidx.compose.material.icons.rounded.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AmountTextStyle
import com.example.ui.theme.LocalPalette
import com.example.utils.HistoryEntry
import com.example.utils.amountSymbol
import com.example.utils.formatAmount
import com.example.utils.formatAmountInput
import com.example.utils.formatExpression
import com.example.utils.getCurrencyInfo
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Past conversions and calculations, shown in place of the keypad (the cards above stay, so a tapped entry
 * shows up in them right away). Opened from the link under the cards, or by tapping the amount.
 */
@Composable
internal fun HistoryPanel(
    history: List<HistoryEntry>,
    onRestore: (HistoryEntry) -> Unit,
    onClear: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalPalette.current
    Column(modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The link above the panel already says "History": the icon is enough here, in line with the flags.
            Icon(
                Icons.Rounded.History,
                contentDescription = stringResource(R.string.history),
                tint = palette.accent,
                modifier = Modifier.padding(start = 14.dp).size(24.dp)
            )
            Spacer(Modifier.weight(1f))
            if (history.isNotEmpty()) {
                ClearButton(onClear)
                Spacer(Modifier.width(10.dp))
            }
            KeyButton(palette.accent, Modifier.width(HeaderKeyWidth).fillMaxHeight(), onClick = onClose) {
                Icon(Icons.Rounded.Dialpad, contentDescription = stringResource(R.string.back_to_keypad), tint = palette.onAccent)
            }
        }
        Spacer(Modifier.height(10.dp))
        if (history.isEmpty()) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(palette.cardSoft)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Rounded.History, contentDescription = null, tint = palette.inkMuted.copy(alpha = 0.6f), modifier = Modifier.size(40.dp))
                Spacer(Modifier.height(10.dp))
                Text(stringResource(R.string.history_empty), style = MaterialTheme.typography.titleSmall, color = palette.ink, textAlign = TextAlign.Center)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.history_empty_hint), style = MaterialTheme.typography.bodySmall, color = palette.inkMuted, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp)),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(history) { entry ->
                    HistoryRow(entry, onClick = { onRestore(entry) })
                }
            }
        }
    }
}

private val HeaderKeyWidth = 72.dp

/**
 * A key the size of the one beside it, with just the bin. Clearing everything takes a second tap: the first
 * one widens it to "Clear all?", so a stray tap does not lose the history.
 */
@Composable
private fun ClearButton(onClear: () -> Unit) {
    val palette = LocalPalette.current
    var confirming by remember { mutableStateOf(false) }
    LaunchedEffect(confirming) {
        if (confirming) {
            delay(3000)
            confirming = false
        }
    }
    val background by animateColorAsState(if (confirming) palette.accent else palette.cardSoft, label = "clear")
    val content = if (confirming) palette.onAccent else palette.ink
    KeyButton(
        color = background,
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(min = HeaderKeyWidth)
            .animateContentSize(),
        onClick = { if (confirming) onClear() else confirming = true }
    ) {
        Row(
            modifier = Modifier.padding(horizontal = if (confirming) 16.dp else 0.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Rounded.DeleteOutline,
                contentDescription = if (confirming) null else stringResource(R.string.history_clear),
                tint = content,
                modifier = Modifier.size(22.dp)
            )
            if (confirming) {
                Spacer(Modifier.width(6.dp))
                Text(
                    stringResource(R.string.history_clear_confirm),
                    style = MaterialTheme.typography.labelLarge,
                    color = content,
                    maxLines = 1
                )
            }
        }
    }
}

/** The amount (and the calculation behind it) on the reading side, what it came to on the other. */
@Composable
private fun HistoryRow(entry: HistoryEntry, onClick: () -> Unit) {
    val palette = LocalPalette.current
    // Amounts and calculations read left to right in every language (see AmountTextStyle use in the cards).
    val ltr = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Ltr)
    val farAlign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextAlign.Left else TextAlign.Right
    val nearAlign = if (LocalLayoutDirection.current == LayoutDirection.Rtl) TextAlign.Right else TextAlign.Left
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(palette.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(getCurrencyInfo(entry.source).flag, fontSize = 22.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = amountWithSymbol(formatAmountInput(entry.amount), amountSymbol(entry.source), palette.accent),
                style = AmountTextStyle.copy(fontSize = 18.sp, textDirection = TextDirection.Ltr),
                color = palette.ink,
                textAlign = nearAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            Text(
                text = listOfNotNull(entry.expression?.let(::formatExpression), historyTime(entry.time)).joinToString("  ·  "),
                style = ltr,
                color = palette.inkMuted,
                textAlign = nearAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = amountWithSymbol(formatAmount(entry.targetAmount), amountSymbol(entry.target), palette.accent),
                style = AmountTextStyle.copy(fontSize = 18.sp, textDirection = TextDirection.Ltr),
                color = palette.ink,
                textAlign = farAlign,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
            val extra = entry.extra
            val extraAmount = entry.extraAmount
            if (extra != null && extraAmount != null) {
                Text(
                    text = amountWithSymbol(formatAmount(extraAmount), amountSymbol(extra), palette.inkMuted),
                    style = ltr.copy(fontWeight = FontWeight.SemiBold),
                    color = palette.inkMuted,
                    textAlign = farAlign,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/** "14:02" today, "18.10 14:02" before: digits only, so it sits beside a calculation in any language. */
private fun historyTime(time: Long, now: Long = System.currentTimeMillis()): String {
    val then = Calendar.getInstance().apply { timeInMillis = time }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    val sameDay = then.get(Calendar.YEAR) == today.get(Calendar.YEAR) && then.get(Calendar.DAY_OF_YEAR) == today.get(Calendar.DAY_OF_YEAR)
    return SimpleDateFormat(if (sameDay) "HH:mm" else "dd.MM HH:mm", Locale.US).format(Date(time))
}
