package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.Stop
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for stop persistence operations (Feature 009).
 *
 * Defines the contract between domain layer use cases and data layer implementation.
 * Technology-agnostic: No dependencies on Room, Android, or specific storage mechanisms.
 *
 * Thread Safety:
 * - All methods are suspend functions or return Flow
 * - Implementation handles threading via Room/coroutines
 *
 * Design Pattern:
 * - Repository pattern with domain model (Stop) instead of entities
 * - Implementation (StopRepositoryImpl) handles entity/model mapping
 *
 * Usage Context:
 * - RideRecordingService: Insert stop on confirmation, update on end
 * - RideViewModel: Collect stop count Flow for live UI updates
 * - Future clustering (Feature 010): Get unclustered stops for algorithm
 */
interface StopRepository {

    /**
     * Insert a new stop record when stop is confirmed (duration threshold met).
     *
     * Called by: RideRecordingService when stop detection state machine confirms stop.
     *
     * Validation:
     * - Stop must have valid rideId (must exist in database)
     * - stopNumber must be unique per rideId
     * - endTimestamp and durationSeconds must be null (active stop)
     * - latitude in [-90.0, 90.0], longitude in [-180.0, 180.0]
     *
     * @param stop Stop domain model with rideId, stopNumber, lat, long, startTimestamp
     * @return Database ID of inserted stop (used as activeStopId in service state)
     * @throws IllegalArgumentException if validation fails
     * @throws SQLiteConstraintException if rideId invalid or stopNumber duplicate
     */
    suspend fun insertStop(stop: Stop): Long

    /**
     * Update stop end timestamp and duration when movement resumes.
     *
     * Called by: RideRecordingService when speed > threshold for 3 consecutive seconds.
     *
     * Immutability: Only endTimestamp and durationSeconds are updated. All other fields remain unchanged.
     *
     * @param stopId Database ID from insertStop return value
     * @param endTimestamp Unix epoch milliseconds when movement detected
     * @param durationSeconds Calculated as (endTimestamp - startTimestamp) / 1000
     * @throws IllegalArgumentException if stopId doesn't exist or timestamps invalid
     */
    suspend fun updateStopEnd(stopId: Long, endTimestamp: Long, durationSeconds: Int)

    /**
     * Get all stops for a specific ride, ordered by sequential stop number.
     *
     * Use Cases:
     * - Ride detail screen: Display list of stops with timestamps/locations
     * - Ride statistics: Calculate total stop time for a ride
     * - Export functionality: Include stop data in GPX/KML files
     *
     * @param rideId Foreign key to rides table
     * @return List of stops ordered by stop_number (1, 2, 3...), empty list if no stops
     */
    suspend fun getStopsByRideId(rideId: Long): List<Stop>

    /**
     * Get live stop count for a ride (reactive Flow).
     *
     * Use Case: Display "Stops: N" counter on Live tab during ride recording.
     *
     * Reactivity:
     * - Flow emits new count immediately when stop inserted via insertStop()
     * - UI automatically updates without manual refresh
     *
     * Lifecycle:
     * - Flow remains active while collected (ViewModel scope)
     * - Stops emitting when ViewModel cleared or ride stopped
     *
     * @param rideId Current ride being recorded
     * @return Flow emitting stop count (0, 1, 2, 3...) as stops are added
     */
    fun getStopCountByRideId(rideId: Long): Flow<Int>

    /**
     * Get all stops without assigned cluster (for Feature 010 clustering algorithm).
     *
     * Use Case: Post-ride clustering finds unclustered stops and assigns cluster IDs.
     *
     * Filtering: Returns only stops with clusterId = null
     * Ordering: Chronological (oldest first) for sequential clustering
     *
     * Feature Scope: This method exists for Feature 010, not used by Feature 009.
     *
     * @return List of unclustered stops ordered by start_timestamp
     */
    suspend fun getUnclusteredStops(): List<Stop>

    /**
     * Update cluster ID for a stop (Feature 010 clustering algorithm only).
     *
     * Called by: ClusteringUseCase after assigning stop to cluster.
     *
     * Feature Scope: Feature 009 never sets clusterId, only Feature 010 does.
     *
     * @param stopId Database ID of stop
     * @param clusterId Foreign key to clusters table (Feature 010)
     */
    suspend fun updateStopCluster(stopId: Long, clusterId: Long)

    /**
     * Get a single stop by ID (for testing/debugging).
     *
     * Use Case: Verify stop data after insert, test CASCADE delete behavior.
     *
     * @param stopId Primary key
     * @return Stop domain model or null if not found
     */
    suspend fun getStopById(stopId: Long): Stop?

    /**
     * Delete a specific stop by its ID (Feature 009 - Stop Detection).
     *
     * Use Case: Remove stop if ride ends during active stop detection
     * (destination stop, not a traffic stop).
     *
     * @param stopId Primary key of the stop to delete
     */
    suspend fun deleteStop(stopId: Long)

    /**
     * Delete all stops for a ride (for testing - CASCADE handles in production).
     *
     * Use Case: Test setup/teardown, manual cleanup in edge cases.
     *
     * Note: In production, deleting ride automatically cascades to stops via foreign key.
     *
     * @param rideId Foreign key to rides table
     */
    suspend fun deleteStopsByRideId(rideId: Long)

    // ========== Feature 010: Stop Clustering Methods ==========

    /**
     * Get all stops across all rides for clustering (Feature 010).
     *
     * Use Case: Full re-clustering on settings change or manual trigger.
     *
     * Ordering: Chronological by start_timestamp for consistent clustering results.
     *
     * Performance: Returns ALL stops (could be 1000+ for active users).
     * Expected load time: <100ms for 1000 stops.
     *
     * @return List of all stops ordered by start_timestamp ASC
     */
    suspend fun getAllStops(): List<Stop>

    /**
     * Batch update cluster IDs for multiple stops (Feature 010 clustering).
     *
     * Use Case: ClusterStopsUseCase assigns all stops in a cluster to same cluster_id.
     *
     * Performance: Single SQL UPDATE with IN clause for efficiency.
     *
     * Transaction: Implementation should use database transaction for atomicity.
     *
     * @param clusterId Cluster ID to assign (1, 2, 3... from DBSCAN result)
     * @param stopIds List of stop primary keys to update
     */
    suspend fun updateClusterIds(clusterId: Long, stopIds: List<Long>)

    // ========== Feature 011: Cluster Visualization Methods ==========

    /**
     * Get all stops that belong to clusters (cluster_id IS NOT NULL).
     *
     * Use Case: Fetch clustered stops for map visualization (Feature 011 - User Story 1).
     *
     * Filtering:
     * - Excludes noise points identified by DBSCAN clustering (cluster_id == null)
     * - Returns only stops assigned to clusters
     *
     * Reactivity:
     * - Flow emits updates when stops table changes
     * - UI automatically refreshes when new stops are clustered
     *
     * Performance: Indexed query on cluster_id (<100ms for 500 stops)
     *
     * @return Flow emitting list of clustered stops, ordered by start_timestamp descending
     */
    fun getClusteredStops(): Flow<List<Stop>>

    /**
     * Get clustered stops within a specific date range.
     *
     * Use Case: Filter clusters by date range (Feature 011 - User Story 3).
     * Common filters: Last 7 days, last 30 days, last 90 days.
     *
     * Filtering:
     * - Combines cluster_id filtering with date range filtering
     * - Boundaries are inclusive (startMillis <= stop.startTime <= endMillis)
     *
     * Reactivity: Flow emits updates when stops table changes
     *
     * Performance: Composite index on (cluster_id, start_timestamp) for optimal filtering
     *
     * @param startMillis Start of date range (inclusive) in epoch milliseconds
     * @param endMillis End of date range (inclusive) in epoch milliseconds
     * @return Flow emitting list of clustered stops within date range
     */
    fun getClusteredStopsByDateRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<Stop>>

    /**
     * Get stops grouped by cluster ID.
     *
     * Use Case: Aggregate stops for ClusterSummary calculation (Feature 011).
     *
     * Filtering: Excludes noise points (cluster_id == null)
     *
     * Grouping:
     * - Map key = cluster_id (1, 2, 3...)
     * - Map value = List of Stop entities in that cluster
     *
     * Reactivity: Flow emits updates when stops table changes
     *
     * Performance:
     * - Single query with in-memory grouping
     * - <100ms for 500 stops across 50 clusters
     *
     * @return Flow emitting map of cluster ID to stops
     */
    fun getStopsGroupedByCluster(): Flow<Map<Long, List<Stop>>>
}
