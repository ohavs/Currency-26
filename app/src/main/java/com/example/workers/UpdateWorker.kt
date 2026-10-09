package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.CurrencyApp
import com.example.widget.updateWidgets

class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        val app = applicationContext as CurrencyApp
        if (!app.repository.refreshRates()) {
            return if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
        // Push the fresh rate to the home-screen widgets as well.
        updateWidgets(applicationContext)
        return Result.success()
    }
}
