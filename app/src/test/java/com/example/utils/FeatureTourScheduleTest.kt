package com.example.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureTourScheduleTest {

  private val day = 24L * 60 * 60 * 1000
  private val now = 1_800_000_000_000L

  @Test
  fun `first launch shows the tour`() {
    assertTrue(FeatureTourSchedule.isDue(timesShown = 0, lastShownAt = 0L, now = now))
  }

  @Test
  fun `it comes back only after three weeks`() {
    assertFalse(FeatureTourSchedule.isDue(timesShown = 1, lastShownAt = now - 20 * day, now = now))
    assertTrue(FeatureTourSchedule.isDue(timesShown = 1, lastShownAt = now - 21 * day, now = now))
  }

  @Test
  fun `never more than three times`() {
    assertFalse(FeatureTourSchedule.isDue(timesShown = 3, lastShownAt = now - 400 * day, now = now))
  }
}
