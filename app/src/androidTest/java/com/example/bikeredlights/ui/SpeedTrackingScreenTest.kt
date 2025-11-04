package com.example.bikeredlights.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.example.bikeredlights.domain.model.GpsStatus
import com.example.bikeredlights.domain.model.SpeedMeasurement
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.domain.model.SpeedSource
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.domain.repository.LocationRepository
import com.example.bikeredlights.domain.usecase.TrackLocationUseCase
import com.example.bikeredlights.ui.screens.SpeedTrackingScreen
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModel
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * UI tests for SpeedTrackingScreen composable.
 *
 * Tests UI rendering based on ViewModel state including:
 * - Speed display updates when ViewModel emits new speed
 * - Permission required UI shown when hasLocationPermission = false
 * - "---" displayed when speedMeasurement is null
 * - Speed format (e.g., "25 km/h")
 *
 * Uses Compose Test Rule for UI testing with mock ViewModel.
 */
class SpeedTrackingScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private lateinit var locationRepository: LocationRepository
    private lateinit var trackLocationUseCase: TrackLocationUseCase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var viewModel: SpeedTrackingViewModel

    @Before
    fun setup() {
        locationRepository = mockk()
        trackLocationUseCase = mockk()
        settingsRepository = mockk()
        // Default: settings repository returns default units system
        every { settingsRepository.unitsSystem } returns flowOf(UnitsSystem.DEFAULT)
    }

    @Test
    fun speedTrackingScreen_displaysPlaceholderWhenSpeedIsNull() {
        // Given: ViewModel with null speed measurement
        every { trackLocationUseCase() } returns flowOf()
        viewModel = SpeedTrackingViewModel(locationRepository, trackLocationUseCase, settingsRepository)

        // Manually set permission granted (bypasses permission dialog in test)
        viewModel.onPermissionGranted()

        // When: Setting screen content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(viewModel = viewModel)
            }
        }

        // Then: "---" placeholder is displayed
        composeTestRule
            .onNodeWithText("--- km/h")
            .assertIsDisplayed()
    }

    @Test
    fun speedTrackingScreen_displaysSpeedWhenMeasurementAvailable() {
        // Given: ViewModel with speed measurement
        val speedMeasurement = SpeedMeasurement(
            speedKmh = 25.7f,  // Will round to 26
            timestamp = System.currentTimeMillis(),
            accuracyKmh = null,
            isStationary = false,
            source = SpeedSource.GPS
        )
        every { trackLocationUseCase() } returns flowOf(speedMeasurement)
        viewModel = SpeedTrackingViewModel(locationRepository, trackLocationUseCase, settingsRepository)
        viewModel.onPermissionGranted()

        // When: Setting screen content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(viewModel = viewModel)
            }
        }

        // Then: Speed is displayed rounded to nearest integer
        composeTestRule
            .onNodeWithText("26 km/h")
            .assertIsDisplayed()
    }

    @Test
    fun speedTrackingScreen_displaysZeroSpeedWhenStationary() {
        // Given: ViewModel with zero speed (stationary)
        val speedMeasurement = SpeedMeasurement(
            speedKmh = 0f,
            timestamp = System.currentTimeMillis(),
            accuracyKmh = null,
            isStationary = true,
            source = SpeedSource.GPS
        )
        every { trackLocationUseCase() } returns flowOf(speedMeasurement)
        viewModel = SpeedTrackingViewModel(locationRepository, trackLocationUseCase, settingsRepository)
        viewModel.onPermissionGranted()

        // When: Setting screen content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(viewModel = viewModel)
            }
        }

        // Then: "0 km/h" is displayed
        composeTestRule
            .onNodeWithText("0 km/h")
            .assertIsDisplayed()
    }

    @Test
    fun speedTrackingScreen_displaysPermissionRequiredWhenNoPermission() {
        // Given: ViewModel with no permission
        every { trackLocationUseCase() } returns flowOf()
        viewModel = SpeedTrackingViewModel(locationRepository, trackLocationUseCase, settingsRepository)

        // Permission explicitly denied (default state is also no permission)
        viewModel.onPermissionDenied()

        // When: Setting screen content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(viewModel = viewModel)
            }
        }

        // Then: Permission required content is displayed
        composeTestRule
            .onNodeWithText("Location Permission Required")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithText("BikeRedlights needs access to your location to track your cycling speed. Your location data is only used while the app is active and is never shared.")
            .assertIsDisplayed()
    }

    @Test
    fun speedTrackingScreen_formatsSpeedCorrectly() {
        // Given: ViewModel with various speed values
        val testCases = listOf(
            0f to "0 km/h",
            5.4f to "5 km/h",
            15.7f to "16 km/h",
            25.5f to "26 km/h",  // Rounds up from .5
            42.3f to "42 km/h",
            99.9f to "100 km/h"
        )

        testCases.forEach { (speedKmh, expectedText) ->
            val speedMeasurement = SpeedMeasurement(
                speedKmh = speedKmh,
                timestamp = System.currentTimeMillis(),
                accuracyKmh = null,
                isStationary = speedKmh < 1f,
                source = SpeedSource.GPS
            )
            every { trackLocationUseCase() } returns flowOf(speedMeasurement)
            viewModel = SpeedTrackingViewModel(locationRepository, trackLocationUseCase, settingsRepository)
            viewModel.onPermissionGranted()

            // When: Setting screen content
            composeTestRule.setContent {
                BikeRedlightsTheme {
                    SpeedTrackingScreen(viewModel = viewModel)
                }
            }

            // Then: Speed is formatted correctly
            composeTestRule
                .onNodeWithText(expectedText)
                .assertIsDisplayed()
        }
    }

    @Test
    fun speedTrackingScreen_hasAccessibilityContentDescription() {
        // Given: ViewModel with speed measurement
        val speedMeasurement = SpeedMeasurement(
            speedKmh = 20f,
            timestamp = System.currentTimeMillis(),
            accuracyKmh = null,
            isStationary = false,
            source = SpeedSource.GPS
        )
        every { trackLocationUseCase() } returns flowOf(speedMeasurement)
        viewModel = SpeedTrackingViewModel(locationRepository, trackLocationUseCase, settingsRepository)
        viewModel.onPermissionGranted()

        // When: Setting screen content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(viewModel = viewModel)
            }
        }

        // Then: Accessibility content description exists
        composeTestRule
            .onNodeWithContentDescription("Current speed: 20 kilometers per hour")
            .assertExists()
    }

    @Test
    fun speedTrackingScreen_hasAccessibilityContentDescriptionForNoSpeed() {
        // Given: ViewModel with null speed
        every { trackLocationUseCase() } returns flowOf()
        viewModel = SpeedTrackingViewModel(locationRepository, trackLocationUseCase, settingsRepository)
        viewModel.onPermissionGranted()

        // When: Setting screen content
        composeTestRule.setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(viewModel = viewModel)
            }
        }

        // Then: Accessibility content description for no speed exists
        composeTestRule
            .onNodeWithContentDescription("Current speed: Not available")
            .assertExists()
    }

    @Test
    fun speedTrackingScreen_updatesWhenSpeedChanges() {
        // Given: ViewModel that emits multiple speed measurements
        val measurement1 = SpeedMeasurement(
            speedKmh = 10f,
            timestamp = 1000L,
            accuracyKmh = null,
            isStationary = false,
            source = SpeedSource.GPS
        )
        val measurement2 = SpeedMeasurement(
            speedKmh = 25f,
            timestamp = 2000L,
            accuracyKmh = null,
            isStationary = false,
            source = SpeedSource.GPS
        )

        // First show 10 km/h
        every { trackLocationUseCase() } returns flowOf(measurement1)
        viewModel = SpeedTrackingViewModel(locationRepository, trackLocationUseCase, settingsRepository)
        viewModel.onPermissionGranted()

        composeTestRule.setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(viewModel = viewModel)
            }
        }

        // Then: Initial speed is displayed
        composeTestRule
            .onNodeWithText("10 km/h")
            .assertIsDisplayed()

        // When: Speed changes to 25 km/h
        every { trackLocationUseCase() } returns flowOf(measurement2)
        // Create new ViewModel instance to simulate state update
        viewModel = SpeedTrackingViewModel(locationRepository, trackLocationUseCase, settingsRepository)
        viewModel.onPermissionGranted()

        composeTestRule.setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(viewModel = viewModel)
            }
        }

        // Then: Updated speed is displayed
        composeTestRule
            .onNodeWithText("25 km/h")
            .assertIsDisplayed()
    }
}
