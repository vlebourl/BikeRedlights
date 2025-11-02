package com.example.bikeredlights.ui.viewmodel

import app.cash.turbine.test
import com.example.bikeredlights.domain.model.GpsStatus
import com.example.bikeredlights.domain.model.SpeedMeasurement
import com.example.bikeredlights.domain.model.SpeedSource
import com.example.bikeredlights.domain.usecase.TrackLocationUseCase
import com.google.common.truth.Truth.assertThat
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SpeedTrackingViewModel.
 *
 * Tests UI state management including:
 * - StateFlow emissions on location updates
 * - Permission granted/denied state changes
 * - Error handling (SecurityException → gpsStatus.Unavailable)
 * - GPS status determination from speed accuracy
 * - Initial state (Acquiring, no permission)
 *
 * This is a safety-critical feature requiring 90%+ test coverage.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SpeedTrackingViewModelTest {

    private lateinit var trackLocationUseCase: TrackLocationUseCase
    private lateinit var viewModel: SpeedTrackingViewModel

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        trackLocationUseCase = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has no permission and Acquiring GPS status`() = runTest {
        // Given: ViewModel created
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Observing initial state
        viewModel.uiState.test {
            val state = awaitItem()

            // Then: Initial state is correct
            assertThat(state.hasLocationPermission).isFalse()
            assertThat(state.gpsStatus).isEqualTo(GpsStatus.Acquiring)
            assertThat(state.speedMeasurement).isNull()
            assertThat(state.locationData).isNull()
            assertThat(state.errorMessage).isNull()
        }
    }

    @Test
    fun `onPermissionGranted updates state and starts location tracking`() = runTest {
        // Given: Use case emits speed measurement
        val speedMeasurement = SpeedMeasurement(
            speedKmh = 25f,
            timestamp = System.currentTimeMillis(),
            accuracyKmh = null,
            isStationary = false,
            source = SpeedSource.GPS
        )
        every { trackLocationUseCase() } returns flowOf(speedMeasurement)
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Permission is granted
        viewModel.uiState.test {
            awaitItem()  // Initial state
            viewModel.onPermissionGranted()

            // Then: State updates with permission (may have multiple emissions)
            // First emission: permission granted
            var state = awaitItem()
            assertThat(state.hasLocationPermission).isTrue()

            // If speed isn't in first emission, check next emission
            if (state.speedMeasurement == null) {
                state = awaitItem()
            }

            assertThat(state.speedMeasurement).isEqualTo(speedMeasurement)
            assertThat(state.gpsStatus).isEqualTo(GpsStatus.Active(5f))
            assertThat(state.errorMessage).isNull()
        }
    }

    @Test
    fun `onPermissionDenied updates state with error message`() = runTest {
        // Given: ViewModel created
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Permission is denied
        viewModel.uiState.test {
            awaitItem()  // Initial state
            viewModel.onPermissionDenied()

            // Then: State updates with denial
            val state = awaitItem()
            assertThat(state.hasLocationPermission).isFalse()
            assertThat(state.errorMessage).isEqualTo("Location permission is required for speed tracking")
        }
    }

    @Test
    fun `startLocationTracking updates state on speed measurement emissions`() = runTest {
        // Given: Use case emits a single speed measurement
        val speedMeasurement = SpeedMeasurement(
            speedKmh = 20f,
            timestamp = System.currentTimeMillis(),
            accuracyKmh = null,
            isStationary = false,
            source = SpeedSource.GPS
        )
        every { trackLocationUseCase() } returns flowOf(speedMeasurement)
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Starting location tracking
        viewModel.uiState.test {
            awaitItem()  // Initial state
            viewModel.startLocationTracking()

            // Then: State updates with speed measurement
            val state = awaitItem()
            assertThat(state.speedMeasurement?.speedKmh).isEqualTo(20f)
            assertThat(state.gpsStatus).isInstanceOf(GpsStatus.Active::class.java)
        }
    }

    @Test
    fun `startLocationTracking sets GPS status to Acquiring for UNKNOWN source`() = runTest {
        // Given: Use case emits measurement with UNKNOWN source
        val speedMeasurement = SpeedMeasurement(
            speedKmh = 0f,
            timestamp = System.currentTimeMillis(),
            accuracyKmh = null,
            isStationary = true,
            source = SpeedSource.UNKNOWN
        )
        every { trackLocationUseCase() } returns flowOf(speedMeasurement)
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Starting location tracking
        viewModel.uiState.test {
            awaitItem()  // Initial state
            viewModel.startLocationTracking()

            // Then: GPS status is Acquiring
            val state = awaitItem()
            assertThat(state.gpsStatus).isEqualTo(GpsStatus.Acquiring)
        }
    }

    @Test
    fun `startLocationTracking sets GPS status to Active for GPS source`() = runTest {
        // Given: Use case emits measurement with GPS source
        val speedMeasurement = SpeedMeasurement(
            speedKmh = 25f,
            timestamp = System.currentTimeMillis(),
            accuracyKmh = null,
            isStationary = false,
            source = SpeedSource.GPS
        )
        every { trackLocationUseCase() } returns flowOf(speedMeasurement)
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Starting location tracking
        viewModel.uiState.test {
            awaitItem()  // Initial state
            viewModel.startLocationTracking()

            // Then: GPS status is Active
            val state = awaitItem()
            assertThat(state.gpsStatus).isInstanceOf(GpsStatus.Active::class.java)
            val activeStatus = state.gpsStatus as GpsStatus.Active
            assertThat(activeStatus.accuracy).isEqualTo(5f)
        }
    }

    @Test
    fun `startLocationTracking handles SecurityException with error message`() = runTest {
        // Given: Use case throws SecurityException
        every { trackLocationUseCase() } returns flow {
            throw SecurityException("Location permission not granted")
        }
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Starting location tracking
        viewModel.uiState.test {
            awaitItem()  // Initial state
            viewModel.startLocationTracking()

            // Then: State updates with error
            val state = awaitItem()
            assertThat(state.gpsStatus).isEqualTo(GpsStatus.Unavailable)
            assertThat(state.errorMessage).isEqualTo("Location permission required")
        }
    }

    @Test
    fun `startLocationTracking handles generic exceptions with GPS unavailable`() = runTest {
        // Given: Use case throws generic exception
        every { trackLocationUseCase() } returns flow {
            throw RuntimeException("GPS hardware failure")
        }
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Starting location tracking
        viewModel.uiState.test {
            awaitItem()  // Initial state
            viewModel.startLocationTracking()

            // Then: State updates with GPS unavailable error
            val state = awaitItem()
            assertThat(state.gpsStatus).isEqualTo(GpsStatus.Unavailable)
            assertThat(state.errorMessage).contains("GPS unavailable")
            assertThat(state.errorMessage).contains("GPS hardware failure")
        }
    }

    @Test
    fun `onPermissionGranted followed by speed updates maintains permission state`() = runTest {
        // Given: Use case emits speed measurement
        val speedMeasurement = SpeedMeasurement(
            speedKmh = 30f,
            timestamp = System.currentTimeMillis(),
            accuracyKmh = null,
            isStationary = false,
            source = SpeedSource.GPS
        )
        every { trackLocationUseCase() } returns flowOf(speedMeasurement)
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Permission granted and speed updates received
        viewModel.uiState.test {
            awaitItem()  // Initial state
            viewModel.onPermissionGranted()

            // Then: Permission state persists (may have multiple emissions)
            var state = awaitItem()
            assertThat(state.hasLocationPermission).isTrue()

            // If speed isn't in first emission, check next emission
            if (state.speedMeasurement == null) {
                state = awaitItem()
            }

            assertThat(state.speedMeasurement?.speedKmh).isEqualTo(30f)
            assertThat(state.errorMessage).isNull()
        }
    }

    @Test
    fun `error message is cleared when valid speed measurement received`() = runTest {
        // Given: ViewModel with initial error
        val speedMeasurement = SpeedMeasurement(
            speedKmh = 15f,
            timestamp = System.currentTimeMillis(),
            accuracyKmh = null,
            isStationary = false,
            source = SpeedSource.GPS
        )
        every { trackLocationUseCase() } returns flowOf(speedMeasurement)
        viewModel = SpeedTrackingViewModel(trackLocationUseCase)

        // When: Permission denied then granted
        viewModel.uiState.test {
            awaitItem()  // Initial state

            viewModel.onPermissionDenied()
            val deniedState = awaitItem()
            assertThat(deniedState.errorMessage).isNotNull()

            viewModel.onPermissionGranted()
            var grantedState = awaitItem()

            // If speed isn't in first emission, check next emission
            if (grantedState.speedMeasurement == null) {
                grantedState = awaitItem()
            }

            // Then: Error message is cleared
            assertThat(grantedState.errorMessage).isNull()
            assertThat(grantedState.speedMeasurement).isNotNull()
        }
    }
}
