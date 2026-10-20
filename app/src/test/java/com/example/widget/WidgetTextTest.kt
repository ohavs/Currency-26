package com.example.widget

import android.app.Application
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [36], application = Application::class, qualifiers = "xxhdpi")
class WidgetTextTest {

  private val context = ApplicationProvider.getApplicationContext<Application>()

  @Test
  fun `the picture is as wide as the label and holds white text`() {
    val picture = renderAppFontText(context, "שקל חדש", sizeSp = 14f, widthDp = 90f, maxLines = 1, minSp = 11f)
    assertEquals(90f, picture.widthDp, 0.5f)
    assertTrue(picture.heightDp in 12f..24f)
    val bitmap = picture.bitmap
    var white = 0
    for (x in 0 until bitmap.width) for (y in 0 until bitmap.height) {
      val pixel = bitmap.getPixel(x, y)
      if (Color.alpha(pixel) > 200 && Color.red(pixel) > 200) white++
    }
    assertTrue("no text drawn", white > 50)
  }

  @Test
  fun `a long name takes a second line when there is one`() {
    val name = "דירהם (איחוד האמירויות)"
    val one = renderAppFontText(context, name, sizeSp = 14f, widthDp = 90f, maxLines = 1, minSp = 14f)
    val two = renderAppFontText(context, name, sizeSp = 14f, widthDp = 90f, maxLines = 2, minSp = 14f)
    assertTrue(two.heightDp > one.heightDp * 1.6f)
  }
}
