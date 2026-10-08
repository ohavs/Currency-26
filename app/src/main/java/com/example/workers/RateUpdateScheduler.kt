package com.example.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.AutoUpdateSettings
import java.util.concurrent.TimeUnit

object RateUpdateScheduler {
    private const val WORK_NAME = "CurrencyUpdateWork"

    /**
     * Applies the auto-update preferences to WorkManager.
     * [replaceExisting] = true when the user changed a setting, so the new interval/constraints take effect;
     * false on app start, so an already-scheduled job keeps its timing.
     */
    fun apply(context: Context, settings: AutoUpdateSettings, replaceExisting: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!settings.enabled) {
            workManager.cancelUniqueWork(WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(if (settings.wifiOnly) NetworkType.UNMETERED else NetworkType.CONNECTED)
            .build()

        val request = PeriodicWorkRequestBuilder<UpdateWorker>(settings.intervalMinutes, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            if (replaceExisting) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }
}
