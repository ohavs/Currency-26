package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickConvertTest {
    // Per US dollar.
    private val rates = mapOf("USD" to 1.0, "ILS" to 3.7, "EUR" to 0.9, "THB" to 36.0, "GBP" to 0.8)

    @Test
    fun `a foreign price shows in the target with the others beside it`() {
        val result = QuickConvert.convert("€90", rates, source = "USD", target = "ILS", extra = "GBP", local = "ILS")
        val line = result.lines.single()
        assertEquals("ILS", line.to)
        assertEquals(370.0, line.value, 1e-9)
        assertEquals(listOf("USD", "GBP"), result.others.map { it.first })
        assertEquals(100.0, result.others[0].second, 1e-9)
    }

    @Test
    fun `a price in the target currency flips to the source`() {
        val line = QuickConvert.convert("₪370", rates, "USD", "ILS", null, "ILS").lines.single()
        assertEquals("USD", line.to)
        assertEquals(100.0, line.value, 1e-9)
    }

    @Test
    fun `bare numbers abroad are in the local currency, at home in the source`() {
        assertEquals("THB", QuickConvert.convert("360", rates, "USD", "ILS", null, "THB").lines.single().price.currency)
        assertEquals("USD", QuickConvert.convert("360", rates, "USD", "ILS", null, "ILS").lines.single().price.currency)
    }

    @Test
    fun `several prices add up and can be replaced in the text`() {
        val text = "Room €90, breakfast €9"
        val result = QuickConvert.convert(text, rates, "USD", "ILS", null, null)
        assertEquals(407.0, result.total!!.second, 1e-9)
        assertEquals("Room ₪370.00, breakfast ₪37.00", result.replaceIn(text))
        assertNull(QuickConvert.convert("€90", rates, "USD", "ILS", null, null).total)
    }

    @Test
    fun `a currency chosen in the bubble comes first, the app's currencies after it`() {
        val result = QuickConvert.convert("$100", rates, "USD", "ILS", null, "ILS", chosen = "EUR")
        assertEquals("EUR", result.target)
        val line = result.lines.single()
        assertEquals("EUR", line.to)
        assertEquals(90.0, line.value, 1e-9)
        assertEquals(listOf("ILS"), result.others.map { it.first })
        // A price already in the chosen currency goes to the target.
        assertEquals("ILS", QuickConvert.convert("€90", rates, "USD", "ILS", null, "ILS", chosen = "EUR").lines.single().to)
    }

    @Test
    fun `opening in the app keeps the user's currencies where it can`() {
        // A new currency to the target: the old source makes way, the extra stays.
        assertEquals(AppCurrencies("EUR", "ILS", "GBP"), QuickConvert.appCurrencies("EUR", "ILS", "USD", "ILS", "GBP"))
        // A price in the target swaps the two.
        assertEquals(AppCurrencies("ILS", "USD", null), QuickConvert.appCurrencies("ILS", "USD", "USD", "ILS", null))
        // A price in the extra currency: the old source takes its place.
        assertEquals(AppCurrencies("GBP", "ILS", "USD"), QuickConvert.appCurrencies("GBP", "ILS", "USD", "ILS", "GBP"))
        // The extra currency chosen in the bubble: the old target takes its place.
        assertEquals(AppCurrencies("EUR", "GBP", "ILS"), QuickConvert.appCurrencies("EUR", "GBP", "USD", "ILS", "GBP"))
    }
}
