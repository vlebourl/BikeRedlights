package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.model.history.SortPreference
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for Ride domain operations.
 *
 * Provides high-level operations for managing cycling ride sessions.
 * Implementations handle data persistence and coordinate between
 * domain layer and data sources.
 *
 * **Design Principles**:
 * - Domain layer interface (independent of Room/DataStore)
 * - Returns domain models, not entities
 * - All suspend functions use Dispatchers.IO
 * - Flow-based reactive queries for UI updates
 *
 * **Operations**:
 * - Create new rides
 * - Update ride statistics
 * - Retrieve rides (single, all, incomplete)
 * - Delete rides
 * - Query by various criteria
 *
 * **Thread Safety**:
 * - All operations are thread-safe
 * - suspend functions run on Dispatchers.IO
 * - Flow emissions are thread-safe
 */
interface RideRepository {

    /**
     * Create a new ride session.
     *
     * @param ride Ride to insert (id will be auto-generated)
     * @return Generated ride ID
     */
    suspend fun createRide(ride: Ride): Long

    /**
     * Update an existing ride's statistics.
     *
     * @param ride Ride with updated data (must have valid id)
     */
    suspend fun updateRide(ride: Ride)

    /**
     * Delete a ride and all associated track points (CASCADE).
     *
     * @param ride Ride to delete (must have valid id)
     */
    suspend fun deleteRide(ride: Ride)

    /**
     * Get a specific ride by ID.
     *
     * @param rideId Ride identifier
     * @return Ride if found, null otherwise
     */
    suspend fun getRideById(rideId: Long): Ride?

    /**
     * Observe a specific ride by ID as a Flow.
     *
     * Emits updates whenever the ride changes in the database.
     * Used for real-time updates during ride recording (duration, distance, etc.).
     *
     * @param rideId Ride identifier
     * @return Flow emitting ride updates, or null if ride not found
     */
    fun getRideByIdFlow(rideId: Long): Flow<Ride?>

    /**
     * Get all rides ordered by start time (most recent first).
     *
     * @return Flow emitting list of rides (updates when database changes)
     */
    fun getAllRidesFlow(): Flow<List<Ride>>

    /**
     * Get all rides as a one-time snapshot.
     *
     * @return List of all rides ordered by start time DESC
     */
    suspend fun getAllRides(): List<Ride>

    /**
     * Get incomplete rides (endTime is null).
     *
     * Used for recovering interrupted rides after app restart.
     *
     * @return List of incomplete rides
     */
    suspend fun getIncompleteRides(): List<Ride>

    /**
     * Get all rides with custom sort order as a reactive Flow.
     *
     * Emits updated list whenever database changes, sorted according to preference.
     * Used by ride history screen to display sorted list.
     *
     * @param sortPreference Sort order preference
     * @return Flow emitting sorted list of rides
     */
    fun getAllRidesSorted(sortPreference: SortPreference): Flow<List<Ride>>

    /**
     * Get rides within a date range as a reactive Flow.
     *
     * Filters rides where startTime falls between startMillis and endMillis (inclusive).
     * Emits updated list whenever database changes.
     *
     * @param startMillis Start of date range (epoch milliseconds, inclusive)
     * @param endMillis End of date range (epoch milliseconds, inclusive)
     * @return Flow emitting filtered list of rides
     */
    fun getRidesInDateRange(startMillis: Long, endMillis: Long): Flow<List<Ride>>

    /**
     * Get rides with both custom sort and date filter as a reactive Flow.
     *
     * Combines filtering by date range and sorting by preference.
     * Emits updated list whenever database changes.
     *
     * @param startMillis Start of date range (epoch milliseconds, inclusive)
     * @param endMillis End of date range (epoch milliseconds, inclusive)
     * @param sortPreference Sort order preference
     * @return Flow emitting filtered and sorted list of rides
     */
    fun getRidesInDateRangeSorted(
        startMillis: Long,
        endMillis: Long,
        sortPreference: SortPreference
    ): Flow<List<Ride>>
}
