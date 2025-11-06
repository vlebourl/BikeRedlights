package com.example.bikeredlights.ui.screens.ride

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.example.bikeredlights.MainActivity
import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.ui.viewmodel.RideRecordingUiState
import com.example.bikeredlights.ui.viewmodel.RideRecordingViewModel
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
 * Instrumented Compose UI tests for LiveRideScreen.
 *
 * **Test Coverage**:
 * - Idle state displays "Start Ride" button
 * - Recording state displays elapsed time and distance
 * - Recording state displays "Stop Ride" button
 * - Paused state displays "Paused" indicator
 * - Save dialog appears after stopping ride
 * - Save dialog has Save and Discard buttons
 * - Button clicks trigger ViewModel actions
 */
@MediumTest
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class LiveRideScreenTest {

    @get:Rule(order = 0)
    val hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    private lateinit var viewModel: RideRecordingViewModel
    private lateinit var uiStateFlow: MutableStateFlow<RideRecordingUiState>

    @Before
    fun setup() {
        hiltRule.inject()

        // Create mock ViewModel with mutable state flow
        viewModel = mockk(relaxed = true)
        uiStateFlow = MutableStateFlow<RideRecordingUiState>(RideRecordingUiState.Idle)
        every { viewModel.uiState } returns uiStateFlow
    }

    @Test
    fun idleState_displaysStartRideButton() {
        // Given
        uiStateFlow.value = RideRecordingUiState.Idle

        // When
        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Ready to ride?")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Start Ride")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun idleState_startRideButtonTriggersStartAction() {
        // Given
        uiStateFlow.value = RideRecordingUiState.Idle

        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // When
        composeTestRule
            .onNodeWithText("Start Ride")
            .performClick()

        // Then
        verify { viewModel.startRide() }
    }

    @Test
    fun recordingState_displaysElapsedTimeAndDistance() {
        // Given
        val ride = createTestRide(
            id = 1L,
            startTime = System.currentTimeMillis() - 60000, // 1 minute ago
            distanceMeters = 500.0 // 0.5 km
        )
        uiStateFlow.value = RideRecordingUiState.Recording(ride)

        // When
        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Recording")
            .assertIsDisplayed()

        // Check that time is displayed (format: MM:SS)
        composeTestRule
            .onNode(hasText("01:00", substring = true) or hasText("00:", substring = true))
            .assertExists()

        // Check that distance is displayed (format: X.XX km)
        composeTestRule
            .onNode(hasText("0.50 km", substring = true) or hasText("km", substring = true))
            .assertExists()
    }

    @Test
    fun recordingState_displaysStopRideButton() {
        // Given
        val ride = createTestRide(id = 1L, startTime = System.currentTimeMillis())
        uiStateFlow.value = RideRecordingUiState.Recording(ride)

        // When
        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Stop Ride")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun recordingState_stopRideButtonTriggersStopAction() {
        // Given
        val ride = createTestRide(id = 1L, startTime = System.currentTimeMillis())
        uiStateFlow.value = RideRecordingUiState.Recording(ride)

        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // When
        composeTestRule
            .onNodeWithText("Stop Ride")
            .performClick()

        // Then
        verify { viewModel.stopRide() }
    }

    @Test
    fun pausedState_displaysPausedIndicator() {
        // Given
        val ride = createTestRide(
            id = 1L,
            startTime = System.currentTimeMillis() - 30000,
            elapsedDurationMillis = 30000
        )
        uiStateFlow.value = RideRecordingUiState.Paused(ride)

        // When
        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Paused")
            .assertIsDisplayed()
    }

    @Test
    fun autoPausedState_displaysPausedIndicator() {
        // Given
        val ride = createTestRide(
            id = 1L,
            startTime = System.currentTimeMillis() - 30000,
            elapsedDurationMillis = 30000
        )
        uiStateFlow.value = RideRecordingUiState.AutoPaused(ride)

        // When
        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // Then
        // AutoPaused uses same UI as Paused for now
        composeTestRule
            .onNodeWithText("Paused")
            .assertIsDisplayed()
    }

    @Test
    fun showingSaveDialogState_displaysSaveDialog() {
        // Given
        val ride = createTestRide(
            id = 1L,
            name = "Test Ride",
            startTime = System.currentTimeMillis() - 60000,
            endTime = System.currentTimeMillis(),
            elapsedDurationMillis = 60000
        )
        uiStateFlow.value = RideRecordingUiState.ShowingSaveDialog(ride)

        // When
        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Save Ride?")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Test Ride", substring = true)
            .assertExists()
    }

    @Test
    fun saveDialog_hasSaveAndDiscardButtons() {
        // Given
        val ride = createTestRide(
            id = 1L,
            name = "Test Ride",
            startTime = System.currentTimeMillis() - 60000,
            endTime = System.currentTimeMillis(),
            elapsedDurationMillis = 60000
        )
        uiStateFlow.value = RideRecordingUiState.ShowingSaveDialog(ride)

        // When
        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // Then
        composeTestRule
            .onNodeWithText("Save")
            .assertIsDisplayed()
            .assertHasClickAction()
        composeTestRule
            .onNodeWithText("Discard")
            .assertIsDisplayed()
            .assertHasClickAction()
    }

    @Test
    fun saveDialog_saveButtonTriggersSaveAction() {
        // Given
        val ride = createTestRide(
            id = 1L,
            name = "Test Ride",
            startTime = System.currentTimeMillis() - 60000,
            endTime = System.currentTimeMillis(),
            elapsedDurationMillis = 60000
        )
        uiStateFlow.value = RideRecordingUiState.ShowingSaveDialog(ride)

        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // When
        composeTestRule
            .onNodeWithText("Save")
            .performClick()

        // Then
        verify { viewModel.saveRide() }
    }

    @Test
    fun saveDialog_discardButtonTriggersDiscardAction() {
        // Given
        val ride = createTestRide(
            id = 1L,
            name = "Test Ride",
            startTime = System.currentTimeMillis() - 60000,
            endTime = System.currentTimeMillis(),
            elapsedDurationMillis = 60000
        )
        uiStateFlow.value = RideRecordingUiState.ShowingSaveDialog(ride)

        composeTestRule.setContent {
            LiveRideScreen(viewModel = viewModel)
        }

        // When
        composeTestRule
            .onNodeWithText("Discard")
            .performClick()

        // Then
        verify { viewModel.discardRide() }
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
