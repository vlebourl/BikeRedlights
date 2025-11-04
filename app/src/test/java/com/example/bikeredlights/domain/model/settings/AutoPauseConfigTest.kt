package com.example.bikeredlights.domain.model.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AutoPauseConfig data class.
 *
 * Tests cover:
 * - Valid threshold values (1, 2, 3, 5, 10, 15)
 * - Invalid threshold values (0, 4, 16, -1, etc.)
 * - Default configuration
 * - DataStore conversion
 * - Threshold milliseconds calculation
 */
class AutoPauseConfigTest {

    @Test
    fun `default config has correct values`() {
        val config = AutoPauseConfig.default()

        assertFalse("Default should be disabled", config.enabled)
        assertEquals("Default threshold should be 5 minutes", 5, config.thresholdMinutes)
    }

    @Test
    fun `valid thresholds create instances successfully`() {
        val validThresholds = listOf(1, 2, 3, 5, 10, 15)

        validThresholds.forEach { threshold ->
            val config = AutoPauseConfig(enabled = true, thresholdMinutes = threshold)
            assertEquals("Threshold should be $threshold", threshold, config.thresholdMinutes)
        }
    }

    @Test
    fun `invalid threshold 0 throws IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = true, thresholdMinutes = 0)
        }
        assertTrue(
            "Exception message should mention valid thresholds",
            exception.message?.contains("must be one of") == true
        )
    }

    @Test
    fun `invalid threshold 4 throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = true, thresholdMinutes = 4)
        }
    }

    @Test
    fun `invalid threshold 16 throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = true, thresholdMinutes = 16)
        }
    }

    @Test
    fun `invalid negative threshold throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = false, thresholdMinutes = -1)
        }
    }

    @Test
    fun `invalid threshold 100 throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = true, thresholdMinutes = 100)
        }
    }

    @Test
    fun `fromDataStore with valid threshold returns correct config`() {
        val config = AutoPauseConfig.fromDataStore(enabled = true, thresholdMinutes = 10)

        assertTrue("Config should be enabled", config.enabled)
        assertEquals("Threshold should be 10", 10, config.thresholdMinutes)
    }

    @Test
    fun `fromDataStore with invalid threshold returns default`() {
        val config = AutoPauseConfig.fromDataStore(enabled = true, thresholdMinutes = 7)

        // Should return default config when threshold is invalid
        assertFalse("Should return default (disabled)", config.enabled)
        assertEquals("Should return default threshold (5)", 5, config.thresholdMinutes)
    }

    @Test
    fun `getThresholdMs calculates correct milliseconds`() {
        val config1 = AutoPauseConfig(enabled = true, thresholdMinutes = 1)
        assertEquals("1 minute should be 60000ms", 60_000L, config1.getThresholdMs())

        val config5 = AutoPauseConfig(enabled = true, thresholdMinutes = 5)
        assertEquals("5 minutes should be 300000ms", 300_000L, config5.getThresholdMs())

        val config15 = AutoPauseConfig(enabled = true, thresholdMinutes = 15)
        assertEquals("15 minutes should be 900000ms", 900_000L, config15.getThresholdMs())
    }

    @Test
    fun `VALID_THRESHOLDS contains expected values`() {
        val expected = listOf(1, 2, 3, 5, 10, 15)
        assertEquals("VALID_THRESHOLDS should match expected list", expected, AutoPauseConfig.VALID_THRESHOLDS)
    }

    @Test
    fun `enabled can be false with any valid threshold`() {
        // Threshold is saved even when disabled
        val config = AutoPauseConfig(enabled = false, thresholdMinutes = 10)

        assertFalse("Config should be disabled", config.enabled)
        assertEquals("Threshold should still be 10", 10, config.thresholdMinutes)
    }
}
