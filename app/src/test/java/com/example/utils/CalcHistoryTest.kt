package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class CalcHistoryTest {

  private val sum = HistoryEntry(1_000L, "USD", "ILS", "EUR", "1550", "1250+300", 5754.07, 1424.61)
  private val plain = HistoryEntry(2_000L, "EUR", "ILS", null, "12.5", null, 50.49, null)

  @Test
  fun `entries survive a round trip`() {
    assertEquals(listOf(sum, plain), CalcHistory.decode(CalcHistory.encode(listOf(sum, plain))))
  }

  @Test
  fun `damaged lines are skipped`() {
    val text = CalcHistory.encode(listOf(sum)) + "\nnot|an|entry\n\n" + CalcHistory.encode(listOf(plain))
    assertEquals(listOf(sum, plain), CalcHistory.decode(text))
    assertEquals(emptyList<HistoryEntry>(), CalcHistory.decode(null))
  }

  @Test
  fun `a repeated conversion moves to the top instead of repeating`() {
    val again = plain.copy(time = 3_000L, targetAmount = 51.0)
    assertEquals(listOf(again, sum), CalcHistory.add(listOf(plain, sum), again))
  }

  @Test
  fun `the history keeps the newest entries`() {
    val many = (1..CalcHistory.MAX_ENTRIES).map { plain.copy(amount = "$it") }
    val added = CalcHistory.add(many, sum)
    assertEquals(CalcHistory.MAX_ENTRIES, added.size)
    assertEquals(sum, added.first())
  }
}
