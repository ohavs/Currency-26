package com.example.utils

import kotlin.math.abs
import kotlin.math.ln

/**
 * Round amounts for one tap, in the source currency: from about 10 US dollars up, on the 1-2-5 steps
 * banknotes use. Dollars and euros get 10 20 50 100, shekels 50 100 200 500, yen 2,000 5,000 10,000 20,000.
 * [unitsPerDollar] is the source currency's rate (units per US dollar); unknown rates count as dollars.
 */
fun quickAmounts(unitsPerDollar: Double?, count: Int = 4): List<Long> {
    val perDollar = unitsPerDollar?.takeIf { it > 0 && it.isFinite() } ?: 1.0
    val target = ln(10 * perDollar)
    var value = niceSteps().minBy { abs(ln(it.toDouble()) - target) }
    return List(count) { value.also { value = nextStep(it) } }
}

/** 1, 2, 5, 10, 20, 50, ... up to the largest amount the keypad takes. */
private fun niceSteps(): Sequence<Long> = generateSequence(1L, ::nextStep).takeWhile { it < 1_000_000_000_000L }

private fun nextStep(value: Long): Long = if (value.toString().first() == '2') value / 2 * 5 else value * 2
