package com.example.utils

import java.util.Locale

/** Which of the app's languages people in a country most likely read, for the first-launch suggestion. */
object CountryLanguage {
    private val BY_COUNTRY: Map<String, String> = buildMap {
        put("IL", "he")
        listOf("ES", "MX", "AR", "CO", "CL", "PE", "VE", "EC", "GT", "CU", "BO", "DO", "HN", "PY", "SV", "NI", "CR",
            "PA", "UY", "PR", "GQ").forEach { put(it, "es") }
        listOf("SA", "AE", "EG", "IQ", "JO", "KW", "LB", "LY", "MA", "OM", "QA", "SY", "TN", "YE", "BH", "DZ", "SD",
            "PS", "MR").forEach { put(it, "ar") }
        listOf("RU", "BY", "KZ", "KG").forEach { put(it, "ru") }
        listOf("FR", "MC", "SN", "CI", "CM", "ML", "BF", "NE", "TG", "BJ", "GA", "CG", "CD", "MG", "GN", "HT")
            .forEach { put(it, "fr") }
        listOf("DE", "AT", "LI").forEach { put(it, "de") }
        listOf("BR", "PT", "AO", "MZ", "CV", "GW", "ST").forEach { put(it, "pt") }
        listOf("IT", "SM", "VA").forEach { put(it, "it") }
        put("TR", "tr")
        listOf("CN", "TW", "HK", "MO").forEach { put(it, "zh") }
        put("JP", "ja")
        put("IN", "hi")
    }

    /** The language for [country], or null when it is English-speaking, multilingual or not covered. */
    fun of(country: String?): String? = country?.let { BY_COUNTRY[it.trim().uppercase(Locale.ROOT)] }

    /** Old ISO codes some Android versions still report (Hebrew as "iw" and so on). */
    fun normalize(language: String): String = when (language) {
        "iw" -> "he"
        "in" -> "id"
        "ji" -> "yi"
        else -> language
    }

    /** What to offer: the country's language when the app is shown in another one, else null. */
    fun suggestion(country: String?, shownLanguage: String): String? = of(country)?.takeIf { it != shownLanguage }
}
