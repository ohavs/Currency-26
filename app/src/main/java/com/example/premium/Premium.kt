package com.example.premium

/**
 * What unlocks Premium (the widget, the colour themes and the calculator): the one-time purchase, the free trial
 * of a new install, or rewarded videos ([PremiumRules.VIDEOS_PER_UNLOCK] of them buy [PremiumRules.REWARD_HOURS]
 * hours). Converting, searching, the extra currency, the history and converting selected text stay free.
 */
data class PremiumState(
    val purchased: Boolean = false,
    /** A purchase that waits for its payment (cash, bank transfer): not unlocked yet. */
    val purchasePending: Boolean = false,
    val trialUntil: Long = 0L,
    val rewardUntil: Long = 0L,
    /** Videos watched toward the next unlock, and when the last one ended. */
    val videosWatched: Int = 0,
    val lastVideoAt: Long = 0L,
) {
    fun access(now: Long): Access = Access(
        unlocked = purchased || now < trialUntil || now < rewardUntil,
        adFree = purchased,
        // Someone who just watched videos for their day of Premium gets no full-screen ads during it.
        interstitialsPaused = purchased || now < rewardUntil,
        purchased = purchased,
        trial = !purchased && now < trialUntil && trialUntil >= rewardUntil,
        until = if (purchased) null else maxOf(trialUntil, rewardUntil).takeIf { it > now },
    )

    /** Videos still to watch for the next unlock (an unfinished pair is kept for a day). */
    fun videosNeeded(now: Long): Int = PremiumRules.VIDEOS_PER_UNLOCK - progress(now)

    internal fun progress(now: Long): Int =
        if (now - lastVideoAt < PremiumRules.VIDEO_PROGRESS_KEPT_MS) videosWatched.coerceIn(0, PremiumRules.VIDEOS_PER_UNLOCK - 1) else 0
}

/** What the user can do right now, worked out from [PremiumState] at a moment in time. */
data class Access(
    val unlocked: Boolean,
    val adFree: Boolean,
    val interstitialsPaused: Boolean,
    val purchased: Boolean,
    /** Unlocked by the free trial of a new install (not by videos). */
    val trial: Boolean,
    /** When a trial or video unlock ends; null when there is none, or for a purchase. */
    val until: Long?,
) {
    companion object {
        /** The GitHub build: everything open, no ads. */
        val Everything = Access(unlocked = true, adFree = true, interstitialsPaused = true, purchased = true, trial = false, until = null)
    }
}

object PremiumRules {
    const val VIDEOS_PER_UNLOCK = 2
    const val REWARD_HOURS = 24
    const val TRIAL_DAYS = 3
    /** Watching more pairs adds a day each, up to this much in hand. */
    const val MAX_BANKED_HOURS = 72
    const val VIDEO_PROGRESS_KEPT_MS = 24 * HOUR_MS

    /** A new install starts with the free trial. */
    fun newInstall(now: Long) = PremiumState(trialUntil = now + TRIAL_DAYS * 24 * HOUR_MS)

    /** One rewarded video was watched to the end. Every [VIDEOS_PER_UNLOCK]th adds [REWARD_HOURS] hours. */
    fun afterVideo(state: PremiumState, now: Long): PremiumState {
        val watched = state.progress(now) + 1
        if (watched < VIDEOS_PER_UNLOCK) return state.copy(videosWatched = watched, lastVideoAt = now)
        val from = maxOf(state.rewardUntil, now)
        return state.copy(
            rewardUntil = minOf(from + REWARD_HOURS * HOUR_MS, now + MAX_BANKED_HOURS * HOUR_MS),
            videosWatched = 0,
            lastVideoAt = now,
        )
    }
}

/** The colour set (Classic) everyone has; the others come with Premium. */
const val FREE_COLOR_THEME = "sage"

internal const val HOUR_MS = 60L * 60 * 1000
internal const val MINUTE_MS = 60L * 1000

/** Why the Premium sheet opened: the feature the user reached for is shown first. */
enum class PremiumReason { WIDGET, COLORS, CALCULATOR, NO_ADS, SETTINGS }

/**
 * When a full-screen ad may show: only at a natural break (leaving the currency picker, settings or the
 * history), never in the first day after install, never while typing, only after the user got something done
 * in this session, at most once every [MIN_GAP_MS] and [MAX_PER_DAY] times a day.
 */
object InterstitialPolicy {
    const val MIN_GAP_MS = 15 * MINUTE_MS
    const val MAX_PER_DAY = 3
    const val GRACE_AFTER_INSTALL_MS = 24 * HOUR_MS
    const val CONVERSIONS_BEFORE = 2

    fun mayShow(
        access: Access,
        now: Long,
        installedAt: Long,
        lastShownAt: Long,
        shownToday: Int,
        conversionsThisSession: Int,
        shownThisSession: Boolean,
    ): Boolean = !access.interstitialsPaused &&
        now - installedAt >= GRACE_AFTER_INSTALL_MS &&
        now - lastShownAt >= MIN_GAP_MS &&
        shownToday < MAX_PER_DAY &&
        conversionsThisSession >= CONVERSIONS_BEFORE &&
        !shownThisSession
}

/** The once-only "rate the app" request: after a few days and sessions of real use, at a break with no ad. */
object ReviewPolicy {
    const val MIN_DAYS = 4
    const val MIN_SESSIONS = 5

    fun mayAsk(asked: Boolean, now: Long, installedAt: Long, sessions: Int, conversionsThisSession: Int, adShownThisSession: Boolean): Boolean =
        !asked && now - installedAt >= MIN_DAYS * 24 * HOUR_MS && sessions >= MIN_SESSIONS &&
            conversionsThisSession >= 1 && !adShownThisSession
}
