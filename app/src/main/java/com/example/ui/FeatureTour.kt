package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import kotlinx.coroutines.delay
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalPalette

/** The features people tend to miss; the tour shows one card per tip, in this order. */
enum class FeatureTip { WIDGET, SELECT_CONVERT, EXTRA_CURRENCY }

/** The tip's card title, also used by the banner that offers the tour. */
internal val FeatureTip.titleRes: Int
    get() = when (this) {
        FeatureTip.WIDGET -> R.string.tip_widget_title
        FeatureTip.SELECT_CONVERT -> R.string.tip_select_title
        FeatureTip.EXTRA_CURRENCY -> R.string.tip_extra_title
    }

/**
 * A short tour over the calculator: the home-screen widget, converting a price selected in another app, and
 * the additional currency, with a way to try them where there is one ("show me how" walks to the widget button
 * in Settings). Opened from the banner that offers it (first launch, then after a while while a feature is
 * still unused) or from Settings.
 */
@Composable
fun FeatureTour(
    tips: List<FeatureTip>,
    canPinWidget: Boolean,
    onShowWidgetGuide: () -> Unit,
    onAddCurrency: () -> Unit,
    onDismiss: () -> Unit,
    initialPage: Int = 0,
) {
    if (tips.isEmpty()) return
    val palette = LocalPalette.current
    var page by rememberSaveable { mutableIntStateOf(initialPage.coerceIn(0, tips.lastIndex)) }
    val appear = remember { Animatable(0f) }
    // Taps right after the sheet slides in were meant for whatever was under the finger (often the keypad):
    // the sheet only reacts once it has settled.
    var settled by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        appear.animateTo(1f, tween(380, easing = FastOutSlowInEasing))
        delay(450)
        settled = true
    }
    BackHandler(onBack = onDismiss)

    fun guarded(action: () -> Unit): () -> Unit = { if (settled) action() }

    fun next() {
        if (page < tips.lastIndex) page++ else onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = appear.value }
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = guarded(onDismiss))
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .safeDrawingPadding()
                .padding(12.dp)
                .graphicsLayer {
                    translationY = (1f - appear.value) * size.height * 0.6f
                    alpha = appear.value
                }
                .clip(RoundedCornerShape(32.dp))
                .background(palette.background)
                // Taps on the card must not reach the scrim behind it.
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                PageDots(count = tips.size, current = page)
                Spacer(Modifier.weight(1f))
                Icon(
                    Icons.Rounded.Close,
                    contentDescription = stringResource(R.string.close),
                    tint = palette.inkMuted,
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(role = Role.Button, onClick = guarded(onDismiss))
                        .padding(6.dp)
                        .size(20.dp)
                )
            }
            Spacer(Modifier.height(12.dp))
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val direction = if (targetState > initialState) 1 else -1
                    (slideInHorizontally { it / 4 * direction } + fadeIn()) togetherWith
                        (slideOutHorizontally { -it / 4 * direction } + fadeOut())
                },
                label = "tour-page"
            ) { index ->
                val tip = tips[index]
                Column {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(196.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.card),
                        contentAlignment = Alignment.Center
                    ) {
                        when (tip) {
                            FeatureTip.WIDGET -> WidgetIllustration()
                            FeatureTip.SELECT_CONVERT -> SelectConvertIllustration()
                            FeatureTip.EXTRA_CURRENCY -> ExtraCurrencyIllustration()
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(tip.titleRes),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.ink
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(
                            when (tip) {
                                FeatureTip.WIDGET -> R.string.tip_widget_body
                                FeatureTip.SELECT_CONVERT -> R.string.tip_select_body
                                FeatureTip.EXTRA_CURRENCY -> R.string.tip_extra_body
                            }
                        ),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkMuted
                    )
                    Spacer(Modifier.height(18.dp))
                    when {
                        // Nothing to try from here: it happens in other apps.
                        tip == FeatureTip.SELECT_CONVERT -> Unit
                        tip == FeatureTip.EXTRA_CURRENCY -> TourButton(stringResource(R.string.add_currency), Icons.Rounded.Add, guarded(onAddCurrency))
                        canPinWidget -> TourButton(stringResource(R.string.tip_widget_action), Icons.Rounded.TouchApp, guarded(onShowWidgetGuide))
                        // Launchers that cannot pin widgets: explain the manual way instead of a dead button.
                        else -> Text(
                            text = stringResource(R.string.add_widget_manual),
                            style = MaterialTheme.typography.bodyMedium,
                            color = palette.ink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(palette.cardSoft)
                                .padding(14.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(CircleShape)
                    .clickable(role = Role.Button, onClick = guarded { next() }),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(if (page < tips.lastIndex) R.string.tip_next else R.string.tip_done),
                    style = MaterialTheme.typography.labelLarge.copy(fontSize = 15.sp),
                    color = palette.ink
                )
            }
        }
    }
}

@Composable
private fun TourButton(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    PillButton(text = text, icon = icon, onClick = onClick, modifier = Modifier.fillMaxWidth())
}

@Composable
private fun PageDots(count: Int, current: Int) {
    val palette = LocalPalette.current
    if (count < 2) return
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(if (index == current) 22.dp else 8.dp)
                    .clip(CircleShape)
                    .background(if (index == current) palette.accent else palette.inkMuted.copy(alpha = 0.3f))
            )
        }
    }
}

/** The real widget in miniature on a home screen: a finger types 1, 0, 0 and both amounts follow. */
@Composable
private fun WidgetIllustration() {
    val palette = LocalPalette.current
    val readingRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val clock = rememberInfiniteTransition(label = "typing")
    val t by clock.animateFloat(
        initialValue = 0f,
        targetValue = TYPING_LOOP,
        animationSpec = infiniteRepeatable(tween((TYPING_LOOP * 1000).toInt(), easing = LinearEasing)),
        label = "t"
    )
    val typed = TAPS.count { t >= it }
    val pressed = TAPS.indexOfFirst { t in (it - 0.05f)..(it + 0.15f) }
    val finger = fingerPosition(t)
    val fingerAlpha = when {
        t < 0.1f -> 0f
        t < 0.3f -> (t - 0.1f) / 0.2f
        t > 4.0f -> 0f
        t > 3.6f -> 1f - (t - 3.6f) / 0.4f
        else -> 1f
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(palette.accent, palette.accent.copy(alpha = 0.7f)))),
        contentAlignment = Alignment.Center
    ) {
        // Other apps on the home screen, either side of the widget.
        Row(horizontalArrangement = Arrangement.spacedBy(MINI_WIDGET_WIDTH + 28.dp), verticalAlignment = Alignment.CenterVertically) {
            repeat(2) {
                Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                    repeat(3) {
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(9.dp))
                                .background(Color.White.copy(alpha = 0.24f))
                        )
                    }
                }
            }
        }
        // Keypads stay 7-8-9 left to right in every language, so the miniature is laid out left to right.
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
            Box {
                MiniWidget(typed = typed, pressedKey = if (pressed >= 0) TAP_KEYS[pressed] else null, flagOnRight = readingRtl)
                Box(
                    modifier = Modifier
                        .offset(x = finger.first - FINGER / 2, y = finger.second - FINGER / 2)
                        .graphicsLayer {
                            alpha = fingerAlpha
                            val press = if (pressed >= 0) 0.82f else 1f
                            scaleX = press
                            scaleY = press
                        }
                        .size(FINGER)
                        .clip(CircleShape)
                        // A soft dark rim keeps the fingertip visible over the light keys.
                        .background(palette.ink.copy(alpha = 0.22f))
                        .padding(3.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.95f))
                )
            }
        }
    }
}

private const val TYPING_LOOP = 4.4f
/** Seconds into the loop at which a key is tapped, and which keys: 1, 0, 0. */
private val TAPS = listOf(0.7f, 1.5f, 2.3f)
private val TAP_KEYS = listOf("1", "0", "0")
private val MINI_WIDGET_WIDTH = 156.dp
private val FINGER = 24.dp
private val MINI_PADDING = 7.dp
private val MINI_CARD = 32.dp
private val MINI_GAP = 3.dp
private val MINI_KEY_HEIGHT = 17.dp
private val MINI_KEYPAD_TOP = MINI_PADDING + MINI_CARD * 2 + MINI_GAP + 6.dp
private val MINI_KEYS = listOf(listOf("7", "8", "9"), listOf("4", "5", "6"), listOf("1", "2", "3"), listOf("C", "0", "⌫"))

/** Center of a key in the miniature, from its top-left corner. */
private fun keyCenter(key: String): Pair<Dp, Dp> {
    val row = MINI_KEYS.indexOfFirst { key in it }
    val column = MINI_KEYS[row].indexOf(key)
    val keyWidth = (MINI_WIDGET_WIDTH - MINI_PADDING * 2 - MINI_GAP * 2) / 3
    val x = MINI_PADDING + (keyWidth + MINI_GAP) * column + keyWidth / 2
    val y = MINI_KEYPAD_TOP + (MINI_KEY_HEIGHT + MINI_GAP) * row + MINI_KEY_HEIGHT / 2
    return x to y
}

/** The finger glides in from below, taps 1, moves to 0 and taps it twice. */
private fun fingerPosition(t: Float): Pair<Dp, Dp> {
    val rest = (MINI_WIDGET_WIDTH * 0.8f) to (MINI_KEYPAD_TOP + 96.dp)
    val one = keyCenter("1")
    val zero = keyCenter("0")
    val path = listOf(0f to rest, 0.6f to one, 0.8f to one, 1.4f to zero, TYPING_LOOP to zero)
    val next = path.indexOfFirst { it.first >= t }.coerceAtLeast(1)
    val (t0, from) = path[next - 1]
    val (t1, to) = path[next]
    val f = if (t1 > t0) FastOutSlowInEasing.transform(((t - t0) / (t1 - t0)).coerceIn(0f, 1f)) else 1f
    return (from.first + (to.first - from.first) * f) to (from.second + (to.second - from.second) * f)
}

@Composable
private fun MiniWidget(typed: Int, pressedKey: String?, flagOnRight: Boolean) {
    val palette = LocalPalette.current
    val amount = listOf("0", "1", "10", "100")[typed]
    val converted = listOf("0.00", "0.27", "2.69", "26.94")[typed]
    Column(
        modifier = Modifier
            .width(MINI_WIDGET_WIDTH)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.background)
            .padding(MINI_PADDING)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Column(verticalArrangement = Arrangement.spacedBy(MINI_GAP)) {
                MiniRow(flag = "🇮🇱", amount = "₪ $amount", background = palette.card, height = MINI_CARD, flagOnRight = flagOnRight)
                MiniRow(flag = "🇺🇸", amount = "$ $converted", background = palette.highlight, height = MINI_CARD, flagOnRight = flagOnRight)
            }
            Box(
                modifier = Modifier
                    .size(width = 30.dp, height = 18.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(palette.background)
                    .padding(2.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                Text("⇅", fontSize = 9.sp, color = palette.onAccent, fontWeight = FontWeight.Bold)
            }
        }
        Spacer(Modifier.height(6.dp))
        Column(verticalArrangement = Arrangement.spacedBy(MINI_GAP)) {
            MINI_KEYS.forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(MINI_GAP)) {
                    row.forEach { key ->
                        val background = when {
                            key == pressedKey -> palette.highlight
                            key == "C" -> palette.accent
                            key == "⌫" -> palette.highlight
                            else -> palette.cardSoft
                        }
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(MINI_KEY_HEIGHT)
                                .clip(RoundedCornerShape(6.dp))
                                .background(background),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                key,
                                fontSize = 9.sp,
                                lineHeight = 10.sp,
                                fontWeight = FontWeight.Medium,
                                color = if (key == "C") palette.onAccent else palette.ink
                            )
                        }
                    }
                }
            }
        }
    }
}

/** Two currency rows, and a third (dashed, "temporary") that keeps sliding in. */
@Composable
private fun ExtraCurrencyIllustration() {
    val palette = LocalPalette.current
    val motion = rememberInfiniteTransition(label = "extra")
    val shown by motion.animateFloat(
        initialValue = 0f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            keyframes {
                durationMillis = 3200
                0f at 0
                0f at 300
                1f at 900 using FastOutSlowInEasing
                1f at 2800
                0f at 3200
            }
        ),
        label = "shown"
    )
    Column(
        modifier = Modifier.width(236.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        MiniRow(flag = "🇺🇸", amount = "$ 100", background = palette.background, height = 36.dp)
        MiniRow(flag = "🇮🇱", amount = "₪ 371.23", background = palette.highlight, height = 36.dp)
        Box(
            modifier = Modifier.graphicsLayer {
                alpha = shown
                translationY = (1f - shown) * 18.dp.toPx()
            }
        ) {
            MiniRow(flag = "🇪🇺", amount = "€ 91.91", background = palette.cardSoft, height = 36.dp, dashed = true)
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    // Just outside the row, beside its flag.
                    .offset(x = (-30).dp)
                    .graphicsLayer {
                        scaleX = 0.6f + shown * 0.4f
                        scaleY = 0.6f + shown * 0.4f
                    }
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(palette.accent),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.onAccent, modifier = Modifier.size(16.dp))
            }
        }
    }
}

/** A tiny currency row: flag on the reading side, symbol and amount on the other (like the real cards). */
@Composable
private fun MiniRow(flag: String, amount: String, background: Color, height: Dp, dashed: Boolean = false, flagOnRight: Boolean = false) {
    val palette = LocalPalette.current
    val outline = palette.inkMuted.copy(alpha = 0.55f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(height / 2.6f))
            .background(background)
            .then(
                if (dashed) {
                    Modifier.drawBehind {
                        val stroke = 1.2.dp.toPx()
                        drawRoundRect(
                            color = outline,
                            topLeft = Offset(stroke / 2, stroke / 2),
                            size = Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(size.height / 2.6f),
                            style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx())))
                        )
                    }
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val flagText = @Composable { Text(flag, fontSize = (height.value * 0.42f).sp) }
        val amountText = @Composable {
            Text(
                text = amount,
                style = MaterialTheme.typography.labelLarge.copy(
                    fontSize = (height.value * 0.38f).sp,
                    fontWeight = FontWeight.Bold,
                    textDirection = TextDirection.Ltr
                ),
                color = palette.ink,
                textAlign = TextAlign.End,
                maxLines = 1
            )
        }
        if (flagOnRight) amountText() else flagText()
        Spacer(Modifier.weight(1f))
        if (flagOnRight) flagText() else amountText()
    }
}

/**
 * A page in another app: a finger long-presses a price, "Convert currency" pops up in the selection menu,
 * the finger taps it and the converted price slides up over the page.
 */
@Composable
private fun SelectConvertIllustration() {
    val palette = LocalPalette.current
    val clock = rememberInfiniteTransition(label = "select")
    val t by clock.animateFloat(
        initialValue = 0f,
        targetValue = SELECT_LOOP,
        animationSpec = infiniteRepeatable(tween((SELECT_LOOP * 1000).toInt(), easing = LinearEasing)),
        label = "t"
    )
    fun ramp(from: Float, to: Float) = ((t - from) / (to - from)).coerceIn(0f, 1f)
    val selection = ramp(0.9f, 1.0f) * (1f - ramp(2.05f, 2.15f))
    val menu = ramp(1.1f, 1.25f) * (1f - ramp(2.0f, 2.1f))
    val bubble = FastOutSlowInEasing.transform(ramp(2.1f, 2.4f)) * (1f - ramp(4.1f, 4.5f))
    val fingerAlpha = ramp(0.1f, 0.3f) * (1f - ramp(2.3f, 2.6f))
    val pressing = t in 0.6f..1.2f || t in 1.85f..2.0f
    val finger = selectFingerPosition(t)

    Box(
        modifier = Modifier
            .size(PAGE_WIDTH, PAGE_HEIGHT)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.background)
    ) {
        // Lines of text, with the price in the middle of one of them.
        listOf(Triple(16, 110, 10), Triple(36, 184, 7), Triple(50, 150, 7), Triple(118, 176, 7), Triple(132, 128, 7))
            .forEach { (y, width, height) -> TextLine(x = 14.dp, y = y.dp, width = width.dp, height = height.dp) }
        TextLine(x = 14.dp, y = PRICE_TOP + 8.dp, width = PAGE_WIDTH / 2 - PRICE_WIDTH / 2 - 22.dp, height = 7.dp)
        TextLine(x = PAGE_WIDTH / 2 + PRICE_WIDTH / 2 + 8.dp, y = PRICE_TOP + 8.dp, width = 46.dp, height = 7.dp)
        Box(
            modifier = Modifier
                .offset(x = PAGE_WIDTH / 2 - PRICE_WIDTH / 2, y = PRICE_TOP)
                .size(PRICE_WIDTH, PRICE_HEIGHT)
                .clip(RoundedCornerShape(4.dp))
                .background(palette.accent.copy(alpha = 0.28f * selection)),
            contentAlignment = Alignment.Center
        ) {
            Text("€120", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = palette.ink)
        }
        // Selection handles under both ends of the price.
        listOf(-1, 1).forEach { side ->
            Box(
                modifier = Modifier
                    .offset(x = PAGE_WIDTH / 2 + PRICE_WIDTH / 2 * side - 4.dp, y = PRICE_TOP + PRICE_HEIGHT - 1.dp)
                    .graphicsLayer { alpha = selection }
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(palette.accent)
            )
        }
        // The selection menu above the price: copy, and ours.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = PRICE_TOP - 34.dp)
                .graphicsLayer {
                    alpha = menu
                    scaleX = 0.9f + menu * 0.1f
                    scaleY = 0.9f + menu * 0.1f
                },
            horizontalArrangement = Arrangement.Center
        ) {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .background(palette.ink)
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.ContentCopy, contentDescription = null, tint = palette.background.copy(alpha = 0.7f), modifier = Modifier.size(13.dp))
                Spacer(Modifier.width(10.dp))
                Text(
                    stringResource(R.string.quick_convert_label),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = palette.background,
                    maxLines = 1
                )
            }
        }
        // The answer, sliding up over the page.
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(10.dp)
                .fillMaxWidth()
                .graphicsLayer {
                    alpha = bubble
                    translationY = (1f - bubble) * 24.dp.toPx()
                }
                .clip(RoundedCornerShape(16.dp))
                .background(palette.highlight)
                .padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("🇪🇺 €120", fontSize = 12.sp, color = palette.inkMuted, modifier = Modifier.weight(1f))
            Text(
                "₪452.30",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, textDirection = TextDirection.Ltr),
                color = palette.ink
            )
        }
        Box(
            modifier = Modifier
                .offset(x = finger.first - FINGER / 2, y = finger.second - FINGER / 2)
                .graphicsLayer {
                    alpha = fingerAlpha
                    val press = if (pressing) 0.82f else 1f
                    scaleX = press
                    scaleY = press
                }
                .size(FINGER)
                .clip(CircleShape)
                .background(palette.ink.copy(alpha = 0.22f))
                .padding(3.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.95f))
        )
    }
}

@Composable
private fun TextLine(x: Dp, y: Dp, width: Dp, height: Dp) {
    Box(
        Modifier
            .offset(x = x, y = y)
            .size(width, height)
            .clip(CircleShape)
            .background(LocalPalette.current.inkMuted.copy(alpha = 0.22f))
    )
}

private const val SELECT_LOOP = 4.6f
private val PAGE_WIDTH = 236.dp
private val PAGE_HEIGHT = 176.dp
private val PRICE_WIDTH = 48.dp
private val PRICE_HEIGHT = 22.dp
private val PRICE_TOP = 78.dp

/** In from below, a long press on the price, then up to "Convert currency" in the menu for a tap. */
private fun selectFingerPosition(t: Float): Pair<Dp, Dp> {
    val rest = (PAGE_WIDTH * 0.72f) to (PAGE_HEIGHT + 14.dp)
    val price = (PAGE_WIDTH / 2) to (PRICE_TOP + PRICE_HEIGHT / 2)
    val menu = (PAGE_WIDTH / 2 + 14.dp) to (PRICE_TOP - 20.dp)
    val path = listOf(0f to rest, 0.55f to price, 1.35f to price, 1.75f to menu, SELECT_LOOP to menu)
    val next = path.indexOfFirst { it.first >= t }.coerceAtLeast(1)
    val (t0, from) = path[next - 1]
    val (t1, to) = path[next]
    val f = if (t1 > t0) FastOutSlowInEasing.transform(((t - t0) / (t1 - t0)).coerceIn(0f, 1f)) else 1f
    return (from.first + (to.first - from.first) * f) to (from.second + (to.second - from.second) * f)
}
