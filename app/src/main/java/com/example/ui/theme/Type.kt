package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.sp

private fun TextStyle.rubik(weight: FontWeight) = copy(fontFamily = Rubik, fontWeight = weight)

private val Base = Typography()

val Typography = Typography(
    displayLarge = Base.displayLarge.rubik(FontWeight.SemiBold),
    displayMedium = Base.displayMedium.rubik(FontWeight.SemiBold),
    displaySmall = Base.displaySmall.rubik(FontWeight.SemiBold),
    headlineLarge = Base.headlineLarge.rubik(FontWeight.SemiBold),
    headlineMedium = Base.headlineMedium.rubik(FontWeight.SemiBold),
    headlineSmall = Base.headlineSmall.rubik(FontWeight.SemiBold),
    titleLarge = Base.titleLarge.rubik(FontWeight.SemiBold),
    titleMedium = Base.titleMedium.rubik(FontWeight.Medium),
    titleSmall = Base.titleSmall.rubik(FontWeight.Medium),
    bodyLarge = Base.bodyLarge.rubik(FontWeight.Normal),
    bodyMedium = Base.bodyMedium.rubik(FontWeight.Normal),
    bodySmall = Base.bodySmall.rubik(FontWeight.Normal),
    labelLarge = Base.labelLarge.rubik(FontWeight.Medium),
    labelMedium = Base.labelMedium.rubik(FontWeight.Medium),
    labelSmall = Base.labelSmall.rubik(FontWeight.Medium),
)

/** Big converted/typed amounts. Numbers always read left-to-right, even inside the RTL UI. */
val AmountTextStyle = TextStyle(
    fontFamily = Rubik,
    fontWeight = FontWeight.SemiBold,
    fontSize = 34.sp,
    lineHeight = 40.sp,
    letterSpacing = (-0.5).sp,
    textDirection = TextDirection.Ltr,
)

/** Currency codes (USD, EUR...) shown as the card headline. */
val CurrencyCodeTextStyle = TextStyle(
    fontFamily = Rubik,
    fontWeight = FontWeight.SemiBold,
    fontSize = 24.sp,
    lineHeight = 28.sp,
    textDirection = TextDirection.Ltr,
)
