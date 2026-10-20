package render

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.CurrencySlot
import com.example.ui.CalculatorContent
import com.example.ui.CalculatorState
import com.example.ui.theme.MyApplicationTheme

/** Simulates touches on the real CalculatorContent: tap, and long-press + drag onto another row. */
@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val density = 2f
    fun px(dp: Float) = dp * density
    var swapped: Pair<CurrencySlot, CurrencySlot>? = null
    var clicked: CurrencySlot? = null
    var removed = 0
    var topSwaps = 0
    val state = CalculatorState(
        sourceCurrency = "USD", targetCurrency = "ILS", extraTargetCurrency = "EUR", sourceAmountRaw = "100",
        rates = mapOf("USD" to 1.0, "ILS" to 3.7, "EUR" to 0.92)
    )
    val scene = ImageComposeScene(width = px(400f).toInt(), height = px(860f).toInt(), density = Density(density)) {
        MyApplicationTheme(themeMode = "light") {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CalculatorContent(
                    state = state, onSwap = { topSwaps++ }, onCurrencyClick = { clicked = it }, onKeypad = {},
                    onOpenSettings = {}, onRefresh = {}, onSwapSlots = { a, b -> swapped = a to b },
                    onRemoveExtra = { removed++ }
                )
            }
        }
    }
    var t = 0L
    fun frame() { t += 16_000_000L; scene.render(t) }
    frame()

    // Rows start at 72dp (68dp top bar + 4dp): source 72-152, target 158-238, extra 244-324.
    fun drag(from: Offset, dyDp: Float, snapshot: String? = null) {
        scene.sendPointerEvent(PointerEventType.Press, from); frame()
        Thread.sleep(800); frame()
        val steps = 25
        for (i in 1..steps) {
            scene.sendPointerEvent(PointerEventType.Move, Offset(from.x, from.y + px(dyDp) * i / steps)); frame()
        }
        if (snapshot != null) {
            frame(); frame()
            java.io.File("out/$snapshot.png").writeBytes(scene.render(t).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.bytes)
        }
        scene.sendPointerEvent(PointerEventType.Release, Offset(from.x, from.y + px(dyDp))); frame()
    }

    // 1) Plain tap on the source flag/name (right side in RTL) opens the picker, no swap.
    val flag = Offset(px(340f), px(112f))
    scene.sendPointerEvent(PointerEventType.Press, flag); frame()
    scene.sendPointerEvent(PointerEventType.Release, flag); frame()
    println("tap on source flag     -> clicked=$clicked swapped=$swapped")

    // Seam buttons: the upper one swaps source/target, the lower one target/extra.
    fun tap(o: Offset) { scene.sendPointerEvent(PointerEventType.Press, o); frame(); scene.sendPointerEvent(PointerEventType.Release, o); frame() }
    clicked = null; swapped = null
    tap(Offset(px(200f), px(155f)))
    println("tap upper seam button  -> topSwaps=$topSwaps swapped=$swapped clicked=$clicked")
    tap(Offset(px(200f), px(241f)))
    println("tap lower seam button  -> swapped=$swapped clicked=$clicked")

    // 2) Long-press the extra row's amount area and drag it up onto the source row.
    clicked = null; swapped = null
    drag(Offset(px(80f), px(284f)), -200f)
    println("drag extra -> source   -> clicked=$clicked swapped=$swapped")

    // 3) Long-press on the target's flag (a clickable child) and drag down onto the extra row.
    clicked = null; swapped = null
    drag(Offset(px(340f), px(198f)), 86f)
    println("drag target flag -> extra -> clicked=$clicked swapped=$swapped")

    // 4) Long-press and release without moving onto another row: nothing happens.
    clicked = null; swapped = null
    drag(Offset(px(80f), px(112f)), 10f)
    println("short drag on source   -> clicked=$clicked swapped=$swapped")

    // 5) Drag the extra currency down onto the trash zone that appears under the rows.
    clicked = null; swapped = null; removed = 0
    drag(Offset(px(80f), px(284f)), 90f, snapshot = "drag_trash")
    println("drag extra -> trash    -> removed=$removed swapped=$swapped clicked=$clicked")

    // 6) Long-press the extra currency and release without moving: the change/remove menu opens.
    removed = 0
    drag(Offset(px(80f), px(284f)), 0f)
    repeat(40) { frame() }
    java.io.File("out/long_press_menu.png").writeBytes(scene.render(t).encodeToData(org.jetbrains.skia.EncodedImageFormat.PNG)!!.bytes)
    println("long-press release     -> removed=$removed (menu snapshot saved)")

    // 7) The source row has no trash zone: dragging it far down does nothing.
    swapped = null; removed = 0
    scene.sendPointerEvent(PointerEventType.Press, Offset(px(20f), px(840f))); frame()
    scene.sendPointerEvent(PointerEventType.Release, Offset(px(20f), px(840f))); frame()
    drag(Offset(px(80f), px(112f)), 300f)
    println("drag source far down   -> removed=$removed swapped=$swapped")

    scene.close()

    // Spotlight guide in Settings: a tap on the highlighted button adds the widget, a tap elsewhere just closes.
    for ((label, point) in listOf("tap on spotlight button" to Offset(px(200f), px(622f)), "tap outside spotlight " to Offset(px(200f), px(120f)))) {
        var added = 0
        var done = 0
        val settings = ImageComposeScene(width = px(400f).toInt(), height = px(860f).toInt(), density = Density(density)) {
            MyApplicationTheme(themeMode = "light") {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    com.example.ui.SettingsScreen(
                        themeMode = "system", colorTheme = "sage", autoUpdate = com.example.data.AutoUpdateSettings(),
                        lastUpdateTimestamp = 0L, isRefreshing = false, lastRefreshFailed = false,
                        canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                        onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                        onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = { added++ },
                        appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.11", checked = true),
                        checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                        highlightAddWidget = true, onWidgetGuideDone = { done++ }
                    )
                }
            }
        }
        var st = 0L
        fun sframe() { st += 16_000_000L; settings.render(st) }
        repeat(150) { sframe() }
        settings.sendPointerEvent(PointerEventType.Press, point); sframe()
        settings.sendPointerEvent(PointerEventType.Release, point); sframe()
        println("$label -> addWidget=$added guideDone=$done")
        settings.close()
    }

    // Sample-price guide in Settings: a long press goes through the spotlight's hole to the price (which ends the
    // guide); a tap elsewhere just closes it.
    for (inside in listOf(true, false)) {
        var done = 0
        com.example.ui.SelectionMenuHarness.longPresses = 0
        val settings = ImageComposeScene(width = px(400f).toInt(), height = px(860f).toInt(), density = Density(density)) {
            MyApplicationTheme(themeMode = "light") {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                    com.example.ui.SettingsScreen(
                        themeMode = "system", colorTheme = "sage", autoUpdate = com.example.data.AutoUpdateSettings(),
                        lastUpdateTimestamp = 0L, isRefreshing = false, lastRefreshFailed = false,
                        canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                        onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                        onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                        appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.24", checked = true),
                        checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                        highlightSelectMenu = true, onSelectGuideDone = { done++ }
                    )
                }
            }
        }
        var st = 0L
        fun sframe() { st += 16_000_000L; settings.render(st) }
        repeat(150) { sframe() }
        val price = com.example.ui.SelectionMenuHarness.priceBounds!!
        val point = if (inside) price.center else Offset(px(200f), px(120f))
        settings.sendPointerEvent(PointerEventType.Press, point); sframe()
        Thread.sleep(800); sframe()
        settings.sendPointerEvent(PointerEventType.Release, point); sframe()
        println("${if (inside) "long-press sample price" else "long-press outside    "} -> pricePressed=${com.example.ui.SelectionMenuHarness.longPresses} guideDone=$done (price at y=${(price.center.y / density).toInt()}dp)")
        settings.close()
    }

    // Feature tour: a tap in the first moment after it slides in is ignored (it was meant for the keypad).
    for ((label, waitFrames) in listOf("tap right after tour appears" to 6, "tap once the tour settled  " to 80)) {
        var dismissed = 0
        val tourScene = ImageComposeScene(width = px(400f).toInt(), height = px(860f).toInt(), density = Density(density)) {
            MyApplicationTheme(themeMode = "light") {
                CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                    com.example.ui.FeatureTour(
                        tips = listOf(com.example.ui.FeatureTip.WIDGET, com.example.ui.FeatureTip.EXTRA_CURRENCY),
                        canPinWidget = true, onShowWidgetGuide = {}, onAddCurrency = {}, onDismiss = { dismissed++ }
                    )
                }
            }
        }
        var tt = 0L
        fun tframe() { tt += 16_000_000L; tourScene.render(tt) }
        repeat(waitFrames) { tframe() }
        // The scrim above the sheet.
        val scrim = Offset(px(200f), px(60f))
        tourScene.sendPointerEvent(PointerEventType.Press, scrim); tframe()
        tourScene.sendPointerEvent(PointerEventType.Release, scrim); tframe()
        println("$label -> dismissed=$dismissed")
        tourScene.close()
    }

    // Keypad and history link (v5.0.18). Keypad area: 428-848dp, bottom row 765-847; columns 116dp from x=16.
    // The history link sits at the end of the row under the cards (the left side in Hebrew), around y=262.
    var history = 0
    var toggles = 0
    val keypadScene = ImageComposeScene(width = px(400f).toInt(), height = px(860f).toInt(), density = Density(density)) {
        MyApplicationTheme(themeMode = "light") {
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
                CalculatorContent(
                    state = state.copy(sourceAmountRaw = "1250", extraTargetCurrency = null), onSwap = {}, onCurrencyClick = {},
                    onKeypad = {}, onOpenSettings = {}, onRefresh = {},
                    onToggleCalculator = { toggles++ }, onOpenHistory = { history++ }
                )
            }
        }
    }
    var kt = 0L
    fun kframe() { kt += 16_000_000L; keypadScene.render(kt) }
    fun ktap(o: Offset) { keypadScene.sendPointerEvent(PointerEventType.Press, o); kframe(); keypadScene.sendPointerEvent(PointerEventType.Release, o); kframe() }
    kframe()
    ktap(Offset(px(60f), px(262f)))
    println("history link                   -> history=$history")
    ktap(Offset(px(80f), px(112f)))
    println("tap on the source amount       -> history=$history clicked=$clicked")
    ktap(Offset(px(326f), px(806f)))
    println("tap on the calculator key      -> toggles=$toggles")
    // A long press lifts the row for dragging; letting go without moving must not open the history.
    keypadScene.sendPointerEvent(PointerEventType.Press, Offset(px(80f), px(112f))); kframe()
    Thread.sleep(800); kframe()
    keypadScene.sendPointerEvent(PointerEventType.Release, Offset(px(80f), px(112f))); kframe()
    println("long press on the amount       -> history=$history")
    keypadScene.close()
    System.exit(0)
}
