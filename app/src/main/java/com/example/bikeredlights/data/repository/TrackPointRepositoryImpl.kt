package com.example.bikeredlights.data.repository

import com.example.bikeredlights.data.local.dao.TrackPointDao
import com.example.bikeredlights.domain.model.TrackPoint
import com.example.bikeredlights.domain.repository.TrackPointRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.example.bikeredlights.data.local.entity.TrackPoint as TrackPointEntity

/**
 * Implementation of TrackPointRepository using Room database.
 *
 * **Design Principles**:
 * - Maps between domain models and Room entities
 * - All database operations on Dispatchers.IO
 * - Flow-based reactive queries
 * - Thread-safe operations
 *
 * **Performance Optimizations**:
 * - Batch insert for multiple points
 * - Indexed queries on rideId and timestamp
 * - Ordered results for chronological access
 *
 * @property trackPointDao Room DAO for track point database operations
 */
class TrackPointRepositoryImpl @Inject constructor(
    private val trackPointDao: TrackPointDao
) : TrackPointRepository {

    override suspend fun insertTrackPoint(trackPoint: TrackPoint): Long = withContext(Dispatchers.IO) {
        trackPointDao.insertTrackPoint(trackPoint.toEntity())
    }

    override suspend fun insertAllTrackPoints(trackPoints: List<TrackPoint>) = withContext(Dispatchers.IO) {
        trackPointDao.insertAllTrackPoints(trackPoints.map { it.toEntity() })
    }

    override suspend fun getTrackPointsForRide(rideId: Long): List<TrackPoint> = withContext(Dispatchers.IO) {
        trackPointDao.getTrackPointsForRide(rideId).map { it.toDomainModel() }
    }

    override fun getTrackPointsForRideFlow(rideId: Long): Flow<List<TrackPoint>> {
        return trackPointDao.getTrackPointsForRideFlow(rideId).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getLastTrackPoint(rideId: Long): TrackPoint? = withContext(Dispatchers.IO) {
        trackPointDao.getLastTrackPoint(rideId)?.toDomainModel()
    }

    override suspend fun getTrackPointCount(rideId: Long): Int = withContext(Dispatchers.IO) {
        trackPointDao.getTrackPointCount(rideId)
    }

    /**
     * Convert domain model to Room entity.
     */
    private fun TrackPoint.toEntity(): TrackPointEntity {
        return TrackPointEntity(
            id = this.id,
            rideId = this.rideId,
            timestamp = this.timestamp,
            latitude = this.latitude,
            longitude = this.longitude,
            speedMetersPerSec = this.speedMetersPerSec,
            accuracy = this.accuracy,
            isManuallyPaused = this.isManuallyPaused,
            isAutoPaused = this.isAutoPaused
        )
    }

    /**
     * Convert Room entity to domain model.
     */
    private fun TrackPointEntity.toDomainModel(): TrackPoint {
        return TrackPoint(
            id = this.id,
            rideId = this.rideId,
            timestamp = this.timestamp,
            latitude = this.latitude,
            longitude = this.longitude,
            speedMetersPerSec = this.speedMetersPerSec,
            accuracy = this.accuracy,
            isManuallyPaused = this.isManuallyPaused,
            isAutoPaused = this.isAutoPaused
        )
    }
}
