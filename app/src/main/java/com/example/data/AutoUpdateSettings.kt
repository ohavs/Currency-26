package com.example.data

/** User preferences for keeping exchange rates (and the widgets) fresh automatically. */
data class AutoUpdateSettings(
    /** Periodic background refresh via WorkManager. */
    val enabled: Boolean = true,
    val intervalMinutes: Long = 60L,
    /** Only refresh in the background on unmetered (Wi-Fi) networks. */
    val wifiOnly: Boolean = false,
    /** Fetch fresh rates every time the app is opened. */
    val refreshOnOpen: Boolean = true,
) {
    companion object {
        /** WorkManager does not run periodic work more often than every 15 minutes. */
        val INTERVAL_OPTIONS = listOf(15L, 60L, 360L, 720L, 1440L)
    }
}
