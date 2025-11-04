package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.Ride
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
}
