package com.example.bikeredlights.ui.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.bikeredlights.domain.model.LocationData
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for LocationDisplay composable.
 *
 * Tests coordinate display formatting and GPS acquisition states:
 * - Coordinates displayed with 6 decimal places
 * - Accuracy shown when available
 * - "Acquiring GPS..." shown when locationData null
 * - Coordinate updates when locationData changes
 *
 * Uses Compose Test Rule for UI testing.
 */
class LocationDisplayTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun locationDisplay_displaysCoordinatesWithSixDecimalPlaces() {
        // Given: Location with specific coordinates
        val locationData = LocationData(
            latitude = 37.774929,
            longitude = -122.419416,
            accuracy = 10.5f,
            timestamp = System.currentTimeMillis(),
            speedMps = null,
            bearing = null
        )

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                LocationDisplay(locationData = locationData)
            }
        }

        // Then: Coordinates displayed with 6 decimal precision
        composeTestRule
            .onNodeWithText("Lat: 37.774929")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Lng: -122.419416")
            .assertIsDisplayed()
    }

    @Test
    fun locationDisplay_displaysAccuracyWhenAvailable() {
        // Given: Location with accuracy
        val locationData = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 8.3f,
            timestamp = System.currentTimeMillis(),
            speedMps = null,
            bearing = null
        )

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                LocationDisplay(locationData = locationData)
            }
        }

        // Then: Accuracy displayed in meters
        composeTestRule
            .onNodeWithText("±8.3 m")
            .assertIsDisplayed()
    }

    @Test
    fun locationDisplay_showsAcquiringWhenLocationIsNull() {
        // Given: No location data
        val locationData: LocationData? = null

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                LocationDisplay(locationData = locationData)
            }
        }

        // Then: "Acquiring GPS..." displayed
        composeTestRule
            .onNodeWithText("Acquiring GPS...")
            .assertIsDisplayed()
    }

    @Test
    fun locationDisplay_updatesWhenLocationDataChanges() {
        // Given: Initial location
        val location1 = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 10f,
            timestamp = 1000L,
            speedMps = null,
            bearing = null
        )

        composeTestRule.setContent {
            BikeRedlightsTheme {
                LocationDisplay(locationData = location1)
            }
        }

        // Then: First location displayed
        composeTestRule
            .onNodeWithText("Lat: 37.774900")
            .assertIsDisplayed()

        // When: Location changes
        val location2 = LocationData(
            latitude = 37.7758,
            longitude = -122.4195,
            accuracy = 5f,
            timestamp = 2000L,
            speedMps = null,
            bearing = null
        )

        composeTestRule.setContent {
            BikeRedlightsTheme {
                LocationDisplay(locationData = location2)
            }
        }

        // Then: Updated location displayed
        composeTestRule
            .onNodeWithText("Lat: 37.775800")
            .assertIsDisplayed()
    }

    @Test
    fun locationDisplay_hasAccessibilityContentDescription() {
        // Given: Location data
        val locationData = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 12.5f,
            timestamp = System.currentTimeMillis(),
            speedMps = null,
            bearing = null
        )

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                LocationDisplay(locationData = locationData)
            }
        }

        // Then: Accessibility content description exists
        composeTestRule
            .onNodeWithContentDescription("Latitude: 37.774900, Longitude: -122.419400, Accuracy: 12 meters")
            .assertExists()
    }

    @Test
    fun locationDisplay_hasAccessibilityContentDescriptionForAcquiring() {
        // Given: No location
        val locationData: LocationData? = null

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                LocationDisplay(locationData = locationData)
            }
        }

        // Then: Accessibility content description for acquiring state
        composeTestRule
            .onNodeWithContentDescription("GPS position: Acquiring signal")
            .assertExists()
    }

    @Test
    fun locationDisplay_handlesNegativeCoordinates() {
        // Given: Location with negative coordinates
        val locationData = LocationData(
            latitude = -33.8688,
            longitude = 151.2093,
            accuracy = 15.0f,
            timestamp = System.currentTimeMillis(),
            speedMps = null,
            bearing = null
        )

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                LocationDisplay(locationData = locationData)
            }
        }

        // Then: Negative coordinates displayed correctly
        composeTestRule
            .onNodeWithText("Lat: -33.868800")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("Lng: 151.209300")
            .assertIsDisplayed()
    }

    @Test
    fun locationDisplay_handlesZeroAccuracy() {
        // Given: Location with zero accuracy
        val locationData = LocationData(
            latitude = 37.7749,
            longitude = -122.4194,
            accuracy = 0f,
            timestamp = System.currentTimeMillis(),
            speedMps = null,
            bearing = null
        )

        // When: Setting content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                LocationDisplay(locationData = locationData)
            }
        }

        // Then: Accuracy not displayed when zero
        composeTestRule
            .onNodeWithText("Lat: 37.774900")
            .assertIsDisplayed()

        // Accuracy should not be shown
        composeTestRule
            .onNodeWithText("±0.0 m")
            .assertDoesNotExist()
    }
}
