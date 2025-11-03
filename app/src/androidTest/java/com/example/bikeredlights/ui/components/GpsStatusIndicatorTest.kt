package com.example.bikeredlights.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.bikeredlights.domain.model.GpsStatus
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for GpsStatusIndicator composable.
 *
 * Tests GPS status display with color-coded indicators:
 * - Correct text and color for each GpsStatus state
 * - Accuracy value displayed for Active state
 * - Semantics content description correct for each state
 *
 * Uses Compose Test Rule for UI testing with different GpsStatus values.
 */
class GpsStatusIndicatorTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun gpsStatusIndicator_displaysUnavailableStatus() {
        // Given: GPS unavailable
        val gpsStatus = GpsStatus.Unavailable

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = gpsStatus)
            }
        }

        // Then: "GPS Unavailable" displayed (red)
        composeTestRule
            .onNodeWithText("GPS Unavailable")
            .assertIsDisplayed()
    }

    @Test
    fun gpsStatusIndicator_displaysAcquiringStatus() {
        // Given: GPS acquiring signal
        val gpsStatus = GpsStatus.Acquiring

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = gpsStatus)
            }
        }

        // Then: "Acquiring GPS..." displayed (yellow/orange)
        composeTestRule
            .onNodeWithText("Acquiring GPS...")
            .assertIsDisplayed()
    }

    @Test
    fun gpsStatusIndicator_displaysActiveStatusWithAccuracy() {
        // Given: GPS active with accuracy
        val gpsStatus = GpsStatus.Active(accuracy = 5.2f)

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = gpsStatus)
            }
        }

        // Then: "GPS Active" with accuracy displayed (green)
        composeTestRule
            .onNodeWithText("GPS Active (±5.2m)")
            .assertIsDisplayed()
    }

    @Test
    fun gpsStatusIndicator_displaysActiveStatusWithoutAccuracy() {
        // Given: GPS active with zero accuracy
        val gpsStatus = GpsStatus.Active(accuracy = 0f)

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = gpsStatus)
            }
        }

        // Then: "GPS Active" without accuracy displayed
        composeTestRule
            .onNodeWithText("GPS Active")
            .assertIsDisplayed()
    }

    @Test
    fun gpsStatusIndicator_hasAccessibilityDescriptionForUnavailable() {
        // Given: GPS unavailable
        val gpsStatus = GpsStatus.Unavailable

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = gpsStatus)
            }
        }

        // Then: Accessibility content description exists
        composeTestRule
            .onNodeWithContentDescription("GPS signal unavailable")
            .assertExists()
    }

    @Test
    fun gpsStatusIndicator_hasAccessibilityDescriptionForAcquiring() {
        // Given: GPS acquiring
        val gpsStatus = GpsStatus.Acquiring

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = gpsStatus)
            }
        }

        // Then: Accessibility content description exists
        composeTestRule
            .onNodeWithContentDescription("Acquiring GPS signal")
            .assertExists()
    }

    @Test
    fun gpsStatusIndicator_hasAccessibilityDescriptionForActiveWithAccuracy() {
        // Given: GPS active with accuracy
        val gpsStatus = GpsStatus.Active(accuracy = 8.5f)

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = gpsStatus)
            }
        }

        // Then: Accessibility content description includes accuracy
        composeTestRule
            .onNodeWithContentDescription("GPS signal active with accuracy 8 meters")
            .assertExists()
    }

    @Test
    fun gpsStatusIndicator_updatesWhenStatusChanges() {
        // Given: Initial GPS acquiring status
        val statusAcquiring = GpsStatus.Acquiring

        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = statusAcquiring)
            }
        }

        // Then: Acquiring displayed
        composeTestRule
            .onNodeWithText("Acquiring GPS...")
            .assertIsDisplayed()

        // When: Status changes to active
        val statusActive = GpsStatus.Active(accuracy = 10f)

        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = statusActive)
            }
        }

        // Then: Active status displayed
        composeTestRule
            .onNodeWithText("GPS Active (±10.0m)")
            .assertIsDisplayed()
    }

    @Test
    fun gpsStatusIndicator_handlesHighAccuracyValues() {
        // Given: GPS active with high accuracy value
        val gpsStatus = GpsStatus.Active(accuracy = 50.8f)

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = gpsStatus)
            }
        }

        // Then: High accuracy value displayed correctly
        composeTestRule
            .onNodeWithText("GPS Active (±50.8m)")
            .assertIsDisplayed()
    }

    @Test
    fun gpsStatusIndicator_handlesLowAccuracyValues() {
        // Given: GPS active with low accuracy value
        val gpsStatus = GpsStatus.Active(accuracy = 2.1f)

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                GpsStatusIndicator(gpsStatus = gpsStatus)
            }
        }

        // Then: Low accuracy value displayed correctly
        composeTestRule
            .onNodeWithText("GPS Active (±2.1m)")
            .assertIsDisplayed()
    }
}
