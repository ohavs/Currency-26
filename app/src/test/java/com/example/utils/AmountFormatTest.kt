package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class AmountFormatTest {

  @Test
  fun `digits replace the initial zero and then append`() {
    assertEquals("5", applyKeypadKey("0", "5"))
    assertEquals("51", applyKeypadKey("5", "1"))
    assertEquals("500", applyKeypadKey("5", "00"))
  }

  @Test
  fun `double zero on zero stays zero`() {
    assertEquals("0", applyKeypadKey("0", "00"))
    assertEquals("0", applyKeypadKey("0", "0"))
  }

  @Test
  fun `decimal point is added once and keeps a leading zero`() {
    assertEquals("0.", applyKeypadKey("0", "."))
    assertEquals("12.", applyKeypadKey("12", "."))
    assertEquals("12.5", applyKeypadKey("12.5", "."))
    assertEquals("0.0", applyKeypadKey("0.", "0"))
  }

  @Test
  fun `clear and backspace`() {
    assertEquals("0", applyKeypadKey("1234", "C"))
    assertEquals("123", applyKeypadKey("1234", "⌫"))
    assertEquals("0", applyKeypadKey("7", "⌫"))
    assertEquals("0", applyKeypadKey("", "⌫"))
  }

  @Test
  fun `input is capped instead of overflowing`() {
    assertEquals("123456789012", applyKeypadKey("123456789012", "3"))
    assertEquals("1.123456", applyKeypadKey("1.123456", "7"))
  }

  @Test
  fun `unknown keys are ignored`() {
    assertEquals("12", applyKeypadKey("12", "x"))
  }

  @Test
  fun `typed amount is grouped but keeps the typed fraction`() {
    assertEquals("1,250", formatAmountInput("1250"))
    assertEquals("1,250.", formatAmountInput("1250."))
    assertEquals("1,250.05", formatAmountInput("1250.05"))
    assertEquals("0", formatAmountInput(""))
  }

  @Test
  fun `converted amounts and rates`() {
    assertEquals("4,640.38", formatAmount(4640.375))
    assertEquals("0.00", formatAmount(0.0))
    assertEquals("0.000271", formatAmount(0.000271))
    assertEquals("3.7123", formatRate(3.71234))
    assertEquals("149.52", formatRate(149.5213))
    assertEquals("0.27229", formatRate(0.27229))
    assertEquals("0.000271", formatRate(0.000271))
  }

  @Test
  fun `update time uses the given day words`() {
    val now = java.util.Calendar.getInstance().apply { set(2026, 9, 9, 18, 0) }.timeInMillis
    val todayAt = java.util.Calendar.getInstance().apply { set(2026, 9, 9, 15, 28) }.timeInMillis
    val yesterdayAt = java.util.Calendar.getInstance().apply { set(2026, 9, 8, 9, 5) }.timeInMillis
    val older = java.util.Calendar.getInstance().apply { set(2026, 0, 20, 7, 30) }.timeInMillis
    assertEquals("today 15:28", formatUpdatedAt(todayAt, "today %1\$s", "yesterday %1\$s", now))
    assertEquals("ayer 09:05", formatUpdatedAt(yesterdayAt, "hoy %1\$s", "ayer %1\$s", now))
    assertEquals("20.01 07:30", formatUpdatedAt(older, "today %1\$s", "yesterday %1\$s", now))
    assertEquals(null, formatUpdatedAt(0L, "today %1\$s", "yesterday %1\$s", now))
  }
}
