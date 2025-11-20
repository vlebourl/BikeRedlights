package com.example.bikeredlights.data.local.dao

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import com.example.bikeredlights.data.local.BikeRedlightsDatabase
import com.example.bikeredlights.data.local.entity.RideEntity
import com.example.bikeredlights.data.local.entity.StopEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

/**
 * Instrumented tests for StopDao (Feature 009).
 *
 * Tests cover:
 * - Basic CRUD operations (insert, update, query)
 * - CASCADE delete behavior (foreign key)
 * - UNIQUE constraint on (ride_id, stop_number)
 * - Flow reactivity for stop count
 * - Edge cases (invalid ride ID, duplicate stop number)
 *
 * Test Strategy:
 * - In-memory database for isolation
 * - Fresh database instance per test
 * - No mocking - tests actual Room behavior
 */
@RunWith(AndroidJUnit4::class)
class StopDaoTest {

    private lateinit var database: BikeRedlightsDatabase
    private lateinit var stopDao: StopDao
    private lateinit var rideDao: RideDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            BikeRedlightsDatabase::class.java
        )
            .allowMainThreadQueries() // OK for tests
            .build()

        stopDao = database.stopDao()
        rideDao = database.rideDao()
    }

    @After
    @Throws(IOException::class)
    fun closeDb() {
        database.close()
    }

    // ====================================================================================
    // INSERT TESTS
    // ====================================================================================

    @Test
    fun insertStop_returnsGeneratedId() = runTest {
        // Given - Create parent ride
        val rideId = createTestRide()

        // When - Insert stop
        val stop = createTestStop(rideId = rideId, stopNumber = 1)
        val stopId = stopDao.insertStop(stop)

        // Then - Should return generated ID
        assertThat(stopId).isGreaterThan(0)
    }

    @Test
    fun insertStop_persistsAllFields() = runTest {
        // Given
        val rideId = createTestRide()
        val stop = createTestStop(
            rideId = rideId,
            stopNumber = 1,
            latitude = 37.7749,
            longitude = -122.4194,
            startTimestamp = 1700000000000L
        )

        // When
        val stopId = stopDao.insertStop(stop)
        val retrieved = stopDao.getStopById(stopId)

        // Then - All fields match
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.rideId).isEqualTo(rideId)
        assertThat(retrieved.stopNumber).isEqualTo(1)
        assertThat(retrieved.latitude).isEqualTo(37.7749)
        assertThat(retrieved.longitude).isEqualTo(-122.4194)
        assertThat(retrieved.startTimestamp).isEqualTo(1700000000000L)
        assertThat(retrieved.endTimestamp).isNull()
        assertThat(retrieved.durationSeconds).isNull()
        assertThat(retrieved.clusterId).isNull()
    }

    @Test
    fun insertStop_withInvalidRideId_throwsException() = runTest {
        // Given - No ride exists with ID 999
        val stop = createTestStop(rideId = 999L, stopNumber = 1)

        // When/Then - Should throw foreign key constraint violation
        try {
            stopDao.insertStop(stop)
            assert(false) { "Expected foreign key constraint exception" }
        } catch (e: Exception) {
            // Expected - foreign key constraint violated
            assertThat(e.message).contains("FOREIGN KEY constraint failed")
        }
    }

    @Test
    fun insertStop_withDuplicateStopNumber_throwsException() = runTest {
        // Given - Insert first stop
        val rideId = createTestRide()
        val stop1 = createTestStop(rideId = rideId, stopNumber = 1)
        stopDao.insertStop(stop1)

        // When/Then - Insert second stop with same stopNumber
        val stop2 = createTestStop(rideId = rideId, stopNumber = 1)
        try {
            stopDao.insertStop(stop2)
            assert(false) { "Expected UNIQUE constraint exception" }
        } catch (e: Exception) {
            // Expected - UNIQUE constraint violated
            assertThat(e.message).contains("UNIQUE constraint failed")
        }
    }

    @Test
    fun insertStop_withDifferentRideIds_allowsSameStopNumber() = runTest {
        // Given - Two different rides
        val rideId1 = createTestRide()
        val rideId2 = createTestRide()

        // When - Insert stop with stopNumber=1 for both rides
        val stop1 = createTestStop(rideId = rideId1, stopNumber = 1)
        val stop2 = createTestStop(rideId = rideId2, stopNumber = 1)

        val stopId1 = stopDao.insertStop(stop1)
        val stopId2 = stopDao.insertStop(stop2)

        // Then - Both inserts succeed (different rides)
        assertThat(stopId1).isGreaterThan(0)
        assertThat(stopId2).isGreaterThan(0)
        assertThat(stopId1).isNotEqualTo(stopId2)
    }

    // ====================================================================================
    // UPDATE TESTS
    // ====================================================================================

    @Test
    fun updateStopEnd_setsEndTimestampAndDuration() = runTest {
        // Given - Insert stop without end time
        val rideId = createTestRide()
        val stop = createTestStop(rideId = rideId, startTimestamp = 1700000000000L)
        val stopId = stopDao.insertStop(stop)

        // When - Update end time
        val endTimestamp = 1700000015000L
        val durationSeconds = 15
        stopDao.updateStopEnd(stopId, endTimestamp, durationSeconds)

        // Then - Fields updated
        val updated = stopDao.getStopById(stopId)
        assertThat(updated!!.endTimestamp).isEqualTo(endTimestamp)
        assertThat(updated.durationSeconds).isEqualTo(durationSeconds)
    }

    @Test
    fun updateStopEnd_withInvalidStopId_doesNotThrow() = runTest {
        // When - Update non-existent stop (Room doesn't throw, just updates 0 rows)
        stopDao.updateStopEnd(stopId = 999L, endTimestamp = 1700000000000L, durationSeconds = 15)

        // Then - No exception thrown (Room behavior)
    }

    // ====================================================================================
    // QUERY TESTS
    // ====================================================================================

    @Test
    fun getStopsByRideId_returnsOrderedByStopNumber() = runTest {
        // Given - Insert 3 stops in non-sequential order
        val rideId = createTestRide()
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 3))
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 1))
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 2))

        // When
        val stops = stopDao.getStopsByRideId(rideId)

        // Then - Returned in order: 1, 2, 3
        assertThat(stops).hasSize(3)
        assertThat(stops[0].stopNumber).isEqualTo(1)
        assertThat(stops[1].stopNumber).isEqualTo(2)
        assertThat(stops[2].stopNumber).isEqualTo(3)
    }

    @Test
    fun getStopsByRideId_withNoStops_returnsEmptyList() = runTest {
        // Given - Ride with no stops
        val rideId = createTestRide()

        // When
        val stops = stopDao.getStopsByRideId(rideId)

        // Then
        assertThat(stops).isEmpty()
    }

    @Test
    fun getStopCountByRideId_reactsToInserts() = runTest {
        // Given
        val rideId = createTestRide()

        // When/Then - Flow emits updates as stops inserted
        stopDao.getStopCountByRideId(rideId).test {
            assertThat(awaitItem()).isEqualTo(0) // Initial count

            stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 1))
            assertThat(awaitItem()).isEqualTo(1)

            stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 2))
            assertThat(awaitItem()).isEqualTo(2)

            stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 3))
            assertThat(awaitItem()).isEqualTo(3)

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun getStopById_withValidId_returnsStop() = runTest {
        // Given
        val rideId = createTestRide()
        val stop = createTestStop(rideId = rideId, stopNumber = 1)
        val stopId = stopDao.insertStop(stop)

        // When
        val retrieved = stopDao.getStopById(stopId)

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved!!.id).isEqualTo(stopId)
    }

    @Test
    fun getStopById_withInvalidId_returnsNull() = runTest {
        // When
        val retrieved = stopDao.getStopById(999L)

        // Then
        assertThat(retrieved).isNull()
    }

    @Test
    fun getUnclusteredStops_returnsOnlyStopsWithoutClusterId() = runTest {
        // Given - Insert stops, some with cluster_id
        val rideId = createTestRide()
        val stopId1 = stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 1))
        val stopId2 = stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 2))
        val stopId3 = stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 3))

        // Assign cluster to stop 2 only
        stopDao.updateStopCluster(stopId2, clusterId = 100L)

        // When
        val unclustered = stopDao.getUnclusteredStops()

        // Then - Only stops 1 and 3 returned
        assertThat(unclustered).hasSize(2)
        assertThat(unclustered.map { it.id }).containsExactly(stopId1, stopId3)
    }

    @Test
    fun getUnclusteredStops_orderedByStartTimestamp() = runTest {
        // Given - Insert stops with different timestamps
        val rideId = createTestRide()
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 1, startTimestamp = 1700000020000L))
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 2, startTimestamp = 1700000010000L))
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 3, startTimestamp = 1700000030000L))

        // When
        val stops = stopDao.getUnclusteredStops()

        // Then - Ordered by timestamp: 2, 1, 3
        assertThat(stops).hasSize(3)
        assertThat(stops[0].stopNumber).isEqualTo(2)
        assertThat(stops[1].stopNumber).isEqualTo(1)
        assertThat(stops[2].stopNumber).isEqualTo(3)
    }

    // ====================================================================================
    // CASCADE DELETE TESTS
    // ====================================================================================

    @Test
    fun deleteRide_cascadesDeleteToStops() = runTest {
        // Given - Ride with 3 stops
        val rideId = createTestRide()
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 1))
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 2))
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 3))

        // Verify stops exist
        assertThat(stopDao.getStopsByRideId(rideId)).hasSize(3)

        // When - Delete parent ride
        rideDao.deleteRide(rideId)

        // Then - All stops automatically deleted via CASCADE
        assertThat(stopDao.getStopsByRideId(rideId)).isEmpty()
    }

    @Test
    fun deleteStopsByRideId_removesAllStopsForRide() = runTest {
        // Given - Two rides with stops
        val rideId1 = createTestRide()
        val rideId2 = createTestRide()

        stopDao.insertStop(createTestStop(rideId = rideId1, stopNumber = 1))
        stopDao.insertStop(createTestStop(rideId = rideId1, stopNumber = 2))
        stopDao.insertStop(createTestStop(rideId = rideId2, stopNumber = 1))

        // When - Delete stops for ride 1 only
        stopDao.deleteStopsByRideId(rideId1)

        // Then
        assertThat(stopDao.getStopsByRideId(rideId1)).isEmpty()
        assertThat(stopDao.getStopsByRideId(rideId2)).hasSize(1) // Ride 2 stops unaffected
    }

    // ====================================================================================
    // HELPER METHODS
    // ====================================================================================

    /**
     * Create a test ride and return its ID.
     */
    private suspend fun createTestRide(): Long {
        val ride = RideEntity(
            id = 0,
            name = "Test Ride",
            startTime = 1700000000000L,
            endTime = null,
            elapsedDurationMillis = 0L,
            movingDurationMillis = 0L,
            manualPausedDurationMillis = 0L,
            autoPausedDurationMillis = 0L,
            distanceMeters = 0.0,
            avgSpeedMps = 0.0,
            maxSpeedMps = 0.0
        )
        return rideDao.insertRide(ride)
    }

    /**
     * Create a test stop entity.
     */
    private fun createTestStop(
        rideId: Long,
        stopNumber: Int = 1,
        latitude: Double = 37.7749,
        longitude: Double = -122.4194,
        startTimestamp: Long = 1700000000000L
    ): StopEntity {
        return StopEntity(
            id = 0,
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
}
