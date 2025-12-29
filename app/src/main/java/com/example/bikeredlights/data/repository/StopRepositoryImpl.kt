package com.example.bikeredlights.data.repository

import com.example.bikeredlights.data.local.dao.StopDao
import com.example.bikeredlights.data.local.entity.StopEntity
import com.example.bikeredlights.domain.model.Stop
import com.example.bikeredlights.domain.repository.StopRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Implementation of StopRepository interface (Feature 009).
 *
 * Responsibilities:
 * - Map between domain models (Stop) and database entities (StopEntity)
 * - Delegate database operations to StopDao
 * - Handle validation and error transformation
 *
 * Thread Safety:
 * - All methods are suspend functions or return Flow
 * - Room handles threading internally
 * - No shared mutable state
 *
 * Dependency Injection:
 * - Injected via Hilt @Inject constructor
 * - Bound to StopRepository interface in DatabaseModule
 *
 * @property stopDao Room DAO for stop database operations
 */
class StopRepositoryImpl @Inject constructor(
    private val stopDao: StopDao
) : StopRepository {

    /**
     * Insert a new stop record when stop is confirmed.
     *
     * Validation: Stop domain model validates lat/long/stopNumber in its init block.
     * Mapping: Domain model → Entity (entity has Room-specific annotations).
     *
     * @param stop Stop domain model with rideId, stopNumber, lat, long, startTimestamp
     * @return Database ID of inserted stop
     * @throws IllegalArgumentException if validation fails in Stop init block
     * @throws SQLiteConstraintException if rideId invalid or stopNumber duplicate
     */
    override suspend fun insertStop(stop: Stop): Long {
        val entity = stop.toEntity()
        return stopDao.insertStop(entity)
    }

    /**
     * Update stop end timestamp and duration when movement resumes.
     *
     * Immutability: Only updates endTimestamp and durationSeconds fields.
     *
     * @param stopId Database ID from insertStop return value
     * @param endTimestamp Unix epoch milliseconds when movement detected
     * @param durationSeconds Calculated as (endTimestamp - startTimestamp) / 1000
     */
    override suspend fun updateStopEnd(stopId: Long, endTimestamp: Long, durationSeconds: Int) {
        stopDao.updateStopEnd(stopId, endTimestamp, durationSeconds)
    }

    /**
     * Get all stops for a specific ride, ordered by sequential stop number.
     *
     * Mapping: Entity list → Domain model list
     *
     * @param rideId Foreign key to rides table
     * @return List of Stop domain models ordered by stop_number
     */
    override suspend fun getStopsByRideId(rideId: Long): List<Stop> {
        val entities = stopDao.getStopsByRideId(rideId)
        return entities.map { it.toDomainModel() }
    }

    /**
     * Get live stop count for a ride (reactive Flow).
     *
     * No mapping needed: Flow<Int> is already technology-agnostic.
     *
     * @param rideId Current ride being recorded
     * @return Flow emitting stop count
     */
    override fun getStopCountByRideId(rideId: Long): Flow<Int> {
        return stopDao.getStopCountByRideId(rideId)
    }

    /**
     * Get all stops without assigned cluster (for Feature 010).
     *
     * Mapping: Entity list → Domain model list
     *
     * @return List of unclustered Stop domain models
     */
    override suspend fun getUnclusteredStops(): List<Stop> {
        val entities = stopDao.getUnclusteredStops()
        return entities.map { it.toDomainModel() }
    }

    /**
     * Update cluster ID for a stop (Feature 010 only).
     *
     * @param stopId Database ID of stop
     * @param clusterId Foreign key to clusters table
     */
    override suspend fun updateStopCluster(stopId: Long, clusterId: Long) {
        stopDao.updateStopCluster(stopId, clusterId)
    }

    /**
     * Get a single stop by ID (for testing/debugging).
     *
     * Mapping: Entity → Domain model (or null)
     *
     * @param stopId Primary key
     * @return Stop domain model or null if not found
     */
    override suspend fun getStopById(stopId: Long): Stop? {
        val entity = stopDao.getStopById(stopId)
        return entity?.toDomainModel()
    }

    /**
     * Delete a specific stop by its ID (Feature 009).
     *
     * Use Case: Remove stop if ride ends during active stop detection.
     *
     * @param stopId Primary key of the stop to delete
     */
    override suspend fun deleteStop(stopId: Long) {
        stopDao.deleteStop(stopId)
    }

    /**
     * Delete all stops for a ride (for testing).
     *
     * Note: In production, CASCADE foreign key handles deletion automatically.
     *
     * @param rideId Foreign key to rides table
     */
    override suspend fun deleteStopsByRideId(rideId: Long) {
        stopDao.deleteStopsByRideId(rideId)
    }

    // ========== Feature 010: Stop Clustering Methods ==========

    /**
     * Get all stops across all rides for clustering (Feature 010).
     *
     * Use Case: Full re-clustering on settings change or manual trigger.
     *
     * Mapping: List<StopEntity> → List<Stop> (domain models)
     *
     * @return List of all stops ordered by start_timestamp ASC
     */
    override suspend fun getAllStops(): List<Stop> {
        return stopDao.getAllStops().map { it.toDomainModel() }
    }

    /**
     * Batch update cluster IDs for multiple stops (Feature 010 clustering).
     *
     * Use Case: ClusterStopsUseCase assigns all stops in a cluster to same cluster_id.
     *
     * Transaction: Room executes UPDATE query in transaction automatically.
     *
     * @param clusterId Cluster ID to assign (1, 2, 3... from DBSCAN result)
     * @param stopIds List of stop primary keys to update
     */
    override suspend fun updateClusterIds(clusterId: Long, stopIds: List<Long>) {
        stopDao.updateClusterIds(clusterId, stopIds)
    }

    // ========== Feature 011: Cluster Visualization Methods ==========

    /**
     * Get all stops that belong to clusters (cluster_id IS NOT NULL).
     *
     * Use Case: Fetch clustered stops for map visualization (Feature 011 - User Story 1).
     *
     * Mapping: Flow<List<StopEntity>> → Flow<List<Stop>> (domain models)
     *
     * Reactivity: Flow emits updates when stops table changes
     *
     * @return Flow emitting list of clustered stops, ordered by start_timestamp descending
     */
    override fun getClusteredStops(): Flow<List<Stop>> {
        return stopDao.getClusteredStops()
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    /**
     * Get clustered stops within a specific date range.
     *
     * Use Case: Filter clusters by date range (Feature 011 - User Story 3).
     *
     * Mapping: Flow<List<StopEntity>> → Flow<List<Stop>> (domain models)
     *
     * @param startMillis Start of date range (inclusive) in epoch milliseconds
     * @param endMillis End of date range (inclusive) in epoch milliseconds
     * @return Flow emitting list of clustered stops within date range
     */
    override fun getClusteredStopsByDateRange(
        startMillis: Long,
        endMillis: Long
    ): Flow<List<Stop>> {
        return stopDao.getClusteredStopsByDateRange(startMillis, endMillis)
            .map { entities ->
                entities.map { it.toDomainModel() }
            }
    }

    /**
     * Get stops grouped by cluster ID.
     *
     * Use Case: Aggregate stops for ClusterSummary calculation (Feature 011).
     *
     * Mapping:
     * 1. Flow<List<StopEntity>> → Flow<List<Stop>> (entity to domain model)
     * 2. List<Stop> → Map<Long, List<Stop>> (in-memory grouping by cluster_id)
     *
     * Performance: Grouping happens in memory after single query (<100ms for 500 stops)
     *
     * @return Flow emitting map of cluster ID to stops
     */
    override fun getStopsGroupedByCluster(): Flow<Map<Long, List<Stop>>> {
        return stopDao.getClusteredStops()
            .map { entities ->
                entities
                    .map { it.toDomainModel() }
                    .groupBy { it.clusterId!! }  // Safe: query filters cluster_id NOT NULL
            }
    }

    /**
     * Convert Stop domain model to StopEntity database entity.
     *
     * Mapping: Domain layer (technology-agnostic) → Data layer (Room-specific)
     *
     * @receiver Stop domain model
     * @return StopEntity for Room database
     */
    private fun Stop.toEntity(): StopEntity {
        return StopEntity(
            id = id,
            rideId = rideId,
            stopNumber = stopNumber,
            latitude = latitude,
            longitude = longitude,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            durationSeconds = durationSeconds,
            clusterId = clusterId
        )
    }

    /**
     * Convert StopEntity database entity to Stop domain model.
     *
     * Mapping: Data layer (Room-specific) → Domain layer (technology-agnostic)
     *
     * @receiver StopEntity from Room database
     * @return Stop domain model
     */
    private fun StopEntity.toDomainModel(): Stop {
        return Stop(
            id = id,
            rideId = rideId,
            stopNumber = stopNumber,
            latitude = latitude,
            longitude = longitude,
            startTimestamp = startTimestamp,
            endTimestamp = endTimestamp,
            durationSeconds = durationSeconds,
            clusterId = clusterId
        )
    }
}
