package com.example.bikeredlights.ui.screens.ride

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.ui.viewmodel.RideReviewUiState
import com.example.bikeredlights.ui.viewmodel.RideReviewViewModel
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented Compose UI tests for RideReviewScreen.
 *
 * **Test Coverage**:
 * - Loading state displays progress indicator
 * - Success state displays ride name in app bar
 * - Success state displays ride date
 * - Success state displays ride statistics
 * - Success state displays map placeholder
 * - Success state displays summary section
 * - Error state displays error message
 * - Back button triggers navigation callback
 */
@MediumTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class RideReviewScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createComposeRule()

    private lateinit var viewModel: RideReviewViewModel
    private lateinit var uiStateFlow: MutableStateFlow<RideReviewUiState>
    private var navigationBackCalled = false

    @Before
    fun setup() {
        hiltRule.inject()

        // Create mock ViewModel with mutable state flow
        viewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow<RideReviewUiState>(RideReviewUiState.Loading)
        every { viewModel.uiState } returns uiStateFlow

        navigationBackCalled = false
    }

    @Test
    fun loadingState_displaysProgressIndicator() {
        // Given
        uiStateFlow.value = RideReviewUiState.Loading

        // When
        composeTestRule.setContent {
            RideReviewScreen(
                rideId = 1L,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // Then
        composeTestRule
            .onNode(hasProgressBarRangeInfo(ProgressBarRangeInfo.Indeterminate))
            .assertExists()
    }

    @Test
    fun successState_displaysRideNameInAppBar() {
        // Given
        val ride = createTestRide(
            id = 1L,
            name = "Morning Commute",
            startTime = System.currentTimeMillis() - 3600000
        )
        uiStateFlow.value = RideReviewUiState.Success(ride)

        // When
        composeTestRule.setContent {
            RideReviewScreen(
                rideId = 1L,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("Morning Commute")
            .assertIsDisplayed()
    }

    @Test
    fun successState_displaysRideDate() {
        // Given
        val ride = createTestRide(
            id = 1L,
            name = "Test Ride",
            startTime = System.currentTimeMillis() - 3600000
        )
        uiStateFlow.value = RideReviewUiState.Success(ride)

        // When
        composeTestRule.setContent {
            RideReviewScreen(
                rideId = 1L,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // Then - Check that a date is displayed (format varies by locale)
        composeTestRule
            .onNode(
                hasText("at", substring = true, ignoreCase = true) or
                hasText("AM", substring = true) or
                hasText("PM", substring = true)
            )
            .assertExists()
    }

    @Test
    fun successState_displaysMapPlaceholder() {
        // Given
        val ride = createTestRide(id = 1L, name = "Test Ride")
        uiStateFlow.value = RideReviewUiState.Success(ride)

        // When
        composeTestRule.setContent {
            RideReviewScreen(
                rideId = 1L,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("Map visualization", substring = true)
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("coming in v0.4.0", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun successState_displaysRideStatistics() {
        // Given
        val ride = createTestRide(
            id = 1L,
            name = "Test Ride",
            startTime = System.currentTimeMillis() - 3600000, // 1 hour ago
            endTime = System.currentTimeMillis(),
            elapsedDurationMillis = 3600000, // 1 hour
            movingDurationMillis = 3000000, // 50 minutes
            distanceMeters = 15000.0, // 15 km
            avgSpeedMetersPerSec = 5.0, // 18 km/h
            maxSpeedMetersPerSec = 10.0 // 36 km/h
        )
        uiStateFlow.value = RideReviewUiState.Success(ride)

        // When
        composeTestRule.setContent {
            RideReviewScreen(
                rideId = 1L,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // Then - Check that statistics are displayed
        composeTestRule
            .onNode(hasText("km", substring = true))
            .assertExists()
        composeTestRule
            .onNode(hasText("km/h", substring = true))
            .assertExists()
    }

    @Test
    fun successState_displaysSummarySection() {
        // Given
        val ride = createTestRide(
            id = 1L,
            name = "Test Ride",
            startTime = System.currentTimeMillis() - 3600000,
            endTime = System.currentTimeMillis(),
            elapsedDurationMillis = 3600000,
            movingDurationMillis = 3000000,
            distanceMeters = 10000.0,
            avgSpeedMetersPerSec = 5.0,
            maxSpeedMetersPerSec = 8.0
        )
        uiStateFlow.value = RideReviewUiState.Success(ride)

        // When
        composeTestRule.setContent {
            RideReviewScreen(
                rideId = 1L,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("Summary")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Total Duration")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Moving Time")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Distance")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Average Speed")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Max Speed")
            .assertIsDisplayed()
    }

    @Test
    fun errorState_displaysErrorMessage() {
        // Given
        uiStateFlow.value = RideReviewUiState.Error("Ride not found (ID: 999)")

        // When
        composeTestRule.setContent {
            RideReviewScreen(
                rideId = 999L,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // Then
        composeTestRule
            .onNodeWithText("Error")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Ride not found", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun errorState_backButtonTriggersNavigation() {
        // Given
        uiStateFlow.value = RideReviewUiState.Error("Ride not found")

        composeTestRule.setContent {
            RideReviewScreen(
                rideId = 1L,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // When
        composeTestRule
            .onNodeWithText("Back to Live")
            .performClick()

        // Then
        assert(navigationBackCalled) { "Navigation callback was not called" }
    }

    @Test
    fun backButton_triggersNavigationCallback() {
        // Given
        val ride = createTestRide(id = 1L, name = "Test Ride")
        uiStateFlow.value = RideReviewUiState.Success(ride)

        composeTestRule.setContent {
            RideReviewScreen(
                rideId = 1L,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // When - Click the back button in top app bar
        composeTestRule
            .onNodeWithContentDescription("Back")
            .performClick()

        // Then
        assert(navigationBackCalled) { "Navigation callback was not called" }
    }

    @Test
    fun loadRide_triggeredOnScreenAppear() {
        // Given
        uiStateFlow.value = RideReviewUiState.Loading
        val testRideId = 42L

        // When
        composeTestRule.setContent {
            RideReviewScreen(
                rideId = testRideId,
                onNavigateBack = { navigationBackCalled = true },
                viewModel = viewModel
            )
        }

        // Then - Verify loadRide was called with correct ID
        composeTestRule.waitForIdle()
        verify { viewModel.loadRide(testRideId) }
    }

    // Helper function

    private fun createTestRide(
        id: Long = 1L,
        name: String = "Test Ride",
        startTime: Long = System.currentTimeMillis(),
        endTime: Long? = null,
        elapsedDurationMillis: Long = 0L,
        movingDurationMillis: Long = 0L,
        manualPausedDurationMillis: Long = 0L,
        autoPausedDurationMillis: Long = 0L,
        distanceMeters: Double = 0.0,
        avgSpeedMetersPerSec: Double = 0.0,
        maxSpeedMetersPerSec: Double = 0.0
    ): Ride {
        return Ride(
            id = id,
            name = name,
            startTime = startTime,
            endTime = endTime,
            elapsedDurationMillis = elapsedDurationMillis,
            movingDurationMillis = movingDurationMillis,
            manualPausedDurationMillis = manualPausedDurationMillis,
            autoPausedDurationMillis = autoPausedDurationMillis,
            distanceMeters = distanceMeters,
            avgSpeedMetersPerSec = avgSpeedMetersPerSec,
            maxSpeedMetersPerSec = maxSpeedMetersPerSec
        )
    }
}
