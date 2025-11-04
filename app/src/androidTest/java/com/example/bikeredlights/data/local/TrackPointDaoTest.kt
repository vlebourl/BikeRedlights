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
 * Instrumented tests for TrackPointDao.
 *
 * Tests all TrackPoint operations including:
 * - Single and batch insertions
 * - Query operations
 * - Flow-based reactive queries
 * - Last track point retrieval
 * - Count operations
 *
 * Test Coverage:
 * - Insert single track points
 * - Batch insert track points
 * - Query track points for a ride
 * - Flow emissions for live updates
 * - Get last track point for distance calculations
 * - Count track points
 * - Ordering by timestamp
 */
@RunWith(AndroidJUnit4::class)
class TrackPointDaoTest {

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
    fun insertTrackPoint_returnsGeneratedId() = runTest {
        // Given
        val rideId = createTestRide()
        val trackPoint = createTestTrackPoint(rideId = rideId)

        // When
        val trackPointId = trackPointDao.insertTrackPoint(trackPoint)

        // Then
        assertThat(trackPointId).isGreaterThan(0)
    }

    @Test
    fun insertTrackPoint_andRetrieve_returnsCorrectData() = runTest {
        // Given
        val rideId = createTestRide()
        val trackPoint = createTestTrackPoint(
            rideId = rideId,
            latitude = 37.7749,
            longitude = -122.4194,
            speedMetersPerSec = 5.5
        )

        // When
        trackPointDao.insertTrackPoint(trackPoint)
        val retrieved = trackPointDao.getTrackPointsForRide(rideId)

        // Then
        assertThat(retrieved).hasSize(1)
        assertThat(retrieved[0].latitude).isEqualTo(37.7749)
        assertThat(retrieved[0].longitude).isEqualTo(-122.4194)
        assertThat(retrieved[0].speedMetersPerSec).isEqualTo(5.5)
    }

    @Test
    fun insertAllTrackPoints_insertsMultiplePoints() = runTest {
        // Given
        val rideId = createTestRide()
        val trackPoints = listOf(
            createTestTrackPoint(rideId, latitude = 37.7749, timestamp = 1000L),
            createTestTrackPoint(rideId, latitude = 37.7750, timestamp = 2000L),
            createTestTrackPoint(rideId, latitude = 37.7751, timestamp = 3000L)
        )

        // When
        trackPointDao.insertAllTrackPoints(trackPoints)

        // Then
        val retrieved = trackPointDao.getTrackPointsForRide(rideId)
        assertThat(retrieved).hasSize(3)
    }

    @Test
    fun getTrackPointsForRide_returnsOnlyPointsForSpecifiedRide() = runTest {
        // Given - Create two rides with different points
        val rideId1 = createTestRide()
        val rideId2 = createTestRide()

        trackPointDao.insertTrackPoint(createTestTrackPoint(rideId1, latitude = 37.7749))
        trackPointDao.insertTrackPoint(createTestTrackPoint(rideId1, latitude = 37.7750))
        trackPointDao.insertTrackPoint(createTestTrackPoint(rideId2, latitude = 38.0000))

        // When
        val ride1Points = trackPointDao.getTrackPointsForRide(rideId1)
        val ride2Points = trackPointDao.getTrackPointsForRide(rideId2)

        // Then
        assertThat(ride1Points).hasSize(2)
        assertThat(ride2Points).hasSize(1)
    }

    @Test
    fun getTrackPointsForRide_ordersPointsByTimestampAscending() = runTest {
        // Given
        val rideId = createTestRide()
        val point1 = createTestTrackPoint(rideId, latitude = 37.7749, timestamp = 3000L)
        val point2 = createTestTrackPoint(rideId, latitude = 37.7750, timestamp = 1000L)
        val point3 = createTestTrackPoint(rideId, latitude = 37.7751, timestamp = 2000L)

        // Insert in random order
        trackPointDao.insertTrackPoint(point1)
        trackPointDao.insertTrackPoint(point2)
        trackPointDao.insertTrackPoint(point3)

        // When
        val retrieved = trackPointDao.getTrackPointsForRide(rideId)

        // Then - Should be ordered by timestamp ASC
        assertThat(retrieved[0].timestamp).isEqualTo(1000L)
        assertThat(retrieved[1].timestamp).isEqualTo(2000L)
        assertThat(retrieved[2].timestamp).isEqualTo(3000L)
    }

    @Test
    fun getTrackPointsForRideFlow_emitsUpdates() = runTest {
        // Given
        val rideId = createTestRide()
        val point = createTestTrackPoint(rideId, latitude = 37.7749)

        // When
        trackPointDao.insertTrackPoint(point)
        val points = trackPointDao.getTrackPointsForRideFlow(rideId).first()

        // Then
        assertThat(points).hasSize(1)
        assertThat(points[0].latitude).isEqualTo(37.7749)
    }

    @Test
    fun getLastTrackPoint_returnsNewestPoint() = runTest {
        // Given
        val rideId = createTestRide()
        val point1 = createTestTrackPoint(rideId, latitude = 37.7749, timestamp = 1000L)
        val point2 = createTestTrackPoint(rideId, latitude = 37.7750, timestamp = 2000L)
        val point3 = createTestTrackPoint(rideId, latitude = 37.7751, timestamp = 3000L)

        trackPointDao.insertTrackPoint(point1)
        trackPointDao.insertTrackPoint(point2)
        trackPointDao.insertTrackPoint(point3)

        // When
        val lastPoint = trackPointDao.getLastTrackPoint(rideId)

        // Then
        assertThat(lastPoint).isNotNull()
        assertThat(lastPoint?.timestamp).isEqualTo(3000L)
        assertThat(lastPoint?.latitude).isEqualTo(37.7751)
    }

    @Test
    fun getLastTrackPoint_withNoPoints_returnsNull() = runTest {
        // Given
        val rideId = createTestRide()

        // When
        val lastPoint = trackPointDao.getLastTrackPoint(rideId)

        // Then
        assertThat(lastPoint).isNull()
    }

    @Test
    fun getTrackPointCount_returnsCorrectCount() = runTest {
        // Given
        val rideId = createTestRide()
        trackPointDao.insertAllTrackPoints(
            listOf(
                createTestTrackPoint(rideId),
                createTestTrackPoint(rideId),
                createTestTrackPoint(rideId)
            )
        )

        // When
        val count = trackPointDao.getTrackPointCount(rideId)

        // Then
        assertThat(count).isEqualTo(3)
    }

    @Test
    fun getTrackPointCount_withNoPoints_returnsZero() = runTest {
        // Given
        val rideId = createTestRide()

        // When
        val count = trackPointDao.getTrackPointCount(rideId)

        // Then
        assertThat(count).isEqualTo(0)
    }

    @Test
    fun trackPoints_storePauseState() = runTest {
        // Given
        val rideId = createTestRide()
        val manuallyPausedPoint = createTestTrackPoint(
            rideId = rideId,
            isManuallyPaused = true,
            isAutoPaused = false
        )
        val autoPausedPoint = createTestTrackPoint(
            rideId = rideId,
            isManuallyPaused = false,
            isAutoPaused = true
        )

        // When
        trackPointDao.insertTrackPoint(manuallyPausedPoint)
        trackPointDao.insertTrackPoint(autoPausedPoint)

        // Then
        val retrieved = trackPointDao.getTrackPointsForRide(rideId)
        assertThat(retrieved[0].isManuallyPaused).isTrue()
        assertThat(retrieved[0].isAutoPaused).isFalse()
        assertThat(retrieved[1].isManuallyPaused).isFalse()
        assertThat(retrieved[1].isAutoPaused).isTrue()
    }

    // Helper functions

    private suspend fun createTestRide(): Long {
        val ride = Ride(
            id = 0,
            name = "Test Ride",
            startTime = System.currentTimeMillis(),
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

    private fun createTestTrackPoint(
        rideId: Long,
        latitude: Double = 37.7749,
        longitude: Double = -122.4194,
        timestamp: Long = System.currentTimeMillis(),
        speedMetersPerSec: Double = 5.0,
        isManuallyPaused: Boolean = false,
        isAutoPaused: Boolean = false
    ): TrackPoint {
        return TrackPoint(
            id = 0,  // Auto-generated
            rideId = rideId,
            timestamp = timestamp,
            latitude = latitude,
            longitude = longitude,
            speedMetersPerSec = speedMetersPerSec,
            accuracy = 10.0f,
            isManuallyPaused = isManuallyPaused,
            isAutoPaused = isAutoPaused
        )
    }
}
