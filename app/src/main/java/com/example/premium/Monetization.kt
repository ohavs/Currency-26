package com.example.premium

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow

/** Where the rewarded video for the next unlock stands. */
enum class VideoStatus { IDLE, LOADING, SHOWING, UNAVAILABLE }

/**
 * Everything the store build adds: Premium (purchase, free trial, rewarded videos), ads and the consent they
 * need. The Google Play build implements it (src/play); the GitHub build has everything unlocked and no ads
 * (src/github). Each build provides `createMonetization(app)`.
 */
interface Monetization {
    /** False in the GitHub build: no Premium screen, no ads. */
    val hasStore: Boolean
    val state: StateFlow<PremiumState>
    /** [state] as of now; changes when a trial or video unlock runs out, too. */
    val access: StateFlow<Access>
    /** The price of "Premium forever" in the user's currency, once Google Play has told us. */
    val price: StateFlow<String?>
    val video: StateFlow<VideoStatus>
    /** In regions with privacy laws (EEA, UK...) the user must be able to change their ad consent. */
    val privacyOptionsRequired: StateFlow<Boolean>

    /** The main screen was created: consent, then ads; the purchase is checked with Google Play. */
    fun start(activity: Activity)
    fun onResume()

    /**
     * Ad strip at the bottom of the main screen. It keeps its height from the start, so the keys above never
     * move; [placeholder] (the app's own Premium line) fills it until an ad arrives, or when none does.
     */
    @Composable
    fun Banner(placeholder: @Composable () -> Unit, modifier: Modifier)

    /** Watch the next rewarded video (loading it first if needed). */
    fun watchVideo(activity: Activity)
    fun preloadVideo(activity: Activity)
    fun buy(activity: Activity)
    fun restorePurchases()
    fun showPrivacyOptions(activity: Activity)

    /** The user typed a new amount or picked a currency. */
    fun noteConversion()

    /** A natural break (back from the picker, settings or history): maybe a full-screen ad, or the review ask. */
    fun onBreak(activity: Activity)
}
