package com.example.data

/** A published app version on GitHub Releases. */
data class AppRelease(
    val versionName: String,
    val apkUrl: String,
    val apkSizeBytes: Long,
)
