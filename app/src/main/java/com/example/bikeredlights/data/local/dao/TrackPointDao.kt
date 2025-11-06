package com.example.bikeredlights.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.bikeredlights.data.local.entity.TrackPoint
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for TrackPoint entity operations.
 *
 * Provides operations for GPS track points including:
 * - Single and batch insertion during ride recording
 * - Querying track points for a specific ride
 * - Reactive Flow-based queries for real-time updates
 * - Last track point retrieval for distance calculations
 *
 * CASCADE Deletion:
 * - TrackPoints are automatically deleted when parent Ride is deleted
 * - No explicit delete method needed
 *
 * Performance Optimization:
 * - Batch insertions preferred over single inserts during recording
 * - Indexed on ride_id and timestamp for fast queries
 *
 * Thread Safety:
 * - All suspend functions execute on Dispatchers.IO by Room
 * - Flow queries emit on collector's dispatcher
 */
@Dao
interface TrackPointDao {

    /**
     * Insert a single track point.
     *
     * Used during GPS tracking to record each location update.
     *
     * Validation (caller responsibility):
     * - accuracy <= 50 meters (spec FR-022)
     * - latitude in -90.0..90.0
     * - longitude in -180.0..180.0
     * - NOT (isManuallyPaused AND isAutoPaused)
     *
     * @param trackPoint TrackPoint entity to insert (id will be auto-generated)
     * @return Generated track point ID (primary key)
     * @throws SQLiteException if foreign key constraint violated or database full
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertTrackPoint(trackPoint: TrackPoint): Long

    /**
     * Insert multiple track points in a single transaction (batch operation).
     *
     * Performance optimization for inserting accumulated points.
     * More efficient than multiple single inserts.
     *
     * @param trackPoints List of TrackPoint entities to insert
     * @throws SQLiteException if foreign key constraint violated or database full
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertAllTrackPoints(trackPoints: List<TrackPoint>)

    /**
     * Get all track points for a specific ride (one-time query).
     *
     * Ordered chronologically by timestamp ascending.
     * Used for route visualization and statistics calculation.
     *
     * @param rideId Ride identifier
     * @return List of track points for the ride, empty if ride has no points
     */
    @Query("SELECT * FROM track_points WHERE ride_id = :rideId ORDER BY timestamp ASC")
    suspend fun getTrackPointsForRide(rideId: Long): List<TrackPoint>

    /**
     * Get track points for a ride as a Flow (reactive updates).
     *
     * Emits new list whenever track_points table changes for this ride.
     * Ordered chronologically by timestamp ascending.
     *
     * Used for live route visualization during recording.
     *
     * @param rideId Ride identifier
     * @return Flow emitting list of track points, empty list if none
     */
    @Query("SELECT * FROM track_points WHERE ride_id = :rideId ORDER BY timestamp ASC")
    fun getTrackPointsForRideFlow(rideId: Long): Flow<List<TrackPoint>>

    /**
     * Get the most recent track point for a ride.
     *
     * Used for calculating distance between consecutive points using Haversine formula.
     *
     * @param rideId Ride identifier
     * @return Most recent TrackPoint if exists, null if ride has no points
     */
    @Query("SELECT * FROM track_points WHERE ride_id = :rideId ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTrackPoint(rideId: Long): TrackPoint?

    /**
     * Count total number of track points for a ride.
     *
     * Useful for debugging and statistics display.
     *
     * @param rideId Ride identifier
     * @return Number of track points for the ride
     */
    @Query("SELECT COUNT(*) FROM track_points WHERE ride_id = :rideId")
    suspend fun getTrackPointCount(rideId: Long): Int
}
