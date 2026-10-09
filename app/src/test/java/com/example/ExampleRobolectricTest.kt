package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class ExampleRobolectricTest {

  @Test
  fun `english is the default for untranslated languages`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Currency Converter", context.getString(R.string.app_name))
  }

  @Test
  @Config(qualifiers = "iw")
  fun `hebrew strings`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("ממיר מטבעות", context.getString(R.string.app_name))
  }

  @Test
  @Config(qualifiers = "es")
  fun `spanish strings`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    assertEquals("Conversor de divisas", context.getString(R.string.app_name))
  }
}
