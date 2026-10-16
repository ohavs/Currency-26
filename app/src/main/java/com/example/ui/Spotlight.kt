package com.example.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.ui.theme.LocalPalette
import kotlin.math.roundToInt

/**
 * Dims the screen except for [target] (window coordinates), which gets a pulsing ring and a tooltip with
 * [message]. Tapping the target runs [onTargetTap]; tapping anywhere else runs [onDismiss].
 */
@Composable
fun Spotlight(target: Rect, message: String, onTargetTap: () -> Unit, onDismiss: () -> Unit) {
    val palette = LocalPalette.current
    val density = LocalDensity.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    var tooltipHeight by remember { mutableIntStateOf(0) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(320, easing = FastOutSlowInEasing)) }
    val pulse = rememberInfiniteTransition(label = "spotlight")
    val ring by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1300, easing = FastOutSlowInEasing), RepeatMode.Restart),
        label = "ring"
    )
    val currentTarget by rememberUpdatedState(target)
    val currentOnTargetTap by rememberUpdatedState(onTargetTap)
    val currentOnDismiss by rememberUpdatedState(onDismiss)
    val margin = with(density) { 8.dp.toPx() }
    val hole = target.translate(-origin).inflate(margin)
    val ringColor = palette.highlight

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = appear.value
            }
            .drawBehind {
                drawRect(Color.Black.copy(alpha = 0.62f))
                drawRoundRect(
                    color = Color.Transparent,
                    topLeft = hole.topLeft,
                    size = hole.size,
                    cornerRadius = CornerRadius(hole.height / 2),
                    blendMode = BlendMode.Clear
                )
                val grow = ring * 16.dp.toPx()
                drawRoundRect(
                    color = ringColor.copy(alpha = 1f - ring),
                    topLeft = Offset(hole.left - grow, hole.top - grow),
                    size = Size(hole.width + grow * 2, hole.height + grow * 2),
                    cornerRadius = CornerRadius(hole.height / 2 + grow),
                    style = Stroke(width = 3.dp.toPx())
                )
            }
            .pointerInput(Unit) {
                detectTapGestures { position ->
                    val cutout = currentTarget.translate(-origin).inflate(margin)
                    if (cutout.contains(position)) currentOnTargetTap() else currentOnDismiss()
                }
            }
    ) {
        // The tooltip sits above the target, or below it when there is no room above.
        val gap = with(density) { 14.dp.toPx() }
        val above = hole.top - gap - tooltipHeight >= with(density) { 24.dp.toPx() }
        val y = if (above) hole.top - gap - tooltipHeight else hole.bottom + gap
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset { IntOffset(0, y.roundToInt()) }
                .onSizeChanged { tooltipHeight = it.height }
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!above) Arrow(up = true)
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(18.dp))
                    .background(palette.background)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Rounded.TouchApp, contentDescription = null, tint = palette.accent, modifier = Modifier.size(22.dp))
                Spacer(Modifier.width(10.dp))
                Text(message, style = MaterialTheme.typography.bodyLarge, color = palette.ink)
            }
            if (above) Arrow(up = false)
        }
    }
}

/** A small pointer joining the tooltip to the target. */
@Composable
private fun Arrow(up: Boolean) {
    val palette = LocalPalette.current
    Box(
        modifier = Modifier
            .offset(y = if (up) 7.dp else (-7).dp)
            .rotate(45f)
            .size(14.dp)
            .background(palette.background)
    )
}
