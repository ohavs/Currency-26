package com.example.update

import android.content.Context
import android.content.Intent

/** The old AI Studio build only matters to people who sideloaded it; Play users never had it. */
object LegacyApp {
    @Suppress("UNUSED_PARAMETER")
    fun isInstalled(context: Context): Boolean = false

    fun uninstallIntent(): Intent = Intent()
}
