package com.example.widget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WidgetSizingTest {

  @Test
  fun `short amounts use the full allowed size`() {
    assertEquals(46f, fitAmountSp("1,250", availableWidthDp = 200f, maxSp = 46f), 0.01f)
  }

  @Test
  fun `long amounts shrink to fit the width`() {
    val size = fitAmountSp("120,337.50", availableWidthDp = 160f, maxSp = 46f)
    assertTrue(size < 46f)
    assertTrue(size * textEm("120,337.50") <= 160f + 0.01f)
  }

  @Test
  fun `never smaller than the minimum`() {
    assertEquals(14f, fitAmountSp("123,456,789,012.25", availableWidthDp = 40f, maxSp = 46f), 0.01f)
  }

  @Test
  fun `the symbol beside the amount shares the width`() {
    val plain = fitAmountSp("120,337.50", availableWidthDp = 200f, maxSp = 46f)
    val withSymbol = fitAmountSp("120,337.50", availableWidthDp = 200f, maxSp = 46f, symbol = "₪")
    assertTrue(withSymbol < plain)
    assertTrue(withSymbol * (textEm("120,337.50") + (textEm("₪") + 0.3f) * SymbolScale) <= 200f + 0.01f)
  }

  @Test
  fun `separators are narrower than digits`() {
    assertTrue(textEm("1,250") < textEm("12500"))
  }

  @Test
  fun `long names are shortened`() {
    assertEquals("שקל חדש", shortName("שקל חדש"))
    assertEquals("דירהם (איחוד…", shortName("דירהם (איחוד האמירויות)"))
  }

  @Test
  fun `a 4x4 widget gets a roomy keypad and a rate line`() {
    val layout = largeWidgetLayout(300f, 380f, hasExtra = false)
    assertTrue(layout.showKeypad && layout.showRate && layout.showNames)
    assertTrue(layout.keyRowDp >= 40f)
    assertTrue(layout.maxAmountSp >= 30f)
  }

  @Test
  fun `a tall widget grows the keys, the flag and the amounts instead of leaving empty cards`() {
    val layout = largeWidgetLayout(400f, 700f, hasExtra = false)
    assertTrue(layout.keyRowDp >= 80f)
    assertTrue(layout.keyFontSp >= 30f)
    assertTrue(layout.cardHeightDp <= 150f)
    assertTrue(layout.flagSp >= 40f)
    assertTrue(layout.maxAmountSp >= 60f)
    assertEquals(2, layout.nameLines)
  }

  @Test
  fun `cards never shrink below 40dp while the keypad is shown`() {
    for (height in 230..800 step 10) {
      for (extra in listOf(false, true)) {
        val layout = largeWidgetLayout(300f, height.toFloat(), extra)
        if (layout.keyRowDp > 22f) assertTrue("$height $extra", layout.cardHeightDp >= 40f - 0.01f)
      }
    }
  }

  @Test
  fun `short widgets drop the rate line and then the keypad`() {
    assertTrue(!largeWidgetLayout(300f, 260f, hasExtra = false).showRate)
    val tiny = largeWidgetLayout(300f, 160f, hasExtra = false)
    assertTrue(!tiny.showKeypad && tiny.cardHeightDp > 50f)
  }
}
