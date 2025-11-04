package com.example.bikeredlights.ui.viewmodel

import android.content.Context
import app.cash.turbine.test
import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.model.RideRecordingState
import com.example.bikeredlights.domain.repository.RideRecordingStateRepository
import com.example.bikeredlights.domain.repository.RideRepository
import com.example.bikeredlights.domain.usecase.FinishRideUseCase
import com.example.bikeredlights.domain.usecase.FinishRideResult
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RideRecordingViewModel.
 *
 * **Test Coverage**:
 * - Initial UI state is Idle
 * - UI state transitions based on recording state
 * - startRide() action and service interaction
 * - stopRide() action with all FinishRideResult scenarios
 * - saveRide() action
 * - discardRide() action
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RideRecordingViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var rideRecordingStateRepository: RideRecordingStateRepository
    private lateinit var rideRepository: RideRepository
    private lateinit var finishRideUseCase: FinishRideUseCase
    private lateinit var viewModel: RideRecordingViewModel

    private lateinit var recordingStateFlow: MutableStateFlow<RideRecordingState>

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        context = mockk(relaxed = true)
        rideRecordingStateRepository = mockk(relaxed = true)
        rideRepository = mockk(relaxed = true)
        finishRideUseCase = mockk()

        // Mock recording state flow
        recordingStateFlow = MutableStateFlow<RideRecordingState>(RideRecordingState.Idle)
        every { rideRecordingStateRepository.getRecordingState() } returns recordingStateFlow

        // Mock static service calls
        mockkObject(com.example.bikeredlights.service.RideRecordingService.Companion)
        every { com.example.bikeredlights.service.RideRecordingService.startRecording(any()) } just Runs
        every { com.example.bikeredlights.service.RideRecordingService.stopRecording(any()) } just Runs
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial UI state is Idle`() = runTest {
        // Given
        viewModel = createViewModel()

        // When - observe initial state
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(RideRecordingUiState.Idle::class.java)
    }

    @Test
    fun `UI state transitions to Recording when recording state is Recording`() = runTest {
        // Given
        viewModel = createViewModel()
        val ride = createTestRide(id = 1L, endTime = null)
        coEvery { rideRepository.getRideById(1L) } returns ride

        // When
        recordingStateFlow.value = RideRecordingState.Recording(rideId = 1L)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(RideRecordingUiState.Recording::class.java)
        assertThat((uiState as RideRecordingUiState.Recording).ride.id).isEqualTo(1L)
    }

    @Test
    fun `UI state transitions to Paused when recording state is ManuallyPaused`() = runTest {
        // Given
        viewModel = createViewModel()
        val ride = createTestRide(id = 1L, endTime = null)
        coEvery { rideRepository.getRideById(1L) } returns ride

        // When
        recordingStateFlow.value = RideRecordingState.ManuallyPaused(rideId = 1L)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(RideRecordingUiState.Paused::class.java)
        assertThat((uiState as RideRecordingUiState.Paused).ride.id).isEqualTo(1L)
    }

    @Test
    fun `UI state transitions to AutoPaused when recording state is AutoPaused`() = runTest {
        // Given
        viewModel = createViewModel()
        val ride = createTestRide(id = 1L, endTime = null)
        coEvery { rideRepository.getRideById(1L) } returns ride

        // When
        recordingStateFlow.value = RideRecordingState.AutoPaused(rideId = 1L)
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(RideRecordingUiState.AutoPaused::class.java)
        assertThat((uiState as RideRecordingUiState.AutoPaused).ride.id).isEqualTo(1L)
    }

    @Test
    fun `UI state remains Idle when recording state is Stopped`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        recordingStateFlow.value = RideRecordingState.Stopped(rideId = 1L)
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(RideRecordingUiState.Idle::class.java)
    }

    @Test
    fun `startRide calls RideRecordingService startRecording`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.startRide()
        advanceUntilIdle()

        // Then
        verify { com.example.bikeredlights.service.RideRecordingService.startRecording(context) }
    }

    @Test
    fun `stopRide with Success result shows save dialog`() = runTest {
        // Given
        viewModel = createViewModel()
        val ride = createTestRide(id = 1L, endTime = System.currentTimeMillis())
        coEvery { rideRecordingStateRepository.getCurrentState() } returns RideRecordingState.Recording(1L)
        coEvery { finishRideUseCase(1L) } returns FinishRideResult.Success(ride)

        // When
        viewModel.stopRide()
        advanceUntilIdle()

        // Then
        val uiState = viewModel.uiState.value
        assertThat(uiState).isInstanceOf(RideRecordingUiState.ShowingSaveDialog::class.java)
        assertThat((uiState as RideRecordingUiState.ShowingSaveDialog).ride.id).isEqualTo(1L)
        verify { com.example.bikeredlights.service.RideRecordingService.stopRecording(context) }
    }

    @Test
    fun `stopRide with TooShort result auto-discards and returns to Idle`() = runTest {
        // Given
        viewModel = createViewModel()
        coEvery { rideRecordingStateRepository.getCurrentState() } returns RideRecordingState.Recording(1L)
        coEvery { finishRideUseCase(1L) } returns FinishRideResult.TooShort(2000L)
        coEvery { rideRepository.getRideById(1L) } returns createTestRide(id = 1L, endTime = null)
        coEvery { rideRepository.deleteRide(any()) } just Runs
        coEvery { rideRecordingStateRepository.clearRecordingState() } just Runs

        // When
        viewModel.stopRide()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(RideRecordingUiState.Idle::class.java)
        coVerify { rideRepository.deleteRide(any()) }
        verify { com.example.bikeredlights.service.RideRecordingService.stopRecording(context) }
    }

    @Test
    fun `stopRide with RideNotFound result returns to Idle`() = runTest {
        // Given
        viewModel = createViewModel()
        coEvery { rideRecordingStateRepository.getCurrentState() } returns RideRecordingState.Recording(1L)
        coEvery { finishRideUseCase(1L) } returns FinishRideResult.RideNotFound

        // When
        viewModel.stopRide()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(RideRecordingUiState.Idle::class.java)
        verify { com.example.bikeredlights.service.RideRecordingService.stopRecording(context) }
    }

    @Test
    fun `stopRide does nothing when no active ride`() = runTest {
        // Given
        viewModel = createViewModel()
        coEvery { rideRecordingStateRepository.getCurrentState() } returns RideRecordingState.Idle

        // When
        viewModel.stopRide()
        advanceUntilIdle()

        // Then
        verify(exactly = 0) { com.example.bikeredlights.service.RideRecordingService.stopRecording(any()) }
        coVerify(exactly = 0) { finishRideUseCase(any()) }
    }

    @Test
    fun `saveRide clears state and returns to Idle`() = runTest {
        // Given
        viewModel = createViewModel()
        val ride = createTestRide(id = 1L, endTime = System.currentTimeMillis())
        coEvery { rideRecordingStateRepository.getCurrentState() } returns RideRecordingState.Stopped(1L)
        coEvery { finishRideUseCase(1L) } returns FinishRideResult.Success(ride)
        coEvery { rideRecordingStateRepository.clearRecordingState() } just Runs

        // Trigger save dialog first
        viewModel.stopRide()
        advanceUntilIdle()

        // When
        viewModel.saveRide()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(RideRecordingUiState.Idle::class.java)
        coVerify { rideRecordingStateRepository.clearRecordingState() }
    }

    @Test
    fun `discardRide deletes ride and returns to Idle`() = runTest {
        // Given
        viewModel = createViewModel()
        val ride = createTestRide(id = 1L, endTime = System.currentTimeMillis())
        coEvery { rideRecordingStateRepository.getCurrentState() } returns RideRecordingState.Stopped(1L)
        coEvery { finishRideUseCase(1L) } returns FinishRideResult.Success(ride)
        coEvery { rideRepository.getRideById(1L) } returns ride  // Mock getRideById call
        coEvery { rideRepository.deleteRide(any()) } just Runs
        coEvery { rideRecordingStateRepository.clearRecordingState() } just Runs

        // Trigger save dialog first
        viewModel.stopRide()
        advanceUntilIdle()

        // When
        viewModel.discardRide()
        advanceUntilIdle()

        // Then
        assertThat(viewModel.uiState.value).isInstanceOf(RideRecordingUiState.Idle::class.java)
        coVerify { rideRepository.getRideById(1L) }
        coVerify { rideRepository.deleteRide(any()) }
        coVerify { rideRecordingStateRepository.clearRecordingState() }
    }

    @Test
    fun `UI state flow emits updates when recording state changes`() = runTest {
        // Given
        viewModel = createViewModel()
        val ride = createTestRide(id = 1L, endTime = null)
        coEvery { rideRepository.getRideById(1L) } returns ride

        // When & Then
        viewModel.uiState.test {
            // Initial state
            assertThat(awaitItem()).isInstanceOf(RideRecordingUiState.Idle::class.java)

            // Transition to Recording
            recordingStateFlow.value = RideRecordingState.Recording(rideId = 1L)
            advanceUntilIdle()
            val recordingState = awaitItem()
            assertThat(recordingState).isInstanceOf(RideRecordingUiState.Recording::class.java)
            assertThat((recordingState as RideRecordingUiState.Recording).ride.id).isEqualTo(1L)

            // Transition to ManuallyPaused
            recordingStateFlow.value = RideRecordingState.ManuallyPaused(rideId = 1L)
            advanceUntilIdle()
            val pausedState = awaitItem()
            assertThat(pausedState).isInstanceOf(RideRecordingUiState.Paused::class.java)

            cancelAndIgnoreRemainingEvents()
        }
    }

    // Helper functions

    private fun createViewModel(): RideRecordingViewModel {
        return RideRecordingViewModel(
            context = context,
            rideRecordingStateRepository = rideRecordingStateRepository,
            rideRepository = rideRepository,
            finishRideUseCase = finishRideUseCase
        )
    }

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
