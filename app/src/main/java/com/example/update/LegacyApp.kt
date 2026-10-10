package com.example.update

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri

/**
 * The build made in AI Studio, before the app got its own id. It installs side by side with this app,
 * and its widget (old white/purple design) shows up in the launcher's widget list too.
 */
object LegacyApp {
    const val PACKAGE = "com.aistudio.currencyconverter.lkhjqw"

    fun isInstalled(context: Context): Boolean = try {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(PACKAGE, 0)
        true
    } catch (e: PackageManager.NameNotFoundException) {
        false
    }

    /** Opens the system "uninstall this app?" dialog for the old build. */
    fun uninstallIntent(): Intent =
        Intent(Intent.ACTION_DELETE, Uri.parse("package:$PACKAGE")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
