package com.example.bikeredlights.data.repository

import com.example.bikeredlights.domain.model.settings.AutoPauseConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SettingsRepository interface contract.
 *
 * Note: Full integration tests (with DataStore, persistence, app restart cycles)
 * are deferred to Phase 3-5 instrumented tests (androidTest directory).
 *
 * These tests verify domain model behavior and interface contracts
 * without requiring Android dependencies.
 */
class SettingsRepositoryTest {

    @Test
    fun `AutoPauseConfig validates thresholds correctly`() {
        // Valid thresholds should work
        val validConfig = AutoPauseConfig(enabled = true, thresholdMinutes = 5)
        assertEquals(5, validConfig.thresholdMinutes)

        // Invalid threshold should throw
        try {
            AutoPauseConfig(enabled = true, thresholdMinutes = 7)
            throw AssertionError("Expected IllegalArgumentException for invalid threshold")
        } catch (e: IllegalArgumentException) {
            assertTrue("Exception message should mention valid thresholds",
                e.message?.contains("must be one of") == true)
        }
    }

    @Test
    fun `AutoPauseConfig companion methods work correctly`() {
        // default() returns expected values
        val defaultConfig = AutoPauseConfig.default()
        assertEquals(false, defaultConfig.enabled)
        assertEquals(5, defaultConfig.thresholdMinutes)

        // fromDataStore with valid threshold
        val validConfig = AutoPauseConfig.fromDataStore(true, 10)
        assertEquals(true, validConfig.enabled)
        assertEquals(10, validConfig.thresholdMinutes)

        // fromDataStore with invalid threshold returns default
        val invalidConfig = AutoPauseConfig.fromDataStore(true, 7)
        assertEquals(false, invalidConfig.enabled)  // Returns default
        assertEquals(5, invalidConfig.thresholdMinutes)
    }

    @Test
    fun `AutoPauseConfig getThresholdMs calculates correctly`() {
        val config1 = AutoPauseConfig(enabled = true, thresholdMinutes = 1)
        assertEquals(60_000L, config1.getThresholdMs())

        val config5 = AutoPauseConfig(enabled = true, thresholdMinutes = 5)
        assertEquals(300_000L, config5.getThresholdMs())

        val config15 = AutoPauseConfig(enabled = true, thresholdMinutes = 15)
        assertEquals(900_000L, config15.getThresholdMs())
    }

    @Test
    fun `VALID_THRESHOLDS contains expected values`() {
        val expected = listOf(1, 2, 3, 5, 10, 15)
        assertEquals(expected, AutoPauseConfig.VALID_THRESHOLDS)
    }
}
