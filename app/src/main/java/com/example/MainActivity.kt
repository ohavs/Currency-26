package com.example

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.CurrencySlot
import com.example.ui.CalculatorScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.isDarkTheme


class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val app = application as CurrencyApp
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(app.repository, app.appUpdater) as T
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (savedInstanceState == null) {
            handleWidgetIntent(intent)
        }
        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val colorThemeStr by viewModel.colorTheme.collectAsState()
            val darkTheme = isDarkTheme(themeMode, isSystemInDarkTheme())

            // Status/navigation bar icons follow the in-app theme, not only the system one.
            DisposableEffect(darkTheme) {
                val barStyle = if (darkTheme) {
                    SystemBarStyle.dark(Color.TRANSPARENT)
                } else {
                    SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                }
                enableEdgeToEdge(statusBarStyle = barStyle, navigationBarStyle = barStyle)
                onDispose {}
            }

            MyApplicationTheme(
                themeMode = themeMode,
                colorThemeStr = colorThemeStr
            ) {
                CalculatorScreen(viewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleWidgetIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        viewModel.reloadSelection()
    }

    private fun handleWidgetIntent(intent: Intent?) {
        if (intent?.action == ACTION_PICK_CURRENCY) {
            val slot = intent.getStringExtra(EXTRA_PICK_SLOT)
                ?.let { name -> CurrencySlot.values().firstOrNull { it.name == name } }
                ?: if (intent.getBooleanExtra(EXTRA_PICK_SOURCE, true)) CurrencySlot.SOURCE else CurrencySlot.TARGET
            viewModel.openCurrencySelector(slot)
        }
    }

    companion object {
        const val ACTION_PICK_CURRENCY = "com.example.action.PICK_CURRENCY"
        const val EXTRA_PICK_SLOT = "pick_slot"
        /** Written by older widget versions. */
        private const val EXTRA_PICK_SOURCE = "pick_source"

        /** Opens the app straight on the currency picker for one of the displayed currencies. */
        fun pickCurrencyIntent(context: Context, slot: CurrencySlot): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_PICK_CURRENCY)
                // Distinct data per slot so the widget PendingIntents never overwrite each other.
                .setData(Uri.parse("currency26://pick/" + slot.name.lowercase()))
                .putExtra(EXTRA_PICK_SLOT, slot.name)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun openAppIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
