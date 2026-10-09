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
    assertTrue(size * 10 * 0.62f <= 160f + 0.01f)
  }

  @Test
  fun `never smaller than the minimum`() {
    assertEquals(14f, fitAmountSp("123,456,789,012.25", availableWidthDp = 40f, maxSp = 46f), 0.01f)
  }

  @Test
  fun `long names are shortened`() {
    assertEquals("שקל חדש", shortName("שקל חדש"))
    assertEquals("דירהם (איחוד…", shortName("דירהם (איחוד האמירויות)"))
  }
}
