package com.example.utils

import java.text.Normalizer
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Finds currencies by whatever the user types: the code, the symbol, the currency name, or any country
 * that uses it - in Hebrew, English and Spanish, whatever the UI language. Accents, niqqud, quote marks
 * ("ארה״ב", "ש"ח") and Hebrew final letters are ignored; every word of the query has to match.
 */
object CurrencySearch {
    private val languages = listOf(Locale.forLanguageTag("he"), Locale.ENGLISH, Locale.forLanguageTag("es"))

    /** Everyday names that neither the system nor the predefined list knows. */
    private val aliases = mapOf(
        "ILS" to listOf("ש\"ח", "שח", "NIS", "shekel", "séquel"),
        "USD" to listOf("US", "buck", "dólar"),
        "GBP" to listOf("pound", "quid", "libra"),
        "EUR" to listOf("euro", "יורו"),
        "CNY" to listOf("RMB", "renminbi", "yuan"),
        "JPY" to listOf("yen"),
        "KRW" to listOf("won"),
        "CHF" to listOf("swiss franc"),
    )

    /** Regions that use each currency: EUR -> DE, FR, IT, ...; USD -> US, EC, PA, SV, ... */
    private val regionsByCurrency: Map<String, List<String>> by lazy {
        Locale.getISOCountries().mapNotNull { region ->
            val currency = try {
                java.util.Currency.getInstance(Locale.Builder().setRegion(region).build())
            } catch (e: Exception) {
                null
            }
            currency?.let { it.currencyCode to region }
        }.groupBy({ it.first }, { it.second })
    }

    private class Entry(val code: String, val names: List<String>, val words: List<String>, val haystack: String)

    private val index = ConcurrentHashMap<String, Entry>()

    private fun entry(code: String): Entry = index.getOrPut(code.uppercase()) { build(code.uppercase()) }

    private fun build(code: String): Entry {
        val info = getCurrencyInfo(code)
        val currency = try { java.util.Currency.getInstance(code) } catch (e: Exception) { null }
        val names = listOf(info.hebrewName) + languages.map { currencyName(code, it) }
        val regions = regionsByCurrency[code].orEmpty()
        val countries = languages.flatMap { language ->
            regions.map { region -> Locale.Builder().setRegion(region).build().getDisplayCountry(language) } +
                countryName(code, language)
        }
        val symbols = listOfNotNull(info.symbol, amountSymbol(code), currency?.symbol, currency?.getSymbol(Locale.ENGLISH))
        val all = (listOf(code) + names + countries + symbols + info.keywords + aliases[code].orEmpty())
            .map(::normalize)
            .filter { it.isNotEmpty() }
            .distinct()
        return Entry(
            code = normalize(code),
            names = (names + aliases[code].orEmpty()).map(::normalize),
            words = all.flatMap { it.split(' ') }.distinct(),
            haystack = all.joinToString(" | ")
        )
    }

    /** Builds the search index ahead of time (it reads locale data for every country). */
    fun warmUp(codes: Collection<String>) {
        codes.forEach { entry(it) }
    }

    /** Well-known currencies (the predefined list, most used first) win ties: "dollar" lists USD before AUD. */
    private val popularity: Map<String, Int> by lazy { currencyMap.keys.withIndex().associate { (i, code) -> code to i } }

    /** The [codes] matching [query], best matches first (code, then name, then word starts); all of them when blank. */
    fun search(codes: Collection<String>, query: String): List<String> {
        val normalized = normalize(query)
        val tokens = normalized.split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return codes.toList()
        return codes
            .mapNotNull { code ->
                val e = entry(code)
                if (tokens.all { e.haystack.contains(it) }) code to score(e, normalized, tokens) else null
            }
            .sortedWith(compareBy({ it.second }, { popularity[it.first] ?: Int.MAX_VALUE }, { it.first }))
            .map { it.first }
    }

    private fun score(e: Entry, query: String, tokens: List<String>): Int = when {
        e.code == query -> 0
        e.names.any { it.startsWith(query) } -> 1
        tokens.all { token -> e.words.any { it.startsWith(token) } } -> 2
        else -> 3
    }

    private val marks = Regex("\\p{Mn}+")
    private val quotes = Regex("[\"'`´‘’“”׳״]")
    private val separators = Regex("[\\s\\-_.,()/|]+")
    private val finals = mapOf('ך' to 'כ', 'ם' to 'מ', 'ן' to 'נ', 'ף' to 'פ', 'ץ' to 'צ')

    internal fun normalize(text: String): String {
        val plain = Normalizer.normalize(text.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace(marks, "")
            .replace(quotes, "")
            .replace(separators, " ")
            .trim()
        return plain.map { finals[it] ?: it }.joinToString("")
    }
}
