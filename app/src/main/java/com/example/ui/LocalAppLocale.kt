package com.example.ui

import androidx.compose.runtime.staticCompositionLocalOf
import java.util.Locale

/** The app's UI language (may differ from the device language), for currency and country names. */
val LocalAppLocale = staticCompositionLocalOf { Locale.getDefault() }
