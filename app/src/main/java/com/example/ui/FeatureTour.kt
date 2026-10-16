package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
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

/** The features people tend to miss; the tour shows one card per tip. */
enum class FeatureTip { WIDGET, EXTRA_CURRENCY }

/**
 * A short tour over the calculator: what the home-screen widget and the additional currency do, each with a
 * one-tap way to try it. Shown on first launch, and again after a while when a feature is still unused.
 */
@Composable
fun FeatureTour(
    tips: List<FeatureTip>,
    canPinWidget: Boolean,
    onAddWidget: () -> Unit,
    onAddCurrency: () -> Unit,
    onDismiss: () -> Unit,
    initialPage: Int = 0,
) {
    if (tips.isEmpty()) return
    val palette = LocalPalette.current
    var page by rememberSaveable { mutableIntStateOf(initialPage.coerceIn(0, tips.lastIndex)) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) }
    BackHandler(onBack = onDismiss)

    fun next() {
        if (page < tips.lastIndex) page++ else onDismiss()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = appear.value }
                .background(Color.Black.copy(alpha = 0.45f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
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
                        .clickable(role = Role.Button, onClick = onDismiss)
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
                            .height(176.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(palette.card),
                        contentAlignment = Alignment.Center
                    ) {
                        when (tip) {
                            FeatureTip.WIDGET -> WidgetIllustration()
                            FeatureTip.EXTRA_CURRENCY -> ExtraCurrencyIllustration()
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        text = stringResource(if (tip == FeatureTip.WIDGET) R.string.tip_widget_title else R.string.tip_extra_title),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = palette.ink
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringResource(if (tip == FeatureTip.WIDGET) R.string.tip_widget_body else R.string.tip_extra_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = palette.inkMuted
                    )
                    Spacer(Modifier.height(18.dp))
                    when {
                        tip == FeatureTip.EXTRA_CURRENCY -> TourButton(stringResource(R.string.add_currency), Icons.Rounded.Add, onAddCurrency)
                        canPinWidget -> TourButton(stringResource(R.string.add_widget), Icons.Rounded.Add) {
                            onAddWidget()
                            next()
                        }
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
                    .clickable(role = Role.Button) { next() },
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

/** A home screen with the widget floating in, and a "+" badge. */
@Composable
private fun WidgetIllustration() {
    val palette = LocalPalette.current
    val motion = rememberInfiniteTransition(label = "widget")
    val lift by motion.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "lift"
    )
    Box(contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .width(220.dp)
                .height(150.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(palette.accent, palette.accent.copy(alpha = 0.72f))))
        ) {
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                repeat(4) {
                    Box(
                        Modifier
                            .size(22.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(Color.White.copy(alpha = 0.28f))
                    )
                }
            }
            MiniWidget(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 14.dp)
                    .graphicsLayer {
                        translationY = -lift * 4.dp.toPx()
                        scaleX = 1f + lift * 0.03f
                        scaleY = 1f + lift * 0.03f
                    }
            )
        }
        Box(
            modifier = Modifier
                .offset(x = 96.dp, y = (-62).dp)
                .graphicsLayer {
                    scaleX = 0.88f + lift * 0.18f
                    scaleY = 0.88f + lift * 0.18f
                }
                .size(30.dp)
                .clip(CircleShape)
                .background(palette.background)
                .padding(3.dp)
                .clip(CircleShape)
                .background(palette.highlight),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Add, contentDescription = null, tint = palette.ink, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun MiniWidget(modifier: Modifier = Modifier) {
    val palette = LocalPalette.current
    Column(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(palette.background)
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        MiniRow(flag = "🇮🇱", amount = "₪ 100", background = palette.card, height = 26.dp)
        MiniRow(flag = "🇺🇸", amount = "$ 26.94", background = palette.highlight, height = 26.dp)
        repeat(2) {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                repeat(3) {
                    Box(
                        Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(CircleShape)
                            .background(palette.cardSoft)
                    )
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
private fun MiniRow(flag: String, amount: String, background: Color, height: Dp, dashed: Boolean = false) {
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
        Text(flag, fontSize = (height.value * 0.42f).sp)
        Spacer(Modifier.weight(1f))
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
}
