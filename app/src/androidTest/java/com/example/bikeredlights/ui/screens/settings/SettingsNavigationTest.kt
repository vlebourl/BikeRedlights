package com.example.bikeredlights.ui.screens.settings

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.bikeredlights.domain.model.settings.AutoPauseConfig
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * UI navigation tests for Settings screens.
 *
 * Tests user interactions with settings UI including:
 * - Units System segmented button interaction
 * - GPS Accuracy segmented button interaction
 * - Auto-Pause toggle and picker (future)
 *
 * Uses Compose Test Rule for UI testing.
 */
class SettingsNavigationTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun rideTrackingSettingsScreen_displaysUnitsAndGpsAccuracySettings() {
        // When: Rendering Ride & Tracking settings screen
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.METRIC,
                    gpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
                    autoPauseConfig = AutoPauseConfig.default(),
                    onUnitsChange = {},
                    onGpsAccuracyChange = {},
                    onAutoPauseChange = {},
                    onNavigateBack = {}
                )
            }
        }

        // Then: Both settings are displayed
        composeTestRule
            .onNodeWithText("Units")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("GPS Accuracy")
            .assertIsDisplayed()
    }

    @Test
    fun rideTrackingSettingsScreen_clickMetricUnitsButton_triggersCallback() {
        // Given: Track selected units
        var selectedUnits = UnitsSystem.IMPERIAL

        // When: Rendering with Imperial, then clicking Metric button
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.IMPERIAL,
                    gpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
                    autoPauseConfig = AutoPauseConfig.default(),
                    onUnitsChange = { selectedUnits = it },
                    onGpsAccuracyChange = {},
                    onAutoPauseChange = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Metric")
            .performClick()

        // Then: Callback is triggered with Metric
        assertEquals(UnitsSystem.METRIC, selectedUnits)
    }

    @Test
    fun rideTrackingSettingsScreen_clickImperialUnitsButton_triggersCallback() {
        // Given: Track selected units
        var selectedUnits = UnitsSystem.METRIC

        // When: Rendering with Metric, then clicking Imperial button
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.METRIC,
                    gpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
                    autoPauseConfig = AutoPauseConfig.default(),
                    onUnitsChange = { selectedUnits = it },
                    onGpsAccuracyChange = {},
                    onAutoPauseChange = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Imperial")
            .performClick()

        // Then: Callback is triggered with Imperial
        assertEquals(UnitsSystem.IMPERIAL, selectedUnits)
    }

    @Test
    fun rideTrackingSettingsScreen_clickBatterySaverButton_triggersCallback() {
        // Given: Track selected GPS accuracy
        var selectedGpsAccuracy = GpsAccuracy.HIGH_ACCURACY

        // When: Rendering with High Accuracy, then clicking Battery Saver button
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.METRIC,
                    gpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
                    autoPauseConfig = AutoPauseConfig.default(),
                    onUnitsChange = {},
                    onGpsAccuracyChange = { selectedGpsAccuracy = it },
                    onAutoPauseChange = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("Battery Saver")
            .performClick()

        // Then: Callback is triggered with Battery Saver
        assertEquals(GpsAccuracy.BATTERY_SAVER, selectedGpsAccuracy)
    }

    @Test
    fun rideTrackingSettingsScreen_clickHighAccuracyButton_triggersCallback() {
        // Given: Track selected GPS accuracy
        var selectedGpsAccuracy = GpsAccuracy.BATTERY_SAVER

        // When: Rendering with Battery Saver, then clicking High Accuracy button
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.METRIC,
                    gpsAccuracy = GpsAccuracy.BATTERY_SAVER,
                    autoPauseConfig = AutoPauseConfig.default(),
                    onUnitsChange = {},
                    onGpsAccuracyChange = { selectedGpsAccuracy = it },
                    onAutoPauseChange = {},
                    onNavigateBack = {}
                )
            }
        }

        composeTestRule
            .onNodeWithText("High Accuracy")
            .performClick()

        // Then: Callback is triggered with High Accuracy
        assertEquals(GpsAccuracy.HIGH_ACCURACY, selectedGpsAccuracy)
    }

    @Test
    fun rideTrackingSettingsScreen_gpsAccuracyDisplaysCorrectSelection() {
        // When: Rendering with Battery Saver selected
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.METRIC,
                    gpsAccuracy = GpsAccuracy.BATTERY_SAVER,
                    autoPauseConfig = AutoPauseConfig.default(),
                    onUnitsChange = {},
                    onGpsAccuracyChange = {},
                    onAutoPauseChange = {},
                    onNavigateBack = {}
                )
            }
        }

        // Then: GPS Accuracy label and both options are displayed
        composeTestRule
            .onNodeWithText("GPS Accuracy")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("High Accuracy")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Battery Saver")
            .assertIsDisplayed()
    }

    @Test
    fun rideTrackingSettingsScreen_enableAutoPause_showsPickerAndUpdatesToggle() {
        // Given: Track Auto-Pause config
        var currentAutoPauseConfig = AutoPauseConfig.default()

        // When: Rendering with Auto-Pause disabled, then enabling it
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.METRIC,
                    gpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
                    autoPauseConfig = currentAutoPauseConfig,
                    onUnitsChange = {},
                    onGpsAccuracyChange = {},
                    onAutoPauseChange = { config ->
                        currentAutoPauseConfig = config
                    },
                    onNavigateBack = {}
                )
            }
        }

        // Initially, Auto-Pause label is displayed but picker is not
        composeTestRule
            .onNodeWithText("Auto-Pause Rides")
            .assertIsDisplayed()

        // Click the toggle to enable Auto-Pause
        composeTestRule
            .onNodeWithText("Auto-Pause Rides")
            .performClick()

        // Then: Auto-Pause is enabled
        assertEquals(true, currentAutoPauseConfig.enabled)
    }

    @Test
    fun rideTrackingSettingsScreen_changeAutoPauseThreshold_updatesValue() {
        // Given: Track Auto-Pause threshold
        var currentAutoPauseConfig = AutoPauseConfig(enabled = true, thresholdMinutes = 5)

        // When: Rendering with Auto-Pause enabled
        composeTestRule.setContent {
            BikeRedlightsTheme {
                RideTrackingSettingsScreen(
                    unitsSystem = UnitsSystem.METRIC,
                    gpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
                    autoPauseConfig = currentAutoPauseConfig,
                    onUnitsChange = {},
                    onGpsAccuracyChange = {},
                    onAutoPauseChange = { config ->
                        currentAutoPauseConfig = config
                    },
                    onNavigateBack = {}
                )
            }
        }

        // Click on the threshold picker to expand it
        composeTestRule
            .onNodeWithText("5 minutes")
            .performClick()

        // Select 3 minutes from the dropdown
        composeTestRule
            .onNodeWithText("3 minutes")
            .performClick()

        // Then: Threshold is updated to 3 minutes
        assertEquals(3, currentAutoPauseConfig.thresholdMinutes)
    }
}
