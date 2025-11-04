package com.example.bikeredlights.ui.screens.settings

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bikeredlights.data.repository.SettingsRepositoryImpl
import com.example.bikeredlights.domain.model.settings.AutoPauseConfig
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for Settings persistence using DataStore.
 *
 * Tests that settings changes persist across:
 * - App restarts (simulated by recreating ViewModel)
 * - DataStore read/write operations
 * - Default values when no preferences exist
 *
 * Uses real DataStore (not mocked) for integration testing.
 */
@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var context: Context
    private lateinit var settingsRepository: SettingsRepositoryImpl

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        settingsRepository = SettingsRepositoryImpl(context)
    }

    @Test
    fun gpsAccuracy_defaultsToHighAccuracy() = runTest {
        // When: Reading GPS accuracy without setting it
        val gpsAccuracy = settingsRepository.gpsAccuracy.first()

        // Then: Default is HIGH_ACCURACY
        assertEquals(GpsAccuracy.HIGH_ACCURACY, gpsAccuracy)
    }

    @Test
    fun gpsAccuracy_persistsBatterySaverAcrossReads() = runTest {
        // Given: Set GPS accuracy to Battery Saver
        settingsRepository.setGpsAccuracy(GpsAccuracy.BATTERY_SAVER)

        // When: Creating new repository instance (simulates app restart)
        val newRepository = SettingsRepositoryImpl(context)
        val persistedGpsAccuracy = newRepository.gpsAccuracy.first()

        // Then: Battery Saver persists
        assertEquals(GpsAccuracy.BATTERY_SAVER, persistedGpsAccuracy)
    }

    @Test
    fun gpsAccuracy_persistsHighAccuracyAcrossReads() = runTest {
        // Given: Set GPS accuracy to High Accuracy explicitly
        settingsRepository.setGpsAccuracy(GpsAccuracy.HIGH_ACCURACY)

        // When: Creating new repository instance
        val newRepository = SettingsRepositoryImpl(context)
        val persistedGpsAccuracy = newRepository.gpsAccuracy.first()

        // Then: High Accuracy persists
        assertEquals(GpsAccuracy.HIGH_ACCURACY, persistedGpsAccuracy)
    }

    @Test
    fun unitsSystem_defaultsToMetric() = runTest {
        // When: Reading units system without setting it
        val unitsSystem = settingsRepository.unitsSystem.first()

        // Then: Default is METRIC
        assertEquals(UnitsSystem.METRIC, unitsSystem)
    }

    @Test
    fun unitsSystem_persistsImperialAcrossReads() = runTest {
        // Given: Set units to Imperial
        settingsRepository.setUnitsSystem(UnitsSystem.IMPERIAL)

        // When: Creating new repository instance
        val newRepository = SettingsRepositoryImpl(context)
        val persistedUnits = newRepository.unitsSystem.first()

        // Then: Imperial persists
        assertEquals(UnitsSystem.IMPERIAL, persistedUnits)
    }

    @Test
    fun unitsSystem_persistsMetricAcrossReads() = runTest {
        // Given: Set units to Metric explicitly
        settingsRepository.setUnitsSystem(UnitsSystem.METRIC)

        // When: Creating new repository instance
        val newRepository = SettingsRepositoryImpl(context)
        val persistedUnits = newRepository.unitsSystem.first()

        // Then: Metric persists
        assertEquals(UnitsSystem.METRIC, persistedUnits)
    }

    @Test
    fun multipleSettings_persistIndependently() = runTest {
        // Given: Set both Units and GPS Accuracy
        settingsRepository.setUnitsSystem(UnitsSystem.IMPERIAL)
        settingsRepository.setGpsAccuracy(GpsAccuracy.BATTERY_SAVER)

        // When: Creating new repository instance
        val newRepository = SettingsRepositoryImpl(context)
        val persistedUnits = newRepository.unitsSystem.first()
        val persistedGpsAccuracy = newRepository.gpsAccuracy.first()

        // Then: Both settings persist independently
        assertEquals(UnitsSystem.IMPERIAL, persistedUnits)
        assertEquals(GpsAccuracy.BATTERY_SAVER, persistedGpsAccuracy)
    }

    @Test
    fun gpsAccuracy_uiChangePersistsInDataStore() = runTest {
        // Given: Initial GPS accuracy is High Accuracy
        var currentGpsAccuracy = GpsAccuracy.HIGH_ACCURACY

        // When: Rendering UI and clicking Battery Saver
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.METRIC,
                    gpsAccuracy = currentGpsAccuracy,
                    autoPauseConfig = AutoPauseConfig.default(),
                    onUnitsChange = {},
                    onGpsAccuracyChange = { newGpsAccuracy ->
                        currentGpsAccuracy = newGpsAccuracy
                        runBlocking {
                            settingsRepository.setGpsAccuracy(newGpsAccuracy)
                        }
                    },
                    onAutoPauseChange = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Battery Saver")
            .performClick()

        // Then: Setting persists in DataStore
        val persistedGpsAccuracy = settingsRepository.gpsAccuracy.first()
        assertEquals(GpsAccuracy.BATTERY_SAVER, persistedGpsAccuracy)
    }

    @Test
    fun unitsSystem_uiChangePersistsInDataStore() = runTest {
        // Given: Initial units system is Metric
        var currentUnits = UnitsSystem.METRIC

        // When: Rendering UI and clicking Imperial
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = currentUnits,
                    gpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
                    autoPauseConfig = AutoPauseConfig.default(),
                    onUnitsChange = { newUnits ->
                        currentUnits = newUnits
                        runBlocking {
                            settingsRepository.setUnitsSystem(newUnits)
                        }
                    },
                    onGpsAccuracyChange = {},
                    onAutoPauseChange = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Imperial")
            .performClick()

        // Then: Setting persists in DataStore
        val persistedUnits = settingsRepository.unitsSystem.first()
        assertEquals(UnitsSystem.IMPERIAL, persistedUnits)
    }

    @Test
    fun autoPauseConfig_defaultsToDisabled() = runTest {
        // When: Reading auto-pause config without setting it
        val autoPauseConfig = settingsRepository.autoPauseConfig.first()

        // Then: Default is disabled with 5 minute threshold
        assertEquals(false, autoPauseConfig.enabled)
        assertEquals(5, autoPauseConfig.thresholdMinutes)
    }

    @Test
    fun autoPauseConfig_persistsEnabledAcrossReads() = runTest {
        // Given: Enable Auto-Pause with 10 minute threshold
        val config = AutoPauseConfig(enabled = true, thresholdMinutes = 10)
        settingsRepository.setAutoPauseConfig(config)

        // When: Creating new repository instance (simulates app restart)
        val newRepository = SettingsRepositoryImpl(context)
        val persistedConfig = newRepository.autoPauseConfig.first()

        // Then: Auto-Pause enabled with 10 minutes persists
        assertEquals(true, persistedConfig.enabled)
        assertEquals(10, persistedConfig.thresholdMinutes)
    }

    @Test
    fun autoPauseConfig_persistsDisabledWithCustomThreshold() = runTest {
        // Given: Disable Auto-Pause but set custom threshold
        val config = AutoPauseConfig(enabled = false, thresholdMinutes = 3)
        settingsRepository.setAutoPauseConfig(config)

        // When: Creating new repository instance
        val newRepository = SettingsRepositoryImpl(context)
        val persistedConfig = newRepository.autoPauseConfig.first()

        // Then: Both enabled state and threshold persist
        assertEquals(false, persistedConfig.enabled)
        assertEquals(3, persistedConfig.thresholdMinutes)
    }

    @Test
    fun autoPauseConfig_uiChangePersistsInDataStore() = runTest {
        // Given: Initial Auto-Pause disabled
        var currentAutoPauseConfig = AutoPauseConfig.default()

        // When: Rendering UI and enabling Auto-Pause with 10 minutes
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.METRIC,
                    gpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
                    autoPauseConfig = currentAutoPauseConfig,
                    onUnitsChange = {},
                    onGpsAccuracyChange = {},
                    onAutoPauseChange = { newConfig ->
                        currentAutoPauseConfig = newConfig
                        runBlocking {
                            settingsRepository.setAutoPauseConfig(newConfig)
                        }
                    },
                    onNavigateBack = {}
                )
            }
        }

        // Enable Auto-Pause by clicking the toggle
        composeTestRule
            .onNodeWithText("Auto-Pause Rides")
            .performClick()

        // Change threshold to 10 minutes
        composeTestRule
            .onNodeWithText("5 minutes")
            .performClick()
        composeTestRule
            .onNodeWithText("10 minutes")
            .performClick()

        // Then: Setting persists in DataStore
        val persistedConfig = settingsRepository.autoPauseConfig.first()
        assertEquals(true, persistedConfig.enabled)
        assertEquals(10, persistedConfig.thresholdMinutes)
    }
}
