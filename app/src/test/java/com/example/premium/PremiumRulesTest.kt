package com.example.premium

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumRulesTest {

  private val hour = 60L * 60 * 1000
  private val t0 = 1_800_000_000_000L

  @Test
  fun `a new install gets a three-day trial with ads`() {
    val state = PremiumRules.newInstall(t0)
    val access = state.access(t0 + hour)
    assertTrue(access.unlocked && access.trial)
    assertFalse(access.adFree)
    assertFalse("the trial is not a reward: full-screen ads may still show", access.interstitialsPaused)
    assertEquals(t0 + 72 * hour, access.until)
    assertFalse(state.access(t0 + 72 * hour).unlocked)
  }

  @Test
  fun `two videos unlock a day, one is not enough`() {
    var state = PremiumState()
    state = PremiumRules.afterVideo(state, t0)
    assertFalse(state.access(t0).unlocked)
    assertEquals(1, state.videosNeeded(t0))
    state = PremiumRules.afterVideo(state, t0 + 60_000)
    val access = state.access(t0 + 60_000)
    assertTrue(access.unlocked && access.interstitialsPaused && !access.adFree && !access.trial)
    assertEquals(t0 + 60_000 + 24 * hour, access.until)
    assertEquals(2, state.videosNeeded(t0 + 60_000))
    assertFalse(state.access(t0 + 60_000 + 24 * hour).unlocked)
  }

  @Test
  fun `a half-watched pair is forgotten after a day`() {
    val state = PremiumRules.afterVideo(PremiumState(), t0)
    assertEquals(1, state.videosNeeded(t0 + 23 * hour))
    assertEquals(2, state.videosNeeded(t0 + 25 * hour))
    val later = PremiumRules.afterVideo(state, t0 + 25 * hour)
    assertFalse(later.access(t0 + 25 * hour).unlocked)
  }

  @Test
  fun `more pairs add a day each, up to three days in hand`() {
    var state = PremiumState()
    repeat(10) { i -> state = PremiumRules.afterVideo(state, t0 + i * 1000L) }
    val now = t0 + 9_000
    assertEquals(now + 72 * hour, state.access(now).until)
  }

  @Test
  fun `the purchase opens everything for good and removes all ads`() {
    val access = PremiumState(purchased = true).access(t0)
    assertTrue(access.unlocked && access.adFree && access.interstitialsPaused && access.purchased)
    assertNull(access.until)
  }

  @Test
  fun `full-screen ads wait for a break, the second day and real use`() {
    val free = PremiumState().access(t0)
    fun may(now: Long = t0, installed: Long = t0 - 48 * hour, last: Long = 0L, today: Int = 0, conversions: Int = 2, shown: Boolean = false, access: Access = free) =
      InterstitialPolicy.mayShow(access, now, installed, last, today, conversions, shown)
    assertTrue(may())
    assertFalse("first day after install", may(installed = t0 - 2 * hour))
    assertFalse("only one conversion", may(conversions = 1))
    assertFalse("one a session", may(shown = true))
    assertFalse("15 minutes apart", may(last = t0 - 10 * 60_000))
    assertFalse("three a day", may(today = 3))
    assertFalse("not during a video unlock", may(access = PremiumState(rewardUntil = t0 + hour).access(t0)))
    assertTrue("but yes during the trial", may(access = PremiumState(trialUntil = t0 + hour).access(t0)))
    assertFalse("never with Premium forever", may(access = PremiumState(purchased = true).access(t0)))
  }
}
