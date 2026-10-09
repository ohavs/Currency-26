package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.CurrencySlot
import com.example.ui.theme.LocalPalette
import com.example.utils.currencyMap
import com.example.utils.formatRate
import com.example.utils.getCurrencyInfo

@Composable
fun CurrencySelector(
    state: CalculatorState,
    onClose: () -> Unit,
    onSelect: (String) -> Unit,
    onSearch: (String) -> Unit,
    onRemove: () -> Unit = {},
) {
    BackHandler(onBack = onClose)
    val palette = LocalPalette.current

    val slot = state.pickerSlot
    val currentCode = state.currencyIn(slot)
    // Rates in the list are shown against the source (or, when choosing the source, against the target).
    val otherCode = if (slot == CurrencySlot.SOURCE) state.targetCurrency else state.sourceCurrency
    // Adding an extra currency only offers ones that are not on screen already.
    val hidden = if (slot == CurrencySlot.EXTRA_TARGET && currentCode == null) {
        setOf(state.sourceCurrency, state.targetCurrency)
    } else {
        emptySet()
    }

    // Before the first download there are no rates yet - still offer the well-known currencies.
    val allCurrencies = remember(state.rates.keys, hidden) {
        state.rates.keys.ifEmpty { currencyMap.keys }.filterNot { it in hidden }.sorted()
    }
    val query = state.searchQuery.trim()
    val filtered = remember(allCurrencies, query) {
        if (query.isEmpty()) {
            allCurrencies
        } else {
            allCurrencies.filter {
                val info = getCurrencyInfo(it)
                it.contains(query, ignoreCase = true) ||
                    info.hebrewName.contains(query, ignoreCase = true) ||
                    info.keywords.any { keyword -> keyword.contains(query, ignoreCase = true) }
            }
        }
    }
    val recents = state.recentCurrencies.filter { it in allCurrencies }.take(8)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeDrawingPadding()
            .padding(horizontal = 16.dp)
    ) {
        AppTopBar(
            title = "בחירת מטבע",
            subtitle = when (slot) {
                CurrencySlot.SOURCE -> "מטבע המקור"
                CurrencySlot.TARGET -> "מטבע היעד"
                CurrencySlot.EXTRA_TARGET -> "מטבע נוסף"
            },
            navigation = { RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "חזרה", onClose) },
            action = if (slot == CurrencySlot.EXTRA_TARGET && currentCode != null) {
                { RoundIconButton(Icons.Rounded.DeleteOutline, "הסרת המטבע הנוסף", onRemove) }
            } else {
                null
            }
        )

        SearchField(query = state.searchQuery, onQueryChange = onSearch)

        Spacer(Modifier.height(12.dp))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentPadding = PaddingValues(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (query.isEmpty() && recents.isNotEmpty()) {
                item(key = "recent-label") { SectionLabel("בשימוש לאחרונה") }
                item(key = "recent-row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        recents.forEach { code ->
                            RecentChip(code = code, selected = code == currentCode, onClick = { onSelect(code) })
                        }
                    }
                }
                item(key = "all-label") {
                    SectionLabel("כל המטבעות", modifier = Modifier.padding(top = 8.dp))
                }
            }

            items(filtered, key = { it }) { code ->
                val rates = state.rates
                val rateText = if (code != otherCode && rates[code] != null && rates[otherCode] != null) {
                    "${formatRate(rates.getValue(otherCode) / rates.getValue(code))} $otherCode"
                } else {
                    null
                }
                CurrencyListItem(
                    code = code,
                    rateText = rateText,
                    selected = code == currentCode,
                    onClick = { onSelect(code) }
                )
            }

            if (filtered.isEmpty()) {
                item(key = "empty") {
                    Text(
                        text = "לא נמצאו מטבעות עבור \"$query\"",
                        style = MaterialTheme.typography.bodyLarge,
                        color = palette.inkMuted,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp, horizontal = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchField(query: String, onQueryChange: (String) -> Unit) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .clip(CircleShape)
            .background(palette.card)
            .padding(horizontal = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Rounded.Search, contentDescription = null, tint = palette.inkMuted)
        Spacer(Modifier.width(10.dp))
        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text("חיפוש לפי מדינה, שם או קוד", style = MaterialTheme.typography.bodyLarge, color = palette.inkMuted)
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = palette.ink),
                cursorBrush = SolidColor(palette.ink),
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (query.isNotEmpty()) {
            Icon(
                Icons.Rounded.Close,
                contentDescription = "ניקוי חיפוש",
                tint = palette.inkMuted,
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(role = Role.Button) { onQueryChange("") }
                    .padding(4.dp)
            )
        }
    }
}

@Composable
private fun RecentChip(code: String, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val info = getCurrencyInfo(code)
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(if (selected) palette.accent else palette.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = 6.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlagBadge(info.flag, background = if (selected) palette.onAccent.copy(alpha = 0.2f) else palette.cardSoft, size = 30.dp)
        Spacer(Modifier.width(8.dp))
        Text(
            code,
            style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr),
            color = if (selected) palette.onAccent else palette.ink
        )
    }
}

@Composable
private fun CurrencyListItem(code: String, rateText: String?, selected: Boolean, onClick: () -> Unit) {
    val palette = LocalPalette.current
    val info = getCurrencyInfo(code)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(if (selected) palette.highlight else palette.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FlagBadge(info.flag, background = if (selected) palette.highlightSoft else palette.cardSoft, size = 42.dp)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = info.hebrewName,
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = if (info.symbol.isNotEmpty() && info.symbol != code) "$code · ${info.symbol}" else code,
                style = MaterialTheme.typography.bodySmall.copy(textDirection = TextDirection.Ltr),
                color = palette.inkMuted,
                maxLines = 1
            )
        }
        if (selected) {
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Check, contentDescription = "נבחר", tint = palette.onAccent, modifier = Modifier.size(18.dp))
            }
        } else if (rateText != null) {
            Text(
                text = rateText,
                style = MaterialTheme.typography.labelMedium.copy(textDirection = TextDirection.Ltr),
                color = palette.inkMuted,
                maxLines = 1
            )
        }
    }
}
