package com.example.bikeredlights.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bikeredlights.data.local.dao.RideDao
import com.example.bikeredlights.data.local.dao.StopDao
import com.example.bikeredlights.data.local.entity.Ride
import com.example.bikeredlights.data.local.entity.StopEntity
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for StopDao (Feature 009).
 *
 * Tests all stop persistence operations, Flow emissions, and cascade delete behavior.
 * Uses in-memory database for fast, isolated testing.
 *
 * Test Coverage:
 * - Insert stops and verify return ID
 * - Update stop end timestamp and duration
 * - Retrieve stops by ride ID
 * - Reactive stop count Flow emissions
 * - Unclustered stop queries
 * - Cluster ID updates
 * - CASCADE delete verification (stops deleted when parent ride deleted)
 * - Foreign key constraint enforcement
 * - NULL cluster_id handling
 */
@RunWith(AndroidJUnit4::class)
class StopDaoTest {

    private lateinit var database: BikeRedlightsDatabase
    private lateinit var stopDao: StopDao
    private lateinit var rideDao: RideDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            BikeRedlightsDatabase::class.java
        ).build()

        stopDao = database.stopDao()
        rideDao = database.rideDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    // ========================================
    // INSERT TESTS
    // ========================================

    @Test
    fun insertStop_returnsGeneratedId() = runTest {
        // Given
        val rideId = createTestRide()
        val stop = createTestStop(rideId = rideId, stopNumber = 1)

        // When
        val stopId = stopDao.insertStop(stop)

        // Then
        assertThat(stopId).isGreaterThan(0)
    }

    @Test
    fun insertStop_andRetrieveById_returnsCorrectStop() = runTest {
        // Given
        val rideId = createTestRide()
        val stop = createTestStop(
            rideId = rideId,
            stopNumber = 1,
            latitude = 46.2064,
            longitude = 6.1416,
            startTimestamp = 1000L
        )

        // When
        val stopId = stopDao.insertStop(stop)
        val retrieved = stopDao.getStopById(stopId)

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.rideId).isEqualTo(rideId)
        assertThat(retrieved?.stopNumber).isEqualTo(1)
        assertThat(retrieved?.latitude).isEqualTo(46.2064)
        assertThat(retrieved?.longitude).isEqualTo(6.1416)
        assertThat(retrieved?.startTimestamp).isEqualTo(1000L)
        assertThat(retrieved?.endTimestamp).isNull()
        assertThat(retrieved?.durationSeconds).isNull()
        assertThat(retrieved?.clusterId).isNull()
    }

    @Test
    fun insertStop_withNonExistentRide_throwsForeignKeyException() = runTest {
        // Given
        val stop = createTestStop(rideId = 99999L, stopNumber = 1)

        // When/Then - Foreign key constraint should fail
        try {
            stopDao.insertStop(stop)
            throw AssertionError("Expected SQLiteConstraintException")
        } catch (e: android.database.sqlite.SQLiteConstraintException) {
            // Expected - foreign key constraint violation
            assertThat(e.message).contains("FOREIGN KEY")
        }
    }

    // ========================================
    // UPDATE TESTS
    // ========================================

    @Test
    fun updateStopEnd_updatesEndTimestampAndDuration() = runTest {
        // Given
        val rideId = createTestRide()
        val stop = createTestStop(rideId = rideId, stopNumber = 1, startTimestamp = 1000L)
        val stopId = stopDao.insertStop(stop)

        // When
        val endTimestamp = 16000L
        val duration = (endTimestamp - 1000L).toInt() / 1000 // 15 seconds
        stopDao.updateStopEnd(stopId, endTimestamp, duration)

        // Then
        val updated = stopDao.getStopById(stopId)
        assertThat(updated?.endTimestamp).isEqualTo(16000L)
        assertThat(updated?.durationSeconds).isEqualTo(15)
    }

    @Test
    fun updateStopEnd_withNonExistentStop_doesNothing() = runTest {
        // When - Update non-existent stop
        stopDao.updateStopEnd(99999L, 16000L, 15)

        // Then - Should not throw, just no-op
        val retrieved = stopDao.getStopById(99999L)
        assertThat(retrieved).isNull()
    }

    // ========================================
    // QUERY TESTS
    // ========================================

    @Test
    fun getStopsByRideId_returnsStopsOrderedByStopNumber() = runTest {
        // Given
        val rideId = createTestRide()
        val stop1 = createTestStop(rideId = rideId, stopNumber = 1, startTimestamp = 1000L)
        val stop3 = createTestStop(rideId = rideId, stopNumber = 3, startTimestamp = 3000L)
        val stop2 = createTestStop(rideId = rideId, stopNumber = 2, startTimestamp = 2000L)

        // Insert in non-sequential order
        stopDao.insertStop(stop1)
        stopDao.insertStop(stop3)
        stopDao.insertStop(stop2)

        // When
        val stops = stopDao.getStopsByRideId(rideId)

        // Then - Should be ordered by stop_number ASC (1, 2, 3)
        assertThat(stops).hasSize(3)
        assertThat(stops[0].stopNumber).isEqualTo(1)
        assertThat(stops[1].stopNumber).isEqualTo(2)
        assertThat(stops[2].stopNumber).isEqualTo(3)
    }

    @Test
    fun getStopsByRideId_withNonExistentRide_returnsEmptyList() = runTest {
        // When
        val stops = stopDao.getStopsByRideId(99999L)

        // Then
        assertThat(stops).isEmpty()
    }

    @Test
    fun getStopsByRideId_withMultipleRides_returnsOnlyRequestedRideStops() = runTest {
        // Given
        val rideId1 = createTestRide()
        val rideId2 = createTestRide()

        stopDao.insertStop(createTestStop(rideId = rideId1, stopNumber = 1))
        stopDao.insertStop(createTestStop(rideId = rideId1, stopNumber = 2))
        stopDao.insertStop(createTestStop(rideId = rideId2, stopNumber = 1))

        // When
        val ride1Stops = stopDao.getStopsByRideId(rideId1)
        val ride2Stops = stopDao.getStopsByRideId(rideId2)

        // Then
        assertThat(ride1Stops).hasSize(2)
        assertThat(ride2Stops).hasSize(1)
    }

    @Test
    fun getStopById_withNonExistentId_returnsNull() = runTest {
        // When
        val retrieved = stopDao.getStopById(99999L)

        // Then
        assertThat(retrieved).isNull()
    }

    // ========================================
    // FLOW (REACTIVE) TESTS
    // ========================================

    @Test
    fun getStopCountByRideId_emitsZeroForNewRide() = runTest {
        // Given
        val rideId = createTestRide()

        // When
        val count = stopDao.getStopCountByRideId(rideId).first()

        // Then
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun getStopCountByRideId_emitsCorrectCountAfterInserts() = runTest {
        // Given
        val rideId = createTestRide()

        // Insert stops one by one
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 1))
        val count1 = stopDao.getStopCountByRideId(rideId).first()

        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 2))
        val count2 = stopDao.getStopCountByRideId(rideId).first()

        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 3))
        val count3 = stopDao.getStopCountByRideId(rideId).first()

        // Then - Flow should emit updated counts
        assertThat(count1).isEqualTo(1)
        assertThat(count2).isEqualTo(2)
        assertThat(count3).isEqualTo(3)
    }

    @Test
    fun getStopCountByRideId_withMultipleRides_returnsOnlyRequestedRideCount() = runTest {
        // Given
        val rideId1 = createTestRide()
        val rideId2 = createTestRide()

        stopDao.insertStop(createTestStop(rideId = rideId1, stopNumber = 1))
        stopDao.insertStop(createTestStop(rideId = rideId1, stopNumber = 2))
        stopDao.insertStop(createTestStop(rideId = rideId2, stopNumber = 1))

        // When
        val count1 = stopDao.getStopCountByRideId(rideId1).first()
        val count2 = stopDao.getStopCountByRideId(rideId2).first()

        // Then
        assertThat(count1).isEqualTo(2)
        assertThat(count2).isEqualTo(1)
    }

    // ========================================
    // UNCLUSTERED STOPS (Feature 010 prep)
    // ========================================

    @Test
    fun getUnclusteredStops_returnsStopsWithNullClusterId() = runTest {
        // Given
        val rideId = createTestRide()
        val stop1 = createTestStop(rideId = rideId, stopNumber = 1, startTimestamp = 1000L)
        val stop2 = createTestStop(rideId = rideId, stopNumber = 2, startTimestamp = 2000L)
        val stop3 = createTestStop(rideId = rideId, stopNumber = 3, startTimestamp = 3000L)

        val stopId1 = stopDao.insertStop(stop1)
        stopDao.insertStop(stop2)
        val stopId3 = stopDao.insertStop(stop3)

        // Assign cluster to stop1 and stop3
        stopDao.updateStopCluster(stopId1, 100L)
        stopDao.updateStopCluster(stopId3, 101L)

        // When
        val unclusteredStops = stopDao.getUnclusteredStops()

        // Then - Only stop2 should be returned (cluster_id = NULL)
        assertThat(unclusteredStops).hasSize(1)
        assertThat(unclusteredStops[0].stopNumber).isEqualTo(2)
        assertThat(unclusteredStops[0].clusterId).isNull()
    }

    @Test
    fun getUnclusteredStops_returnsStopsOrderedByStartTimestamp() = runTest {
        // Given
        val rideId = createTestRide()
        val stop1 = createTestStop(rideId = rideId, stopNumber = 1, startTimestamp = 3000L)
        val stop2 = createTestStop(rideId = rideId, stopNumber = 2, startTimestamp = 1000L)
        val stop3 = createTestStop(rideId = rideId, stopNumber = 3, startTimestamp = 2000L)

        stopDao.insertStop(stop1)
        stopDao.insertStop(stop2)
        stopDao.insertStop(stop3)

        // When
        val unclusteredStops = stopDao.getUnclusteredStops()

        // Then - Should be ordered by start_timestamp ASC (oldest first)
        assertThat(unclusteredStops).hasSize(3)
        assertThat(unclusteredStops[0].startTimestamp).isEqualTo(1000L)
        assertThat(unclusteredStops[1].startTimestamp).isEqualTo(2000L)
        assertThat(unclusteredStops[2].startTimestamp).isEqualTo(3000L)
    }

    @Test
    fun getUnclusteredStops_withAllClustered_returnsEmptyList() = runTest {
        // Given
        val rideId = createTestRide()
        val stopId1 = stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 1))
        val stopId2 = stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 2))

        // Assign all stops to clusters
        stopDao.updateStopCluster(stopId1, 100L)
        stopDao.updateStopCluster(stopId2, 100L)

        // When
        val unclusteredStops = stopDao.getUnclusteredStops()

        // Then
        assertThat(unclusteredStops).isEmpty()
    }

    // ========================================
    // CLUSTER UPDATE TESTS
    // ========================================

    @Test
    fun updateStopCluster_assignsClusterIdToStop() = runTest {
        // Given
        val rideId = createTestRide()
        val stop = createTestStop(rideId = rideId, stopNumber = 1)
        val stopId = stopDao.insertStop(stop)

        // Verify initially NULL
        val beforeUpdate = stopDao.getStopById(stopId)
        assertThat(beforeUpdate?.clusterId).isNull()

        // When
        stopDao.updateStopCluster(stopId, 42L)

        // Then
        val afterUpdate = stopDao.getStopById(stopId)
        assertThat(afterUpdate?.clusterId).isEqualTo(42L)
    }

    @Test
    fun updateStopCluster_withNonExistentStop_doesNothing() = runTest {
        // When - Update non-existent stop
        stopDao.updateStopCluster(99999L, 42L)

        // Then - Should not throw, just no-op
        val retrieved = stopDao.getStopById(99999L)
        assertThat(retrieved).isNull()
    }

    // ========================================
    // DELETE TESTS
    // ========================================

    @Test
    fun deleteStopsByRideId_removesAllStopsForRide() = runTest {
        // Given
        val rideId = createTestRide()
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 1))
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 2))
        stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 3))

        // Verify stops exist
        val beforeDelete = stopDao.getStopsByRideId(rideId)
        assertThat(beforeDelete).hasSize(3)

        // When
        stopDao.deleteStopsByRideId(rideId)

        // Then
        val afterDelete = stopDao.getStopsByRideId(rideId)
        assertThat(afterDelete).isEmpty()
    }

    @Test
    fun deleteStopsByRideId_withNonExistentRide_doesNothing() = runTest {
        // When
        stopDao.deleteStopsByRideId(99999L)

        // Then - Should not throw, just no-op
        val stops = stopDao.getStopsByRideId(99999L)
        assertThat(stops).isEmpty()
    }

    // ========================================
    // CASCADE DELETE TESTS (Critical!)
    // ========================================

    @Test
    fun deleteRide_cascadeDeletesStops() = runTest {
        // Given - Create ride with stops
        val ride = createTestRide()
        val rideId = rideDao.insertRide(ride)

        val stop1 = createTestStop(rideId = rideId, stopNumber = 1)
        val stop2 = createTestStop(rideId = rideId, stopNumber = 2)
        stopDao.insertStop(stop1)
        stopDao.insertStop(stop2)

        // Verify stops exist
        val stopsBeforeDelete = stopDao.getStopsByRideId(rideId)
        assertThat(stopsBeforeDelete).hasSize(2)

        // When - Delete parent ride
        val toDelete = rideDao.getRideById(rideId)!!
        rideDao.deleteRide(toDelete)

        // Then - Stops should be CASCADE deleted
        val stopsAfterDelete = stopDao.getStopsByRideId(rideId)
        assertThat(stopsAfterDelete).isEmpty()
    }

    @Test
    fun deleteRide_cascadeDeletesStopsWithClusters() = runTest {
        // Given - Create ride with clustered stops
        val ride = createTestRide()
        val rideId = rideDao.insertRide(ride)

        val stopId1 = stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 1))
        val stopId2 = stopDao.insertStop(createTestStop(rideId = rideId, stopNumber = 2))

        // Assign clusters
        stopDao.updateStopCluster(stopId1, 100L)
        stopDao.updateStopCluster(stopId2, 100L)

        // When - Delete parent ride
        val toDelete = rideDao.getRideById(rideId)!!
        rideDao.deleteRide(toDelete)

        // Then - All stops (even clustered ones) should be deleted
        val stopsAfterDelete = stopDao.getStopsByRideId(rideId)
        assertThat(stopsAfterDelete).isEmpty()

        // Verify stops are gone from database
        val stop1AfterDelete = stopDao.getStopById(stopId1)
        val stop2AfterDelete = stopDao.getStopById(stopId2)
        assertThat(stop1AfterDelete).isNull()
        assertThat(stop2AfterDelete).isNull()
    }

    // ========================================
    // HELPER FUNCTIONS
    // ========================================

    private suspend fun createTestRide(
        name: String = "Test Ride",
        startTime: Long = System.currentTimeMillis()
    ): Long {
        val ride = Ride(
            id = 0,  // Auto-generated
            name = name,
            startTime = startTime,
            endTime = null,
            elapsedDurationMillis = 0L,
            movingDurationMillis = 0L,
            manualPausedDurationMillis = 0L,
            autoPausedDurationMillis = 0L,
            distanceMeters = 0.0,
            avgSpeedMetersPerSec = 0.0,
            maxSpeedMetersPerSec = 0.0
        )
        return rideDao.insertRide(ride)
    }

    private fun createTestStop(
        rideId: Long,
        stopNumber: Int,
        latitude: Double = 46.2064,
        longitude: Double = 6.1416,
        startTimestamp: Long = System.currentTimeMillis()
    ): StopEntity {
        return StopEntity(
            id = 0,  // Auto-generated
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
