package com.example.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VersionCompareTest {

  @Test
  fun `higher patch, minor and major are newer`() {
    assertTrue(isNewerVersion("5.0.2", "5.0.1"))
    assertTrue(isNewerVersion("5.1.0", "5.0.9"))
    assertTrue(isNewerVersion("6.0.0", "5.9.9"))
    assertTrue(isNewerVersion("5.0.10", "5.0.9"))
  }

  @Test
  fun `same or older is not newer`() {
    assertFalse(isNewerVersion("5.0.1", "5.0.1"))
    assertFalse(isNewerVersion("5.0.1", "5.0.2"))
    assertFalse(isNewerVersion("4.0", "5.0.0"))
  }

  @Test
  fun `tag prefix, suffixes and missing parts`() {
    assertTrue(isNewerVersion("v5.0.3", "5.0.2"))
    assertTrue(isNewerVersion("5.0.1", "5.0.0-dev"))
    assertFalse(isNewerVersion("5.0", "5.0.0"))
    assertFalse(isNewerVersion("latest", "5.0.0"))
  }
}
