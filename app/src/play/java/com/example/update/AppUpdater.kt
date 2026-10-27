package com.example.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.example.data.AppRelease
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.requestAppUpdateInfo
import java.io.File

/**
 * Google Play installs the updates; this build only asks Play whether a newer version is out, so the main screen
 * can say so (a banner with an X, never forced), and "Update" opens the app's store page.
 */
class AppUpdater(private val context: Context) {
    private val manager = AppUpdateManagerFactory.create(context)

    /** The newer version on Google Play, or null. Throws when Play can't tell (say, the app wasn't installed from it). */
    suspend fun fetchLatestRelease(): AppRelease? {
        val info = manager.requestAppUpdateInfo()
        if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return null
        // Play gives the version code only; release.yml numbers them 500 + N for version 5.0.N.
        val code = info.availableVersionCode()
        return AppRelease(versionName = if (code > 500) "5.0.${code - 500}" else code.toString(), apkUrl = "", apkSizeBytes = 0)
    }

    /** "Update" goes to the app's page in the Play Store. */
    fun open() {
        val market = Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${context.packageName}"))
            .setPackage("com.android.vending")
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            context.startActivity(market)
        } catch (e: ActivityNotFoundException) {
            val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            runCatching { context.startActivity(web) }
        }
    }

    /** True: the update is not downloaded here (see [opensStore]). */
    val opensStore: Boolean = true

    @Suppress("UNUSED_PARAMETER")
    suspend fun download(release: AppRelease, onProgress: (Float) -> Unit): File =
        throw UnsupportedOperationException("Updates come from Google Play")

    fun canInstallPackages(): Boolean = false

    fun installPermissionIntent(): Intent = Intent()

    @Suppress("UNUSED_PARAMETER")
    fun installIntent(apk: File): Intent = Intent()
}
