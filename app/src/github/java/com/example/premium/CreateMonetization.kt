package com.example.premium

import android.app.Activity
import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** The GitHub build (the owner's own): everything unlocked, no ads, nothing to buy. */
@Suppress("UNUSED_PARAMETER")
fun createMonetization(app: Application): Monetization = Unlocked

private object Unlocked : Monetization {
    override val hasStore = false
    override val state: StateFlow<PremiumState> = MutableStateFlow(PremiumState(purchased = true))
    override val access: StateFlow<Access> = MutableStateFlow(Access.Everything)
    override val price: StateFlow<String?> = MutableStateFlow(null)
    override val video: StateFlow<VideoStatus> = MutableStateFlow(VideoStatus.IDLE)
    override val privacyOptionsRequired: StateFlow<Boolean> = MutableStateFlow(false)

    override fun start(activity: Activity) = Unit
    override fun onResume() = Unit

    @Composable
    override fun Banner(placeholder: @Composable () -> Unit, modifier: Modifier) = Unit

    override fun watchVideo(activity: Activity) = Unit
    override fun preloadVideo(activity: Activity) = Unit
    override fun buy(activity: Activity) = Unit
    override fun restorePurchases() = Unit
    override fun showPrivacyOptions(activity: Activity) = Unit
    override fun noteConversion() = Unit
    override fun onBreak(activity: Activity) = Unit
}
