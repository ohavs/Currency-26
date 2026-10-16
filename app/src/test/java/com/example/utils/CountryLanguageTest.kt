package com.example.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CountryLanguageTest {
    @Test
    fun `countries map to the language people read there`() {
        assertEquals("he", CountryLanguage.of("IL"))
        assertEquals("he", CountryLanguage.of("il"))
        assertEquals("es", CountryLanguage.of("MX"))
        assertEquals("pt", CountryLanguage.of("BR"))
        assertEquals("ar", CountryLanguage.of("AE"))
        assertEquals("ja", CountryLanguage.of("JP"))
    }

    @Test
    fun `english speaking and multilingual countries suggest nothing`() {
        assertNull(CountryLanguage.of("US"))
        assertNull(CountryLanguage.of("GB"))
        assertNull(CountryLanguage.of("CH"))
        assertNull(CountryLanguage.of(null))
        assertNull(CountryLanguage.of(""))
    }

    @Test
    fun `suggests only a language other than the one shown`() {
        assertEquals("he", CountryLanguage.suggestion("IL", "en"))
        assertNull(CountryLanguage.suggestion("IL", "he"))
        assertNull(CountryLanguage.suggestion("US", "en"))
        assertEquals("de", CountryLanguage.suggestion("AT", "en"))
    }

    @Test
    fun `old hebrew code is normalized`() {
        assertEquals("he", CountryLanguage.normalize("iw"))
        assertEquals("fr", CountryLanguage.normalize("fr"))
    }
}
