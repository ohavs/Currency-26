package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.CurrencyApp
import com.example.widget.updateWidgets
import java.util.concurrent.TimeUnit

/** Redraws the widgets when a free trial or a video unlock runs out, even with the app closed. */
class PremiumExpiryWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        (applicationContext as CurrencyApp).monetization.onResume()
        updateWidgets(applicationContext)
        return Result.success()
    }

    companion object {
        private const val WORK_NAME = "PremiumExpiry"

        /** One refresh just after [until]; null cancels it (purchased, or nothing running out). */
        fun schedule(context: Context, until: Long?) {
            val workManager = WorkManager.getInstance(context)
            if (until == null) {
                workManager.cancelUniqueWork(WORK_NAME)
                return
            }
            val delay = (until - System.currentTimeMillis()).coerceAtLeast(0L) + 60_000L
            val request = OneTimeWorkRequestBuilder<PremiumExpiryWorker>()
                .setInitialDelay(delay, TimeUnit.MILLISECONDS)
                .build()
            workManager.enqueueUniqueWork(WORK_NAME, ExistingWorkPolicy.REPLACE, request)
        }
    }
}
