package com.example

import android.app.Application
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import com.example.ui.CalculatorContent
import com.example.ui.CalculatorState
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
// Plain Application: CurrencyApp schedules WorkManager jobs, which is not initialized under Robolectric.
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36], application = Application::class)
class CalculatorScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  private val state = CalculatorState(
    sourceCurrency = "USD",
    targetCurrency = "ILS",
    sourceAmountRaw = "1250",
    rates = mapOf("USD" to 1.0, "ILS" to 3.7123),
  )

  @Test
  fun calculator_light_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(themeMode = "light") {
        CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/calculator_light.png")
  }

  @Test
  fun calculator_dark_screenshot() {
    composeTestRule.setContent {
      MyApplicationTheme(themeMode = "dark") {
        CalculatorContent(state, onSwap = {}, onCurrencyClick = {}, onKeypad = {}, onOpenSettings = {}, onRefresh = {})
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/calculator_dark.png")
  }
}
