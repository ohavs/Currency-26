package com.example.premium

import android.app.Activity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow

/** Stand-in for the Play build's Premium and ads: a fixed state, the ad strip drawn as its placeholder. */
class FakeMonetization(state: PremiumState = PremiumState(), private val now: Long = System.currentTimeMillis()) : Monetization {
    override val hasStore = true
    override val state = MutableStateFlow(state)
    override val access = MutableStateFlow(state.access(now))
    override val price = MutableStateFlow<String?>("₪14.90")
    override val video = MutableStateFlow(VideoStatus.IDLE)
    override val privacyOptionsRequired = MutableStateFlow(true)

    fun set(next: PremiumState) {
        state.value = next
        access.value = next.access(now)
    }

    override fun start(activity: Activity) = Unit
    override fun onResume() = Unit

    @Composable
    override fun Banner(placeholder: @Composable () -> Unit, modifier: Modifier) {
        if (access.value.adFree) return
        // An anchored adaptive banner on a phone is about 50-60dp tall.
        Box(modifier.fillMaxWidth().height(56.dp)) { placeholder() }
    }

    override fun watchVideo(activity: Activity) = Unit
    override fun preloadVideo(activity: Activity) = Unit
    override fun buy(activity: Activity) = Unit
    override fun restorePurchases() = Unit
    override fun showPrivacyOptions(activity: Activity) = Unit
    var conversions = 0
    override fun noteConversion() { conversions++ }
    override fun onBreak(activity: Activity) = Unit
}
