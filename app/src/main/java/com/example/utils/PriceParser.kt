package com.example.utils

import java.util.Locale

/** A price found in selected text; [range] covers the number and its currency marker. */
data class FoundPrice(
    val amount: Double,
    val currency: String,
    /** The text named no currency, so [currency] is a guess. */
    val assumed: Boolean,
    val range: IntRange,
)

/**
 * Finds prices in text someone selected in another app: "$49.99", "1.250,00 €", "USD 1,250", "120 euros",
 * "350 baht", "100 ש״ח", "1.5k", "100円". Symbols that several currencies share ("$", "kr", "¥") resolve to
 * the first of [preferred] that uses them (the local currency first), else the most common one.
 */
object PriceParser {
    private class Marker(val text: String, val candidates: List<String>, val ignoreCase: Boolean)

    private val DOLLARS = listOf("USD", "CAD", "AUD", "NZD", "SGD", "HKD", "TWD", "MXN", "ARS", "CLP", "COP", "UYU", "DOP")
    private val KRONOR = listOf("SEK", "NOK", "DKK", "ISK")
    private val RUPEES = listOf("INR", "PKR", "LKR", "NPR")
    private val PESOS = listOf("MXN", "ARS", "CLP", "COP", "PHP", "UYU", "DOP", "CUP")
    private val DINARS = listOf("JOD", "KWD", "BHD", "IQD", "TND", "RSD", "LYD", "DZD")
    private val RIYALS = listOf("SAR", "QAR", "OMR", "YER")
    private val DIRHAMS = listOf("AED", "MAD")
    private val LIRAS = listOf("TRY", "LBP", "SYP")
    private val POUNDS = listOf("GBP", "EGP")
    private val YUAN = listOf("CNY", "TWD", "HKD", "MOP")

    /** Signs and abbreviations written next to amounts (case matters: "R" is a rand, "r" is not). */
    private val SYMBOLS: Map<String, List<String>> = mapOf(
        "US$" to listOf("USD"), "C$" to listOf("CAD"), "CA$" to listOf("CAD"), "A$" to listOf("AUD"), "AU$" to listOf("AUD"),
        "NZ$" to listOf("NZD"), "HK$" to listOf("HKD"), "S$" to listOf("SGD"), "NT$" to listOf("TWD"), "MX$" to listOf("MXN"),
        "R$" to listOf("BRL"), "$" to DOLLARS, "€" to listOf("EUR"), "£" to POUNDS, "E£" to listOf("EGP"),
        "¥" to listOf("JPY", "CNY"), "￥" to listOf("JPY", "CNY"), "₪" to listOf("ILS"), "₹" to listOf("INR"), "₩" to listOf("KRW"),
        "₽" to listOf("RUB"), "₺" to listOf("TRY"), "₫" to listOf("VND"), "฿" to listOf("THB"), "₱" to listOf("PHP"),
        "₴" to listOf("UAH"), "₦" to listOf("NGN"), "₸" to listOf("KZT"), "₾" to listOf("GEL"), "₼" to listOf("AZN"),
        "₡" to listOf("CRC"), "₲" to listOf("PYG"), "₵" to listOf("GHS"), "₭" to listOf("LAK"), "₮" to listOf("MNT"),
        "៛" to listOf("KHR"), "zł" to listOf("PLN"), "Kč" to listOf("CZK"), "Ft" to listOf("HUF"), "kr" to KRONOR,
        "kr." to KRONOR, "Kr" to KRONOR, "lei" to listOf("RON"), "RM" to listOf("MYR"), "Rp" to listOf("IDR"),
        "Rs" to RUPEES, "Rs." to RUPEES, "R" to listOf("ZAR"), "Fr." to listOf("CHF"), "SFr." to listOf("CHF"),
        "лв" to listOf("BGN"), "TL" to listOf("TRY"), "﷼" to RIYALS, "د.إ" to listOf("AED"), "ر.س" to listOf("SAR"),
        "د.ك" to listOf("KWD"), "د.ا" to listOf("JOD"), "ج.م" to listOf("EGP"), "ر.ق" to listOf("QAR"), "ل.ل" to listOf("LBP"),
        "د.م." to listOf("MAD"), "د.ت" to listOf("TND"), "元" to YUAN, "円" to listOf("JPY"),
    )

    /** Currency names as people write them after an amount, in the app's languages (singular and plural). */
    private val WORDS: Map<String, List<String>> = buildMap {
        fun add(currencies: List<String>, vararg words: String) = words.forEach { put(it, currencies) }
        add(DOLLARS, "dollar", "dollars", "dólar", "dólares", "dollaro", "dollari", "dolar", "доллар", "доллара", "долларов",
            "דולר", "דולרים", "دولار", "دولارات", "डॉलर", "ドル")
        add(listOf("USD"), "美元")
        add(listOf("EUR"), "euro", "euros", "eur", "евро", "יורו", "אירו", "يورو", "यूरो", "ユーロ", "欧元")
        add(POUNDS, "pound", "pounds", "quid", "libra", "libras", "livre", "livres", "pfund", "sterlina", "sterline", "sterlin",
            "фунт", "фунта", "фунтов", "פאונד", "ליש\"ט", "ליש״ט", "جنيه", "पाउंड", "ポンド", "英镑")
        add(listOf("ILS"), "shekel", "shekels", "sheqel", "nis", "שקל", "שקלים", "ש\"ח", "ש״ח", "שח", "шекель", "шекеля", "шекелей", "شيكل")
        add(listOf("JPY"), "yen", "yenes", "ין", "иена", "иен", "ين", "日元")
        add(YUAN, "yuan", "rmb", "юань", "юаня", "юаней", "יואן", "يوان", "人民币", "块")
        add(listOf("THB"), "baht", "בהט", "באט", "бат", "бата", "батов", "バーツ", "泰铢")
        add(RUPEES, "rupee", "rupees", "rupia", "rupias", "roupie", "roupies", "рупия", "рупий", "רופי", "روبية", "रुपये", "रुपया", "रुपए", "₨")
        add(listOf("RUB"), "ruble", "rubles", "rouble", "roubles", "rublo", "rublos", "рубль", "рубля", "рублей", "руб", "руб.", "רובל", "רובלים", "روبل")
        add(PESOS, "peso", "pesos", "פסו", "פסוס", "песо", "بيزو")
        add(listOf("BRL"), "real", "reais", "реал", "реалов")
        add(listOf("CHF"), "franc", "francs", "franken", "franco", "franchi", "francos", "פרנק", "פרנקים", "франк", "франков", "فرنك")
        add(KRONOR, "krona", "kronor", "krone", "kroner", "króna", "כתר", "כתרים", "крона", "крон")
        add(listOf("PLN"), "zloty", "zlotys", "złoty", "złotych", "זלוטי", "злотый", "злотых")
        add(listOf("HUF"), "forint", "forints", "פורינט", "форинт", "форинтов")
        add(listOf("CZK"), "koruna", "korun", "крон")
        add(DIRHAMS, "dirham", "dirhams", "דירהם", "درهم", "дирхам", "дирхамов")
        add(RIYALS, "riyal", "riyals", "rial", "rials", "ריאל", "ريال", "риял", "риялов")
        add(DINARS, "dinar", "dinars", "דינר", "דינרים", "دينار", "динар", "динаров")
        add(LIRAS, "lira", "liras", "lire", "לירה", "לירות", "ليرة", "лира", "лир")
        add(listOf("KRW"), "won", "вон", "ウォン", "韩元")
        add(listOf("VND"), "dong", "донг")
        add(listOf("MYR"), "ringgit")
        add(listOf("IDR"), "rupiah")
        add(listOf("ZAR"), "rand", "rands")
        add(listOf("UAH"), "hryvnia", "hryvnias", "гривна", "гривен", "гривні")
        add(listOf("HKD"), "港币", "港元")
        add(listOf("RON"), "leu")
    }

    /** Codes accepted in lower case too ("100 usd"); others must be upper case, so words like "all" stay words. */
    private val LOWERCASE_CODES = setOf("usd", "eur", "gbp", "ils", "nis", "jpy", "chf", "cad", "aud", "thb", "try", "rub", "inr", "cny", "aed")

    private val MARKERS: List<Marker> = (SYMBOLS.map { (k, v) -> Marker(k, v, ignoreCase = false) } +
        WORDS.map { (k, v) -> Marker(k, v, ignoreCase = true) })
        .sortedByDescending { it.text.length }

    // Indian grouping ("1,23,456"), grouped thousands ("1,234.56", "1.234,56", "1 234", "1'234") or a plain
    // number ("1234.5", "49,99"), optionally with a "k".
    private val NUMBER = Regex(
        """(?<![\d.,])(\d{1,2}(?:,\d{2})+,\d{3}(?:\.\d+)?|\d{1,3}(?:[,.\u00A0\u202F\u2009' ]\d{3})+(?:[.,]\d+)?|\d+(?:[.,]\d+)?)(?:\s?([kK])(?![\p{L}]))?"""
    )

    private const val MAX_PRICES = 8

    /**
     * The prices in [text]. When some numbers carry a currency, the bare ones (dates, counts) are skipped;
     * when none do, every number is a price in [fallback]. [known] limits codes to currencies with a rate.
     */
    fun parse(text: String, known: Set<String>, preferred: List<String>, fallback: String): List<FoundPrice> {
        val source = normalizeDigits(text)
        val found = NUMBER.findAll(source).mapNotNull { match ->
            val amount = parseNumber(match.groupValues[1]) ?: return@mapNotNull null
            val value = if (match.groupValues[2].isNotEmpty()) amount * 1000 else amount
            val before = markerBefore(source, match.range.first, known)
            val after = markerAfter(source, match.range.last + 1, known)
            // Prefer a marker touching the number, then the one in front ("USD 100 EUR 90" is a list).
            val marker = listOfNotNull(before, after).minByOrNull { (if (it.gap) 2 else 0) + (if (it === before) 0 else 1) }
            val currency = marker?.let { pick(it.candidates, preferred, known) }
            val start = if (marker != null && marker === before) marker.start else match.range.first
            val end = if (marker != null && marker === after) marker.end else match.range.last + 1
            Triple(value, currency, start until end)
        }.toList()
        val marked = found.filter { it.second != null }
        return if (marked.isNotEmpty()) {
            marked.take(MAX_PRICES).map { (amount, currency, range) -> FoundPrice(amount, currency!!, false, range) }
        } else {
            found.take(MAX_PRICES).map { (amount, _, range) -> FoundPrice(amount, fallback, true, range) }
        }
    }

    private class Hit(val candidates: List<String>, val start: Int, val end: Int, val gap: Boolean)

    private fun markerBefore(text: String, numberStart: Int, known: Set<String>): Hit? {
        var end = numberStart
        val gap = end > 0 && text[end - 1].isWhitespace()
        if (gap) end--
        val head = text.substring(0, end)
        codeAt(head.takeLast(3), known)?.let { code ->
            if (head.length == 3 || !head[head.length - 4].isLetter()) return Hit(listOf(code), end - 3, numberStart, gap)
        }
        for (m in MARKERS) {
            if (!head.endsWith(m.text, m.ignoreCase)) continue
            val start = end - m.text.length
            if (needsBoundary(m.text.first()) && start > 0 && text[start - 1].isLetter()) continue
            return Hit(m.candidates, start, numberStart, gap)
        }
        return null
    }

    private fun markerAfter(text: String, numberEnd: Int, known: Set<String>): Hit? {
        var start = numberEnd
        val gap = start < text.length && text[start].isWhitespace()
        if (gap) start++
        val tail = text.substring(start)
        codeAt(tail.take(3), known)?.let { code ->
            if (tail.length == 3 || !tail[3].isLetter()) return Hit(listOf(code), numberEnd, start + 3, gap)
        }
        for (m in MARKERS) {
            if (!tail.startsWith(m.text, m.ignoreCase)) continue
            val end = start + m.text.length
            if (needsBoundary(m.text.last()) && end < text.length && text[end].isLetter()) continue
            return Hit(m.candidates, numberEnd, end, gap)
        }
        return null
    }

    /** An ISO code such as "USD" (or a popular one in lower case) that has a rate. */
    private fun codeAt(word: String, known: Set<String>): String? {
        if (word.length != 3 || !word.all { it in 'A'..'Z' || it in 'a'..'z' }) return null
        val upper = word.uppercase(Locale.ROOT)
        if (word != upper && word !in LOWERCASE_CODES) return null
        val code = if (upper == "NIS") "ILS" else upper
        return code.takeIf { it in known.ifEmpty { currencyMap.keys } }
    }

    /** Words in scripts that put spaces between words must not be glued to other letters ("Rs" in "Rsvp"). */
    private fun needsBoundary(c: Char): Boolean = c.isLetter() && Character.UnicodeScript.of(c.code) in setOf(
        Character.UnicodeScript.LATIN, Character.UnicodeScript.CYRILLIC, Character.UnicodeScript.GREEK,
        Character.UnicodeScript.HEBREW, Character.UnicodeScript.ARABIC, Character.UnicodeScript.DEVANAGARI,
    )

    private fun pick(candidates: List<String>, preferred: List<String>, known: Set<String>): String? {
        val usable = if (known.isEmpty()) candidates else candidates.filter { it in known }
        return preferred.firstOrNull { it in usable } ?: usable.firstOrNull()
    }

    /** "1,234.56" / "1.234,56" / "1 234" / "49,99" / "1.500" (a thousand and a half: three digits after one separator). */
    internal fun parseNumber(raw: String): Double? {
        val s = raw.filterNot { it == ' ' || it == '\u00A0' || it == '\u202F' || it == '\u2009' || it == '\'' }
        val dot = s.lastIndexOf('.')
        val comma = s.lastIndexOf(',')
        val normalized = when {
            dot >= 0 && comma >= 0 -> {
                val decimal = if (dot > comma) '.' else ','
                val group = if (decimal == '.') ',' else '.'
                s.replace(group.toString(), "").replace(decimal, '.')
            }
            dot >= 0 || comma >= 0 -> {
                val sep = if (dot >= 0) '.' else ','
                val count = s.count { it == sep }
                val digitsAfter = s.length - s.lastIndexOf(sep) - 1
                val thousands = count > 1 || (digitsAfter == 3 && !s.startsWith("0$sep"))
                if (thousands) s.replace(sep.toString(), "") else s.replace(sep, '.')
            }
            else -> s
        }
        return normalized.toDoubleOrNull()
    }

    /** Arabic-Indic and Persian digits as 0-9, so "١٢٠ درهم" reads as 120. */
    private fun normalizeDigits(text: String): String = buildString(text.length) {
        for (c in text) append(
            when (c) {
                in '\u0660'..'\u0669' -> '0' + (c - '\u0660')
                in '\u06F0'..'\u06F9' -> '0' + (c - '\u06F0')
                '\u066B' -> '.'
                '\u066C' -> ','
                else -> c
            }
        )
    }
}
