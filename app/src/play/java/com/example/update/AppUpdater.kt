package com.example.update

import android.content.Context
import android.content.Intent
import com.example.data.AppRelease
import java.io.File

/** Google Play updates the app itself; this build never looks for or installs APKs. */
@Suppress("UNUSED_PARAMETER")
class AppUpdater(context: Context) {
    suspend fun fetchLatestRelease(): AppRelease? = null

    suspend fun download(release: AppRelease, onProgress: (Float) -> Unit): File =
        throw UnsupportedOperationException("Updates come from Google Play")

    fun canInstallPackages(): Boolean = false

    fun installPermissionIntent(): Intent = Intent()

    fun installIntent(apk: File): Intent = Intent()
}
