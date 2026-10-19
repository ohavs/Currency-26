package com.example.utils

import java.math.BigDecimal
import java.math.RoundingMode
import kotlin.math.abs

/*
 * The calculator line behind the keypad's calculator keys. It is kept as plain ASCII ("1250+15%", "(3-1)*2")
 * and shown with proper signs by formatExpression. The rules follow phone calculators: "200+10%" adds 10%
 * of 200, "200*10%" is 20, and a bracket after a number multiplies.
 */

private const val MAX_INTEGER_DIGITS = 12
private const val MAX_FRACTION_DIGITS = 6
private const val MAX_LENGTH = 80
private const val OPERATORS = "+-*/"

private fun Char.isOperator() = this in OPERATORS

/** Ends a number or a bracket, so an operator, "%" or ")" can follow. */
private fun Char.endsValue() = isDigit() || this == '.' || this == ')' || this == '%'

/** The number being typed at the end of [expression] ("" after an operator or a bracket). */
private fun trailingNumber(expression: String): String = expression.takeLastWhile { it.isDigit() || it == '.' }

/** True when [expression] has something to compute, not just a (possibly negative) number. */
fun hasOperation(expression: String): Boolean =
    expression.withIndex().any { (i, c) -> c in "*/+()%" || (c == '-' && i > 0) }

/**
 * Applies a calculator key to [expression]: digits, ".", "+", "-", "*", "/", "%", "()" (opens or closes a
 * bracket as fits), "C" and "⌫". With [fresh] (the shown number is a result), a digit, "." or bracket starts
 * a new calculation while an operator continues from the number. Returns [expression] unchanged when the key
 * does not fit (a second "." in a number, an operator after "(" other than minus, too many digits).
 */
fun applyCalcKey(expression: String, key: String, fresh: Boolean = false): String {
    val expr = expression.ifEmpty { "0" }
    val last = expr.last()
    val next = when (key) {
        "C" -> return "0"
        "⌫" -> return expr.dropLast(1).let { if (it.isEmpty() || it == "-") "0" else it }
        "." -> when {
            fresh -> "0."
            last == ')' || last == '%' -> "$expr*0."
            '.' in trailingNumber(expr) -> expr
            trailingNumber(expr).isEmpty() -> "${expr}0."
            else -> "$expr."
        }
        "%" -> if (last.isDigit() || last == '.' || last == ')') "$expr%" else expr
        "()" -> {
            val open = expr.count { it == '(' } - expr.count { it == ')' }
            when {
                fresh || expr == "0" -> "("
                last.endsValue() && open > 0 -> "$expr)"
                last.endsValue() -> "$expr*("
                else -> "$expr("
            }
        }
        "+", "-", "*", "/" -> when {
            last.isOperator() -> {
                val before = expr.dropLast(1)
                // A minus right after "(" (or at the start) is a sign, not an operator to replace.
                if (before.isEmpty() || before.last() == '(') expr else before + key
            }
            last == '(' -> if (key == "-") "$expr-" else expr
            else -> expr + key
        }
        else -> {
            if (key.isEmpty() || !key.all { it.isDigit() }) return expr
            when {
                fresh -> key.trimStart('0').ifEmpty { "0" }
                last == ')' || last == '%' -> "$expr*$key"
                else -> {
                    val number = trailingNumber(expr)
                    val prefix = expr.dropLast(number.length)
                    val candidate = if (number == "0") key.trimStart('0').ifEmpty { "0" } else number + key
                    val integerDigits = candidate.substringBefore('.').length
                    val fractionDigits = if ('.' in candidate) candidate.substringAfter('.').length else 0
                    if (integerDigits > MAX_INTEGER_DIGITS || fractionDigits > MAX_FRACTION_DIGITS) expr else prefix + candidate
                }
            }
        }
    }
    return if (next.length > MAX_LENGTH) expr else next
}

/** The value of a complete expression, or null when it is not one (unbalanced brackets, "5/0", "1.2.3"). */
fun evaluateExpression(expression: String): Double? = ExpressionParser(expression).parse()

/** The expression as far as it goes: without a trailing operator or "(", brackets closed ("2*(3+1-" -> "2*(3+1)"). */
fun completeExpression(expression: String): String {
    var e = expression
    while (e.isNotEmpty() && (e.last().isOperator() || e.last() == '(')) e = e.dropLast(1)
    val open = e.count { it == '(' } - e.count { it == ')' }
    return if (open > 0) e + ")".repeat(open) else e
}

/**
 * The live result while typing, of [completeExpression]: "1250+" is 1250 and "2*(3+1" is 8. Null when there
 * is no result ("5/0", nothing typed yet).
 */
fun previewExpression(expression: String): Double? =
    completeExpression(expression).let { if (it.isEmpty()) null else evaluateExpression(it) }

/**
 * A result as a keypad amount ("1550", "33.33", "-30", "0.001"): rounded to cents, except amounts under one,
 * which keep up to six decimals (like formatAmount); no trailing zeros. Null when it does not fit the amount
 * (12 integer digits) or is not a number.
 */
fun toRawAmount(value: Double): String? {
    if (!value.isFinite()) return null
    val scale = if (abs(value) >= 1) 2 else MAX_FRACTION_DIGITS
    val rounded = BigDecimal.valueOf(value).setScale(scale, RoundingMode.HALF_UP).stripTrailingZeros()
    if (rounded.abs() >= BigDecimal.TEN.pow(MAX_INTEGER_DIGITS)) return null
    return if (rounded.signum() == 0) "0" else rounded.toPlainString()
}

/** The expression for display: grouped numbers and real signs, "1250+15%" -> "1,250 + 15%", "(-2)*3" -> "(−2) × 3". */
fun formatExpression(expression: String): String = buildString {
    var i = 0
    while (i < expression.length) {
        val c = expression[i]
        when {
            c.isDigit() || c == '.' -> {
                val end = (i until expression.length).firstOrNull { !expression[it].isDigit() && expression[it] != '.' } ?: expression.length
                append(formatAmountInput(expression.substring(i, end)))
                i = end
                continue
            }
            // A sign at the start or after "(" or another operator.
            c == '-' && (i == 0 || expression[i - 1] == '(' || expression[i - 1].isOperator()) -> append('−')
            c.isOperator() -> append(' ').append(displaySign(c)).append(' ')
            else -> append(c)
        }
        i++
    }
}

private fun displaySign(c: Char) = when (c) {
    '-' -> '−'
    '*' -> '×'
    '/' -> '÷'
    else -> c
}

/** expr := term (("+" | "-") term)*, term := factor (("*" | "/") factor)*, factor := "-" factor | primary "%"*. */
private class ExpressionParser(private val s: String) {
    private var i = 0

    /** A factor's value, and for "x%" the x (an added or subtracted "x%" is x percent of what precedes it). */
    private class Factor(val value: Double, val percent: Double?)

    fun parse(): Double? {
        val value = expr() ?: return null
        return if (i == s.length && value.isFinite()) value else null
    }

    private fun expr(): Double? {
        var value = term()?.first ?: return null
        while (i < s.length && (s[i] == '+' || s[i] == '-')) {
            val op = s[i++]
            val (right, percent) = term() ?: return null
            val amount = if (percent != null) value * percent / 100 else right
            value = if (op == '+') value + amount else value - amount
        }
        return value
    }

    /** The term's value, and its percent when the whole term is a single "x%". */
    private fun term(): Pair<Double, Double?>? {
        val first = factor() ?: return null
        var value = first.value
        var percent = first.percent
        while (i < s.length && (s[i] == '*' || s[i] == '/')) {
            val op = s[i++]
            val right = factor()?.value ?: return null
            percent = null
            value = if (op == '*') value * right else if (right == 0.0) return null else value / right
        }
        return value to percent
    }

    private fun factor(): Factor? {
        if (i < s.length && s[i] == '-') {
            i++
            val inner = factor() ?: return null
            return Factor(-inner.value, inner.percent?.let { -it })
        }
        var value = primary() ?: return null
        var percent: Double? = null
        while (i < s.length && s[i] == '%') {
            i++
            percent = value
            value /= 100
        }
        return Factor(value, percent)
    }

    private fun primary(): Double? {
        if (i < s.length && s[i] == '(') {
            i++
            val value = expr() ?: return null
            if (i >= s.length || s[i] != ')') return null
            i++
            return value
        }
        val start = i
        while (i < s.length && (s[i].isDigit() || s[i] == '.')) i++
        val token = s.substring(start, i)
        if (token.isEmpty() || token == ".") return null
        return token.toDoubleOrNull()
    }
}
