package com.example.premium

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

/**
 * Premium and ad bookkeeping, kept in its own preferences file ("premium"). A new install starts its free
 * trial here. [access] is recomputed when a trial or video unlock runs out, so the app locks again by itself.
 */
class PremiumStore(
    context: Context,
    private val scope: CoroutineScope,
    private val clock: () -> Long = System::currentTimeMillis,
    /** Called with the next moment access changes by itself (or null), e.g. to refresh the widgets then. */
    private val onNextChange: (Long?) -> Unit = {},
) {
    private val prefs = context.getSharedPreferences("premium", Context.MODE_PRIVATE)

    init {
        if (!prefs.contains(KEY_INSTALLED_AT)) {
            val now = clock()
            write(PremiumRules.newInstall(now))
            prefs.edit().putLong(KEY_INSTALLED_AT, now).apply()
        }
    }

    private val _state = MutableStateFlow(read())
    val state: StateFlow<PremiumState> = _state.asStateFlow()

    private val _access = MutableStateFlow(_state.value.access(clock()))
    val access: StateFlow<Access> = _access.asStateFlow()

    private var timer: Job? = null

    init {
        refresh()
    }

    val installedAt: Long get() = prefs.getLong(KEY_INSTALLED_AT, 0L)

    fun update(transform: (PremiumState) -> PremiumState) {
        val next = transform(_state.value)
        if (next == _state.value) return
        write(next)
        _state.value = next
        refresh()
    }

    /** Works [access] out again (on resume, and when an unlock ends). */
    fun refresh() {
        val now = clock()
        val access = _state.value.access(now)
        _access.value = access
        timer?.cancel()
        val until = access.until
        if (until != null) {
            timer = scope.launch {
                delay(until - now + 1_000)
                refresh()
            }
        }
        onNextChange(until)
    }

    // Full-screen ads: when the last one showed and how many today.
    val lastInterstitialAt: Long get() = prefs.getLong(KEY_LAST_INTERSTITIAL, 0L)

    fun interstitialsOn(now: Long): Int =
        if (prefs.getInt(KEY_INTERSTITIAL_DAY, -1) == dayOf(now)) prefs.getInt(KEY_INTERSTITIAL_COUNT, 0) else 0

    fun markInterstitialShown(now: Long) {
        prefs.edit()
            .putLong(KEY_LAST_INTERSTITIAL, now)
            .putInt(KEY_INTERSTITIAL_COUNT, interstitialsOn(now) + 1)
            .putInt(KEY_INTERSTITIAL_DAY, dayOf(now))
            .apply()
    }

    /** Test builds: as if installed [ago] ms ago, with no full-screen ad shown yet. */
    fun setInstalledAgoForTesting(ago: Long) {
        prefs.edit()
            .putLong(KEY_INSTALLED_AT, clock() - ago)
            .remove(KEY_LAST_INTERSTITIAL)
            .remove(KEY_INTERSTITIAL_COUNT)
            .remove(KEY_INTERSTITIAL_DAY)
            .apply()
    }

    val sessions: Int get() = prefs.getInt(KEY_SESSIONS, 0)
    fun countSession() { prefs.edit().putInt(KEY_SESSIONS, sessions + 1).apply() }

    val reviewAsked: Boolean get() = prefs.getBoolean(KEY_REVIEW_ASKED, false)
    fun markReviewAsked() { prefs.edit().putBoolean(KEY_REVIEW_ASKED, true).apply() }

    private fun read() = PremiumState(
        purchased = prefs.getBoolean(KEY_PURCHASED, false),
        purchasePending = prefs.getBoolean(KEY_PENDING, false),
        trialUntil = prefs.getLong(KEY_TRIAL_UNTIL, 0L),
        rewardUntil = prefs.getLong(KEY_REWARD_UNTIL, 0L),
        videosWatched = prefs.getInt(KEY_VIDEOS, 0),
        lastVideoAt = prefs.getLong(KEY_LAST_VIDEO, 0L),
    )

    private fun write(state: PremiumState) {
        prefs.edit()
            .putBoolean(KEY_PURCHASED, state.purchased)
            .putBoolean(KEY_PENDING, state.purchasePending)
            .putLong(KEY_TRIAL_UNTIL, state.trialUntil)
            .putLong(KEY_REWARD_UNTIL, state.rewardUntil)
            .putInt(KEY_VIDEOS, state.videosWatched)
            .putLong(KEY_LAST_VIDEO, state.lastVideoAt)
            .apply()
    }

    private fun dayOf(time: Long): Int {
        val calendar = Calendar.getInstance().apply { timeInMillis = time }
        return calendar.get(Calendar.YEAR) * 1000 + calendar.get(Calendar.DAY_OF_YEAR)
    }

    private companion object {
        const val KEY_INSTALLED_AT = "installed_at"
        const val KEY_PURCHASED = "purchased"
        const val KEY_PENDING = "purchase_pending"
        const val KEY_TRIAL_UNTIL = "trial_until"
        const val KEY_REWARD_UNTIL = "reward_until"
        const val KEY_VIDEOS = "videos_watched"
        const val KEY_LAST_VIDEO = "last_video_at"
        const val KEY_LAST_INTERSTITIAL = "last_interstitial_at"
        const val KEY_INTERSTITIAL_DAY = "interstitial_day"
        const val KEY_INTERSTITIAL_COUNT = "interstitial_count"
        const val KEY_SESSIONS = "sessions"
        const val KEY_REVIEW_ASKED = "review_asked"
    }
}
