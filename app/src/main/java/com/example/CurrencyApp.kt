package com.example

import android.app.Application
import com.example.data.CurrencyRepository
import com.example.premium.Monetization
import com.example.premium.createMonetization
import com.example.update.AppUpdater
import com.example.utils.AppLanguage
import com.example.workers.RateUpdateScheduler

class CurrencyApp : Application() {
    lateinit var repository: CurrencyRepository
        private set

    val appUpdater: AppUpdater by lazy { AppUpdater(this) }

    /** Premium and ads in the Play build; everything unlocked in the GitHub build. */
    val monetization: Monetization by lazy { createMonetization(this) }

    override fun onCreate() {
        super.onCreate()
        // Before the repository and the first activity read the language.
        AppLanguage.initialize(this)
        repository = CurrencyRepository(this)

        RateUpdateScheduler.apply(this, repository.autoUpdate.value, replaceExisting = false)
    }
}
