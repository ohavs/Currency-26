package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculatorTest {

  private fun type(vararg keys: String, start: String = "0", fresh: Boolean = false): String {
    var expression = start
    keys.forEachIndexed { i, key -> expression = applyCalcKey(expression, key, fresh = fresh && i == 0) }
    return expression
  }

  private fun assertValue(expected: Double, expression: String) {
    assertEquals(expected, evaluateExpression(expression)!!, 1e-9)
  }

  @Test
  fun `precedence and brackets`() {
    assertValue(14.0, "2+3*4")
    assertValue(20.0, "(2+3)*4")
    assertValue(2.5, "10/4")
    assertValue(-6.0, "(-2)*3")
    assertValue(1.0, "10-3*3")
  }

  @Test
  fun `percent works like a phone calculator`() {
    assertValue(220.0, "200+10%")
    assertValue(180.0, "200-10%")
    assertValue(20.0, "200*10%")
    assertValue(2000.0, "200/10%")
    assertValue(0.5, "50%")
    assertValue(5.1, "10%+5")
    assertValue(138.0, "120+15%")
  }

  @Test
  fun `incomplete or broken expressions`() {
    assertNull(evaluateExpression("5/0"))
    assertNull(evaluateExpression("2*(3"))
    assertNull(evaluateExpression("1.2.3"))
    assertNull(evaluateExpression("+"))
    assertNull(previewExpression("5/0"))
    assertNull(previewExpression("("))
  }

  @Test
  fun `the live result ignores what is not finished yet`() {
    assertEquals(1250.0, previewExpression("1250+")!!, 1e-9)
    assertEquals(8.0, previewExpression("2*(3+1")!!, 1e-9)
    assertEquals(5.0, previewExpression("5*(")!!, 1e-9)
    assertEquals(12.0, previewExpression("12.")!!, 1e-9)
  }

  @Test
  fun `an unfinished expression is completed`() {
    assertEquals("1250", completeExpression("1250+"))
    assertEquals("2*(3+1)", completeExpression("2*(3+1-"))
    assertEquals("(5)", completeExpression("(5"))
    assertEquals("", completeExpression("(-"))
  }

  @Test
  fun `digits replace a lone zero and respect the limits`() {
    assertEquals("5", type("5"))
    assertEquals("12+5", type("0", "5", start = "12+"))
    assertEquals("123456789012", type("3", start = "123456789012"))
    assertEquals("1+1.123456", type("7", start = "1+1.123456"))
  }

  @Test
  fun `a fresh result is replaced by a digit but kept by an operator`() {
    assertEquals("7", type("7", start = "1550", fresh = true))
    assertEquals("0.", type(".", start = "1550", fresh = true))
    assertEquals("1550+", type("+", start = "1550", fresh = true))
    assertEquals("1550%", type("%", start = "1550", fresh = true))
  }

  @Test
  fun `operators replace each other and a minus can start a bracket`() {
    assertEquals("5-", type("*", "-", start = "5"))
    assertEquals("(-", type("()", "-"))
    assertEquals("(-", type("*", start = "(-"))
    assertEquals("(", type("*", start = "("))
  }

  @Test
  fun `the bracket key opens, closes and multiplies`() {
    assertEquals("(", type("()"))
    assertEquals("2*(", type("()", start = "2"))
    assertEquals("2*(3)", type("3", "()", start = "2*("))
    assertEquals("(2+3)*4", type("()", "2", "+", "3", "()", "*", "4"))
    assertEquals("(1)*5", type("5", start = "(1)"))
  }

  @Test
  fun `decimal point and percent`() {
    assertEquals("2+0.", type(".", start = "2+"))
    assertEquals("2.5", type(".", start = "2.5"))
    assertEquals("10%", type("%", start = "10"))
    assertEquals("10%", type("%", start = "10%"))
    assertEquals("10+", type("%", start = "10+"))
  }

  @Test
  fun `clear and backspace`() {
    assertEquals("0", type("C", start = "1+2"))
    assertEquals("1+", type("⌫", start = "1+2"))
    assertEquals("0", type("⌫", start = "7"))
    assertEquals("0", type("⌫", start = "-3"))
  }

  @Test
  fun `an operation is more than a negative number`() {
    assertFalse(hasOperation("1250"))
    assertFalse(hasOperation("-30"))
    assertFalse(hasOperation("12."))
    assertTrue(hasOperation("5-3"))
    assertTrue(hasOperation("1250+"))
    assertTrue(hasOperation("50%"))
    assertTrue(hasOperation("(2)"))
  }

  @Test
  fun `results become keypad amounts`() {
    assertEquals("1550", toRawAmount(1550.0))
    assertEquals("0.3", toRawAmount(0.1 + 0.2))
    assertEquals("33.33", toRawAmount(100.0 / 3))
    assertEquals("17451.06", toRawAmount(17451.058333))
    assertEquals("0.001", toRawAmount(1.0 / 1000))
    assertEquals("0.333333", toRawAmount(1.0 / 3))
    assertEquals("-30", toRawAmount(-30.0))
    assertEquals("0", toRawAmount(-0.0000001))
    assertNull(toRawAmount(1e13))
    assertNull(toRawAmount(Double.NaN))
  }

  @Test
  fun `expressions are shown with grouped numbers and real signs`() {
    assertEquals("1,250 + 15%", formatExpression("1250+15%"))
    assertEquals("(−2) × 3", formatExpression("(-2)*3"))
    assertEquals("12,000 ÷ 4 − 1.5", formatExpression("12000/4-1.5"))
    assertEquals("2 × (", formatExpression("2*("))
  }

  @Test
  fun `negative amounts are grouped too`() {
    assertEquals("-1,250", formatAmountInput("-1250"))
    assertEquals("0", applyKeypadKey("-3", "⌫"))
  }
}
