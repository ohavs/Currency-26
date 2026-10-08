package com.example

import android.app.Application
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.CurrencyRepository
import com.example.workers.UpdateWorker
import java.util.concurrent.TimeUnit

class CurrencyApp : Application() {
    lateinit var repository: CurrencyRepository
        private set

    override fun onCreate() {
        super.onCreate()
        repository = CurrencyRepository(this)
        
        schedulePeriodicUpdates()
    }

    private fun schedulePeriodicUpdates() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
            
        val updateWorkRequest = PeriodicWorkRequestBuilder<UpdateWorker>(1, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
            
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "CurrencyUpdateWork",
            ExistingPeriodicWorkPolicy.KEEP,
            updateWorkRequest
        )
    }
}
