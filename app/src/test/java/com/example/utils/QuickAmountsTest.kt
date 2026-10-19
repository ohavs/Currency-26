package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class QuickAmountsTest {

  @Test
  fun `round amounts follow the currency's value`() {
    assertEquals(listOf(10L, 20L, 50L, 100L), quickAmounts(1.0))
    assertEquals(listOf(10L, 20L, 50L, 100L), quickAmounts(0.9191))
    assertEquals(listOf(50L, 100L, 200L, 500L), quickAmounts(3.7123))
    assertEquals(listOf(2_000L, 5_000L, 10_000L, 20_000L), quickAmounts(149.52))
    assertEquals(listOf(500L, 1_000L, 2_000L, 5_000L), quickAmounts(35.81))
    assertEquals(listOf(2L, 5L, 10L, 20L), quickAmounts(0.307))
  }

  @Test
  fun `unknown rates count as dollars`() {
    assertEquals(listOf(10L, 20L, 50L, 100L), quickAmounts(null))
    assertEquals(listOf(10L, 20L, 50L, 100L), quickAmounts(0.0))
  }
}
