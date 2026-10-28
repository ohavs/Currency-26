package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.premium.QuickConvertAllowance
import com.example.ui.theme.LocalPalette
import com.example.ui.theme.paletteTexture
import com.example.utils.ConvertedPrice
import com.example.utils.QuickConversion
import com.example.utils.amountSymbol
import com.example.utils.currencyMap
import com.example.utils.currencyName
import com.example.utils.formatAmount
import com.example.utils.formatOriginal
import com.example.utils.formatRate
import com.example.utils.formatUntil
import com.example.utils.getCurrencyInfo
import com.example.utils.withSymbol

/**
 * The bubble that pops over another app when the user picks "Convert currency" on selected text.
 * [conversion] is null while the very first rates load ([offline] when they could not); tapping outside the
 * card dismisses it. Tapping the currency it converts to opens a picker in the bubble ([onChooseCurrency]), and so
 * does tapping the price's own currency, when the text meant another one ([onChooseSource]); [recents] are offered
 * first there. A [locked] bubble (this week's free conversions are used up) converts nothing and offers Premium
 * ([onUnlock]), saying when the next free one comes back ([nextFree]); [freeLeft] is how many free ones are left
 * after this one (null with Premium).
 */
@Composable
fun QuickConvertSheet(
    conversion: QuickConversion?,
    colorTheme: String,
    locked: Boolean = false,
    nextFree: Long? = null,
    freeLeft: Int? = null,
    onUnlock: () -> Unit = {},
    offline: Boolean,
    canReplace: Boolean,
    copied: Boolean,
    onCopy: () -> Unit,
    onReplace: () -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit,
    rates: Map<String, Double> = emptyMap(),
    recents: List<String> = emptyList(),
    onChooseCurrency: ((String) -> Unit)? = null,
    onChooseSource: ((String) -> Unit)? = null,
    startPicking: Boolean = false,
) {
    val palette = LocalPalette.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(180, easing = FastOutSlowInEasing)) }
    val noRipple = remember { MutableInteractionSource() }
    // Which currency the picker changes: the one prices go to, or the one they are in (null: no picker).
    var picking by rememberSaveable { mutableStateOf(if (startPicking) PICK_TARGET else null) }
    var query by rememberSaveable { mutableStateOf("") }
    val closePicker = {
        picking = null
        query = ""
    }
    BackHandler(enabled = picking != null, onBack = closePicker)
    val choose = onChooseCurrency?.let { { picking = PICK_TARGET } }
    val chooseSource = onChooseSource?.let { { picking = PICK_SOURCE } }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = appear.value }
            .background(Color.Black.copy(alpha = 0.28f))
            .clickable(interactionSource = noRipple, indication = null, onClick = onDismiss)
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                // The picker's search field brings the keyboard up under the card.
                .windowInsetsPadding(WindowInsets.navigationBars.union(WindowInsets.ime))
                .padding(12.dp)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - appear.value) * 48.dp.toPx() }
                .clip(RoundedCornerShape(30.dp))
                .background(palette.background)
                .paletteTexture(colorTheme, palette)
                .clickable(interactionSource = noRipple, indication = null, onClick = {})
                .animateContentSize()
                .padding(16.dp)
        ) {
            when {
                locked -> UsedUp(nextFree, onUnlock)
                picking != null && conversion != null -> {
                    val source = picking == PICK_SOURCE
                    CurrencyPage(
                        conversion = conversion,
                        source = source,
                        rates = rates,
                        recents = recents,
                        query = query,
                        onQueryChange = { query = it },
                        onBack = closePicker,
                        onSelect = { code ->
                            closePicker()
                            (if (source) onChooseSource else onChooseCurrency)?.invoke(code)
                        }
                    )
                }
                conversion == null && offline -> Message(stringResource(R.string.offline), Icons.Rounded.CloudOff, onOpenApp)
                conversion == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.accent, strokeWidth = 3.dp)
                }
                conversion.lines.isEmpty() -> Message(stringResource(R.string.quick_no_price), Icons.Rounded.SearchOff, onOpenApp)
                else -> {
                    val single = conversion.lines.singleOrNull()
                    if (single != null) {
                        SinglePrice(single, conversion.others, choose, chooseSource)
                    } else {
                        PriceList(conversion, choose, chooseSource)
                    }
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        PillButton(
                            text = stringResource(if (copied) R.string.quick_copied else R.string.quick_copy),
                            icon = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                            onClick = onCopy,
                            modifier = Modifier.weight(1f)
                        )
                        if (canReplace) {
                            PillButton(
                                text = stringResource(R.string.quick_replace),
                                icon = Icons.Rounded.FindReplace,
                                onClick = onReplace,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        RoundIconButton(
                            icon = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = stringResource(R.string.quick_open),
                            onClick = onOpenApp,
                            containerColor = palette.card
                        )
                    }
                    if (freeLeft != null) {
                        Spacer(Modifier.height(10.dp))
                        Text(
                            stringResource(R.string.quick_free_left, freeLeft, QuickConvertAllowance.FREE_PER_WEEK),
                            style = MaterialTheme.typography.labelMedium,
                            color = palette.inkMuted,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

private const val PICK_TARGET = "target"
private const val PICK_SOURCE = "source"

/**
 * The currencies to convert to (or, with [source], the one the prices are in), inside the bubble: the price on top
 * (a single one's other currency is not offered), then the app's picker with its search and the user's currencies first.
 */
@Composable
private fun CurrencyPage(
    conversion: QuickConversion,
    source: Boolean,
    rates: Map<String, Double>,
    recents: List<String>,
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit,
    onSelect: (String) -> Unit,
) {
    val single = conversion.lines.singleOrNull()
    val from = single?.price?.currency ?: conversion.lines.firstOrNull()?.price?.currency
    val to = single?.to ?: conversion.target
    val current = if (source) from else to
    val other = if (source) to else from
    val currencies = remember(rates.keys, single, other) {
        rates.keys.ifEmpty { currencyMap.keys }.filter { single == null || it != other }.sorted()
    }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.8f)) {
        AppTopBar(
            title = stringResource(R.string.choose_currency),
            subtitle = single?.let { withSymbol(it.price.currency, it.price.amount, original = true) },
            navigation = { RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, stringResource(R.string.back), onBack) }
        )
        CurrencyChooser(
            currencies = currencies,
            rates = rates,
            currentCode = current,
            otherCode = other ?: to,
            recents = recents,
            query = query,
            onQueryChange = onQueryChange,
            onSelect = onSelect,
            modifier = Modifier.weight(1f)
        )
    }
}

/** The price as found (with a hint when its currency was a guess) above the converted amount, like the app's cards. */
@Composable
private fun SinglePrice(
    line: ConvertedPrice,
    others: List<Pair<String, Double>>,
    onChooseCurrency: (() -> Unit)?,
    onChooseSource: (() -> Unit)?,
) {
    val palette = LocalPalette.current
    val from = line.price
    PriceCard(
        code = from.currency,
        amount = formatOriginal(from.amount),
        note = if (from.assumed) stringResource(R.string.quick_assumed) else null,
        background = palette.card,
        fontSize = 22,
        onClick = onChooseSource
    )
    Spacer(Modifier.height(6.dp))
    PriceCard(
        code = line.to,
        amount = formatAmount(line.value),
        note = null,
        background = palette.highlight,
        fontSize = 34,
        onClick = onChooseCurrency
    )
    val rate = if (from.amount != 0.0) line.value / from.amount else 0.0
    val details = (others.map { (code, value) -> withSymbol(code, value) } + "1 ${from.currency} = ${formatRate(rate)} ${line.to}")
    Spacer(Modifier.height(10.dp))
    Text(
        text = details.joinToString("  ·  "),
        style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr),
        color = palette.inkMuted,
        textAlign = TextAlign.Center,
        maxLines = 2,
        modifier = Modifier.fillMaxWidth()
    )
}

/**
 * A currency card: flag and name on the reading side, the amount with its symbol on the other. With [onClick] the
 * name carries a chevron, and the card changes the currency.
 */
@Composable
private fun PriceCard(code: String, amount: String, note: String?, background: Color, fontSize: Int, onClick: (() -> Unit)? = null) {
    val palette = LocalPalette.current
    val locale = LocalAppLocale.current
    val changeLabel = stringResource(R.string.change_currency)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClickLabel = changeLabel, onClick = onClick) else Modifier)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(getCurrencyInfo(code).flag, fontSize = 26.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    currencyName(code, locale),
                    style = MaterialTheme.typography.titleSmall,
                    color = palette.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (onClick != null) {
                    Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = palette.inkMuted, modifier = Modifier.size(20.dp))
                }
            }
            if (note != null) {
                Text(note, style = MaterialTheme.typography.labelMedium, color = palette.inkMuted, maxLines = 1)
            }
        }
        Spacer(Modifier.width(10.dp))
        Text(
            text = amountWithSymbol(amount, amountSymbol(code), palette.accent),
            style = MaterialTheme.typography.headlineMedium.copy(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                textDirection = TextDirection.Ltr
            ),
            color = palette.ink,
            maxLines = 1
        )
    }
}

/**
 * Several prices: each one converted, and their sum when they share a currency. The currency they go to heads the
 * converted side, as a chip that changes it; the currency they are in heads the other side (all of them, once changed).
 */
@Composable
private fun PriceList(conversion: QuickConversion, onChooseCurrency: (() -> Unit)?, onChooseSource: (() -> Unit)?) {
    val palette = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.card)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        if (onChooseCurrency != null || onChooseSource != null) {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 2.dp)) {
                val from = conversion.lines.first().price.currency
                if (onChooseSource != null) CurrencyChip(from, onChooseSource, background = palette.background)
                Spacer(Modifier.weight(1f))
                if (onChooseCurrency != null) CurrencyChip(conversion.target, onChooseCurrency)
            }
        }
        conversion.lines.forEach { line ->
            ListRow(withSymbol(line.price.currency, line.price.amount, original = true), withSymbol(line.to, line.value))
        }
    }
    conversion.total?.let { (code, value) ->
        Spacer(Modifier.height(6.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(palette.highlight)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.quick_total), style = MaterialTheme.typography.titleMedium, color = palette.ink, modifier = Modifier.weight(1f))
            Text(
                text = amountWithSymbol(formatAmount(value), amountSymbol(code), palette.accent),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Ltr),
                color = palette.ink,
                maxLines = 1
            )
        }
    }
}

/** A currency's flag and code with a chevron: tapping it picks another one. */
@Composable
private fun CurrencyChip(code: String, onClick: () -> Unit, background: Color = LocalPalette.current.highlight) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .clip(CircleShape)
            .background(background)
            .clickable(role = Role.Button, onClickLabel = stringResource(R.string.change_currency), onClick = onClick)
            .padding(start = 10.dp, end = 6.dp, top = 4.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(getCurrencyInfo(code).flag, fontSize = 16.sp)
        Spacer(Modifier.width(6.dp))
        Text(code, style = MaterialTheme.typography.labelLarge.copy(textDirection = TextDirection.Ltr), color = palette.ink)
        Icon(Icons.Rounded.ExpandMore, contentDescription = null, tint = palette.inkMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun ListRow(original: String, converted: String) {
    val palette = LocalPalette.current
    // Each amount reads left to right, while the pair follows the layout (original on the reading side).
    Row(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(original, style = MaterialTheme.typography.bodyLarge.copy(textDirection = TextDirection.Ltr), color = palette.inkMuted)
        Spacer(Modifier.weight(1f))
        Text(
            converted,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold, textDirection = TextDirection.Ltr),
            color = palette.ink
        )
    }
}

/** This week's free conversions are used up: when the next one comes back, and Premium for no limit. */
@Composable
private fun UsedUp(nextFree: Long?, onUnlock: () -> Unit) {
    val palette = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(palette.accent),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Lock, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(22.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.quick_used_up, QuickConvertAllowance.FREE_PER_WEEK),
                style = MaterialTheme.typography.titleMedium,
                color = palette.ink
            )
            if (nextFree != null) {
                val time = formatUntil(
                    nextFree, System.currentTimeMillis(), LocalAppLocale.current,
                    todayFormat = stringResource(R.string.today_at),
                    tomorrowFormat = stringResource(R.string.tomorrow_at)
                )
                Text(stringResource(R.string.quick_next_free, time), style = MaterialTheme.typography.bodyMedium, color = palette.inkMuted)
            }
        }
    }
    Spacer(Modifier.height(14.dp))
    PillButton(
        text = stringResource(R.string.quick_unlimited),
        icon = Icons.Rounded.WorkspacePremium,
        onClick = onUnlock,
        modifier = Modifier.fillMaxWidth()
    )
}

/** Nothing to convert (no price in the text, or no rates yet): say so, and offer the app itself. */
@Composable
private fun Message(text: String, icon: ImageVector, onOpenApp: () -> Unit) {
    val palette = LocalPalette.current
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(4.dp)) {
        Icon(icon, contentDescription = null, tint = palette.inkMuted, modifier = Modifier.size(24.dp))
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge, color = palette.ink, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(14.dp))
    PillButton(
        text = stringResource(R.string.quick_open),
        icon = Icons.AutoMirrored.Rounded.OpenInNew,
        onClick = onOpenApp,
        modifier = Modifier.fillMaxWidth()
    )
}
