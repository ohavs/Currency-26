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
import com.example.ui.CalculatorScreen
import com.example.ui.MainViewModel
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.isDarkTheme


class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel((application as CurrencyApp).repository) as T
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
            viewModel.openCurrencySelector(intent.getBooleanExtra(EXTRA_PICK_SOURCE, true))
        }
    }

    companion object {
        const val ACTION_PICK_CURRENCY = "com.example.action.PICK_CURRENCY"
        const val EXTRA_PICK_SOURCE = "pick_source"

        /** Opens the app straight on the currency picker for the source (or target) side. */
        fun pickCurrencyIntent(context: Context, forSource: Boolean): Intent =
            Intent(context, MainActivity::class.java)
                .setAction(ACTION_PICK_CURRENCY)
                // Distinct data per side so the two widget PendingIntents never overwrite each other.
                .setData(Uri.parse("currency26://pick/" + if (forSource) "source" else "target"))
                .putExtra(EXTRA_PICK_SOURCE, forSource)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)

        fun openAppIntent(context: Context): Intent =
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP)
    }
}
