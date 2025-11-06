package com.example.bikeredlights.domain.model.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AutoPauseConfig data class.
 *
 * Bug #10: Updated tests from minutes to seconds (5-60s range).
 *
 * Tests cover:
 * - Valid threshold values (5, 10, 15, 20, 30, 45, 60)
 * - Invalid threshold values (0, 4, 61, -1, etc.)
 * - Default configuration
 * - DataStore conversion
 * - Threshold milliseconds calculation
 */
class AutoPauseConfigTest {

    @Test
    fun `default config has correct values`() {
        val config = AutoPauseConfig.default()

        assertFalse("Default should be disabled", config.enabled)
        assertEquals("Default threshold should be 30 seconds", 30, config.thresholdSeconds)
    }

    @Test
    fun `valid thresholds create instances successfully`() {
        val validThresholds = listOf(5, 10, 15, 20, 30, 45, 60)

        validThresholds.forEach { threshold ->
            val config = AutoPauseConfig(enabled = true, thresholdSeconds = threshold)
            assertEquals("Threshold should be $threshold", threshold, config.thresholdSeconds)
        }
    }

    @Test
    fun `invalid threshold 0 throws IllegalArgumentException`() {
        val exception = assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = true, thresholdSeconds = 0)
        }
        assertTrue(
            "Exception message should mention valid thresholds",
            exception.message?.contains("must be one of") == true
        )
    }

    @Test
    fun `invalid threshold 4 throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = true, thresholdSeconds = 4)
        }
    }

    @Test
    fun `invalid threshold 61 throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = true, thresholdSeconds = 61)
        }
    }

    @Test
    fun `invalid negative threshold throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = false, thresholdSeconds = -1)
        }
    }

    @Test
    fun `invalid threshold 100 throws IllegalArgumentException`() {
        assertThrows(IllegalArgumentException::class.java) {
            AutoPauseConfig(enabled = true, thresholdSeconds = 100)
        }
    }

    @Test
    fun `fromDataStore with valid threshold returns correct config`() {
        val config = AutoPauseConfig.fromDataStore(enabled = true, thresholdSeconds = 30)

        assertTrue("Config should be enabled", config.enabled)
        assertEquals("Threshold should be 30", 30, config.thresholdSeconds)
    }

    @Test
    fun `fromDataStore with invalid threshold returns default`() {
        val config = AutoPauseConfig.fromDataStore(enabled = true, thresholdSeconds = 7)

        // Should return default config when threshold is invalid
        assertFalse("Should return default (disabled)", config.enabled)
        assertEquals("Should return default threshold (30)", 30, config.thresholdSeconds)
    }

    @Test
    fun `getThresholdMs calculates correct milliseconds`() {
        val config5 = AutoPauseConfig(enabled = true, thresholdSeconds = 5)
        assertEquals("5 seconds should be 5000ms", 5_000L, config5.getThresholdMs())

        val config30 = AutoPauseConfig(enabled = true, thresholdSeconds = 30)
        assertEquals("30 seconds should be 30000ms", 30_000L, config30.getThresholdMs())

        val config60 = AutoPauseConfig(enabled = true, thresholdSeconds = 60)
        assertEquals("60 seconds should be 60000ms", 60_000L, config60.getThresholdMs())
    }

    @Test
    fun `VALID_THRESHOLDS contains expected values`() {
        val expected = listOf(5, 10, 15, 20, 30, 45, 60)
        assertEquals("VALID_THRESHOLDS should match expected list", expected, AutoPauseConfig.VALID_THRESHOLDS)
    }

    @Test
    fun `enabled can be false with any valid threshold`() {
        // Threshold is saved even when disabled
        val config = AutoPauseConfig(enabled = false, thresholdSeconds = 30)

        assertFalse("Config should be disabled", config.enabled)
        assertEquals("Threshold should still be 30", 30, config.thresholdSeconds)
    }
}
