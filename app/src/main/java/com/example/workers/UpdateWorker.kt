package com.example.workers

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.CurrencyApp

class UpdateWorker(appContext: Context, workerParams: WorkerParameters) :
    CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            val app = applicationContext as CurrencyApp
            app.repository.refreshRates()
            Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }
}
