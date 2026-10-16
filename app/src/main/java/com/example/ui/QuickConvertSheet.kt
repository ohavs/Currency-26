package com.example.ui

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CloudOff
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.FindReplace
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalPalette
import com.example.ui.theme.paletteTexture
import com.example.utils.ConvertedPrice
import com.example.utils.QuickConversion
import com.example.utils.amountSymbol
import com.example.utils.currencyName
import com.example.utils.formatAmount
import com.example.utils.formatOriginal
import com.example.utils.formatRate
import com.example.utils.getCurrencyInfo
import com.example.utils.withSymbol

/**
 * The bubble that pops over another app when the user picks "Convert currency" on selected text.
 * [conversion] is null while the very first rates load ([offline] when they could not); tapping outside the
 * card dismisses it.
 */
@Composable
fun QuickConvertSheet(
    conversion: QuickConversion?,
    colorTheme: String,
    offline: Boolean,
    canReplace: Boolean,
    copied: Boolean,
    onCopy: () -> Unit,
    onReplace: () -> Unit,
    onOpenApp: () -> Unit,
    onDismiss: () -> Unit,
) {
    val palette = LocalPalette.current
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(180, easing = FastOutSlowInEasing)) }
    val noRipple = remember { MutableInteractionSource() }

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
                .navigationBarsPadding()
                .padding(12.dp)
                .fillMaxWidth()
                .graphicsLayer { translationY = (1f - appear.value) * 48.dp.toPx() }
                .clip(RoundedCornerShape(30.dp))
                .background(palette.background)
                .paletteTexture(colorTheme, palette)
                .clickable(interactionSource = noRipple, indication = null, onClick = {})
                .padding(16.dp)
        ) {
            when {
                conversion == null && offline -> Message(stringResource(R.string.offline), Icons.Rounded.CloudOff, onOpenApp)
                conversion == null -> Box(Modifier.fillMaxWidth().height(120.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = palette.accent, strokeWidth = 3.dp)
                }
                conversion.lines.isEmpty() -> Message(stringResource(R.string.quick_no_price), Icons.Rounded.SearchOff, onOpenApp)
                else -> {
                    val single = conversion.lines.singleOrNull()
                    if (single != null) SinglePrice(single, conversion.others) else PriceList(conversion)
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
                }
            }
        }
    }
}

/** The price as found (with a hint when its currency was a guess) above the converted amount, like the app's cards. */
@Composable
private fun SinglePrice(line: ConvertedPrice, others: List<Pair<String, Double>>) {
    val palette = LocalPalette.current
    val from = line.price
    PriceCard(
        code = from.currency,
        amount = formatOriginal(from.amount),
        note = if (from.assumed) stringResource(R.string.quick_assumed) else null,
        background = palette.card,
        fontSize = 22
    )
    Spacer(Modifier.height(6.dp))
    PriceCard(code = line.to, amount = formatAmount(line.value), note = null, background = palette.highlight, fontSize = 34)
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

/** A currency card: flag and name on the reading side, the amount with its symbol on the other. */
@Composable
private fun PriceCard(code: String, amount: String, note: String?, background: Color, fontSize: Int) {
    val palette = LocalPalette.current
    val locale = LocalAppLocale.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(background)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(getCurrencyInfo(code).flag, fontSize = 26.sp)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                currencyName(code, locale),
                style = MaterialTheme.typography.titleSmall,
                color = palette.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
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

/** Several prices: each one converted, and their sum when they share a currency. */
@Composable
private fun PriceList(conversion: QuickConversion) {
    val palette = LocalPalette.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .background(palette.card)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
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
