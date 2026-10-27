package com.example.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onPlaced
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.LocalPalette
import com.example.utils.ConvertedPrice
import com.example.utils.currencyName
import com.example.utils.getCurrencyInfo
import com.example.utils.withSymbol
import kotlin.math.roundToInt

/**
 * "Convert from any app", acted out on a loop: a finger long-presses a price on a page, the phone's selection menu
 * opens, ⋮ shows the rest of it, the finger taps "Convert currency" and the bubble pops up with the price converted.
 * With [setup], Samsung's one-time step comes first: ⋮ → "Manage apps", our app switched on, "Done", and only then
 * does "Convert currency" join the menu.
 * The menu is in the phone's language and reading direction, like the real one; the bubble is the app's.
 * [converted] is [price] in the user's currency (null until there are rates: the bubble then shows the price alone).
 */
@Composable
fun SelectionMenuDemo(
    info: SelectionMenuInfo,
    price: String,
    converted: ConvertedPrice?,
    modifier: Modifier = Modifier,
    setup: Boolean = false,
) {
    val palette = LocalPalette.current
    val script: DemoScript = if (setup) SetupTimeline else DemoTimeline
    val time = rememberInfiniteTransition(label = "selectionDemo").animateFloat(
        initialValue = 0f,
        targetValue = script.loopMs.toFloat(),
        animationSpec = infiniteRepeatable(tween(script.loopMs, easing = LinearEasing)),
        label = "time"
    )
    // Read only inside layout and draw lambdas, so the frames don't recompose anything.
    val frame = { script.frame(time.value) }
    val spots = remember { DemoSpots() }
    val appRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val menuSurface = if (palette.isDark) Color(0xFF303035) else Color.White
    val menuInk = if (palette.isDark) Color(0xFFEDEDF0) else Color(0xFF1C1B1F)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(196.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(palette.cardSoft)
            // Decoration: the text above says the same.
            .clearAndSetSemantics { }
            .onPlaced { spots.scene = it }
    ) {
        // Someone else's page: a few lines of text around the price.
        val line = palette.inkMuted.copy(alpha = 0.22f)
        Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp)) {
            SkeletonLine(0.7f, line)
            Spacer(Modifier.height(7.dp))
            SkeletonLine(0.45f, line)
            Spacer(Modifier.height(62.dp))
            Text(
                text = price,
                style = TextStyle(fontSize = 30.sp, fontWeight = FontWeight.Bold, textDirection = TextDirection.Ltr),
                color = palette.ink,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .onPlaced { spots.at[DemoSpot.PRICE] = it }
                    .drawBehind { drawSelection(frame().selection, palette.accent) }
                    .padding(horizontal = 2.dp)
            )
            Spacer(Modifier.height(18.dp))
            SkeletonLine(0.6f, line)
            Spacer(Modifier.height(7.dp))
            SkeletonLine(0.35f, line)
        }

        val deviceDirection = if (info.rtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        CompositionLocalProvider(LocalLayoutDirection provides deviceDirection) {
            MenuLayer(info.rtl) {
                Toolbar(info, menuSurface, menuInk, spots, frame)
                Overflow(info, menuSurface, menuInk, spots, frame, setup)
            }
        }

        Box(
            Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = frame().scrim }
                .background(Color.Black.copy(alpha = 0.28f))
        )
        if (setup) {
            val center = Modifier.align(Alignment.Center)
            CompositionLocalProvider(LocalLayoutDirection provides deviceDirection) {
                ManagePanel(info, menuSurface, menuInk, spots, frame, center)
            }
        }
        Bubble(price, converted, frame, Modifier.align(Alignment.BottomCenter))

        // The finger: rings where it presses, and the hand, its fingertip on the spot.
        Canvas(Modifier.fillMaxSize()) {
            val f = frame()
            val tip = spots.finger(f, appRtl, density) ?: return@Canvas
            if (f.hold > 0f) {
                drawCircle(palette.accent.copy(alpha = 0.18f * f.holdAlpha), radius = 26.dp.toPx() * f.hold, center = tip)
                drawCircle(
                    palette.accent.copy(alpha = 0.55f * f.holdAlpha),
                    radius = 26.dp.toPx() * f.hold,
                    center = tip,
                    style = Stroke(2.dp.toPx())
                )
            }
            if (f.tap > 0f) {
                drawCircle(palette.accent.copy(alpha = 0.35f * (1f - f.tap)), radius = 6.dp.toPx() + 18.dp.toPx() * f.tap, center = tip)
            }
        }
        Icon(
            Icons.Rounded.TouchApp,
            contentDescription = null,
            tint = palette.ink,
            modifier = Modifier
                .align(AbsoluteAlignment.TopLeft)
                .absoluteOffset {
                    val tip = spots.finger(frame(), appRtl, density) ?: Offset(-1000f, 0f)
                    IntOffset((tip.x - HAND.toPx() * TIP_X).roundToInt(), (tip.y - HAND.toPx() * TIP_Y).roundToInt())
                }
                .size(HAND)
                .graphicsLayer {
                    val f = frame()
                    alpha = f.fingerAlpha
                    val scale = 1f - 0.12f * f.press
                    scaleX = scale
                    scaleY = scale
                    transformOrigin = TransformOrigin(TIP_X, TIP_Y)
                }
        )
    }
}

/** Copy and Share on the toolbar (Select all when the phone has no Share label to borrow), the rest under ⋮. */
private val SelectionMenuInfo.toolbarItems get() = listOfNotNull(copyLabel, shareLabel ?: selectAllLabel)
private val SelectionMenuInfo.overflowItems get() = if (shareLabel != null) listOf(selectAllLabel) else emptyList()

private val HAND = 44.dp
/** The fingertip in the TouchApp icon, as a fraction of its size. */
internal const val TIP_X = 11.5f / 24f
internal const val TIP_Y = 6.5f / 24f

@Composable
private fun SkeletonLine(fraction: Float, color: Color) {
    Box(Modifier.fillMaxWidth(fraction).height(7.dp).clip(RoundedCornerShape(4.dp)).background(color))
}

/** The selection's tint behind the price, and its two handles hanging from the bottom corners. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawSelection(amount: Float, accent: Color) {
    if (amount <= 0f) return
    drawRect(accent.copy(alpha = 0.35f * amount))
    val r = 7.dp.toPx()
    val handle = accent.copy(alpha = amount)
    val bottom = size.height
    drawCircle(handle, radius = r, center = Offset(-r, bottom + r))
    drawRect(handle, topLeft = Offset(-r, bottom), size = Size(r, r))
    drawCircle(handle, radius = r, center = Offset(size.width + r, bottom + r))
    drawRect(handle, topLeft = Offset(size.width, bottom), size = Size(r, r))
}

/**
 * The toolbar centred over the price, and the overflow list opening from its ⋮ end (the left in a right-to-left
 * phone language), as the system draws them.
 */
@Composable
private fun MenuLayer(rtl: Boolean, content: @Composable () -> Unit) {
    Layout(content = content, modifier = Modifier.fillMaxSize()) { measurables, constraints ->
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val (toolbar, overflow) = measurables.map { it.measure(loose) }
        val margin = 8.dp.roundToPx()
        val top = 48.dp.roundToPx()
        layout(constraints.maxWidth, constraints.maxHeight) {
            val toolbarX = ((constraints.maxWidth - toolbar.width) / 2).coerceAtLeast(margin)
            val overflowX = (if (rtl) toolbarX else toolbarX + toolbar.width - overflow.width)
                .coerceIn(margin, (constraints.maxWidth - overflow.width - margin).coerceAtLeast(margin))
            toolbar.place(toolbarX, top)
            overflow.place(overflowX, top)
        }
    }
}

private val MenuText = TextStyle(fontFamily = FontFamily.Default, fontSize = 15.sp, fontWeight = FontWeight.Medium)

@Composable
private fun Toolbar(info: SelectionMenuInfo, surface: Color, ink: Color, spots: DemoSpots, frame: () -> DemoFrame) {
    val shape = RoundedCornerShape(22.dp)
    Row(
        modifier = Modifier
            .graphicsLayer {
                val shown = frame().toolbar
                alpha = shown
                scaleX = 0.9f + 0.1f * shown
                scaleY = 0.9f + 0.1f * shown
                transformOrigin = TransformOrigin(0.5f, 1f)
            }
            .shadow(6.dp, shape)
            .background(surface, shape)
            .height(44.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        info.toolbarItems.forEach { label ->
            Text(label, style = MenuText, color = ink, maxLines = 1, modifier = Modifier.padding(horizontal = 10.dp))
        }
        Box(
            modifier = Modifier
                .size(36.dp)
                .onPlaced { spots.at[DemoSpot.DOTS] = it }
                .drawBehind { drawCircle(ink.copy(alpha = 0.12f * frame().dotsPressed)) },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.MoreVert, contentDescription = null, tint = ink, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun Overflow(info: SelectionMenuInfo, surface: Color, ink: Color, spots: DemoSpots, frame: () -> DemoFrame, setup: Boolean) {
    val shape = RoundedCornerShape(18.dp)
    Column(
        modifier = Modifier
            .graphicsLayer {
                val shown = frame().overflow
                alpha = shown
                scaleX = 0.92f + 0.08f * shown
                scaleY = 0.85f + 0.15f * shown
                transformOrigin = TransformOrigin(if (info.rtl) 0f else 1f, 0f)
            }
            .shadow(8.dp, shape)
            .background(surface, shape)
            .width(IntrinsicSize.Max)
            .widthIn(min = 150.dp)
            .padding(vertical = 6.dp)
    ) {
        info.overflowItems.forEach { label ->
            Text(label, style = MenuText, color = ink, maxLines = 1, modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 10.dp))
        }
        Text(
            text = info.menuLabel,
            style = MenuText,
            color = ink,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (setup) Modifier.revealed { frame().itemShown } else Modifier)
                .onPlaced { spots.at[DemoSpot.ITEM] = it }
                .drawBehind { drawRect(ink.copy(alpha = 0.1f * frame().itemPressed)) }
                .padding(horizontal = 18.dp, vertical = 10.dp)
        )
        // Samsung's own last item.
        if (setup) {
            Text(
                text = info.manageLabel,
                style = MenuText,
                color = ink,
                maxLines = 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .onPlaced { spots.at[DemoSpot.MANAGE] = it }
                    .drawBehind { drawRect(ink.copy(alpha = 0.1f * frame().managePressed)) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
            )
        }
    }
}

/** Opens to its full height as [shown] goes from 0 to 1: an item joining the menu. */
private fun Modifier.revealed(shown: () -> Float) = clipToBounds()
    .layout { measurable, constraints ->
        val placeable = measurable.measure(constraints)
        layout(placeable.width, (placeable.height * shown()).roundToInt()) { placeable.place(0, 0) }
    }
    .graphicsLayer { alpha = shown() }

/** Samsung's "Manage apps" panel, roughly: another app that is already on, and ours, which the finger turns on. */
@Composable
private fun ManagePanel(info: SelectionMenuInfo, surface: Color, ink: Color, spots: DemoSpots, frame: () -> DemoFrame, modifier: Modifier) {
    val shape = RoundedCornerShape(22.dp)
    val faint = ink.copy(alpha = 0.16f)
    Column(
        modifier = modifier
            .padding(horizontal = 28.dp)
            .fillMaxWidth()
            .graphicsLayer {
                val shown = frame().panel
                alpha = shown
                scaleX = 0.94f + 0.06f * shown
                scaleY = 0.94f + 0.06f * shown
            }
            .shadow(8.dp, shape)
            .background(surface, shape)
            .padding(start = 18.dp, end = 18.dp, top = 14.dp, bottom = 4.dp)
    ) {
        Text(info.manageLabel, style = MenuText.copy(fontSize = 16.sp, fontWeight = FontWeight.SemiBold), color = ink, maxLines = 1)
        Spacer(Modifier.height(4.dp))
        PanelRow(
            icon = { Box(Modifier.size(24.dp).clip(RoundedCornerShape(7.dp)).background(faint)) },
            label = { SkeletonLine(0.6f, faint) },
            switch = { PanelSwitch(on = { 1f }, ink = ink) }
        )
        PanelRow(
            icon = { Image(painterResource(R.drawable.palette_icon_sage), null, Modifier.size(24.dp).clip(RoundedCornerShape(7.dp))) },
            label = { Text(info.appLabel, style = MenuText, color = ink, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            switch = { PanelSwitch(on = { frame().switchOn }, ink = ink, modifier = Modifier.onPlaced { spots.at[DemoSpot.SWITCH] = it }) }
        )
        Text(
            text = info.doneLabel,
            style = MenuText.copy(fontWeight = FontWeight.SemiBold),
            color = SwitchOn,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.End)
                .onPlaced { spots.at[DemoSpot.DONE] = it }
                .drawBehind { drawRoundRect(ink.copy(alpha = 0.1f * frame().donePressed), cornerRadius = CornerRadius(size.height / 2)) }
                .padding(horizontal = 14.dp, vertical = 10.dp)
        )
    }
}

@Composable
private fun PanelRow(icon: @Composable () -> Unit, label: @Composable () -> Unit, switch: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().height(40.dp), verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(12.dp))
        Box(Modifier.weight(1f)) { label() }
        Spacer(Modifier.width(10.dp))
        switch()
    }
}

/** The phone's switch: [on] runs from 0 (off) to 1 (on), the thumb sliding to the end of the line. */
@Composable
private fun PanelSwitch(on: () -> Float, ink: Color, modifier: Modifier = Modifier) {
    val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Canvas(modifier.size(width = 38.dp, height = 22.dp)) {
        val o = on()
        drawRoundRect(lerp(ink.copy(alpha = 0.3f), SwitchOn, o), cornerRadius = CornerRadius(size.height / 2))
        val off = size.height / 2
        val end = size.width - size.height / 2
        val x = if (rtl) end + (off - end) * o else off + (end - off) * o
        drawCircle(Color.White, radius = size.height / 2 - 3.dp.toPx(), center = Offset(x, size.height / 2))
    }
}

private val SwitchOn = Color(0xFF3E82F7)

/** The quick-convert bubble in miniature: the price, then the same in the user's currency. */
@Composable
private fun Bubble(price: String, converted: ConvertedPrice?, frame: () -> DemoFrame, modifier: Modifier) {
    val palette = LocalPalette.current
    val shape = RoundedCornerShape(22.dp)
    Column(
        modifier = modifier
            .padding(8.dp)
            .fillMaxWidth()
            .graphicsLayer {
                val f = frame()
                alpha = f.bubble
                translationY = (1f - f.bubble) * 40.dp.toPx()
            }
            .shadow(8.dp, shape)
            .background(palette.background, shape)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        val from = converted?.price
        BubbleRow(
            code = from?.currency ?: "EUR",
            amount = from?.let { withSymbol(it.currency, it.amount, original = true) } ?: price,
            background = palette.card,
            fontSize = 17
        )
        if (converted != null) {
            BubbleRow(converted.to, withSymbol(converted.to, converted.value), palette.highlight, 24)
        }
    }
}

@Composable
private fun BubbleRow(code: String, amount: String, background: Color, fontSize: Int) {
    val palette = LocalPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(getCurrencyInfo(code).flag, fontSize = 18.sp)
        Spacer(Modifier.width(8.dp))
        Text(
            currencyName(code, LocalAppLocale.current),
            style = MaterialTheme.typography.labelLarge,
            color = palette.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            amount,
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = fontSize.sp,
                fontWeight = FontWeight.Bold,
                textDirection = TextDirection.Ltr
            ),
            color = palette.ink,
            maxLines = 1
        )
    }
}

internal enum class DemoSpot { OUTSIDE, PRICE, DOTS, ITEM, MANAGE, SWITCH, DONE }

/** Where the finger goes: each spot's centre, from the last layout, in the demo's own coordinates. */
private class DemoSpots {
    var scene: LayoutCoordinates? = null
    val at = mutableMapOf<DemoSpot, LayoutCoordinates>()

    private fun center(spot: DemoSpot): Offset? {
        val scene = scene?.takeIf { it.isAttached } ?: return null
        val item = at[spot]?.takeIf { it.isAttached } ?: return null
        return scene.localPositionOf(item, Offset(item.size.width / 2f, item.size.height / 2f))
    }

    /** The fingertip; it comes in from below the price, on the side a hand would. */
    fun finger(frame: DemoFrame, rtl: Boolean, density: Float): Offset? {
        fun spot(spot: DemoSpot): Offset? = if (spot == DemoSpot.OUTSIDE) {
            center(DemoSpot.PRICE)?.let { it + Offset((if (rtl) -70f else 70f) * density, 120f * density) }
        } else {
            center(spot)
        }
        val from = spot(frame.fingerFrom) ?: return null
        val to = spot(frame.fingerTo) ?: return null
        return lerp(from, to, frame.fingerMove)
    }
}

/** One moment of the demo; every value runs 0 to 1. */
internal data class DemoFrame(
    val fingerFrom: DemoSpot,
    val fingerTo: DemoSpot,
    val fingerMove: Float,
    val fingerAlpha: Float,
    /** The finger pushed down (a long press or a tap). */
    val press: Float,
    /** The long press filling up around the fingertip, and that ring's fade once the menu is up. */
    val hold: Float,
    val holdAlpha: Float,
    /** A tap's ring spreading from the fingertip. */
    val tap: Float,
    val selection: Float,
    val toolbar: Float,
    val dotsPressed: Float,
    val overflow: Float,
    val itemPressed: Float,
    val scrim: Float,
    val bubble: Float,
    /** Samsung's setup ([SetupTimeline]): "Convert currency" joining the menu, "Manage apps" pressed, its panel, our switch, "Done". */
    val itemShown: Float = 1f,
    val managePressed: Float = 0f,
    val panel: Float = 0f,
    val switchOn: Float = 0f,
    val donePressed: Float = 0f,
)

/** A demo's script, in milliseconds; pure, so the preview can step through it too. */
internal interface DemoScript {
    val loopMs: Int
    fun frame(t: Float): DemoFrame
}

private fun progress(t: Float, from: Int, to: Int) = ((t - from) / (to - from)).coerceIn(0f, 1f)
private fun eased(t: Float, from: Int, to: Int) = FastOutSlowInEasing.transform(progress(t, from, to))
/** Up over [from, from + edge], held, and back down over [to - edge, to]. */
private fun held(t: Float, from: Int, to: Int, edge: Int) = progress(t, from, from + edge) * (1f - progress(t, to - edge, to))
/** A tap's ring: spreading over the 360 ms after [at]. */
private fun ring(t: Float, at: Int) = if (t >= at && t <= at + 360) progress(t, at, at + 360) else 0f

internal object DemoTimeline : DemoScript {
    const val LOOP_MS = 7600
    override val loopMs = LOOP_MS

    override fun frame(t: Float): DemoFrame {
        val (from, to, move) = when {
            t < 1000 -> Triple(DemoSpot.OUTSIDE, DemoSpot.PRICE, eased(t, 250, 950))
            t < 2900 -> Triple(DemoSpot.PRICE, DemoSpot.DOTS, eased(t, 2250, 2800))
            else -> Triple(DemoSpot.DOTS, DemoSpot.ITEM, eased(t, 3550, 4100))
        }
        return DemoFrame(
            fingerFrom = from,
            fingerTo = to,
            fingerMove = move,
            fingerAlpha = progress(t, 200, 500) * (1f - progress(t, 4450, 4750)),
            press = held(t, 1050, 1900, 90) + held(t, 2900, 3060, 60) + held(t, 4150, 4310, 60),
            // The long press: 1050..1850; the menu answers as it completes.
            hold = progress(t, 1050, 1850),
            holdAlpha = 1f - progress(t, 1850, 2050),
            tap = ring(t, 2940) + ring(t, 4190),
            selection = eased(t, 1850, 2000) * (1f - progress(t, 4400, 4600)),
            toolbar = eased(t, 1880, 2080) * (1f - progress(t, 3060, 3200)),
            dotsPressed = held(t, 2900, 3120, 60),
            overflow = eased(t, 3100, 3350) * (1f - progress(t, 4400, 4550)),
            itemPressed = held(t, 4150, 4550, 60),
            scrim = progress(t, 4450, 4700) * (1f - progress(t, 7000, 7300)),
            bubble = eased(t, 4500, 4900) * (1f - progress(t, 7000, 7300)),
        )
    }
}

/**
 * Samsung's one-time setup, then the conversion: long press, ⋮, "Manage apps", our switch on, "Done"; ⋮ again, and
 * "Convert currency" is there now. The bubble ends it.
 */
internal object SetupTimeline : DemoScript {
    const val LOOP_MS = 12600
    override val loopMs = LOOP_MS

    override fun frame(t: Float): DemoFrame {
        val (from, to, move) = when {
            t < 1000 -> Triple(DemoSpot.OUTSIDE, DemoSpot.PRICE, eased(t, 250, 950))
            t < 2900 -> Triple(DemoSpot.PRICE, DemoSpot.DOTS, eased(t, 2250, 2800))
            t < 4150 -> Triple(DemoSpot.DOTS, DemoSpot.MANAGE, eased(t, 3550, 4100))
            t < 5650 -> Triple(DemoSpot.MANAGE, DemoSpot.SWITCH, eased(t, 4950, 5550))
            t < 6950 -> Triple(DemoSpot.SWITCH, DemoSpot.DONE, eased(t, 6250, 6850))
            t < 8350 -> Triple(DemoSpot.DONE, DemoSpot.DOTS, eased(t, 7650, 8250))
            else -> Triple(DemoSpot.DOTS, DemoSpot.ITEM, eased(t, 8950, 9500))
        }
        return DemoFrame(
            fingerFrom = from,
            fingerTo = to,
            fingerMove = move,
            fingerAlpha = progress(t, 200, 500) * (1f - progress(t, 9850, 10150)),
            press = held(t, 1050, 1900, 90) + listOf(2900, 4150, 5650, 6950, 8350, 9550).sumOf { held(t, it, it + 160, 60).toDouble() }.toFloat(),
            hold = progress(t, 1050, 1850),
            holdAlpha = 1f - progress(t, 1850, 2050),
            tap = listOf(2940, 4190, 5690, 6990, 8390, 9590).sumOf { ring(t, it).toDouble() }.toFloat(),
            selection = eased(t, 1850, 2000) * (1f - progress(t, 9800, 10000)),
            // The menu closes for the panel and comes back after "Done".
            toolbar = eased(t, 1880, 2080) * (1f - progress(t, 3060, 3200)) + eased(t, 7250, 7450) * (1f - progress(t, 8510, 8650)),
            dotsPressed = held(t, 2900, 3120, 60) + held(t, 8350, 8570, 60),
            overflow = eased(t, 3100, 3350) * (1f - progress(t, 4350, 4500)) + eased(t, 8550, 8800) * (1f - progress(t, 9800, 9950)),
            itemPressed = held(t, 9550, 9900, 60),
            scrim = progress(t, 4450, 4700) * (1f - progress(t, 7050, 7250)) + progress(t, 9850, 10100) * (1f - progress(t, 11900, 12200)),
            bubble = eased(t, 9900, 10300) * (1f - progress(t, 11900, 12200)),
            // Joins the menu while it is closed, so the second ⋮ opens on it.
            itemShown = if (t >= 6000) 1f else 0f,
            managePressed = held(t, 4150, 4450, 60),
            panel = eased(t, 4450, 4750) * (1f - progress(t, 7050, 7250)),
            switchOn = eased(t, 5700, 5900),
            donePressed = held(t, 6950, 7170, 60),
        )
    }
}
