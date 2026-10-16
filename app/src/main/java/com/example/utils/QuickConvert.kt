package com.example.utils

import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale
import kotlin.math.floor

/** One found price and what it comes to in the user's main currency. */
data class ConvertedPrice(val price: FoundPrice, val to: String, val value: Double)

/** What the quick-convert bubble shows for a piece of selected text. */
data class QuickConversion(
    val lines: List<ConvertedPrice>,
    /** For a single price: the same amount in the user's other currencies. */
    val others: List<Pair<String, Double>>,
    /** The sum, when there are several prices in one currency. */
    val total: Pair<String, Double>?,
) {
    /** The selected text with every price replaced by its conversion. */
    fun replaceIn(text: String): String {
        val result = StringBuilder(text)
        lines.sortedByDescending { it.price.range.first }.forEach { line ->
            result.replace(line.price.range.first, line.price.range.last + 1, withSymbol(line.to, line.value))
        }
        return result.toString()
    }
}

object QuickConvert {
    /**
     * Converts the prices in [text] into the user's currencies ([source], [target] and the optional [extra];
     * the target first, so "€120" shows in shekels and "₪120" in dollars). Rates are per US dollar.
     * [local] is the currency of the country the phone is in, which settles "$", "kr" and bare numbers abroad.
     */
    fun convert(
        text: String,
        rates: Map<String, Double>,
        source: String,
        target: String,
        extra: String?,
        local: String?,
    ): QuickConversion {
        val mine = listOfNotNull(target, source, extra).distinct()
        // A bare number is in the local currency when abroad (it isn't the target), else in the source.
        val fallback = local?.takeIf { it != target && it in rates } ?: source
        val prices = PriceParser.parse(text, rates.keys, listOfNotNull(local, source, target, extra), fallback)
        val lines = prices.map { price ->
            val to = mine.firstOrNull { it != price.currency } ?: target
            ConvertedPrice(price, to, convertAmount(price.amount, price.currency, to, rates))
        }
        val single = lines.singleOrNull()
        val others = single?.let { line ->
            mine.filter { it != line.price.currency && it != line.to }
                .map { it to convertAmount(line.price.amount, line.price.currency, it, rates) }
        }.orEmpty()
        val total = lines.takeIf { it.size > 1 && it.all { l -> l.to == it[0].to } }
            ?.let { it[0].to to it.sumOf { l -> l.value } }
        return QuickConversion(lines, others, total)
    }

    fun convertAmount(amount: Double, from: String, to: String, rates: Map<String, Double>): Double =
        amount / (rates[from] ?: 1.0) * (rates[to] ?: 1.0)
}

/** "₪452.30" or "1,250.00 Ft": the amount with its symbol on the side it is written. */
fun withSymbol(code: String, amount: Double, original: Boolean = false): String {
    val symbol = amountSymbol(code)
    val number = if (original) formatOriginal(amount) else formatAmount(amount)
    return if (symbolLeads(symbol)) "$symbol$number" else "$number $symbol"
}

/** A price as it was written: no ".00" on whole amounts ("€120", "€49.99"). */
fun formatOriginal(amount: Double): String {
    val pattern = if (amount == floor(amount)) "#,##0" else "#,##0.00"
    return DecimalFormat(pattern, DecimalFormatSymbols(Locale.US)).format(amount)
}
