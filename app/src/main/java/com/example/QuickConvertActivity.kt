package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import com.example.premium.PremiumReason
import com.example.ui.LocalAppLocale
import com.example.ui.QuickConvertSheet
import com.example.ui.theme.MyApplicationTheme
import com.example.utils.AppLanguage
import com.example.utils.DeviceCountry
import com.example.utils.QuickConversion
import com.example.utils.QuickConvert
import com.example.utils.withSymbol
import com.example.widget.triggerWidgetUpdate
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * "Convert currency" in other apps' text-selection menu (ACTION_PROCESS_TEXT) and in the share sheet
 * (ACTION_SEND, for phones that hide selection-menu apps, like recent Samsungs). It shows a bubble over the
 * app the text came from, with the price converted, and tapping outside returns to that app. In editable text
 * picked from the selection menu it can also put the converted price in place of the original. Tapping the
 * currency it converts to picks another one, which the bubble keeps until the app's own target changes; tapping the
 * price's currency says what it really is in (a "$" that meant Canadian dollars), for this text only. Without Premium
 * it converts [com.example.premium.QuickConvertAllowance.FREE_PER_WEEK] times a week, then offers Premium instead.
 */
class QuickConvertActivity : ComponentActivity() {
    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(AppLanguage.wrap(newBase, AppLanguage.stored(newBase)))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        val shared = intent.action == Intent.ACTION_SEND
        val text = if (shared) {
            // Browsers share a selection with a link to it, whose digits are not prices.
            intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString().orEmpty().replace(LINK, " ")
        } else {
            intent.getCharSequenceExtra(Intent.EXTRA_PROCESS_TEXT)?.toString().orEmpty()
        }
        // Callers mark editable text explicitly; anything else (and anything shared) is treated as read-only.
        val canReplace = !shared && !intent.getBooleanExtra(Intent.EXTRA_PROCESS_TEXT_READONLY, true)
        val repository = (application as CurrencyApp).repository
        val local = DeviceCountry.currency(this)
        repository.markQuickConvertUsed()
        val monetization = (application as CurrencyApp).monetization
        counted = savedInstanceState?.getBoolean(KEY_COUNTED) ?: false
        // Out of free conversions this week: the bubble says so instead of converting (one counted before a
        // rotation is still shown).
        val freeLeftAtStart = monetization.quickConvertsLeft()
        val locked = freeLeftAtStart == 0 && !counted
        val nextFree = if (locked) monetization.nextFreeQuickConvert() else null
        var freeLeft by mutableStateOf(if (counted) freeLeftAtStart else null)

        var offline by mutableStateOf(false)
        var chosen by mutableStateOf(repository.getQuickTarget())
        from = savedInstanceState?.getString(KEY_FROM)
        lifecycleScope.launch {
            // Stored rates answer at once; old ones are refreshed underneath and the bubble follows.
            val age = System.currentTimeMillis() - repository.getLastUpdateTimestamp()
            if (age > STALE_AFTER_MS && !repository.refreshRates() && repository.getLastUpdateTimestamp() == 0L) offline = true
        }

        setContent {
            val themeMode by repository.themeMode.collectAsState()
            val colorTheme by repository.colorTheme.collectAsState()
            val rates by repository.allRatesFlow.collectAsState(initial = null)
            val recentlyUsed by repository.recentCurrenciesFlow.collectAsState(initial = emptyList())
            var copied by remember { mutableStateOf(false) }
            val conversion = remember(rates, chosen, from) {
                rates?.takeIf { it.isNotEmpty() }?.let {
                    QuickConvert.convert(
                        text, it,
                        source = repository.getSourceCurrency(),
                        target = repository.getTargetCurrency(),
                        extra = repository.getExtraTargetCurrency(),
                        local = local,
                        chosen = chosen,
                        from = from
                    )
                }
            }
            // A free conversion counts once the bubble really shows a price converted.
            val converted = conversion?.lines?.isNotEmpty() == true
            LaunchedEffect(converted) {
                if (converted && !locked && !counted && freeLeftAtStart != null) {
                    counted = true
                    monetization.noteQuickConvert()
                    freeLeft = monetization.quickConvertsLeft()
                }
            }
            CompositionLocalProvider(LocalAppLocale provides resources.configuration.locales[0]) {
                MyApplicationTheme(themeMode = themeMode, colorThemeStr = colorTheme) {
                    QuickConvertSheet(
                        conversion = conversion,
                        colorTheme = colorTheme,
                        locked = locked,
                        nextFree = nextFree,
                        freeLeft = freeLeft,
                        onUnlock = {
                            startActivity(MainActivity.openPremiumIntent(this, PremiumReason.QUICK_CONVERT))
                            finish()
                        },
                        offline = offline,
                        canReplace = canReplace,
                        copied = copied,
                        onCopy = {
                            conversion?.let { copy(it) }
                            copied = true
                            lifecycleScope.launch { delay(550); finish() }
                        },
                        onReplace = {
                            conversion?.let { setResult(RESULT_OK, Intent().putExtra(Intent.EXTRA_PROCESS_TEXT, it.replaceIn(text))) }
                            finish()
                        },
                        onOpenApp = { openInApp(conversion) },
                        onDismiss = ::finish,
                        rates = rates.orEmpty(),
                        // The user's own currencies first, then the ones picked lately in the app.
                        recents = listOfNotNull(
                            conversion?.target,
                            repository.getTargetCurrency(),
                            repository.getSourceCurrency(),
                            repository.getExtraTargetCurrency()
                        ) + recentlyUsed,
                        onChooseCurrency = { code ->
                            repository.setQuickTarget(code)
                            chosen = repository.getQuickTarget()
                            copied = false
                            lifecycleScope.launch { repository.markCurrencyUsed(code) }
                        },
                        onChooseSource = { code ->
                            from = code
                            copied = false
                            lifecycleScope.launch { repository.markCurrencyUsed(code) }
                        }
                    )
                }
            }
        }
    }

    /** The currency the user said the prices are in, picked in the bubble (null: as the text reads). */
    private var from by mutableStateOf<String?>(null)

    /** This bubble's conversion was counted toward the free ones of the week. */
    private var counted = false

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putString(KEY_FROM, from)
        outState.putBoolean(KEY_COUNTED, counted)
    }

    /** Leaving for another app (home, recents) closes the bubble instead of leaving it behind. */
    override fun onStop() {
        super.onStop()
        if (!isChangingConfigurations) finish()
    }

    private fun copy(conversion: QuickConversion) {
        val single = conversion.lines.singleOrNull()
        val clip = if (single != null) {
            withSymbol(single.to, single.value)
        } else {
            (conversion.lines.map { "${withSymbol(it.price.currency, it.price.amount, original = true)} = ${withSymbol(it.to, it.value)}" } +
                listOfNotNull(conversion.total?.let { (code, value) -> "${getString(R.string.quick_total)}: ${withSymbol(code, value)}" }))
                .joinToString("\n")
        }
        getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(getString(R.string.app_name), clip))
    }

    /** Opens the app on the first price as the bubble showed it: from its currency, to the one it went to. */
    private fun openInApp(conversion: QuickConversion?) {
        val repository = (application as CurrencyApp).repository
        conversion?.lines?.firstOrNull()?.let { line ->
            val app = QuickConvert.appCurrencies(
                price = line.price.currency,
                to = line.to,
                source = repository.getSourceCurrency(),
                target = repository.getTargetCurrency(),
                extra = repository.getExtraTargetCurrency()
            )
            repository.setSourceCurrency(app.source)
            repository.setTargetCurrency(app.target)
            repository.setExtraTargetCurrency(app.extra)
            repository.setAmount(line.price.amount.toBigDecimal().stripTrailingZeros().toPlainString())
            triggerWidgetUpdate(this)
        }
        startActivity(MainActivity.openAppIntent(this))
        finish()
    }

    private companion object {
        const val STALE_AFTER_MS = 6 * 60 * 60 * 1000L
        const val KEY_FROM = "from"
        const val KEY_COUNTED = "counted"
        val LINK = Regex("""https?://\S+""")
    }
}
