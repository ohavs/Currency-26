package androidx.compose.ui.platform

import androidx.compose.runtime.staticCompositionLocalOf

/** Android's LocalContext; the desktop renders never compose the screens that read it. */
val LocalContext = staticCompositionLocalOf<android.content.Context> { error("No Android context on the desktop") }
