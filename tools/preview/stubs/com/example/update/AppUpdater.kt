package com.example.update

import com.example.data.AppRelease
import java.io.File

/** Signature-compatible stand-in for the OkHttp/FileProvider based updater. */
class AppUpdater(private val context: android.content.Context) {
    suspend fun fetchLatestRelease(): AppRelease? = null
    suspend fun download(release: AppRelease, onProgress: (Float) -> Unit): File = File("x")
    fun canInstallPackages(): Boolean = true
    fun installPermissionIntent(): android.content.Intent = throw UnsupportedOperationException()
    fun installIntent(apk: File): android.content.Intent = throw UnsupportedOperationException()
}
