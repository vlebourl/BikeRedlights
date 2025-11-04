package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.repository.RideRepository
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StartRideUseCase.
 *
 * **Test Coverage**:
 * - Ride creation with generated name
 * - Ride initialization with zero statistics
 * - Repository interaction
 * - Return value (ride ID)
 */
class StartRideUseCaseTest {

    private lateinit var rideRepository: RideRepository
    private lateinit var useCase: StartRideUseCase

    @Before
    fun setup() {
        rideRepository = mockk()
        useCase = StartRideUseCase(rideRepository)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `invoke creates ride with generated name`() = runTest {
        // Given
        val expectedId = 42L
        coEvery { rideRepository.createRide(any()) } returns expectedId

        // When
        val result = useCase()

        // Then
        assertThat(result).isEqualTo(expectedId)
        coVerify {
            rideRepository.createRide(match { ride ->
                ride.name.startsWith("Ride on")
            })
        }
    }

    @Test
    fun `invoke creates ride with current timestamp`() = runTest {
        // Given
        val beforeTimestamp = System.currentTimeMillis()
        coEvery { rideRepository.createRide(any()) } returns 1L

        // When
        useCase()
        val afterTimestamp = System.currentTimeMillis()

        // Then
        coVerify {
            rideRepository.createRide(match { ride ->
                ride.startTime >= beforeTimestamp && ride.startTime <= afterTimestamp
            })
        }
    }

    @Test
    fun `invoke creates ride with zero statistics`() = runTest {
        // Given
        coEvery { rideRepository.createRide(any()) } returns 1L

        // When
        useCase()

        // Then
        coVerify {
            rideRepository.createRide(match { ride ->
                ride.elapsedDurationMillis == 0L &&
                ride.movingDurationMillis == 0L &&
                ride.manualPausedDurationMillis == 0L &&
                ride.autoPausedDurationMillis == 0L &&
                ride.distanceMeters == 0.0 &&
                ride.avgSpeedMetersPerSec == 0.0 &&
                ride.maxSpeedMetersPerSec == 0.0
            })
        }
    }

    @Test
    fun `invoke creates ride with null endTime`() = runTest {
        // Given
        coEvery { rideRepository.createRide(any()) } returns 1L

        // When
        useCase()

        // Then
        coVerify {
            rideRepository.createRide(match { ride ->
                ride.endTime == null
            })
        }
    }

    @Test
    fun `invoke returns ride ID from repository`() = runTest {
        // Given
        val expectedId = 123456L
        coEvery { rideRepository.createRide(any()) } returns expectedId

        // When
        val result = useCase()

        // Then
        assertThat(result).isEqualTo(expectedId)
    }

    @Test
    fun `invoke calls repository createRide exactly once`() = runTest {
        // Given
        coEvery { rideRepository.createRide(any()) } returns 1L

        // When
        useCase()

        // Then
        coVerify(exactly = 1) { rideRepository.createRide(any()) }
    }
}
