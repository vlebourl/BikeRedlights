package com.example.bikeredlights.data.repository

import com.example.bikeredlights.data.local.dao.RideDao
import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.model.history.SortPreference
import com.example.bikeredlights.domain.repository.RideRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.example.bikeredlights.data.local.entity.Ride as RideEntity

/**
 * Implementation of RideRepository using Room database.
 *
 * **Design Principles**:
 * - Maps between domain models and Room entities
 * - All database operations on Dispatchers.IO
 * - Flow-based reactive queries
 * - Thread-safe operations
 *
 * **Mapping Strategy**:
 * - Domain model → Entity (for inserts/updates)
 * - Entity → Domain model (for queries)
 * - One-to-one field mapping
 *
 * @property rideDao Room DAO for ride database operations
 */
class RideRepositoryImpl @Inject constructor(
    private val rideDao: RideDao
) : RideRepository {

    override suspend fun createRide(ride: Ride): Long = withContext(Dispatchers.IO) {
        rideDao.insertRide(ride.toEntity())
    }

    override suspend fun updateRide(ride: Ride) = withContext(Dispatchers.IO) {
        rideDao.updateRide(ride.toEntity())
    }

    override suspend fun deleteRide(ride: Ride) = withContext(Dispatchers.IO) {
        rideDao.deleteRide(ride.toEntity())
    }

    override suspend fun getRideById(rideId: Long): Ride? = withContext(Dispatchers.IO) {
        rideDao.getRideById(rideId)?.toDomainModel()
    }

    override fun getRideByIdFlow(rideId: Long): Flow<Ride?> {
        return rideDao.getRideByIdFlow(rideId).map { entity ->
            entity?.toDomainModel()
        }
    }

    override fun getAllRidesFlow(): Flow<List<Ride>> {
        return rideDao.getAllRidesFlow().map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override suspend fun getAllRides(): List<Ride> = withContext(Dispatchers.IO) {
        rideDao.getAllRides().map { it.toDomainModel() }
    }

    override suspend fun getIncompleteRides(): List<Ride> = withContext(Dispatchers.IO) {
        rideDao.getIncompleteRides().map { it.toDomainModel() }
    }

    override fun getAllRidesSorted(sortPreference: SortPreference): Flow<List<Ride>> {
        val daoFlow = when (sortPreference) {
            SortPreference.NEWEST_FIRST -> rideDao.getAllRidesNewestFirst()
            SortPreference.OLDEST_FIRST -> rideDao.getAllRidesOldestFirst()
            SortPreference.LONGEST_DISTANCE -> rideDao.getAllRidesLongestDistance()
            SortPreference.LONGEST_DURATION -> rideDao.getAllRidesLongestDuration()
        }
        return daoFlow.map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRidesInDateRange(startMillis: Long, endMillis: Long): Flow<List<Ride>> {
        return rideDao.getRidesInDateRange(startMillis, endMillis).map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    override fun getRidesInDateRangeSorted(
        startMillis: Long,
        endMillis: Long,
        sortPreference: SortPreference
    ): Flow<List<Ride>> {
        val daoFlow = when (sortPreference) {
            SortPreference.NEWEST_FIRST -> rideDao.getRidesInDateRangeNewestFirst(startMillis, endMillis)
            SortPreference.OLDEST_FIRST -> rideDao.getRidesInDateRangeOldestFirst(startMillis, endMillis)
            SortPreference.LONGEST_DISTANCE -> rideDao.getRidesInDateRangeLongestDistance(startMillis, endMillis)
            SortPreference.LONGEST_DURATION -> rideDao.getRidesInDateRangeLongestDuration(startMillis, endMillis)
        }
        return daoFlow.map { entities ->
            entities.map { it.toDomainModel() }
        }
    }

    /**
     * Convert domain model to Room entity.
     */
    private fun Ride.toEntity(): RideEntity {
        return RideEntity(
            id = this.id,
            name = this.name,
            startTime = this.startTime,
            endTime = this.endTime,
            elapsedDurationMillis = this.elapsedDurationMillis,
            movingDurationMillis = this.movingDurationMillis,
            manualPausedDurationMillis = this.manualPausedDurationMillis,
            autoPausedDurationMillis = this.autoPausedDurationMillis,
            distanceMeters = this.distanceMeters,
            avgSpeedMetersPerSec = this.avgSpeedMetersPerSec,
            maxSpeedMetersPerSec = this.maxSpeedMetersPerSec
        )
    }

    /**
     * Convert Room entity to domain model.
     */
    private fun RideEntity.toDomainModel(): Ride {
        return Ride(
            id = this.id,
            name = this.name,
            startTime = this.startTime,
            endTime = this.endTime,
            elapsedDurationMillis = this.elapsedDurationMillis,
            movingDurationMillis = this.movingDurationMillis,
            manualPausedDurationMillis = this.manualPausedDurationMillis,
            autoPausedDurationMillis = this.autoPausedDurationMillis,
            distanceMeters = this.distanceMeters,
            avgSpeedMetersPerSec = this.avgSpeedMetersPerSec,
            maxSpeedMetersPerSec = this.maxSpeedMetersPerSec
        )
    }
}
