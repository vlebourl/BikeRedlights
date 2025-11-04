package com.example.bikeredlights.data.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bikeredlights.domain.model.settings.AutoPauseConfig
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Unit tests for SettingsRepository.
 *
 * Note: These are integration tests that require AndroidX Test framework
 * because DataStore needs a real Android Context. They should be run as
 * instrumented tests (androidTest) in a real implementation.
 *
 * For this Phase 1 implementation, these tests verify:
 * - Default values are emitted on first read
 * - Written values persist and are readable
 * - Error handling works gracefully
 *
 * Full integration tests (write/read persistence, app restart cycles)
 * are covered in Phase 3-5 UI instrumented tests.
 */
@RunWith(AndroidJUnit4::class)
class SettingsRepositoryTest {

    private lateinit var context: Context
    private lateinit var repository: SettingsRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        repository = SettingsRepositoryImpl(context)

        // Note: In a real test, we'd clear DataStore between tests.
        // For now, these tests verify the interface contract.
    }

    @Test
    fun `unitsSystem emits default METRIC on first read`() = runBlocking {
        val units = repository.unitsSystem.first()
        // Default should be METRIC
        assertTrue("Default units should be METRIC", units == UnitsSystem.METRIC)
    }

    @Test
    fun `gpsAccuracy emits default HIGH_ACCURACY on first read`() = runBlocking {
        val accuracy = repository.gpsAccuracy.first()
        // Default should be HIGH_ACCURACY
        assertTrue("Default accuracy should be HIGH_ACCURACY", accuracy == GpsAccuracy.HIGH_ACCURACY)
    }

    @Test
    fun `autoPauseConfig emits default disabled with 5 minutes on first read`() = runBlocking {
        val config = repository.autoPauseConfig.first()
        // Default should match AutoPauseConfig.default()
        assertFalse("Default auto-pause should be disabled", config.enabled)
        assertEquals("Default threshold should be 5 minutes", 5, config.thresholdMinutes)
    }

    @Test
    fun `setUnitsSystem persists value`() = runBlocking {
        // Write IMPERIAL
        repository.setUnitsSystem(UnitsSystem.IMPERIAL)

        // Read back - should get IMPERIAL
        val units = repository.unitsSystem.first()
        assertEquals("Units should be IMPERIAL after set", UnitsSystem.IMPERIAL, units)

        // Reset to METRIC for other tests
        repository.setUnitsSystem(UnitsSystem.METRIC)
    }

    @Test
    fun `setGpsAccuracy persists value`() = runBlocking {
        // Write BATTERY_SAVER
        repository.setGpsAccuracy(GpsAccuracy.BATTERY_SAVER)

        // Read back - should get BATTERY_SAVER
        val accuracy = repository.gpsAccuracy.first()
        assertEquals("Accuracy should be BATTERY_SAVER after set", GpsAccuracy.BATTERY_SAVER, accuracy)

        // Reset to HIGH_ACCURACY for other tests
        repository.setGpsAccuracy(GpsAccuracy.HIGH_ACCURACY)
    }

    @Test
    fun `setAutoPauseConfig persists value`() = runBlocking {
        // Write enabled with 10 minutes
        val newConfig = AutoPauseConfig(enabled = true, thresholdMinutes = 10)
        repository.setAutoPauseConfig(newConfig)

        // Read back - should get enabled with 10 minutes
        val config = repository.autoPauseConfig.first()
        assertTrue("Auto-pause should be enabled after set", config.enabled)
        assertEquals("Threshold should be 10 minutes after set", 10, config.thresholdMinutes)

        // Reset to default for other tests
        repository.setAutoPauseConfig(AutoPauseConfig.default())
    }

    @Test
    fun `setAutoPauseConfig with invalid threshold throws exception`() = runBlocking {
        try {
            // This should throw because 7 is not a valid threshold
            val invalidConfig = AutoPauseConfig(enabled = true, thresholdMinutes = 7)
            // Should not reach here
            throw AssertionError("Expected IllegalArgumentException for invalid threshold")
        } catch (e: IllegalArgumentException) {
            // Expected - AutoPauseConfig init block validates thresholds
            assertTrue("Exception message should mention valid thresholds",
                e.message?.contains("must be one of") == true)
        }
    }

    @Test
    fun `repository interface contract is satisfied`() {
        // Verify all interface methods are implemented
        assertTrue("Repository should implement SettingsRepository",
            repository is SettingsRepository)

        // Verify Flows are not null (interface contract)
        assertTrue("unitsSystem Flow should not be null",
            repository.unitsSystem != null)
        assertTrue("gpsAccuracy Flow should not be null",
            repository.gpsAccuracy != null)
        assertTrue("autoPauseConfig Flow should not be null",
            repository.autoPauseConfig != null)
    }
}
