package com.example.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SymbolPlacementTest {

  @Test
  fun `sign-like symbols lead the amount`() {
    listOf("$", "€", "£", "₪", "¥", "฿", "₺", "R$").forEach { assertTrue(it, symbolLeads(it)) }
  }

  @Test
  fun `lettered symbols and codes follow the amount`() {
    listOf("Ft", "zł", "Kč", "kr", "CHF", "د.إ").forEach { assertFalse(it, symbolLeads(it)) }
  }
}
