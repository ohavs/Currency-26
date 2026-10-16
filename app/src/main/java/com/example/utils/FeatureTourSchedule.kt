package com.example.utils

/**
 * When the feature tour comes up by itself: on first launch, then (while a feature is still unused) at most
 * twice more, three weeks apart. Opening it from Settings is always possible.
 */
object FeatureTourSchedule {
    const val MAX_SHOWINGS = 3
    const val REPEAT_AFTER_MS = 21L * 24 * 60 * 60 * 1000

    fun isDue(timesShown: Int, lastShownAt: Long, now: Long): Boolean = when {
        timesShown <= 0 -> true
        timesShown >= MAX_SHOWINGS -> false
        else -> now - lastShownAt >= REPEAT_AFTER_MS
    }
}
