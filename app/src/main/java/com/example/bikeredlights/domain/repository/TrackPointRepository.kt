package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.TrackPoint
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for TrackPoint domain operations.
 *
 * Provides high-level operations for managing GPS track points
 * captured during cycling rides.
 *
 * **Design Principles**:
 * - Domain layer interface (independent of Room)
 * - Returns domain models, not entities
 * - All suspend functions use Dispatchers.IO
 * - Flow-based reactive queries for UI updates
 *
 * **Operations**:
 * - Insert single track points
 * - Batch insert track points (performance optimization)
 * - Retrieve track points for a ride
 * - Get last track point (for distance calculations)
 * - Count track points
 *
 * **Performance Notes**:
 * - Use batch insert for multiple points
 * - Track points ordered by timestamp ASC
 * - Indexed on rideId and timestamp for fast queries
 */
interface TrackPointRepository {

    /**
     * Insert a single track point.
     *
     * @param trackPoint TrackPoint to insert (id will be auto-generated)
     * @return Generated track point ID
     */
    suspend fun insertTrackPoint(trackPoint: TrackPoint): Long

    /**
     * Batch insert multiple track points (performance optimized).
     *
     * Use this for inserting multiple points at once (e.g., during sync).
     *
     * @param trackPoints List of track points to insert
     */
    suspend fun insertAllTrackPoints(trackPoints: List<TrackPoint>)

    /**
     * Get all track points for a ride, ordered by timestamp ASC.
     *
     * @param rideId Ride identifier
     * @return List of track points in chronological order
     */
    suspend fun getTrackPointsForRide(rideId: Long): List<TrackPoint>

    /**
     * Get all track points for a ride as a Flow (reactive).
     *
     * @param rideId Ride identifier
     * @return Flow emitting list of track points (updates when database changes)
     */
    fun getTrackPointsForRideFlow(rideId: Long): Flow<List<TrackPoint>>

    /**
     * Get the last (most recent) track point for a ride.
     *
     * Used for distance calculations between consecutive points.
     *
     * @param rideId Ride identifier
     * @return Last track point if exists, null otherwise
     */
    suspend fun getLastTrackPoint(rideId: Long): TrackPoint?

    /**
     * Count total track points for a ride.
     *
     * @param rideId Ride identifier
     * @return Number of track points
     */
    suspend fun getTrackPointCount(rideId: Long): Int
}
