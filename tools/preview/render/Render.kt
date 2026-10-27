package render

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.ImageComposeScene
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.data.AutoUpdateSettings
import com.example.ui.CalculatorContent
import com.example.ui.CalculatorState
import com.example.ui.CurrencySelector
import com.example.ui.SettingsScreen
import com.example.ui.theme.LocalPalette
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.paletteTexture
import org.jetbrains.skia.EncodedImageFormat
import java.io.File

private val rates = mapOf(
    "USD" to 1.0, "ILS" to 3.7123, "EUR" to 0.9191, "GBP" to 0.7874, "JPY" to 149.52,
    "CHF" to 0.8812, "AUD" to 1.5231, "CAD" to 1.3644, "THB" to 35.81, "TRY" to 34.12,
    "CZK" to 23.11, "PLN" to 3.98, "HUF" to 357.4, "AED" to 3.6725, "JOD" to 0.709, "EGP" to 48.6
)

private val state = CalculatorState(
    sourceCurrency = "USD",
    targetCurrency = "ILS",
    sourceAmountRaw = "1250",
    rates = rates,
    recentCurrencies = listOf("EUR", "ILS", "GBP", "THB"),
    lastUpdateTimestamp = System.currentTimeMillis() - 25 * 60 * 1000,
)

@Composable
private fun Frame(dark: Boolean, theme: String, lang: String = "iw", content: @Composable () -> Unit) {
    androidx.compose.ui.res.HarnessStrings.language = lang
    val locale = java.util.Locale.forLanguageTag(if (lang == "iw") "he" else lang)
    MyApplicationTheme(themeMode = if (dark) "dark" else "light", colorThemeStr = theme) {
        CompositionLocalProvider(
            LocalLayoutDirection provides if (lang == "iw" || lang == "ar") LayoutDirection.Rtl else LayoutDirection.Ltr,
            com.example.ui.LocalAppLocale provides locale
        ) {
            Box(Modifier.fillMaxSize().background(LocalPalette.current.background).paletteTexture(theme, LocalPalette.current)) { content() }
        }
    }
}

private fun render(name: String, widthDp: Int = 400, heightDp: Int = 860, content: @Composable () -> Unit) {
    System.getenv("ONLY")?.let { f -> if (f.split(",").none { name.contains(it) }) return }
    val density = 2f
    val scene = ImageComposeScene(
        width = (widthDp * density).toInt(),
        height = (heightDp * density).toInt(),
        density = Density(density),
        content = content
    )
    scene.render(0)
    val image = scene.render(2_000_000_000L)
    File("out").mkdirs()
    File("out/$name.png").writeBytes(image.encodeToData(EncodedImageFormat.PNG)!!.bytes)
    scene.close()
    println("rendered out/$name.png")
}

@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun renderTapped(name: String, tapsDp: List<Pair<Float, Float>>, widthDp: Int = 400, heightDp: Int = 860, frames: Int = 60, content: @Composable () -> Unit) {
    System.getenv("ONLY")?.let { f -> if (f.split(",").none { name.contains(it) }) return }
    val density = 2f
    val scene = ImageComposeScene(width = (widthDp * density).toInt(), height = (heightDp * density).toInt(), density = Density(density), content = content)
    var t = 0L
    fun frame() { t += 16_000_000L; scene.render(t) }
    frame()
    for ((x, y) in tapsDp) {
        val o = androidx.compose.ui.geometry.Offset(x * density, y * density)
        scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, o); frame()
        scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, o); frame()
    }
    repeat(frames) { frame() }
    File("out").mkdirs()
    File("out/$name.png").writeBytes(scene.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    scene.close()
    println("rendered out/$name.png")
}

/** Taps, lets the screen settle, then saves a frame every [stepMs] for [durationMs]: out/<name>/000.png... */
@OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)
private fun renderFrames(
    name: String, tapsDp: List<Pair<Float, Float>>, widthDp: Int = 400, heightDp: Int = 860,
    durationMs: Int = com.example.ui.DemoTimeline.LOOP_MS, stepMs: Int = 40, content: @Composable () -> Unit,
) {
    System.getenv("ONLY")?.let { f -> if (f.split(",").none { name.contains(it) }) return }
    val density = 2f
    val scene = ImageComposeScene(width = (widthDp * density).toInt(), height = (heightDp * density).toInt(), density = Density(density), content = content)
    var t = 0L
    fun frame() { t += 16_000_000L; scene.render(t) }
    frame()
    for ((x, y) in tapsDp) {
        val o = androidx.compose.ui.geometry.Offset(x * density, y * density)
        scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Press, o); frame()
        scene.sendPointerEvent(androidx.compose.ui.input.pointer.PointerEventType.Release, o); frame()
    }
    repeat(40) { frame() }
    val dir = File("out/$name").apply { deleteRecursively(); mkdirs() }
    for (i in 0 until durationMs / stepMs) {
        t += stepMs * 1_000_000L
        File(dir, "%03d.png".format(i)).writeBytes(scene.render(t).encodeToData(EncodedImageFormat.PNG)!!.bytes)
    }
    scene.close()
    println("rendered out/$name/ (${durationMs / stepMs} frames)")
}

private val now = System.currentTimeMillis()
private val sampleHistory = listOf(
    com.example.utils.HistoryEntry(now - 4 * 60_000, "USD", "ILS", "EUR", "1550", "1250+300", 5754.07, 1424.61),
    com.example.utils.HistoryEntry(now - 50 * 60_000, "EUR", "ILS", null, "138", "120+15%", 557.39, null),
    com.example.utils.HistoryEntry(now - 3 * 3600_000, "USD", "ILS", null, "89.9", null, 333.74, null),
    com.example.utils.HistoryEntry(now - 26 * 3600_000, "THB", "ILS", null, "2400", "3*800", 248.80, null),
    com.example.utils.HistoryEntry(now - 50 * 3600_000, "USD", "ILS", null, "250", null, 928.08, null),
    com.example.utils.HistoryEntry(now - 70 * 3600_000, "JPY", "USD", null, "12000", null, 80.26, null),
)

private fun calcState(expression: String?, last: String? = null, raw: String = "1250", extra: String? = null, history: Boolean = false) =
    state.copy(
        sourceAmountRaw = raw, expression = expression, lastExpression = last, extraTargetCurrency = extra,
        showHistory = history, history = sampleHistory, freshAmount = last != null
    )

@Composable
private fun Calc(s: com.example.ui.CalculatorState, offer: com.example.ui.FeatureTip? = null) =
    CalculatorContent(s, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {}, tourOffer = offer)

/** Google Play screenshots: the app in each listing language, with currencies people there would use. */
private data class StoreLang(val lang: String, val src: String, val tgt: String, val extra: String, val quote: String)
private val storeLangs = listOf(
    StoreLang("iw", "USD", "ILS", "EUR", "€189"), StoreLang("en", "USD", "EUR", "GBP", "€189"),
    StoreLang("es", "EUR", "USD", "MXN", "$189"), StoreLang("ar", "USD", "SAR", "AED", "€189"),
    StoreLang("ru", "USD", "RUB", "EUR", "€189"), StoreLang("fr", "EUR", "USD", "GBP", "$189"),
    StoreLang("de", "EUR", "USD", "CHF", "$189"), StoreLang("pt", "USD", "BRL", "EUR", "€189"),
    StoreLang("it", "EUR", "USD", "GBP", "$189"), StoreLang("tr", "EUR", "TRY", "USD", "€189"),
    StoreLang("zh", "USD", "CNY", "JPY", "€189"), StoreLang("ja", "USD", "JPY", "EUR", "€189"),
    StoreLang("hi", "USD", "INR", "EUR", "€189"),
)
private val storeRates = mapOf(
    "USD" to 1.0, "EUR" to 0.9191, "GBP" to 0.7874, "ILS" to 3.7123, "MXN" to 18.31, "SAR" to 3.75, "AED" to 3.6725,
    "RUB" to 92.48, "CHF" to 0.8812, "BRL" to 5.418, "TRY" to 34.27, "CNY" to 7.214, "JPY" to 151.24, "INR" to 84.07,
    "THB" to 35.81, "CAD" to 1.3644,
)

private fun storeShots() {
    for (l in storeLangs) {
        val rtl = l.lang == "iw" || l.lang == "ar"
        val base = state.copy(sourceCurrency = l.src, targetCurrency = l.tgt, rates = storeRates, recentCurrencies = listOf(l.extra, l.tgt))
        render("store_${l.lang}_1_main") { Frame(false, "sage", l.lang) { Calc(base.copy(sourceAmountRaw = "250", extraTargetCurrency = l.extra)) } }
        render("store_${l.lang}_3_calc") { Frame(false, "ocean", l.lang) { Calc(base.copy(sourceAmountRaw = "138", expression = "120+15%")) } }
        render("store_${l.lang}_5a_theme") { Frame(true, "lavender", l.lang) { Calc(base.copy(sourceAmountRaw = "250")) } }
        render("store_${l.lang}_5b_theme") { Frame(false, "gold", l.lang) { Calc(base.copy(sourceAmountRaw = "250")) } }
        render("store_${l.lang}_5c_theme") { Frame(false, "rose", l.lang) { Calc(base.copy(sourceAmountRaw = "250")) } }
        renderTapped("store_${l.lang}_4_quick", emptyList(), frames = 40) {
            androidx.compose.ui.res.HarnessStrings.language = l.lang
            val locale = java.util.Locale.forLanguageTag(if (l.lang == "iw") "he" else l.lang)
            MyApplicationTheme(themeMode = "light", colorThemeStr = "sage") {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (rtl) LayoutDirection.Rtl else LayoutDirection.Ltr,
                    com.example.ui.LocalAppLocale provides locale
                ) {
                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFFF4F4F6))) {
                        FakeBrowserPage(l.quote, false)
                        com.example.ui.QuickConvertSheet(
                            conversion = com.example.utils.QuickConvert.convert(l.quote, storeRates, l.src, l.tgt, null, null),
                            colorTheme = "sage", offline = false, canReplace = false, copied = false,
                            onCopy = {}, onReplace = {}, onOpenApp = {}, onDismiss = {}
                        )
                    }
                }
            }
        }
        val strings = { key: String -> androidx.compose.ui.res.HarnessStrings.get(l.lang, key) }
        val time = String.format(strings("today_at"), "09:41")
        val locale = java.util.Locale.forLanguageTag(if (l.lang == "iw") "he" else l.lang)
        val rate = storeRates.getValue(l.tgt) / storeRates.getValue(l.src)
        val p = com.example.ui.theme.Palettes.resolve("sunset", false)
        render("store_${l.lang}_2a_widget", widthDp = 300, heightDp = 380) {
            com.example.widget.LargeWidgetMock2(
                p, 300f, 380f, rtl, locale, amountRaw = "250", src = l.src, tgt = l.tgt, rate = rate, extra = null, extraRate = 0.0,
                updatedTime = time, updatedLabel = String.format(strings("updated_at"), time), launcherRtl = rtl, theme = "sunset"
            )
        }
        render("store_${l.lang}_2b_widget", widthDp = 300, heightDp = 130) {
            com.example.widget.SmallWidgetMock2(
                com.example.ui.theme.Palettes.resolve("ocean", false), 300f, 130f, rtl, "250", l.src, l.extra,
                storeRates.getValue(l.extra) / storeRates.getValue(l.src), null, 0.0, launcherRtl = rtl, theme = "ocean"
            )
        }
    }
}

fun main() {
    if (System.getenv("ONLY")?.startsWith("store_") == true) {
        storeShots()
        System.exit(0)
    }
    // v5.0.24: Samsung's one-time setup acted out (⋮ → "Manage apps" → switch on → Done), the tour card's "show me
    // how", and the guide it opens: Settings on the sample price, in a spotlight.
    for ((name, app, device, theme, dark) in listOf(
        DemoCase("he_he", "iw", "iw", "sunset", false), DemoCase("en_en", "en", "en", "sage", false),
        DemoCase("he_en_dark", "iw", "en", "lavender", true), DemoCase("ar_ar", "ar", "ar", "ocean", false),
    )) {
        val sample = com.example.utils.QuickConvert.convert(com.example.ui.SAMPLE_PRICE, rates, "USD", "ILS", null, null).lines.firstOrNull()
        renderFrames("v24_setup_$name", emptyList(), heightDp = 240, durationMs = com.example.ui.SetupTimeline.LOOP_MS) {
            com.example.ui.SelectionMenuHarness.deviceLanguage = device
            com.example.ui.SelectionMenuHarness.samsung = true
            Frame(dark, theme, app) {
                Box(
                    Modifier.padding(12.dp).fillMaxWidth()
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(28.dp))
                        .background(LocalPalette.current.card).padding(14.dp)
                ) {
                    com.example.ui.SelectionMenuDemo(com.example.ui.rememberSelectionMenuInfo(), com.example.ui.SAMPLE_PRICE, sample, setup = true)
                }
            }
        }
    }
    for ((lang, dark) in listOf("iw" to false, "en" to false, "iw" to true)) {
        renderTapped("v24_tour_select_${lang}_${if (dark) "dark" else "light"}", emptyList(), frames = 150) {
            com.example.ui.SelectionMenuHarness.deviceLanguage = lang
            Frame(dark, "sage", lang) {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
                com.example.ui.FeatureTour(
                    tips = listOf(com.example.ui.FeatureTip.WIDGET, com.example.ui.FeatureTip.SELECT_CONVERT, com.example.ui.FeatureTip.CALCULATOR),
                    canPinWidget = true, onShowWidgetGuide = {}, onAddCurrency = {}, onDismiss = {}, initialPage = 1
                )
            }
        }
    }
    for ((name, app, device, samsung, dark) in listOf(
        MenuCase("he_en_samsung", "iw", "en", true, false), MenuCase("en_en_samsung", "en", "en", true, false),
        MenuCase("he_he_pixel", "iw", "iw", false, false), MenuCase("he_he_samsung_dark", "iw", "iw", true, true),
    )) {
        val sample = com.example.utils.QuickConvert.convert(com.example.ui.SAMPLE_PRICE, rates, "USD", "ILS", null, null).lines.firstOrNull()
        for ((suffix, guide) in listOf("guide" to true, "section" to false)) {
            val tall = !guide
            renderTapped("v24_${suffix}_$name", if (guide) emptyList() else listOf(200f to (System.getenv("MENU_TAP_Y")?.toFloat() ?: 400f)),
                heightDp = if (tall) 1700 else 860, frames = 150) {
                com.example.ui.SelectionMenuHarness.deviceLanguage = device
                com.example.ui.SelectionMenuHarness.samsung = samsung
                Frame(dark, "sage", app) {
                    SettingsScreen(
                        themeMode = "system", colorTheme = "sage", autoUpdate = AutoUpdateSettings(),
                        lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                        canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                        onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                        onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                        appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.24", checked = true),
                        checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                        language = if (app == "iw") "he" else app, onLanguageChange = {}, quickSample = sample,
                        highlightSelectMenu = guide
                    )
                }
            }
        }
    }
    // v5.0.23: the "Convert from any app" demo on a loop, as frames (out/<name>/NNN.png; tools/preview/gif.py makes the GIF).
    for ((name, app, device, theme, dark) in listOf(
        DemoCase("he_he", "iw", "iw", "sunset", false), DemoCase("en_en", "en", "en", "sage", false),
        DemoCase("he_en_dark", "iw", "en", "lavender", true), DemoCase("ar_ar", "ar", "ar", "ocean", false),
    )) {
        val sample = com.example.utils.QuickConvert.convert(com.example.ui.SAMPLE_PRICE, rates, "USD", "ILS", null, null).lines.firstOrNull()
        renderFrames("v23_demo_$name", listOf(200f to (System.getenv("MENU_TAP_Y")?.toFloat() ?: 400f)), heightDp = 1000) {
            com.example.ui.SelectionMenuHarness.deviceLanguage = device
            com.example.ui.SelectionMenuHarness.samsung = true
            Frame(dark, theme, app) {
                SettingsScreen(
                    themeMode = "system", colorTheme = theme, autoUpdate = AutoUpdateSettings(),
                    lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                    canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                    onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                    onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                    appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.23", checked = true),
                    checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                    language = if (app == "iw") "he" else app, onLanguageChange = {}, quickSample = sample
                )
            }
        }
    }
    // v5.0.22: "Convert from any app" in Settings (a price to long-press, Samsung's steps); the bubble's currency picker.
    for ((name, app, device, samsung, dark) in listOf(
        MenuCase("he_en_samsung", "iw", "en", true, false), MenuCase("en_en_samsung", "en", "en", true, false),
        MenuCase("he_he_pixel", "iw", "iw", false, false), MenuCase("he_en_samsung_dark", "iw", "en", true, true),
    )) {
        val tapY = System.getenv("MENU_TAP_Y")?.toFloat() ?: 400f
        for (open in listOf(false, true)) {
            renderTapped("v22_settings_${name}_${if (open) "open" else "closed"}", if (open) listOf(200f to tapY) else emptyList(), heightDp = 1100, frames = 40) {
                com.example.ui.SelectionMenuHarness.deviceLanguage = device
                com.example.ui.SelectionMenuHarness.samsung = samsung
                Frame(dark, "sage", app) {
                    SettingsScreen(
                        themeMode = "system", colorTheme = "sage", autoUpdate = AutoUpdateSettings(),
                        lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                        canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                        onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                        onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                        appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.22", checked = true),
                        checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                        language = if (app == "iw") "he" else app, onLanguageChange = {}
                    )
                }
            }
        }
    }
    val pickRates = mapOf("USD" to 1.0, "ILS" to 3.7123, "EUR" to 0.9191, "GBP" to 0.7874, "THB" to 35.81, "JPY" to 149.52, "CAD" to 1.3644,
        "CHF" to 0.8812, "HUF" to 357.4, "CZK" to 23.11, "PLN" to 3.98, "TRY" to 34.12, "AED" to 3.6725)
    for ((name, spec, chosen, picking) in listOf(
        Quad2("single_iw", QuickSpec("iw", false, "€120", "ILS", "USD", null, "ILS", canReplace = false), null, false),
        Quad2("single_iw_chosen_huf", QuickSpec("iw", false, "€120", "ILS", "USD", null, "ILS", canReplace = false), "HUF", false),
        Quad2("single_iw_picker", QuickSpec("iw", false, "€120", "ILS", "USD", null, "ILS", canReplace = false), null, true),
        Quad2("single_en_picker", QuickSpec("en", false, "Rooms from $189 per night", "ILS", "USD", null, "ILS", canReplace = false, theme = "ocean"), null, true),
        Quad2("single_iw_dark_picker", QuickSpec("iw", true, "€120", "ILS", "USD", "GBP", "ILS", canReplace = false, theme = "lavender"), null, true),
        Quad2("list_iw", QuickSpec("iw", false, "Room €90, breakfast €12, city tax €4", "ILS", "USD", null, "ILS", canReplace = true, theme = "sunset"), null, false),
        Quad2("list_iw_picker", QuickSpec("iw", false, "Room €90, breakfast €12, city tax €4", "ILS", "USD", null, "ILS", canReplace = true, theme = "sunset"), null, true),
    )) {
        renderTapped("v22_quick_$name", emptyList(), frames = 40) {
            androidx.compose.ui.res.HarnessStrings.language = spec.lang
            val locale = java.util.Locale.forLanguageTag(if (spec.lang == "iw") "he" else spec.lang)
            MyApplicationTheme(themeMode = if (spec.dark) "dark" else "light", colorThemeStr = spec.theme) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (spec.lang == "iw") LayoutDirection.Rtl else LayoutDirection.Ltr,
                    com.example.ui.LocalAppLocale provides locale
                ) {
                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(if (spec.dark) 0xFF1B1B1F else 0xFFF4F4F6))) {
                        FakeBrowserPage(spec.text, spec.dark)
                        com.example.ui.QuickConvertSheet(
                            conversion = com.example.utils.QuickConvert.convert(spec.text, pickRates, spec.source, spec.target, spec.extra, spec.local, chosen),
                            colorTheme = spec.theme, offline = false, canReplace = spec.canReplace, copied = false,
                            onCopy = {}, onReplace = {}, onOpenApp = {}, onDismiss = {},
                            rates = pickRates, recents = listOfNotNull(chosen, spec.target, spec.source, spec.extra, "EUR", "THB"),
                            onChooseCurrency = {}, startPicking = picking
                        )
                    }
                }
            }
        }
    }
    // v5.0.21 (Play build): Premium sheet, the locked calculator key, the ad strip, Settings and the locked widget.
    val hourMs = 3_600_000L
    val freeState = com.example.premium.PremiumState()
    val trialState = com.example.premium.PremiumState(trialUntil = now + 50 * hourMs)
    val halfState = com.example.premium.PremiumState(videosWatched = 1, lastVideoAt = now - 60_000)
    val rewardState = com.example.premium.PremiumState(rewardUntil = now + 23 * hourMs)
    fun sheet(reason: com.example.premium.PremiumReason, st: com.example.premium.PremiumState, video: com.example.premium.VideoStatus = com.example.premium.VideoStatus.IDLE): @Composable () -> Unit = {
        com.example.ui.PremiumSheet(
            reason = reason, access = st.access(now), videosNeeded = st.videosNeeded(now), price = "₪14.90", video = video,
            purchasePending = false, onWatch = {}, onBuy = {}, onRestore = {}, onDismiss = {}, now = now
        )
    }
    val promo: @Composable () -> Unit = {
        Box(Modifier.fillMaxWidth().height(56.dp)) { com.example.ui.PremiumPromo(onClick = {}) }
    }
    @Composable fun LockedCalc(s: com.example.ui.CalculatorState = calcState(null)) =
        CalculatorContent(s, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {}, calculatorLocked = true, adSlot = promo)
    for ((lang, dark, theme) in listOf(Triple("iw", false, "sage"), Triple("en", false, "sage"), Triple("iw", true, "sage"), Triple("ar", false, "sage"), Triple("ru", false, "sage"))) {
        val tag = "${lang}_${if (dark) "dark" else "light"}"
        render("v21_a_locked_calc_$tag") { Frame(dark, theme, lang) { LockedCalc() } }
        renderTapped("v21_b_sheet_calc_$tag", emptyList()) { Frame(dark, theme, lang) { LockedCalc(); sheet(com.example.premium.PremiumReason.CALCULATOR, freeState)() } }
    }
    renderTapped("v21_c_sheet_half_iw", emptyList()) { Frame(false, "sage", "iw") { LockedCalc(); sheet(com.example.premium.PremiumReason.CALCULATOR, halfState)() } }
    renderTapped("v21_c_sheet_loading_iw", emptyList()) { Frame(false, "sage", "iw") { LockedCalc(); sheet(com.example.premium.PremiumReason.WIDGET, halfState, com.example.premium.VideoStatus.LOADING)() } }
    renderTapped("v21_c_sheet_unavailable_iw", emptyList()) { Frame(false, "sage", "iw") { LockedCalc(); sheet(com.example.premium.PremiumReason.COLORS, freeState, com.example.premium.VideoStatus.UNAVAILABLE)() } }
    renderTapped("v21_c_sheet_trial_iw", emptyList()) { Frame(false, "ocean", "iw") { Calc(calcState(null)); sheet(com.example.premium.PremiumReason.SETTINGS, trialState)() } }
    renderTapped("v21_c_sheet_reward_iw", emptyList()) { Frame(false, "gold", "iw") { Calc(calcState(null)); sheet(com.example.premium.PremiumReason.NO_ADS, rewardState)() } }
    renderTapped("v21_c_sheet_owned_iw", emptyList()) { Frame(false, "gold", "iw") { Calc(calcState(null)); sheet(com.example.premium.PremiumReason.SETTINGS, com.example.premium.PremiumState(purchased = true))() } }
    render("v21_d_small_phone_iw", widthDp = 360, heightDp = 640) { Frame(false, "sage", "iw") { LockedCalc() } }
    renderTapped("v21_d_small_phone_sheet_iw", emptyList(), widthDp = 360, heightDp = 640) { Frame(false, "sage", "iw") { LockedCalc(); sheet(com.example.premium.PremiumReason.CALCULATOR, freeState)() } }
    for ((lang, st, tag) in listOf(Triple("iw", freeState, "free"), Triple("iw", trialState, "trial"), Triple("en", rewardState, "reward"))) {
        // A tap on "Appearance" opens it, to show the locked colour sets.
        renderTapped("v21_e_settings_${lang}_$tag", listOf(200f to 250f), frames = 40) {
            Frame(false, "sage", lang) {
                SettingsScreen(
                    themeMode = "system", colorTheme = "sage", autoUpdate = AutoUpdateSettings(),
                    lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                    canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                    onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                    onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                    appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.21", checked = true),
                    checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                    language = if (lang == "iw") "he" else lang, onLanguageChange = {},
                    showAppUpdates = false,
                    premium = com.example.ui.PremiumSummary(st.access(now), false),
                    colorsLocked = !st.access(now).unlocked,
                    adPrivacyOptions = true
                )
            }
        }
    }
    for ((lang, dark) in listOf("he" to false, "he" to true, "en" to false)) {
        val p = com.example.ui.theme.Palettes.resolve("sage", dark)
        val strings = { key: String -> androidx.compose.ui.res.HarnessStrings.get(if (lang == "he") "iw" else lang, key) }
        render("v21_f_widget_locked_${lang}_${if (dark) "dark" else "light"}", widthDp = 560, heightDp = 420) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF3B4E66)).padding(10.dp)) {
                androidx.compose.foundation.layout.Row(horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                    com.example.widget.LockedWidgetMock(p, 300f, 380f, strings("widget_locked_title"), strings("widget_locked_action"),
                        hint = String.format(strings("premium_settings_free"), 2, 24))
                    androidx.compose.foundation.layout.Column(verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)) {
                        com.example.widget.LockedWidgetMock(p, 220f, 120f, strings("widget_locked_title"), strings("widget_locked_action"))
                        com.example.widget.LockedWidgetMock(p, 220f, 80f, strings("widget_locked_title"), strings("widget_locked_action"))
                    }
                }
            }
        }
    }
    // v5.0.19: the history header keeps only its icon; the clear key matches the keypad key beside it.
    for ((lang, dark, theme) in listOf(Triple("iw", true, "sunset"), Triple("iw", false, "sage"), Triple("en", false, "ocean"), Triple("ru", false, "gold"))) {
        val tag = "${lang}_${theme}"
        render("v19_history_$tag") { Frame(dark, theme, lang) { Calc(calcState(null, raw = "1550", history = true)) } }
        // A tap on the clear key (left of the header in Hebrew, right in English): the "Clear all?" step.
        val x = if (lang == "iw") 134f else 266f
        renderTapped("v19_confirm_$tag", listOf(x to 454f), frames = 40) { Frame(dark, theme, lang) { Calc(calcState(null, raw = "1550", history = true)) } }
    }
    render("v19_history_empty_iw") { Frame(false, "sage", "iw") { Calc(calcState(null, history = true).copy(history = emptyList())) } }
    // v5.0.18: no quick amounts, the history link under the cards, the calculation without the symbol.
    val huf = calcState("90*3").copy(sourceCurrency = "HUF")
    render("v18_owner_before_after") { Frame(true, "sunset", "iw") { Calc(huf) } }
    for ((lang, dark) in listOf("iw" to false, "en" to false, "iw" to true)) {
        val tag = "${lang}_${if (dark) "dark" else "light"}"
        render("v18_a_plain_$tag") { Frame(dark, "sage", lang) { Calc(calcState(null)) } }
        render("v18_b_calc_$tag") { Frame(dark, "sage", lang) { Calc(calcState("1250+300*2", extra = "EUR")) } }
        render("v18_c_equals_$tag") { Frame(dark, "sage", lang) { Calc(calcState("1850", last = "1250+300*2", raw = "1850", extra = "EUR")) } }
        render("v18_d_history_$tag") { Frame(dark, "sage", lang) { Calc(calcState(null, raw = "1550", last = "1250+300", history = true)) } }
        render("v18_e_extra_$tag") { Frame(dark, "sage", lang) { Calc(calcState(null, extra = "EUR")) } }
    }
    render("v18_f_long_iw") { Frame(false, "sage", "iw") { Calc(calcState("1250+300*2-(45.5+12)/3+999+12345+15%")) } }
    render("v18_f_divzero_iw") { Frame(false, "sage", "iw") { Calc(calcState("5/0")) } }
    render("v18_f_open_iw") { Frame(false, "sage", "iw") { Calc(calcState("1250")) } }
    render("v18_f_operator_iw") { Frame(false, "sage", "iw") { Calc(calcState("1250+")) } }
    render("v18_f_huf_light") { Frame(false, "sunset", "iw") { Calc(huf) } }
    render("v18_g_small_calc_iw", widthDp = 360, heightDp = 640) { Frame(false, "sage", "iw") { Calc(calcState("1250+300", extra = "THB")) } }
    render("v18_g_small_plain_iw", widthDp = 360, heightDp = 640) { Frame(false, "sage", "iw") { Calc(calcState(null, extra = "THB")) } }
    render("v18_g_small_history_iw", widthDp = 360, heightDp = 640) { Frame(false, "sage", "iw") { Calc(calcState(null, extra = "THB", history = true)) } }
    render("v18_g_ru_calc", widthDp = 360, heightDp = 640) { Frame(false, "sage", "ru") { Calc(calcState("1250+300").copy(targetCurrency = "ILS")) } }
    for (theme in listOf("ocean", "sunset", "rose", "lavender", "gold")) {
        render("v18_h_${theme}_calc") { Frame(theme == "lavender", theme, "iw") { Calc(calcState("1250+300", extra = "EUR")) } }
    }
    for (frames in listOf(30, 75, 140, 175, 220, 275)) {
        renderTapped("v18_tour_iw_light_$frames", emptyList(), frames = frames) {
            Frame(false, "sage", "iw") {
                Calc(calcState(null))
                com.example.ui.FeatureTour(
                    tips = listOf(com.example.ui.FeatureTip.CALCULATOR),
                    canPinWidget = true, onShowWidgetGuide = {}, onAddCurrency = {}, onDismiss = {}
                )
            }
        }
    }
    // v5.0.17: the calculator key, quick amounts and the history.
    for ((lang, dark) in listOf("iw" to false, "en" to false, "iw" to true)) {
        val tag = "${lang}_${if (dark) "dark" else "light"}"
        render("v17_a_plain_$tag") { Frame(dark, "sage", lang) { Calc(calcState(null)) } }
        render("v17_b_zero_$tag") { Frame(dark, "sage", lang) { Calc(calcState(null, raw = "0")) } }
        render("v17_c_calc_$tag") { Frame(dark, "sage", lang) { Calc(calcState("1250+300", extra = "EUR")) } }
        render("v17_d_equals_$tag") { Frame(dark, "sage", lang) { Calc(calcState("1550", last = "1250+300", raw = "1550", extra = "EUR")) } }
        render("v17_e_history_$tag") { Frame(dark, "sage", lang) { Calc(calcState(null, raw = "1550", last = "1250+300", history = true)) } }
    }
    render("v17_f_long_iw") { Frame(false, "sage", "iw") { Calc(calcState("1250+300*2-(45.5+12)/3+999+12345+15%")) } }
    render("v17_f_divzero_iw") { Frame(false, "sage", "iw") { Calc(calcState("5/0")) } }
    render("v17_f_open_iw") { Frame(false, "sage", "iw") { Calc(calcState("1250")) } }
    render("v17_f_history_empty_iw") { Frame(false, "sage", "iw") { Calc(calcState(null, history = true).copy(history = emptyList())) } }
    render("v17_g_small_calc_iw", widthDp = 360, heightDp = 640) { Frame(false, "sage", "iw") { Calc(calcState("1250+300", extra = "THB")) } }
    render("v17_g_small_history_iw", widthDp = 360, heightDp = 640) { Frame(false, "sage", "iw") { Calc(calcState(null, extra = "THB", history = true)) } }
    render("v17_g_small_zero_iw", widthDp = 360, heightDp = 640) { Frame(false, "sage", "iw") { Calc(calcState(null, raw = "0", extra = "THB")) } }
    for (theme in listOf("ocean", "sunset", "rose", "lavender", "gold")) {
        render("v17_h_${theme}_calc") { Frame(theme == "lavender", theme, "iw") { Calc(calcState("1250+300", extra = "EUR")) } }
    }
    render("v17_h_jpy_zero") { Frame(false, "gold", "iw") { Calc(calcState(null, raw = "0").copy(sourceCurrency = "JPY")) } }
    render("v17_h_offer_iw") { Frame(false, "sage", "iw") { Calc(calcState(null), offer = com.example.ui.FeatureTip.CALCULATOR) } }
    for ((lang, dark) in listOf("iw" to false, "en" to false, "ar" to false, "iw" to true)) {
        for (frames in listOf(30, 75, 140, 175, 220, 275)) {
            renderTapped("v17_tour_${lang}_${if (dark) "dark" else "light"}_$frames", emptyList(), frames = frames) {
                Frame(dark, "sage", lang) {
                    Calc(calcState(null))
                    com.example.ui.FeatureTour(
                        tips = listOf(com.example.ui.FeatureTip.CALCULATOR),
                        canPinWidget = true, onShowWidgetGuide = {}, onAddCurrency = {}, onDismiss = {}
                    )
                }
            }
        }
    }
    // The tour offer banner (instead of the sheet popping up over the keypad).
    for ((lang, dark, tip) in listOf(
        Triple("iw", false, com.example.ui.FeatureTip.SELECT_CONVERT),
        Triple("en", false, com.example.ui.FeatureTip.WIDGET),
        Triple("iw", true, com.example.ui.FeatureTip.WIDGET),
    )) {
        render("v15b_offer_${lang}_${if (dark) "dark" else "light"}") {
            Frame(dark, "sage", lang) {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {}, tourOffer = tip)
            }
        }
    }
    render("v15b_offer_small_phone", widthDp = 360, heightDp = 640) {
        Frame(false, "sage", "iw") {
            CalculatorContent(state.copy(extraTargetCurrency = "EUR"), onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {}, tourOffer = com.example.ui.FeatureTip.WIDGET)
        }
    }
    // v5.0.15: the quick-convert bubble over another app, and its tour card.
    val quickRates = mapOf("USD" to 1.0, "ILS" to 3.7123, "EUR" to 0.9191, "GBP" to 0.7874, "THB" to 35.81, "JPY" to 149.52, "CAD" to 1.3644)
    for ((name, spec) in listOf(
        "single_iw" to QuickSpec("iw", false, "€120", "ILS", "USD", "GBP", "ILS", canReplace = false),
        "single_iw_dark" to QuickSpec("iw", true, "€120", "ILS", "USD", null, "ILS", canReplace = true, theme = "lavender"),
        "single_en" to QuickSpec("en", false, "Rooms from $189 per night", "ILS", "USD", null, "ILS", canReplace = false, theme = "ocean"),
        "assumed_en" to QuickSpec("en", false, "1,250", "ILS", "USD", null, "THB", canReplace = true, theme = "gold"),
        "list_iw" to QuickSpec("iw", false, "Room €90, breakfast €12, city tax €4", "ILS", "USD", null, "ILS", canReplace = true, theme = "sunset"),
        "none_iw" to QuickSpec("iw", false, "Hotel Lisboa", "ILS", "USD", null, "ILS", canReplace = false),
    )) {
        renderTapped("v15_quick_$name", emptyList(), frames = 40) {
            androidx.compose.ui.res.HarnessStrings.language = spec.lang
            val locale = java.util.Locale.forLanguageTag(if (spec.lang == "iw") "he" else spec.lang)
            MyApplicationTheme(themeMode = if (spec.dark) "dark" else "light", colorThemeStr = spec.theme) {
                CompositionLocalProvider(
                    LocalLayoutDirection provides if (spec.lang == "iw") LayoutDirection.Rtl else LayoutDirection.Ltr,
                    com.example.ui.LocalAppLocale provides locale
                ) {
                    Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(if (spec.dark) 0xFF1B1B1F else 0xFFF4F4F6))) {
                        FakeBrowserPage(spec.text, spec.dark)
                        com.example.ui.QuickConvertSheet(
                            conversion = com.example.utils.QuickConvert.convert(spec.text, quickRates, spec.source, spec.target, spec.extra, spec.local),
                            colorTheme = spec.theme, offline = false, canReplace = spec.canReplace, copied = false,
                            onCopy = {}, onReplace = {}, onOpenApp = {}, onDismiss = {}
                        )
                    }
                }
            }
        }
    }
    for ((lang, dark) in listOf("iw" to false, "en" to false, "iw" to true)) {
        for (frames in listOf(70, 115, 150)) {
            renderTapped("v15_tour_${lang}_${if (dark) "dark" else "light"}_$frames", emptyList(), frames = frames) {
                Frame(dark, "sage", lang) {
                    CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
                    com.example.ui.FeatureTour(
                        tips = listOf(com.example.ui.FeatureTip.SELECT_CONVERT),
                        canPinWidget = true, onShowWidgetGuide = {}, onAddCurrency = {}, onDismiss = {}
                    )
                }
            }
        }
    }
    // v5.0.14: the six textures, light and dark.
    for (theme in listOf("sage", "ocean", "sunset", "rose", "lavender", "gold")) for (dark in listOf(false, true)) {
        render("v14_calc_${theme}_${if (dark) "dark" else "light"}") {
            Frame(dark, theme) {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
            }
        }
    }
    for ((lang, dark) in listOf("iw" to false, "en" to true)) {
        renderTapped("v14_settings_${lang}_${if (dark) "dark" else "light"}", listOf(200f to 200f)) {
            Frame(dark, "gold", lang) {
                SettingsScreen(
                    themeMode = "system", colorTheme = "gold", autoUpdate = AutoUpdateSettings(),
                    lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                    canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                    onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                    onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                    appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.13", checked = true),
                    checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                    language = if (lang == "iw") "he" else lang, onLanguageChange = {}
                )
            }
        }
    }
    for ((name, ui, suggestion) in listOf(
        Triple("en_he", "en", com.example.ui.LanguageSuggestion("he", "להשתמש באפליקציה בעברית?", "החלפה")),
        Triple("en_es", "en", com.example.ui.LanguageSuggestion("es", "¿Usar la app en español?", "Cambiar")),
        Triple("en_ar", "en", com.example.ui.LanguageSuggestion("ar", "استخدام التطبيق باللغة العربية؟", "تغيير")),
    )) {
        render("v14_lang_$name") {
            Frame(false, "sage", ui) {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {}, languageSuggestion = suggestion)
            }
        }
    }
    for (theme in listOf("sage", "ocean", "sunset", "rose", "lavender", "gold")) for (dark in listOf(false, true)) {
        val p = com.example.ui.theme.Palettes.resolve(theme, dark)
        val tag = "${theme}_${if (dark) "dark" else "light"}"
        render("v14_wlarge_$tag", widthDp = 320, heightDp = 400) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF3B4E66)).padding(10.dp)) {
                com.example.widget.LargeWidgetMock2(
                    p, 300f, 380f, true, java.util.Locale.forLanguageTag("he"),
                    amountRaw = "1250", src = "ILS", tgt = "EUR", rate = 0.247582, extra = null, extraRate = 0.0,
                    updatedTime = "היום 22:26", updatedLabel = "עודכן היום 22:26", theme = theme
                )
            }
        }
        render("v14_wsmall_$tag", widthDp = 240, heightDp = 140) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF3B4E66)).padding(10.dp)) {
                com.example.widget.SmallWidgetMock2(p, 220f, 120f, true, "1250", "ILS", "EUR", 0.247582, null, 0.0, theme = theme)
            }
        }
    }
    for (dark in listOf(false, true)) {
        val mode = if (dark) "dark" else "light"
        render("calculator_$mode") {
            Frame(dark, "sage") {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
            }
        }
        render("calculator_extra_$mode") {
            Frame(dark, "sage") {
                CalculatorContent(state.copy(extraTargetCurrency = "EUR"), onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
            }
        }
        render("picker_extra_$mode") {
            Frame(dark, "sage") {
                CurrencySelector(state.copy(showCurrencySelector = true, extraTargetCurrency = "EUR", pickerSlot = com.example.data.CurrencySlot.EXTRA_TARGET), onClose = {}, onSelect = {}, onSearch = {})
            }
        }
        render("calculator_update_$mode") {
            Frame(dark, "sage") {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {},
                    appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.3", available = com.example.data.AppRelease("5.0.4", "", 0)))
            }
        }
        render("picker_$mode") {
            Frame(dark, "sage") {
                CurrencySelector(state.copy(showCurrencySelector = true, pickerSlot = com.example.data.CurrencySlot.TARGET), onClose = {}, onSelect = {}, onSearch = {})
            }
        }
        render("settings_$mode", heightDp = 1180) {
            Frame(dark, "sage") {
                SettingsScreen(
                    themeMode = mode, colorTheme = "sage", autoUpdate = AutoUpdateSettings(),
                    lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                    canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                    onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                    onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                    appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.3", checked = true,
                        available = com.example.data.AppRelease("5.0.4", "", 0)),
                    checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {}
                )
            }
        }
    }
    for (theme in listOf("ocean", "sunset", "rose", "lavender")) {
        render("calculator_${theme}_light") {
            Frame(false, theme) {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
            }
        }
        render("calculator_${theme}_dark") {
            Frame(true, theme) {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
            }
        }
    }
    // Small phone: keypad must shrink instead of pushing content off-screen.
    render("calculator_small_phone_extra", widthDp = 360, heightDp = 640) {
        Frame(false, "sage") {
            CalculatorContent(state.copy(sourceAmountRaw = "1250", extraTargetCurrency = "THB"), onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
        }
    }
    render("calculator_small_phone", widthDp = 360, heightDp = 640) {
        Frame(false, "sage") {
            CalculatorContent(state.copy(sourceAmountRaw = "123456789.25"), onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
        }
    }
    for (dark in listOf(false, true)) {
        val p = com.example.ui.theme.Palettes.resolve("sage", dark)
        val mode = if (dark) "dark" else "light"
        render("widget_large_$mode", widthDp = 320, heightDp = 420) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF6B7F8E)).padding(10.dp)) {
                LargeWidgetMock(p, "1250", "USD", "ILS", 3.7123, "היום 21:34")
            }
        }
        render("widget_large_extra_$mode", widthDp = 320, heightDp = 420) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF6B7F8E)).padding(10.dp)) {
                LargeWidgetMock(p, "1250", "USD", "ILS", 3.7123, "היום 21:34", extra = "EUR", extraRate = 0.9191)
            }
        }
        render("widget_small_$mode", widthDp = 220, heightDp = 120) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF6B7F8E)).padding(10.dp)) {
                SmallWidgetMock(p, "1250", "USD", "ILS", 3.7123)
            }
        }
    }
    for ((lang, dark) in listOf("iw" to false, "iw" to true, "en" to false)) {
        val settings = @Composable {
            Frame(dark, "sage", lang) {
                SettingsScreen(
                    themeMode = "system", colorTheme = "ocean", autoUpdate = AutoUpdateSettings(),
                    lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                    canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                    onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                    onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                    appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.10", checked = true),
                    checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                    language = if (lang == "iw") "he" else lang, onLanguageChange = {}
                )
            }
        }
        val tag = "${lang}_${if (dark) "dark" else "light"}"
        renderTapped("settings_closed_$tag", emptyList(), content = settings)
        // Tap the "Appearance" title row (second card) to open it.
        renderTapped("settings_open_$tag", listOf(200f to 200f), content = settings)
    }
    for ((lang, dark, page, pin) in listOf(
        Quad("iw", false, 0, true), Quad("iw", false, 1, true), Quad("iw", true, 0, true),
        Quad("en", false, 0, true), Quad("en", false, 1, true), Quad("en", false, 0, false), Quad("ar", false, 0, true),
    )) {
        val tag = "tour_${if (page == 0) "widget" else "extra"}_${lang}_${if (dark) "dark" else "light"}${if (pin) "" else "_manual"}"
        renderTapped(tag, emptyList()) {
            Frame(dark, "sage", lang) {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
                com.example.ui.FeatureTour(
                    tips = listOf(com.example.ui.FeatureTip.WIDGET, com.example.ui.FeatureTip.EXTRA_CURRENCY),
                    canPinWidget = pin, onShowWidgetGuide = {}, onAddCurrency = {}, onDismiss = {}, initialPage = page
                )
            }
        }
    }
    // 5.0.28: the crown on Settings while Premium is on (trial, videos or bought).
    for ((lang, dark) in listOf("iw" to false, "iw" to true, "en" to false)) {
        render("v28_crown_${lang}_${if (dark) "dark" else "light"}") {
            Frame(dark, "sage", lang) {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {}, premiumBadge = true)
            }
        }
    }
    // The typing animation at a few moments: finger arriving, "1" typed, "100" typed.
    for (frames in listOf(30, 60, 100, 160)) {
        renderTapped("tourtyping_iw_$frames", emptyList(), frames = frames) {
            Frame(false, "sage", "iw") {
                CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
                com.example.ui.FeatureTour(
                    tips = listOf(com.example.ui.FeatureTip.WIDGET, com.example.ui.FeatureTip.EXTRA_CURRENCY),
                    canPinWidget = true, onShowWidgetGuide = {}, onAddCurrency = {}, onDismiss = {}
                )
            }
        }
    }
    for ((lang, dark) in listOf("iw" to false, "iw" to true, "en" to false)) {
        renderTapped("guide_widget_${lang}_${if (dark) "dark" else "light"}", emptyList(), frames = 150) {
            Frame(dark, "sage", lang) {
                SettingsScreen(
                    themeMode = "system", colorTheme = "sage", autoUpdate = AutoUpdateSettings(),
                    lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                    canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                    onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                    onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                    appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.11", checked = true),
                    checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                    language = if (lang == "iw") "he" else lang, onLanguageChange = {},
                    highlightAddWidget = true
                )
            }
        }
    }
    for (lang in listOf("ar", "ru", "fr", "de", "tr", "ja", "zh", "hi")) {
        render("lang_calculator_$lang") {
            Frame(false, "ocean", lang) {
                CalculatorContent(state.copy(extraTargetCurrency = "EUR"), onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
            }
        }
    }
    for (theme in listOf("sage", "ocean", "sunset", "rose", "lavender")) {
        for (dark in listOf(false, true)) {
            render("texture_${theme}_${if (dark) "dark" else "light"}") {
                Frame(dark, theme) {
                    CalculatorContent(state.copy(extraTargetCurrency = "EUR"), onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
                }
            }
        }
    }
    for ((theme, dark) in listOf("sage" to false, "lavender" to true)) {
        renderTapped("settings_appearance_${theme}_${if (dark) "dark" else "light"}", listOf(200f to 168f), frames = 90) {
            Frame(dark, theme, "iw") {
                SettingsScreen(
                    themeMode = "system", colorTheme = theme, autoUpdate = AutoUpdateSettings(),
                    lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                    canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                    onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                    onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                    appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.12", checked = true),
                    checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                    language = "he", onLanguageChange = {}
                )
            }
        }
    }
    renderTapped("settings_language_open_iw", listOf(200f to 104f)) {
        Frame(false, "sage", "iw") {
            SettingsScreen(
                themeMode = "system", colorTheme = "sage", autoUpdate = AutoUpdateSettings(),
                lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.11", checked = true),
                checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                language = "he", onLanguageChange = {}
            )
        }
    }
    render("legacy_banner_iw") {
        Frame(false, "sage") {
            CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {}, legacyAppInstalled = true)
        }
    }
    for ((i, q) in listOf("גרמניה", "dolar", "פאונד", "ש\"ח", "hungría").withIndex()) {
        render("search_$i", heightDp = 520) {
            Frame(false, "sage", if (q.first().isLetter() && q.first() < 'z') "es" else "iw") {
                CurrencySelector(state.copy(showCurrencySelector = true, searchQuery = q, rates = rates + mapOf("HUF" to 357.4, "ECU" to 1.0, "MXN" to 17.1, "ARS" to 870.0, "SGD" to 1.34), pickerSlot = com.example.data.CurrencySlot.TARGET), onClose = {}, onSelect = {}, onSearch = {})
            }
        }
    }
    for (lang in listOf("iw", "en", "es")) {
        render("i18n_calculator_$lang") {
            Frame(false, "sage", lang) {
                CalculatorContent(state.copy(extraTargetCurrency = "EUR"), onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
            }
        }
        render("i18n_settings_$lang", heightDp = 1500) {
            Frame(false, "sage", lang) {
                SettingsScreen(
                    themeMode = "light", colorTheme = "sage", autoUpdate = AutoUpdateSettings(),
                    lastUpdateTimestamp = state.lastUpdateTimestamp, isRefreshing = false, lastRefreshFailed = false,
                    canPinWidget = true, onClose = {}, onThemeModeChange = {}, onColorThemeChange = {},
                    onAutoUpdateEnabledChange = {}, onIntervalChange = {}, onWifiOnlyChange = {},
                    onRefreshOnOpenChange = {}, onRefreshNow = {}, onAddWidget = {},
                    appUpdate = com.example.ui.AppUpdateState(installedVersion = "5.0.3", checked = true),
                    checkAppUpdates = true, onCheckAppUpdatesChange = {}, onCheckForAppUpdate = {}, onStartAppUpdate = {},
                    language = if (lang == "iw") "he" else lang, onLanguageChange = {}
                )
            }
        }
        render("i18n_picker_$lang") {
            Frame(false, "sage", lang) {
                CurrencySelector(state.copy(showCurrencySelector = true, pickerSlot = com.example.data.CurrencySlot.TARGET), onClose = {}, onSelect = {}, onSearch = {})
            }
        }
    }
    for ((lang, dark) in listOf("iw" to true, "iw" to false, "en" to true)) {
        render("names_${lang}_${if (dark) "dark" else "light"}") {
            Frame(dark, "sunset", lang) {
                CalculatorContent(
                    state.copy(sourceCurrency = "ILS", targetCurrency = "EUR", extraTargetCurrency = "HUF", sourceAmountRaw = "100"),
                    onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {}
                )
            }
        }
    }
    val he = java.util.Locale.forLanguageTag("he")
    // 5.0.20: currency names in the app's font, before and after, beside the app's own card.
    for (lang in listOf("he", "en")) for (dark in listOf(false, true)) for (before in listOf(true, false)) {
        val p = com.example.ui.theme.Palettes.resolve("sage", dark)
        val heb = lang == "he"
        render("v20_name_${lang}_${if (dark) "dark" else "light"}_${if (before) "before" else "after"}", widthDp = 320, heightDp = 400) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF3B4E66)).padding(10.dp)) {
                com.example.widget.LargeWidgetMock2(
                    p, 300f, 380f, heb, if (heb) he else java.util.Locale.ENGLISH,
                    amountRaw = "1250", src = "USD", tgt = "ILS", rate = 3.7123, extra = null, extraRate = 0.0,
                    updatedTime = if (heb) "היום 22:26" else "today 22:26", updatedLabel = if (heb) "עודכן היום 22:26" else "Updated today 22:26",
                    systemNameFont = before
                )
            }
        }
    }
    for (lang in listOf("he", "en")) {
        val p = com.example.ui.theme.Palettes.resolve("sage", false)
        val heb = lang == "he"
        render("v20_name_${lang}_extra_after", widthDp = 320, heightDp = 400) {
            Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF3B4E66)).padding(10.dp)) {
                com.example.widget.LargeWidgetMock2(
                    p, 300f, 380f, heb, if (heb) he else java.util.Locale.ENGLISH,
                    amountRaw = "1250", src = "AED", tgt = "ILS", rate = 1.01, extra = "HUF", extraRate = 96.27,
                    updatedTime = if (heb) "היום 22:26" else "today 22:26", updatedLabel = if (heb) "עודכן היום 22:26" else "Updated today 22:26"
                )
            }
        }
    }
    render("v20_app_card_he", widthDp = 400, heightDp = 520) {
        Frame(false, "sage") { Calc(state.copy(sourceAmountRaw = "1250")) }
    }
    val only = System.getenv("ONLY")
    data class W(val name: String, val w: Float, val h: Float, val appRtl: Boolean, val extra: String?, val launcherRtl: Boolean = false, val amount: String = "1250")
    for (c in listOf(
        W("widget3_4x4_he", 300f, 380f, true, null),
        W("widget3_4x4_he_extra", 300f, 380f, true, "HUF"),
        W("widget3_4x4_en_extra", 300f, 380f, false, "HUF"),
        W("widget3_4x4_he_rtl_launcher", 300f, 380f, true, "HUF", launcherRtl = true),
        W("widget3_4x4_he_big", 300f, 380f, true, null, amount = "1234567"),
        W("widget3_max_he", 340f, 400f, true, null),
        W("widget3_short_he", 300f, 260f, true, null),
        W("widget3_narrow_he", 200f, 380f, true, null),
        W("widget3_short_he_extra", 300f, 260f, true, "HUF"),
        W("widget3_tiny_he", 300f, 170f, true, null),
        W("widget3_4x3_he", 300f, 290f, true, null),
        W("widget4_phone_he", 400f, 700f, true, null, amount = "50"),
        W("widget4_phone_he_extra", 400f, 700f, true, "THB", amount = "50"),
        W("widget4_narrow_tall_he", 340f, 620f, true, null, amount = "1250"),
    )) {
        for (dark in listOf(false, true)) {
            val tag = "${c.name}_${if (dark) "dark" else "light"}"
            val p = com.example.ui.theme.Palettes.resolve("sunset", dark)
            render(tag, widthDp = c.w.toInt() + 20, heightDp = c.h.toInt() + 20) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF3B4E66)).padding(10.dp)) {
                    com.example.widget.LargeWidgetMock2(
                        p, c.w, c.h, c.appRtl, if (c.appRtl) he else java.util.Locale.ENGLISH,
                        amountRaw = c.amount, src = "ILS", tgt = if (c.name.startsWith("widget4")) "HUF" else "EUR",
                        rate = if (c.name.startsWith("widget4")) 106.4782 else 0.247582, extra = c.extra, extraRate = if (c.extra == "THB") 9.06 else 96.27,
                        updatedTime = "היום 22:26".takeIf { c.appRtl } ?: "today 22:26",
                        updatedLabel = if (c.appRtl) "עודכן היום 22:26" else "Updated today 22:26",
                        launcherRtl = c.launcherRtl
                    )
                }
            }
        }
    }
    for ((name, w, h) in listOf(Triple("small2_2x1", 220f, 100f), Triple("small2_2x2", 220f, 200f), Triple("small2_compact", 180f, 70f))) {
        for ((lang, extra) in listOf("he" to null, "he" to "HUF", "en" to "HUF")) {
            val tag = "${name}_${lang}${if (extra != null) "_extra" else ""}"
            val p = com.example.ui.theme.Palettes.resolve("sunset", false)
            render(tag, widthDp = w.toInt() + 20, heightDp = h.toInt() + 20) {
                Box(Modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color(0xFF3B4E66)).padding(10.dp)) {
                    com.example.widget.SmallWidgetMock2(p, w, h, lang == "he", "1250", "ILS", "EUR", 0.247582, extra, 96.27)
                }
            }
        }
    }
    System.exit(0)
}

private data class Quint(val a: String, val b: Float, val c: Float, val d: Boolean, val e: String?)

private data class Quad(val a: String, val b: Boolean, val c: Int, val d: Boolean)

private data class Quad2(val a: String, val b: QuickSpec, val c: String?, val d: Boolean)

private data class DemoCase(val name: String, val app: String, val device: String, val theme: String, val dark: Boolean)
private data class MenuCase(val name: String, val app: String, val device: String, val samsung: Boolean, val dark: Boolean)

private data class QuickSpec(
    val lang: String, val dark: Boolean, val text: String, val target: String, val source: String, val extra: String?,
    val local: String?, val canReplace: Boolean, val theme: String = "sage",
)

/** A stand-in for the app the text was selected in: a booking page with the selection marked. */
@Composable
private fun FakeBrowserPage(selected: String, dark: Boolean) {
    val ink = androidx.compose.ui.graphics.Color(if (dark) 0xFFE6E6EA else 0xFF202124)
    val muted = ink.copy(alpha = 0.55f)
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
        androidx.compose.foundation.layout.Column(Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 48.dp)) {
            androidx.compose.material3.Text("booking-site.com", color = muted, fontSize = androidx.compose.ui.unit.TextUnit(13f, androidx.compose.ui.unit.TextUnitType.Sp))
            androidx.compose.foundation.layout.Spacer(Modifier.padding(8.dp))
            androidx.compose.material3.Text("Hotel Lisboa Centro", color = ink, fontSize = androidx.compose.ui.unit.TextUnit(26f, androidx.compose.ui.unit.TextUnitType.Sp))
            androidx.compose.foundation.layout.Spacer(Modifier.padding(6.dp))
            androidx.compose.material3.Text("A bright room in the old town, five minutes from the river. Free cancellation until 2 days before arrival.", color = muted)
            androidx.compose.foundation.layout.Spacer(Modifier.padding(10.dp))
            androidx.compose.material3.Text(
                selected, color = ink,
                modifier = Modifier.background(androidx.compose.ui.graphics.Color(0x553D7BFD)).padding(horizontal = 2.dp)
            )
        }
    }
}
