package com.example.bikeredlights.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.bikeredlights.data.local.dao.RideDao
import com.example.bikeredlights.data.local.dao.TrackPointDao
import com.example.bikeredlights.data.local.entity.Ride
import com.example.bikeredlights.data.local.entity.TrackPoint
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for RideDao.
 *
 * Tests all CRUD operations, Flow emissions, and cascade delete behavior.
 * Uses in-memory database for fast, isolated testing.
 *
 * Test Coverage:
 * - Insert and retrieve rides
 * - Update ride statistics
 * - Delete rides
 * - Query incomplete rides
 * - Flow-based reactive queries
 * - CASCADE delete verification
 */
@RunWith(AndroidJUnit4::class)
class RideDaoTest {

    private lateinit var database: BikeRedlightsDatabase
    private lateinit var rideDao: RideDao
    private lateinit var trackPointDao: TrackPointDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(
            context,
            BikeRedlightsDatabase::class.java
        ).build()

        rideDao = database.rideDao()
        trackPointDao = database.trackPointDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun insertRide_returnsGeneratedId() = runTest {
        // Given
        val ride = createTestRide(name = "Test Ride")

        // When
        val rideId = rideDao.insertRide(ride)

        // Then
        assertThat(rideId).isGreaterThan(0)
    }

    @Test
    fun insertRide_andRetrieveById_returnsCorrectRide() = runTest {
        // Given
        val ride = createTestRide(name = "Morning Commute")

        // When
        val rideId = rideDao.insertRide(ride)
        val retrieved = rideDao.getRideById(rideId)

        // Then
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.name).isEqualTo("Morning Commute")
        assertThat(retrieved?.startTime).isEqualTo(ride.startTime)
        assertThat(retrieved?.distanceMeters).isEqualTo(ride.distanceMeters)
    }

    @Test
    fun getRideById_withNonExistentId_returnsNull() = runTest {
        // When
        val retrieved = rideDao.getRideById(99999L)

        // Then
        assertThat(retrieved).isNull()
    }

    @Test
    fun updateRide_updatesExistingRide() = runTest {
        // Given
        val ride = createTestRide(name = "Original", distanceMeters = 100.0)
        val rideId = rideDao.insertRide(ride)

        // When
        val updated = ride.copy(
            id = rideId,
            name = "Updated",
            distanceMeters = 500.0,
            endTime = System.currentTimeMillis()
        )
        rideDao.updateRide(updated)

        // Then
        val retrieved = rideDao.getRideById(rideId)
        assertThat(retrieved?.name).isEqualTo("Updated")
        assertThat(retrieved?.distanceMeters).isEqualTo(500.0)
        assertThat(retrieved?.endTime).isNotNull()
    }

    @Test
    fun deleteRide_removesRideFromDatabase() = runTest {
        // Given
        val ride = createTestRide(name = "To Delete")
        val rideId = rideDao.insertRide(ride)

        // When
        val toDelete = rideDao.getRideById(rideId)!!
        rideDao.deleteRide(toDelete)

        // Then
        val retrieved = rideDao.getRideById(rideId)
        assertThat(retrieved).isNull()
    }

    @Test
    fun getIncompleteRides_returnsOnlyRidesWithNullEndTime() = runTest {
        // Given - Insert completed and incomplete rides
        val completedRide = createTestRide(
            name = "Completed",
            endTime = System.currentTimeMillis()
        )
        val incompleteRide1 = createTestRide(name = "Incomplete 1", endTime = null)
        val incompleteRide2 = createTestRide(name = "Incomplete 2", endTime = null)

        rideDao.insertRide(completedRide)
        rideDao.insertRide(incompleteRide1)
        rideDao.insertRide(incompleteRide2)

        // When
        val incompleteRides = rideDao.getIncompleteRides()

        // Then
        assertThat(incompleteRides).hasSize(2)
        assertThat(incompleteRides.map { it.name }).containsExactly("Incomplete 1", "Incomplete 2")
    }

    @Test
    fun getAllRidesFlow_emitsAllRides() = runTest {
        // Given
        val ride1 = createTestRide(name = "Ride 1", startTime = 1000L)
        val ride2 = createTestRide(name = "Ride 2", startTime = 2000L)

        rideDao.insertRide(ride1)
        rideDao.insertRide(ride2)

        // When
        val rides = rideDao.getAllRidesFlow().first()

        // Then
        assertThat(rides).hasSize(2)
        // Should be ordered by start_time DESC (most recent first)
        assertThat(rides[0].name).isEqualTo("Ride 2")
        assertThat(rides[1].name).isEqualTo("Ride 1")
    }

    @Test
    fun getAllRides_returnsRidesOrderedByStartTimeDescending() = runTest {
        // Given
        val ride1 = createTestRide(name = "Old Ride", startTime = 1000L)
        val ride2 = createTestRide(name = "New Ride", startTime = 3000L)
        val ride3 = createTestRide(name = "Middle Ride", startTime = 2000L)

        rideDao.insertRide(ride1)
        rideDao.insertRide(ride2)
        rideDao.insertRide(ride3)

        // When
        val rides = rideDao.getAllRides()

        // Then
        assertThat(rides).hasSize(3)
        assertThat(rides[0].name).isEqualTo("New Ride")
        assertThat(rides[1].name).isEqualTo("Middle Ride")
        assertThat(rides[2].name).isEqualTo("Old Ride")
    }

    @Test
    fun deleteRide_cascadeDeletesTrackPoints() = runTest {
        // Given - Create ride with track points
        val ride = createTestRide(name = "Ride with Points")
        val rideId = rideDao.insertRide(ride)

        val trackPoint1 = createTestTrackPoint(rideId = rideId, latitude = 37.7749)
        val trackPoint2 = createTestTrackPoint(rideId = rideId, latitude = 37.7750)
        trackPointDao.insertTrackPoint(trackPoint1)
        trackPointDao.insertTrackPoint(trackPoint2)

        // Verify track points exist
        val pointsBeforeDelete = trackPointDao.getTrackPointsForRide(rideId)
        assertThat(pointsBeforeDelete).hasSize(2)

        // When - Delete parent ride
        val toDelete = rideDao.getRideById(rideId)!!
        rideDao.deleteRide(toDelete)

        // Then - Track points should be CASCADE deleted
        val pointsAfterDelete = trackPointDao.getTrackPointsForRide(rideId)
        assertThat(pointsAfterDelete).isEmpty()
    }

    // Helper functions

    private fun createTestRide(
        name: String = "Test Ride",
        startTime: Long = System.currentTimeMillis(),
        endTime: Long? = null,
        distanceMeters: Double = 0.0
    ): Ride {
        return Ride(
            id = 0,  // Auto-generated
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

    private fun createTestTrackPoint(
        rideId: Long,
        latitude: Double = 37.7749,
        longitude: Double = -122.4194
    ): TrackPoint {
        return TrackPoint(
            id = 0,  // Auto-generated
            rideId = rideId,
            timestamp = System.currentTimeMillis(),
            latitude = latitude,
            longitude = longitude,
            speedMetersPerSec = 5.0,
            accuracy = 10.0f,
            isManuallyPaused = false,
            isAutoPaused = false
        )
    }
}
