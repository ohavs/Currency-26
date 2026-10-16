package com.example

import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.utils.AppLanguage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], application = Application::class)
class AppLanguageInitTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val prefs = context.getSharedPreferences("currency_prefs", Context.MODE_PRIVATE)

    @Test
    fun `a fresh install follows the device and gets one suggestion`() {
        AppLanguage.initialize(context)
        assertEquals(AppLanguage.SYSTEM, AppLanguage.stored(context))
        assertTrue(prefs.getBoolean(AppLanguage.SUGGEST_KEY, false))
    }

    @Test
    fun `an install from the hebrew only days keeps hebrew`() {
        prefs.edit().putString("source", "USD").commit()
        AppLanguage.initialize(context)
        assertEquals(AppLanguage.HEBREW, AppLanguage.stored(context))
        assertFalse(prefs.getBoolean(AppLanguage.SUGGEST_KEY, false))
    }

    @Test
    fun `a language the user picked is kept`() {
        prefs.edit().putString(AppLanguage.KEY, "fr").commit()
        AppLanguage.initialize(context)
        assertEquals("fr", AppLanguage.stored(context))
    }

    @Test
    fun `the shown language resolves the device setting`() {
        assertEquals("en", AppLanguage.effective(AppLanguage.SYSTEM))
        assertEquals("he", AppLanguage.effective("he"))
    }
}
