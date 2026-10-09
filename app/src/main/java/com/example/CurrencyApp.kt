package com.example

import android.app.Application
import com.example.data.CurrencyRepository
import com.example.update.AppUpdater
import com.example.workers.RateUpdateScheduler

class CurrencyApp : Application() {
    lateinit var repository: CurrencyRepository
        private set

    val appUpdater: AppUpdater by lazy { AppUpdater(this) }

    override fun onCreate() {
        super.onCreate()
        repository = CurrencyRepository(this)

        RateUpdateScheduler.apply(this, repository.autoUpdate.value, replaceExisting = false)
    }
}
