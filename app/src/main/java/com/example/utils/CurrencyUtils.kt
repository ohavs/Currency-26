package com.example.utils

data class CurrencyInfo(val code: String, val hebrewName: String, val flag: String, val symbol: String, val keywords: List<String> = emptyList())

fun getCurrencyInfo(code: String): CurrencyInfo {
    val predefined = currencyMap.entries.find { it.key.equals(code, ignoreCase = true) }?.value
    if (predefined != null) return predefined
    
    val countryCode = if (code.length >= 2) code.substring(0, 2).uppercase() else "UN"
    val flag = try {
        val firstLetter = Character.codePointAt(countryCode, 0) - 0x41 + 0x1F1E6
        val secondLetter = Character.codePointAt(countryCode, 1) - 0x41 + 0x1F1E6
        String(Character.toChars(firstLetter)) + String(Character.toChars(secondLetter))
    } catch (e: Exception) {
        "🌍"
    }
    
    val symbol = nativeSymbol(code)
    val dynamicHebrewName = try { 
        val currency = java.util.Currency.getInstance(code)
        val name = currency.getDisplayName(java.util.Locale("he", "IL"))
        if (name == code) currency.getDisplayName(java.util.Locale("iw", "IL")) else name 
    } catch (e: Exception) { 
        code 
    }
    
    return CurrencyInfo(code, dynamicHebrewName, flag, symbol)
}

/** Hebrew country (or region) name for a currency, e.g. USD -> "ארצות הברית". Falls back to the currency name. */
fun getCountryName(code: String): String {
    val predefined = currencyMap.entries.find { it.key.equals(code, ignoreCase = true) }?.value
    predefined?.keywords?.firstOrNull()?.let { return it }
    val region = if (code.length >= 2) code.substring(0, 2).uppercase() else return code
    val name = try {
        java.util.Locale.Builder().setRegion(region).build().getDisplayCountry(java.util.Locale.forLanguageTag("he"))
    } catch (e: Exception) {
        ""
    }
    return if (name.isNotBlank() && !name.equals(region, ignoreCase = true)) name else getCurrencyInfo(code).hebrewName
}

private val nativeSymbolCache = java.util.concurrent.ConcurrentHashMap<String, String>()

/** The symbol as written in the currency's home country (HUF -> "Ft"); "" when it has none besides its code. */
private fun nativeSymbol(code: String): String = nativeSymbolCache.getOrPut(code.uppercase()) {
    try {
        val currency = java.util.Currency.getInstance(code.uppercase())
        val region = code.take(2).uppercase()
        val home = java.util.Locale.getAvailableLocales().firstOrNull { it.country == region }
        val symbol = if (home != null) currency.getSymbol(home) else currency.symbol
        if (symbol.equals(code, ignoreCase = true)) "" else symbol
    } catch (e: Exception) {
        ""
    }
}

private fun java.util.Locale.isHebrew() = language == "he" || language == "iw"

/** Currency name in the UI language, e.g. USD -> "דולר אמריקאי" / "US Dollar" / "Dólar estadounidense". */
fun currencyName(code: String, locale: java.util.Locale): String {
    if (locale.isHebrew()) return getCurrencyInfo(code).hebrewName
    val name = try {
        java.util.Currency.getInstance(code).getDisplayName(locale)
    } catch (e: Exception) {
        code
    }
    return name.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
}

/** Country (or region) of a currency in the UI language, e.g. USD -> "ארצות הברית" / "United States" / "Estados Unidos". */
fun countryName(code: String, locale: java.util.Locale): String {
    if (locale.isHebrew()) return getCountryName(code)
    val region = if (code.equals("EUR", ignoreCase = true)) "EU" else code.take(2).uppercase()
    val name = try {
        java.util.Locale.Builder().setRegion(region).build().getDisplayCountry(locale)
    } catch (e: Exception) {
        ""
    }
    return if (name.isNotBlank() && !name.equals(region, ignoreCase = true)) name else currencyName(code, locale)
}

val currencyMap = mapOf(
    "USD" to CurrencyInfo("USD", "דולר אמריקאי", "🇺🇸", "$", listOf("ארצות הברית", "ארה\"ב", "דולר", "אמריקה")),
    "ILS" to CurrencyInfo("ILS", "שקל חדש", "🇮🇱", "₪", listOf("ישראל", "שקל", "שקלים", "ארץ")),
    "EUR" to CurrencyInfo("EUR", "אירו", "🇪🇺", "€", listOf("אירופה", "יורו", "איחוד", "גרמניה", "צרפת", "איטליה", "ספרד", "יוון", "הולנד", "בלגיה", "אוסטריה", "קפריסין")),
    "GBP" to CurrencyInfo("GBP", "לירה שטרלינג", "🇬🇧", "£", listOf("בריטניה", "אנגליה", "פאונד", "לשטרלינג", "לונדון")),
    "JPY" to CurrencyInfo("JPY", "ין יפני", "🇯🇵", "¥", listOf("יפן", "ין", "טוקיו")),
    "CHF" to CurrencyInfo("CHF", "פרנק שוויצרי", "🇨🇭", "Fr", listOf("שוויץ", "פרנק", "פראנק")),
    "AUD" to CurrencyInfo("AUD", "דולר אוסטרלי", "🇦🇺", "A$", listOf("אוסטרליה")),
    "CAD" to CurrencyInfo("CAD", "דולר קנדי", "🇨🇦", "C$", listOf("קנדה")),
    "CNY" to CurrencyInfo("CNY", "יואן סיני", "🇨🇳", "¥", listOf("סין", "יואן")),
    "RUB" to CurrencyInfo("RUB", "רובל רוסי", "🇷🇺", "₽", listOf("רוסיה", "רובל")),
    "INR" to CurrencyInfo("INR", "רופי הודי", "🇮🇳", "₹", listOf("הודו", "רופי")),
    "BRL" to CurrencyInfo("BRL", "ריאל ברזילאי", "🇧🇷", "R$", listOf("ברזיל", "ריאל")),
    "ZAR" to CurrencyInfo("ZAR", "ראנד דרום אפריקאי", "🇿🇦", "R", listOf("דרום אפריקה", "ראנד")),
    "JOD" to CurrencyInfo("JOD", "דינר ירדני", "🇯🇴", "JD", listOf("ירדן", "דינר")),
    "EGP" to CurrencyInfo("EGP", "לירה מצרית", "🇪🇬", "E£", listOf("מצרים", "לירה", "סיני")),
    "TRY" to CurrencyInfo("TRY", "לירה טורקית", "🇹🇷", "₺", listOf("טורקיה", "לירה", "איסטנבול")),
    "AED" to CurrencyInfo("AED", "דירהם (איחוד האמירויות)", "🇦🇪", "د.إ", listOf("איחוד האמירויות", "אמירויות", "דובאי", "אבו דאבי", "דירהם")),
    "KRW" to CurrencyInfo("KRW", "וון דרום קוריאני", "🇰🇷", "₩", listOf("דרום קוריאה", "קוריאה", "וון")),
    "MXN" to CurrencyInfo("MXN", "פסו מקסיקני", "🇲🇽", "$", listOf("מקסיקו", "פסו")),
    "SGD" to CurrencyInfo("SGD", "דולר סינגפורי", "🇸🇬", "S$", listOf("סינגפור")),
    "NZD" to CurrencyInfo("NZD", "דולר ניו זילנדי", "🇳🇿", "NZ$", listOf("ניו זילנד")),
    "CZK" to CurrencyInfo("CZK", "קורונה צ'כית", "🇨🇿", "Kč", listOf("צ'כיה", "פראג", "קורונה")),
    "PLN" to CurrencyInfo("PLN", "זלוטי פולני", "🇵🇱", "zł", listOf("פולין", "זלוטי", "ורשה")),
    "THB" to CurrencyInfo("THB", "בהט תאילנדי", "🇹🇭", "฿", listOf("תאילנד", "בהט", "בנגקוק")),
    "ZMW" to CurrencyInfo("ZMW", "קוואצ'ה זמבי", "🇿🇲", "ZK", listOf("זמביה", "קוואצ'ה"))
)
