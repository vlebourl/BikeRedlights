package com.example.bikeredlights.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.bikeredlights.data.local.entity.Ride
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for Ride entity operations.
 *
 * Provides CRUD operations for rides with support for:
 * - Single ride queries by ID
 * - Incomplete ride recovery (where endTime IS NULL)
 * - Reactive Flow-based queries for real-time UI updates
 * - Cascade deletion (deleting Ride auto-deletes all TrackPoints)
 *
 * Thread Safety:
 * - All suspend functions execute on Dispatchers.IO by Room
 * - Flow queries emit on collector's dispatcher
 */
@Dao
interface RideDao {

    /**
     * Insert a new ride and return its generated ID.
     *
     * @param ride Ride entity to insert (id will be auto-generated)
     * @return Generated ride ID (primary key)
     * @throws SQLiteException if database constraint violated
     */
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertRide(ride: Ride): Long

    /**
     * Update an existing ride with new values.
     *
     * Typically used to set endTime and final statistics when ride is completed.
     *
     * @param ride Ride entity with updated values (ID must match existing record)
     * @throws SQLiteException if ride doesn't exist
     */
    @Update
    suspend fun updateRide(ride: Ride)

    /**
     * Delete a ride from the database.
     *
     * CASCADE behavior will automatically delete all associated TrackPoints.
     *
     * @param ride Ride entity to delete
     */
    @Delete
    suspend fun deleteRide(ride: Ride)

    /**
     * Get a single ride by its ID.
     *
     * @param rideId Unique ride identifier
     * @return Ride if found, null otherwise
     */
    @Query("SELECT * FROM rides WHERE id = :rideId")
    suspend fun getRideById(rideId: Long): Ride?

    /**
     * Observe a single ride by its ID as a Flow.
     *
     * Emits updates whenever the ride changes in the database.
     * Used for real-time updates during ride recording.
     *
     * @param rideId Unique ride identifier
     * @return Flow emitting ride updates, or null if ride not found
     */
    @Query("SELECT * FROM rides WHERE id = :rideId")
    fun getRideByIdFlow(rideId: Long): Flow<Ride?>

    /**
     * Get all incomplete rides (where endTime is NULL).
     *
     * Used for recovery when app crashes or is killed during recording.
     * Typically called on app launch to detect and recover unfinished rides.
     *
     * @return List of incomplete rides, empty if none found
     */
    @Query("SELECT * FROM rides WHERE end_time IS NULL")
    suspend fun getIncompleteRides(): List<Ride>

    /**
     * Get all rides as a Flow for reactive UI updates.
     *
     * Emits new list whenever rides table changes.
     * Results sorted by start time descending (most recent first).
     *
     * For future history screen (Feature F3).
     *
     * @return Flow emitting list of all rides, empty list if no rides
     */
    @Query("SELECT * FROM rides ORDER BY start_time DESC")
    fun getAllRidesFlow(): Flow<List<Ride>>

    /**
     * Get all rides (one-time query).
     *
     * Sorted by start time descending (most recent first).
     *
     * @return List of all rides, empty if no rides exist
     */
    @Query("SELECT * FROM rides ORDER BY start_time DESC")
    suspend fun getAllRides(): List<Ride>

    // ===== Sorted Queries =====

    /**
     * Get all rides sorted by start time descending (newest first).
     */
    @Query("SELECT * FROM rides ORDER BY start_time DESC")
    fun getAllRidesNewestFirst(): Flow<List<Ride>>

    /**
     * Get all rides sorted by start time ascending (oldest first).
     */
    @Query("SELECT * FROM rides ORDER BY start_time ASC")
    fun getAllRidesOldestFirst(): Flow<List<Ride>>

    /**
     * Get all rides sorted by distance descending (longest distance first).
     * Secondary sort by start_time DESC for stable ordering.
     */
    @Query("SELECT * FROM rides ORDER BY distance_meters DESC, start_time DESC")
    fun getAllRidesLongestDistance(): Flow<List<Ride>>

    /**
     * Get all rides sorted by moving duration descending (longest duration first).
     * Secondary sort by start_time DESC for stable ordering.
     */
    @Query("SELECT * FROM rides ORDER BY moving_duration_millis DESC, start_time DESC")
    fun getAllRidesLongestDuration(): Flow<List<Ride>>

    // ===== Date Range Queries =====

    /**
     * Get rides within a date range.
     *
     * @param startMillis Start of date range (epoch milliseconds, inclusive)
     * @param endMillis End of date range (epoch milliseconds, inclusive)
     * @return Flow emitting rides in the date range, sorted newest first
     */
    @Query("SELECT * FROM rides WHERE start_time BETWEEN :startMillis AND :endMillis ORDER BY start_time DESC")
    fun getRidesInDateRange(startMillis: Long, endMillis: Long): Flow<List<Ride>>

    // ===== Combined Date Range + Sort Queries =====

    /**
     * Get rides in date range sorted by start time descending (newest first).
     */
    @Query("SELECT * FROM rides WHERE start_time BETWEEN :startMillis AND :endMillis ORDER BY start_time DESC")
    fun getRidesInDateRangeNewestFirst(startMillis: Long, endMillis: Long): Flow<List<Ride>>

    /**
     * Get rides in date range sorted by start time ascending (oldest first).
     */
    @Query("SELECT * FROM rides WHERE start_time BETWEEN :startMillis AND :endMillis ORDER BY start_time ASC")
    fun getRidesInDateRangeOldestFirst(startMillis: Long, endMillis: Long): Flow<List<Ride>>

    /**
     * Get rides in date range sorted by distance descending (longest distance first).
     */
    @Query("SELECT * FROM rides WHERE start_time BETWEEN :startMillis AND :endMillis ORDER BY distance_meters DESC, start_time DESC")
    fun getRidesInDateRangeLongestDistance(startMillis: Long, endMillis: Long): Flow<List<Ride>>

    /**
     * Get rides in date range sorted by moving duration descending (longest duration first).
     */
    @Query("SELECT * FROM rides WHERE start_time BETWEEN :startMillis AND :endMillis ORDER BY moving_duration_millis DESC, start_time DESC")
    fun getRidesInDateRangeLongestDuration(startMillis: Long, endMillis: Long): Flow<List<Ride>>
}
