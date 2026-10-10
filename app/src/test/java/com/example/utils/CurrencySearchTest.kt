package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencySearchTest {

  private val codes = listOf("AUD", "CAD", "CHF", "EUR", "GBP", "HUF", "ILS", "JPY", "THB", "USD")

  private fun first(query: String) = CurrencySearch.search(codes, query).firstOrNull()

  private fun finds(query: String, code: String) =
    assertTrue("'$query' should find $code", code in CurrencySearch.search(codes, query))

  @Test
  fun `countries that use a currency find it in every language`() {
    finds("גרמניה", "EUR")
    finds("germany", "EUR")
    finds("Alemania", "EUR")
    finds("ecuador", "USD")
    finds("הונגריה", "HUF")
    finds("hungría", "HUF")
  }

  @Test
  fun `currency names match in any language and without accents`() {
    finds("dolar", "USD")
    finds("dolar", "CAD")
    finds("פורינט", "HUF")
    finds("forint", "HUF")
    finds("Pound", "GBP")
    finds("פאונד", "GBP")
  }

  @Test
  fun `well-known currencies come first among equal matches`() {
    assertEquals("USD", first("dolar"))
    assertEquals("USD", first("dollar"))
  }

  @Test
  fun `every word of the query has to match`() {
    assertEquals("CAD", first("canadian dollar"))
    assertEquals("CAD", first("דולר קנדי"))
    assertEquals(listOf("CAD"), CurrencySearch.search(codes, "dolar canadá"))
  }

  @Test
  fun `codes, symbols and everyday names`() {
    assertEquals("HUF", first("huf"))
    assertEquals("EUR", first("€"))
    finds("₪", "ILS")
    finds("ש\"ח", "ILS")
    finds("nis", "ILS")
    finds("ארה״ב", "USD")
    finds("ארהב", "USD")
  }

  @Test
  fun `a blank query keeps everything in order`() {
    assertEquals(codes, CurrencySearch.search(codes, "  "))
  }

  @Test
  fun `normalizing ignores case, accents, quotes and final letters`() {
    assertEquals("dolar canada", CurrencySearch.normalize("Dólar  Canadá"))
    assertEquals("שח", CurrencySearch.normalize("ש\"ח"))
    assertEquals("ינ יפני", CurrencySearch.normalize("ין יפני"))
  }
}
