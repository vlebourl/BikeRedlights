package com.example.bikeredlights.data.repository

import com.example.bikeredlights.data.local.dao.RideDao
import com.example.bikeredlights.data.local.entity.Ride as RideEntity
import com.example.bikeredlights.domain.model.Ride
import com.google.common.truth.Truth.assertThat
import io.mockk.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for RideRepositoryImpl.
 *
 * **Test Coverage**:
 * - Create ride operations
 * - Update ride operations
 * - Delete ride operations
 * - Query operations (by ID, all rides, incomplete rides)
 * - Flow-based reactive queries
 * - Domain model ↔ Entity mapping
 *
 * **Mocking Strategy**:
 * - Mock RideDao using MockK
 * - Verify correct DAO method calls
 * - Verify correct mapping between domain models and entities
 * - Use Dispatchers.IO for database operations
 */
class RideRepositoryImplTest {

    private lateinit var rideDao: RideDao
    private lateinit var repository: RideRepositoryImpl

    @Before
    fun setup() {
        rideDao = mockk()
        repository = RideRepositoryImpl(rideDao)
    }

    @After
    fun teardown() {
        unmockkAll()
    }

    @Test
    fun `createRide inserts ride entity and returns generated ID`() = runTest {
        // Given
        val ride = createTestRide(name = "Test Ride")
        val expectedId = 123L

        coEvery { rideDao.insertRide(any()) } returns expectedId

        // When
        val resultId = repository.createRide(ride)

        // Then
        assertThat(resultId).isEqualTo(expectedId)
        coVerify { rideDao.insertRide(any()) }
    }

    @Test
    fun `updateRide calls DAO updateRide with correct entity`() = runTest {
        // Given
        val ride = createTestRide(id = 1L, name = "Updated Ride")

        coEvery { rideDao.updateRide(any()) } just Runs

        // When
        repository.updateRide(ride)

        // Then
        coVerify { rideDao.updateRide(match { it.id == 1L && it.name == "Updated Ride" }) }
    }

    @Test
    fun `deleteRide calls DAO deleteRide with correct entity`() = runTest {
        // Given
        val ride = createTestRide(id = 1L, name = "To Delete")

        coEvery { rideDao.deleteRide(any()) } just Runs

        // When
        repository.deleteRide(ride)

        // Then
        coVerify { rideDao.deleteRide(match { it.id == 1L }) }
    }

    @Test
    fun `getRideById returns domain model when ride exists`() = runTest {
        // Given
        val rideEntity = createTestRideEntity(id = 1L, name = "Test Ride")

        coEvery { rideDao.getRideById(1L) } returns rideEntity

        // When
        val result = repository.getRideById(1L)

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(1L)
        assertThat(result?.name).isEqualTo("Test Ride")
        coVerify { rideDao.getRideById(1L) }
    }

    @Test
    fun `getRideById returns null when ride does not exist`() = runTest {
        // Given
        coEvery { rideDao.getRideById(999L) } returns null

        // When
        val result = repository.getRideById(999L)

        // Then
        assertThat(result).isNull()
        coVerify { rideDao.getRideById(999L) }
    }

    @Test
    fun `getAllRidesFlow emits domain models`() = runTest {
        // Given
        val rideEntities = listOf(
            createTestRideEntity(id = 1L, name = "Ride 1"),
            createTestRideEntity(id = 2L, name = "Ride 2")
        )

        every { rideDao.getAllRidesFlow() } returns flowOf(rideEntities)

        // When
        val result = repository.getAllRidesFlow().first()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].name).isEqualTo("Ride 1")
        assertThat(result[1].name).isEqualTo("Ride 2")
        verify { rideDao.getAllRidesFlow() }
    }

    @Test
    fun `getAllRides returns list of domain models`() = runTest {
        // Given
        val rideEntities = listOf(
            createTestRideEntity(id = 1L, name = "Ride 1"),
            createTestRideEntity(id = 2L, name = "Ride 2"),
            createTestRideEntity(id = 3L, name = "Ride 3")
        )

        coEvery { rideDao.getAllRides() } returns rideEntities

        // When
        val result = repository.getAllRides()

        // Then
        assertThat(result).hasSize(3)
        assertThat(result.map { it.name }).containsExactly("Ride 1", "Ride 2", "Ride 3")
        coVerify { rideDao.getAllRides() }
    }

    @Test
    fun `getIncompleteRides returns only rides with null endTime`() = runTest {
        // Given
        val incompleteEntities = listOf(
            createTestRideEntity(id = 1L, name = "Incomplete 1", endTime = null),
            createTestRideEntity(id = 2L, name = "Incomplete 2", endTime = null)
        )

        coEvery { rideDao.getIncompleteRides() } returns incompleteEntities

        // When
        val result = repository.getIncompleteRides()

        // Then
        assertThat(result).hasSize(2)
        assertThat(result[0].endTime).isNull()
        assertThat(result[1].endTime).isNull()
        coVerify { rideDao.getIncompleteRides() }
    }

    @Test
    fun `domain model to entity mapping preserves all fields`() = runTest {
        // Given
        val ride = Ride(
            id = 42L,
            name = "Test Ride",
            startTime = 1000L,
            endTime = 2000L,
            elapsedDurationMillis = 1000L,
            movingDurationMillis = 800L,
            manualPausedDurationMillis = 100L,
            autoPausedDurationMillis = 100L,
            distanceMeters = 1500.0,
            avgSpeedMetersPerSec = 5.0,
            maxSpeedMetersPerSec = 10.0
        )

        coEvery { rideDao.insertRide(any()) } returns 42L

        // When
        repository.createRide(ride)

        // Then
        coVerify {
            rideDao.insertRide(match { entity ->
                entity.id == 42L &&
                entity.name == "Test Ride" &&
                entity.startTime == 1000L &&
                entity.endTime == 2000L &&
                entity.elapsedDurationMillis == 1000L &&
                entity.movingDurationMillis == 800L &&
                entity.manualPausedDurationMillis == 100L &&
                entity.autoPausedDurationMillis == 100L &&
                entity.distanceMeters == 1500.0 &&
                entity.avgSpeedMetersPerSec == 5.0 &&
                entity.maxSpeedMetersPerSec == 10.0
            })
        }
    }

    @Test
    fun `entity to domain model mapping preserves all fields`() = runTest {
        // Given
        val entity = RideEntity(
            id = 42L,
            name = "Test Ride",
            startTime = 1000L,
            endTime = 2000L,
            elapsedDurationMillis = 1000L,
            movingDurationMillis = 800L,
            manualPausedDurationMillis = 100L,
            autoPausedDurationMillis = 100L,
            distanceMeters = 1500.0,
            avgSpeedMetersPerSec = 5.0,
            maxSpeedMetersPerSec = 10.0
        )

        coEvery { rideDao.getRideById(42L) } returns entity

        // When
        val result = repository.getRideById(42L)

        // Then
        assertThat(result).isNotNull()
        assertThat(result?.id).isEqualTo(42L)
        assertThat(result?.name).isEqualTo("Test Ride")
        assertThat(result?.startTime).isEqualTo(1000L)
        assertThat(result?.endTime).isEqualTo(2000L)
        assertThat(result?.elapsedDurationMillis).isEqualTo(1000L)
        assertThat(result?.movingDurationMillis).isEqualTo(800L)
        assertThat(result?.manualPausedDurationMillis).isEqualTo(100L)
        assertThat(result?.autoPausedDurationMillis).isEqualTo(100L)
        assertThat(result?.distanceMeters).isEqualTo(1500.0)
        assertThat(result?.avgSpeedMetersPerSec).isEqualTo(5.0)
        assertThat(result?.maxSpeedMetersPerSec).isEqualTo(10.0)
    }

    // Helper functions

    private fun createTestRide(
        id: Long = 0,
        name: String = "Test Ride",
        startTime: Long = System.currentTimeMillis(),
        endTime: Long? = null,
        distanceMeters: Double = 0.0
    ): Ride {
        return Ride(
            id = id,
            name = name,
            startTime = startTime,
            endTime = endTime,
            elapsedDurationMillis = 0L,
            movingDurationMillis = 0L,
            manualPausedDurationMillis = 0L,
            autoPausedDurationMillis = 0L,
            distanceMeters = distanceMeters,
            avgSpeedMetersPerSec = 0.0,
            maxSpeedMetersPerSec = 0.0
        )
    }

    private fun createTestRideEntity(
        id: Long = 0,
        name: String = "Test Ride",
        startTime: Long = System.currentTimeMillis(),
        endTime: Long? = null
    ): RideEntity {
        return RideEntity(
            id = id,
            name = name,
            startTime = startTime,
            endTime = endTime,
            elapsedDurationMillis = 0L,
            movingDurationMillis = 0L,
            manualPausedDurationMillis = 0L,
            autoPausedDurationMillis = 0L,
            distanceMeters = 0.0,
            avgSpeedMetersPerSec = 0.0,
            maxSpeedMetersPerSec = 0.0
        )
    }
}
