package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PriceParserTest {
    private val known = setOf("USD", "EUR", "GBP", "ILS", "JPY", "CNY", "CAD", "AUD", "THB", "SEK", "NOK", "HUF", "INR", "CHF", "BRL", "RUB", "AED", "PLN", "ZAR")

    private fun parse(text: String, local: String? = null, fallback: String = "USD") =
        PriceParser.parse(text, known, listOfNotNull(local, "USD", "ILS"), fallback)

    private fun one(text: String, local: String? = null): Pair<Double, String> =
        parse(text, local).single().let { it.amount to it.currency }

    @Test
    fun `symbols before and after the number`() {
        assertEquals(49.99 to "USD", one("$49.99"))
        assertEquals(120.0 to "EUR", one("€120"))
        assertEquals(1250.0 to "EUR", one("1.250,00 €"))
        assertEquals(100.0 to "ILS", one("₪100"))
        assertEquals(350.0 to "THB", one("฿350"))
        assertEquals(1500.0 to "HUF", one("1 500 Ft"))
        assertEquals(19.9 to "PLN", one("19,90 zł"))
    }

    @Test
    fun `codes and names in several languages`() {
        assertEquals(1250.0 to "USD", one("USD 1,250"))
        assertEquals(80.0 to "EUR", one("80 eur"))
        assertEquals(120.0 to "EUR", one("120 euros"))
        assertEquals(100.0 to "ILS", one("100 ש״ח"))
        assertEquals(250.0 to "ILS", one("250 שקלים"))
        assertEquals(50.0 to "USD", one("ב-50 דולר"))
        assertEquals(500.0 to "RUB", one("500 руб."))
        assertEquals(100.0 to "JPY", one("100円"))
        assertEquals(100.0 to "USD", one("100美元"))
        assertEquals(120.0 to "AED", one("١٢٠ درهم"))
    }

    @Test
    fun `shared symbols follow the country you are in`() {
        assertEquals(50.0 to "USD", one("$50"))
        assertEquals(50.0 to "CAD", one("$50", local = "CAD"))
        assertEquals(200.0 to "NOK", one("200 kr", local = "NOK"))
        assertEquals(200.0 to "SEK", one("200 kr"))
        assertEquals(10.0 to "CNY", one("¥10", local = "CNY"))
        assertEquals(10.0 to "JPY", one("¥10"))
        assertEquals(30.0 to "AUD", one("A$30", local = "CAD"))
    }

    @Test
    fun `number formats`() {
        assertEquals(1234.56, PriceParser.parseNumber("1,234.56")!!, 1e-9)
        assertEquals(1234.56, PriceParser.parseNumber("1.234,56")!!, 1e-9)
        assertEquals(49.99, PriceParser.parseNumber("49,99")!!, 1e-9)
        assertEquals(1500.0, PriceParser.parseNumber("1.500")!!, 1e-9)
        assertEquals(0.5, PriceParser.parseNumber("0.500")!!, 1e-9)
        assertEquals(1234.0, PriceParser.parseNumber("1'234")!!, 1e-9)
        assertEquals(123456.0, one("₹1,23,456").first, 1e-9)
        assertEquals(1500.0, one("$1.5k").first, 1e-9)
    }

    @Test
    fun `bare numbers are skipped next to real prices`() {
        val prices = parse("Room for 2 adults: $120, breakfast $15 (3 nights)")
        assertEquals(listOf(120.0, 15.0), prices.map { it.amount })
        assertTrue(prices.none { it.assumed })
    }

    @Test
    fun `a bare number is a price in the fallback currency`() {
        val price = parse("49.99", fallback = "THB").single()
        assertEquals(49.99 to "THB", price.amount to price.currency)
        assertTrue(price.assumed)
    }

    @Test
    fun `words glued to letters are not currencies`() {
        assertTrue(parse("Elira 20", fallback = "USD").all { it.assumed })
        assertEquals("USD", one("USD100").second)
    }

    @Test
    fun `ranges cover the marker so the text can be replaced`() {
        val text = "Total: €120 today"
        val price = parse(text).single()
        assertEquals("€120", text.substring(price.range.first, price.range.last + 1))
    }
}
