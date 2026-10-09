package com.example.utils

import java.math.BigInteger
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

private const val MAX_INTEGER_DIGITS = 12
private const val MAX_FRACTION_DIGITS = 6

private fun decimalFormat(pattern: String) = DecimalFormat(pattern, DecimalFormatSymbols(Locale.US))

/** Converted amount, e.g. 4,637.50 (tiny non-zero values keep their significant digits). */
fun formatAmount(value: Double): String {
    val pattern = if (value != 0.0 && abs(value) < 0.01) "#,##0.######" else "#,##0.00"
    return decimalFormat(pattern).format(value)
}

/** Exchange rate, e.g. 3.7123 or 0.000271. */
fun formatRate(rate: Double): String {
    val pattern = when {
        abs(rate) >= 100 -> "#,##0.00"
        abs(rate) >= 1 -> "#,##0.0000"
        else -> "0.0000##"
    }
    return decimalFormat(pattern).format(rate)
}

/** Groups the integer part of what the user typed while keeping the fraction exactly as typed: "1250.5" -> "1,250.5". */
fun formatAmountInput(raw: String): String {
    if (raw.isEmpty()) return "0"
    val integerPart = raw.substringBefore('.')
    val groupedInteger = integerPart.toBigIntegerOrNull()?.let { String.format(Locale.US, "%,d", it) } ?: integerPart
    return if (raw.contains('.')) "$groupedInteger.${raw.substringAfter('.')}" else groupedInteger
}

private fun String.toBigIntegerOrNull(): BigInteger? = if (isNotEmpty() && all { it.isDigit() }) BigInteger(this) else null

/**
 * Applies a keypad key ("0"-"9", "00", ".", "C", "⌫") to the raw amount string.
 * Returns the unchanged amount when the key would exceed the supported precision.
 */
fun applyKeypadKey(current: String, key: String): String {
    val amount = current.ifEmpty { "0" }
    return when (key) {
        "C" -> "0"
        "⌫" -> if (amount.length <= 1) "0" else amount.dropLast(1)
        "." -> if (amount.contains('.')) amount else "$amount."
        else -> {
            if (key.isEmpty() || !key.all { it.isDigit() }) return amount
            val candidate = if (amount == "0") key.trimStart('0').ifEmpty { "0" } else amount + key
            val integerDigits = candidate.substringBefore('.').length
            val fractionDigits = if (candidate.contains('.')) candidate.substringAfter('.').length else 0
            if (integerDigits > MAX_INTEGER_DIGITS || fractionDigits > MAX_FRACTION_DIGITS) amount else candidate
        }
    }
}

/**
 * "today 15:28" / "yesterday 09:10" / "20.01 15:28" (the day words come from [todayFormat] / [yesterdayFormat],
 * e.g. "today %1$s"); null when rates were never downloaded.
 */
fun formatUpdatedAt(
    timestamp: Long,
    todayFormat: String,
    yesterdayFormat: String,
    now: Long = System.currentTimeMillis(),
): String? {
    if (timestamp <= 0L) return null
    val time = SimpleDateFormat("HH:mm", Locale.US).format(Date(timestamp))
    val then = Calendar.getInstance().apply { timeInMillis = timestamp }
    val today = Calendar.getInstance().apply { timeInMillis = now }
    fun Calendar.sameDayAs(other: Calendar) =
        get(Calendar.YEAR) == other.get(Calendar.YEAR) && get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    val yesterday = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -1) }
    return when {
        then.sameDayAs(today) -> String.format(todayFormat, time)
        then.sameDayAs(yesterday) -> String.format(yesterdayFormat, time)
        else -> "${SimpleDateFormat("dd.MM", Locale.US).format(Date(timestamp))} $time"
    }
}
