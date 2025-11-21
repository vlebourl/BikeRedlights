package com.example.bikeredlights.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.bikeredlights.data.local.entity.StopEntity
import kotlinx.coroutines.flow.Flow

/**
 * DAO Contract: Stop Detection & Recording (Feature 009)
 *
 * Data Access Object for stop persistence operations.
 * Defines the contract between domain layer and Room database for stop entities.
 *
 * Design Decisions:
 * - Flow<Int> for live stop count (reactive updates to UI)
 * - Suspend functions for one-shot queries (insert, update, get)
 * - No delete method (CASCADE handles via ride deletion)
 * - Separate update method for ending stops (immutability pattern)
 *
 * Performance:
 * - All queries use indexes (ride_id, cluster_id, start_timestamp)
 * - Insert: <50ms, Update: <30ms, Query: <100ms (typical 5-20 stops/ride)
 *
 * Thread Safety:
 * - All methods are suspend or return Flow (Room handles threading)
 * - No direct database access on main thread
 */
@Dao
interface StopDao {

    /**
     * Insert a new stop record when stop is confirmed.
     *
     * Called when: Duration threshold met during ride recording.
     *
     * Constraints:
     * - rideId must exist in rides table (foreign key)
     * - stopNumber must be unique per rideId (UNIQUE constraint)
     * - endTimestamp and durationSeconds must be NULL on insert
     *
     * @param stop StopEntity with: rideId, stopNumber, lat, long, startTimestamp
     * @return Database ID of inserted stop (used as activeStopId in state machine)
     * @throws SQLiteConstraintException if ride doesn't exist or stopNumber duplicate
     */
    @Insert
    suspend fun insertStop(stop: StopEntity): Long

    /**
     * Update stop end timestamp and duration when movement resumes.
     *
     * Called when: Speed > threshold for 3 consecutive seconds during active stop.
     *
     * Immutability: Only endTimestamp and durationSeconds can be updated post-creation.
     *
     * @param stopId Database ID from insertStop return value
     * @param endTimestamp Unix epoch milliseconds when movement detected
     * @param durationSeconds Calculated as (endTimestamp - startTimestamp) / 1000
     * @throws IllegalArgumentException if stopId doesn't exist
     */
    @Query("""
        UPDATE stops
        SET end_timestamp = :endTimestamp,
            duration_seconds = :durationSeconds
        WHERE id = :stopId
    """)
    suspend fun updateStopEnd(
        stopId: Long,
        endTimestamp: Long,
        durationSeconds: Int
    )

    /**
     * Get all stops for a specific ride, ordered by sequential stop number.
     *
     * Use Cases:
     * - Display stop list in ride history detail screen
     * - Calculate total stop time for ride statistics
     * - Export ride data with stops included
     *
     * @param rideId Foreign key to rides table
     * @return List of stops ordered by stop_number (1, 2, 3...)
     */
    @Query("""
        SELECT * FROM stops
        WHERE ride_id = :rideId
        ORDER BY stop_number ASC
    """)
    suspend fun getStopsByRideId(rideId: Long): List<StopEntity>

    /**
     * Get live stop count for a ride (reactive Flow).
     *
     * Use Case: Display "Stops: 5" counter on Live tab during ride recording.
     *
     * Reactivity: Flow emits new count immediately when stop inserted via insertStop.
     *
     * Performance: COUNT query with indexed ride_id column (<50ms typical).
     *
     * @param rideId Current ride being recorded
     * @return Flow emitting stop count (0, 1, 2, 3...) as stops are added
     */
    @Query("""
        SELECT COUNT(*) FROM stops
        WHERE ride_id = :rideId
    """)
    fun getStopCountByRideId(rideId: Long): Flow<Int>

    /**
     * Get all stops without assigned cluster (for Feature 010 clustering algorithm).
     *
     * Use Case: Post-ride clustering finds unclustered stops and assigns cluster IDs.
     *
     * Ordering: Chronological (oldest first) for sequential clustering.
     *
     * @return List of stops with cluster_id = NULL, ordered by start_timestamp
     */
    @Query("""
        SELECT * FROM stops
        WHERE cluster_id IS NULL
        ORDER BY start_timestamp ASC
    """)
    suspend fun getUnclusteredStops(): List<StopEntity>

    /**
     * Update cluster ID for a stop (Feature 010 clustering algorithm only).
     *
     * Called by: ClusteringUseCase after assigning stop to cluster.
     *
     * Note: This feature (009) never sets cluster_id, only Feature 010 does.
     *
     * @param stopId Database ID of stop
     * @param clusterId Foreign key to clusters table (Feature 010)
     */
    @Query("""
        UPDATE stops
        SET cluster_id = :clusterId
        WHERE id = :stopId
    """)
    suspend fun updateStopCluster(stopId: Long, clusterId: Long)

    /**
     * Get a single stop by ID (for testing/debugging).
     *
     * Use Case: Verify stop data after insert, test CASCADE delete behavior.
     *
     * @param stopId Primary key
     * @return StopEntity or null if not found
     */
    @Query("SELECT * FROM stops WHERE id = :stopId")
    suspend fun getStopById(stopId: Long): StopEntity?

    /**
     * Delete a specific stop by its ID (Feature 009 - Stop Detection).
     *
     * Use Case: Remove stop if ride ends during active stop detection
     * (destination stop, not a traffic stop).
     *
     * @param stopId Primary key of the stop to delete
     */
    @Query("DELETE FROM stops WHERE id = :stopId")
    suspend fun deleteStop(stopId: Long)

    /**
     * Delete all stops for a ride (for testing - CASCADE handles in production).
     *
     * Use Case: Test setup/teardown, manual cleanup in edge cases.
     *
     * Note: In production, deleting ride automatically cascades to stops.
     *
     * @param rideId Foreign key to rides table
     */
    @Query("DELETE FROM stops WHERE ride_id = :rideId")
    suspend fun deleteStopsByRideId(rideId: Long)
}
