package com.example.bikeredlights.data.repository

import com.example.bikeredlights.data.local.dao.StopDao
import com.example.bikeredlights.data.local.entity.StopEntity
import com.example.bikeredlights.domain.model.Stop
import com.google.common.truth.Truth.assertThat
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for StopRepository implementation (Feature 009).
 *
 * Test Strategy:
 * - Mock StopDao to isolate repository logic from database
 * - Test domain/entity mapping (bidirectional)
 * - Verify DAO methods are called with correct parameters
 * - Test error propagation from DAO
 *
 * Coverage:
 * - CRUD operations (insertStop, updateStopEnd, getStopsByRideId)
 * - Flow operations (getStopCountByRideId)
 * - Entity ↔ Domain model mapping
 * - Edge cases (null values, empty lists)
 */
class StopRepositoryTest {

    private lateinit var stopDao: StopDao
    private lateinit var repository: StopRepositoryImpl

    @Before
    fun setUp() {
        stopDao = mockk()
        repository = StopRepositoryImpl(stopDao)
    }

    // ====================================================================================
    // INSERT TESTS
    // ====================================================================================

    @Test
    fun `insertStop converts domain model to entity and delegates to dao`() = runTest {
        // Given
        val stop = createTestStop(id = 0, rideId = 1L, stopNumber = 1)
        val expectedStopId = 42L

        coEvery { stopDao.insertStop(any()) } returns expectedStopId

        // When
        val returnedStopId = repository.insertStop(stop)

        // Then - Should convert Stop → StopEntity and call DAO
        coVerify {
            stopDao.insertStop(
                match { entity ->
                    entity.rideId == 1L &&
                        entity.stopNumber == 1 &&
                        entity.latitude == 37.7749 &&
                        entity.longitude == -122.4194 &&
                        entity.endTimestamp == null &&
                        entity.durationSeconds == null &&
                        entity.clusterId == null
                }
            )
        }
        assertThat(returnedStopId).isEqualTo(expectedStopId)
    }

    @Test
    fun `insertStop preserves all stop fields during mapping`() = runTest {
        // Given - Stop with all fields populated
        val stop = Stop(
            id = 5L,
            rideId = 10L,
            stopNumber = 3,
            latitude = 40.7128,
            longitude = -74.0060,
            startTimestamp = 1700000000000L,
            endTimestamp = 1700000015000L,
            durationSeconds = 15,
            clusterId = 100L
        )

        coEvery { stopDao.insertStop(any()) } returns 42L

        // When
        repository.insertStop(stop)

        // Then - All fields mapped correctly
        coVerify {
            stopDao.insertStop(
                match { entity ->
                    entity.id == 5L &&
                        entity.rideId == 10L &&
                        entity.stopNumber == 3 &&
                        entity.latitude == 40.7128 &&
                        entity.longitude == -74.0060 &&
                        entity.startTimestamp == 1700000000000L &&
                        entity.endTimestamp == 1700000015000L &&
                        entity.durationSeconds == 15 &&
                        entity.clusterId == 100L
                }
            )
        }
    }

    // ====================================================================================
    // UPDATE TESTS
    // ====================================================================================

    @Test
    fun `updateStopEnd delegates to dao with correct parameters`() = runTest {
        // Given
        val stopId = 42L
        val endTimestamp = 1700000015000L
        val durationSeconds = 15

        coEvery { stopDao.updateStopEnd(any(), any(), any()) } returns Unit

        // When
        repository.updateStopEnd(stopId, endTimestamp, durationSeconds)

        // Then
        coVerify {
            stopDao.updateStopEnd(stopId, endTimestamp, durationSeconds)
        }
    }

    // ====================================================================================
    // QUERY TESTS - Single Stop
    // ====================================================================================

    @Test
    fun `getStopById converts entity to domain model`() = runTest {
        // Given - DAO returns entity
        val entity = StopEntity(
            id = 42L,
            rideId = 10L,
            stopNumber = 1,
            latitude = 37.7749,
            longitude = -122.4194,
            startTimestamp = 1700000000000L,
            endTimestamp = 1700000015000L,
            durationSeconds = 15,
            clusterId = null
        )

        coEvery { stopDao.getStopById(42L) } returns entity

        // When
        val stop = repository.getStopById(42L)

        // Then - Converted to domain model
        assertThat(stop).isNotNull()
        assertThat(stop!!.id).isEqualTo(42L)
        assertThat(stop.rideId).isEqualTo(10L)
        assertThat(stop.stopNumber).isEqualTo(1)
        assertThat(stop.latitude).isEqualTo(37.7749)
        assertThat(stop.longitude).isEqualTo(-122.4194)
        assertThat(stop.startTimestamp).isEqualTo(1700000000000L)
        assertThat(stop.endTimestamp).isEqualTo(1700000015000L)
        assertThat(stop.durationSeconds).isEqualTo(15)
        assertThat(stop.clusterId).isNull()
    }

    @Test
    fun `getStopById returns null when dao returns null`() = runTest {
        // Given - DAO returns null (stop not found)
        coEvery { stopDao.getStopById(999L) } returns null

        // When
        val stop = repository.getStopById(999L)

        // Then
        assertThat(stop).isNull()
    }

    // ====================================================================================
    // QUERY TESTS - Stop List
    // ====================================================================================

    @Test
    fun `getStopsByRideId converts entity list to domain model list`() = runTest {
        // Given - DAO returns 3 entities
        val entities = listOf(
            createTestEntity(id = 1L, rideId = 10L, stopNumber = 1),
            createTestEntity(id = 2L, rideId = 10L, stopNumber = 2),
            createTestEntity(id = 3L, rideId = 10L, stopNumber = 3)
        )

        coEvery { stopDao.getStopsByRideId(10L) } returns entities

        // When
        val stops = repository.getStopsByRideId(10L)

        // Then - All entities converted
        assertThat(stops).hasSize(3)
        assertThat(stops[0].stopNumber).isEqualTo(1)
        assertThat(stops[1].stopNumber).isEqualTo(2)
        assertThat(stops[2].stopNumber).isEqualTo(3)
    }

    @Test
    fun `getStopsByRideId returns empty list when dao returns empty list`() = runTest {
        // Given - Ride with no stops
        coEvery { stopDao.getStopsByRideId(10L) } returns emptyList()

        // When
        val stops = repository.getStopsByRideId(10L)

        // Then
        assertThat(stops).isEmpty()
    }

    @Test
    fun `getUnclusteredStops converts entity list to domain model list`() = runTest {
        // Given - DAO returns unclustered stops
        val entities = listOf(
            createTestEntity(id = 1L, rideId = 10L, stopNumber = 1, clusterId = null),
            createTestEntity(id = 2L, rideId = 10L, stopNumber = 2, clusterId = null)
        )

        coEvery { stopDao.getUnclusteredStops() } returns entities

        // When
        val stops = repository.getUnclusteredStops()

        // Then
        assertThat(stops).hasSize(2)
        assertThat(stops.all { it.clusterId == null }).isTrue()
    }

    // ====================================================================================
    // FLOW TESTS
    // ====================================================================================

    @Test
    fun `getStopCountByRideId returns flow from dao`() = runTest {
        // Given - DAO returns Flow<Int>
        val countFlow = flowOf(0, 1, 2, 3)
        every { stopDao.getStopCountByRideId(10L) } returns countFlow

        // When
        val flow = repository.getStopCountByRideId(10L)

        // Then - Flow delegates directly to DAO (no mapping needed)
        assertThat(flow.first()).isEqualTo(0)
    }

    // ====================================================================================
    // DELETE TESTS
    // ====================================================================================

    @Test
    fun `deleteStopsByRideId delegates to dao`() = runTest {
        // Given
        coEvery { stopDao.deleteStopsByRideId(10L) } returns Unit

        // When
        repository.deleteStopsByRideId(10L)

        // Then
        coVerify {
            stopDao.deleteStopsByRideId(10L)
        }
    }

    // ====================================================================================
    // CLUSTER UPDATE TESTS (Feature 010)
    // ====================================================================================

    @Test
    fun `updateStopCluster delegates to dao`() = runTest {
        // Given
        coEvery { stopDao.updateStopCluster(42L, 100L) } returns Unit

        // When
        repository.updateStopCluster(stopId = 42L, clusterId = 100L)

        // Then
        coVerify {
            stopDao.updateStopCluster(42L, 100L)
        }
    }

    // ====================================================================================
    // DOMAIN MODEL VALIDATION TESTS
    // ====================================================================================

    @Test
    fun `Stop domain model validates latitude range`() {
        // When/Then - Invalid latitude throws exception
        try {
            Stop(
                rideId = 1L,
                stopNumber = 1,
                latitude = 91.0, // Invalid: outside [-90, 90]
                longitude = 0.0,
                startTimestamp = 1700000000000L
            )
            assert(false) { "Expected IllegalArgumentException for invalid latitude" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Latitude must be in range")
        }
    }

    @Test
    fun `Stop domain model validates longitude range`() {
        // When/Then - Invalid longitude throws exception
        try {
            Stop(
                rideId = 1L,
                stopNumber = 1,
                latitude = 0.0,
                longitude = 181.0, // Invalid: outside [-180, 180]
                startTimestamp = 1700000000000L
            )
            assert(false) { "Expected IllegalArgumentException for invalid longitude" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Longitude must be in range")
        }
    }

    @Test
    fun `Stop domain model validates stopNumber is positive`() {
        // When/Then - Zero or negative stopNumber throws exception
        try {
            Stop(
                rideId = 1L,
                stopNumber = 0, // Invalid: must be positive
                latitude = 0.0,
                longitude = 0.0,
                startTimestamp = 1700000000000L
            )
            assert(false) { "Expected IllegalArgumentException for invalid stopNumber" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Stop number must be positive")
        }
    }

    @Test
    fun `Stop domain model validates endTimestamp after startTimestamp`() {
        // When/Then - endTimestamp before startTimestamp throws exception
        try {
            Stop(
                rideId = 1L,
                stopNumber = 1,
                latitude = 0.0,
                longitude = 0.0,
                startTimestamp = 1700000015000L,
                endTimestamp = 1700000000000L, // Invalid: before start
                durationSeconds = 15
            )
            assert(false) { "Expected IllegalArgumentException for invalid timestamps" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("End timestamp")
            assertThat(e.message).contains("must be >= start timestamp")
        }
    }

    @Test
    fun `Stop domain model validates duration matches timestamps`() {
        // When/Then - durationSeconds doesn't match timestamps
        try {
            Stop(
                rideId = 1L,
                stopNumber = 1,
                latitude = 0.0,
                longitude = 0.0,
                startTimestamp = 1700000000000L,
                endTimestamp = 1700000015000L,
                durationSeconds = 999 // Invalid: should be 15
            )
            assert(false) { "Expected IllegalArgumentException for invalid duration" }
        } catch (e: IllegalArgumentException) {
            assertThat(e.message).contains("Duration")
            assertThat(e.message).contains("does not match timestamps")
        }
    }

    // ====================================================================================
    // HELPER METHODS
    // ====================================================================================

    /**
     * Create test Stop domain model.
     */
    private fun createTestStop(
        id: Long = 0,
        rideId: Long = 1L,
        stopNumber: Int = 1,
        latitude: Double = 37.7749,
        longitude: Double = -122.4194,
        startTimestamp: Long = 1700000000000L
    ): Stop {
        return Stop(
            id = id,
            rideId = rideId,
            stopNumber = stopNumber,
            latitude = latitude,
            longitude = longitude,
            startTimestamp = startTimestamp,
            endTimestamp = null,
            durationSeconds = null,
            clusterId = null
        )
    }

    /**
     * Create test StopEntity.
     */
    private fun createTestEntity(
        id: Long = 0,
        rideId: Long = 1L,
        stopNumber: Int = 1,
        latitude: Double = 37.7749,
        longitude: Double = -122.4194,
        startTimestamp: Long = 1700000000000L,
        clusterId: Long? = null
    ): StopEntity {
        return StopEntity(
            id = id,
            rideId = rideId,
            stopNumber = stopNumber,
            latitude = latitude,
            longitude = longitude,
            startTimestamp = startTimestamp,
            endTimestamp = null,
            durationSeconds = null,
            clusterId = clusterId
        )
    }
}
