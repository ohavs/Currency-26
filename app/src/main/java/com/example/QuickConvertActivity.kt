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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
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
 * picked from the selection menu it can also put the converted price in place of the original.
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

        var offline by mutableStateOf(false)
        lifecycleScope.launch {
            // Stored rates answer at once; old ones are refreshed underneath and the bubble follows.
            val age = System.currentTimeMillis() - repository.getLastUpdateTimestamp()
            if (age > STALE_AFTER_MS && !repository.refreshRates() && repository.getLastUpdateTimestamp() == 0L) offline = true
        }

        setContent {
            val themeMode by repository.themeMode.collectAsState()
            val colorTheme by repository.colorTheme.collectAsState()
            val rates by repository.allRatesFlow.collectAsState(initial = null)
            var copied by remember { mutableStateOf(false) }
            val conversion = remember(rates) {
                rates?.takeIf { it.isNotEmpty() }?.let {
                    QuickConvert.convert(
                        text, it,
                        source = repository.getSourceCurrency(),
                        target = repository.getTargetCurrency(),
                        extra = repository.getExtraTargetCurrency(),
                        local = local
                    )
                }
            }
            CompositionLocalProvider(LocalAppLocale provides resources.configuration.locales[0]) {
                MyApplicationTheme(themeMode = themeMode, colorThemeStr = colorTheme) {
                    QuickConvertSheet(
                        conversion = conversion,
                        colorTheme = colorTheme,
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
                        onDismiss = ::finish
                    )
                }
            }
        }
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

    /** Opens the app with the first price typed in: its currency becomes the source (the target stays different). */
    private fun openInApp(conversion: QuickConversion?) {
        val repository = (application as CurrencyApp).repository
        conversion?.lines?.firstOrNull()?.let { line ->
            val source = repository.getSourceCurrency()
            // The old source takes the place the price's currency had, so no currency shows twice.
            if (line.price.currency == repository.getTargetCurrency()) repository.setTargetCurrency(source)
            if (line.price.currency == repository.getExtraTargetCurrency()) repository.setExtraTargetCurrency(source)
            repository.setSourceCurrency(line.price.currency)
            repository.setAmount(line.price.amount.toBigDecimal().stripTrailingZeros().toPlainString())
            triggerWidgetUpdate(this)
        }
        startActivity(MainActivity.openAppIntent(this))
        finish()
    }

    private companion object {
        const val STALE_AFTER_MS = 6 * 60 * 60 * 1000L
        val LINK = Regex("""https?://\S+""")
    }
}
