package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.repository.RideRepository
import javax.inject.Inject

/**
 * Use case for deleting a ride from storage.
 *
 * **Business Logic**:
 * - Deletes ride and all associated data (TrackPoints cascade via Room)
 * - Permanent operation - cannot be undone at repository level
 * - UI layer responsible for confirmation and undo mechanism
 *
 * **Architecture**:
 * - Part of domain layer (pure business logic)
 * - No Android dependencies
 * - Injected via Hilt
 *
 * **Usage**:
 * ```kotlin
 * // Delete by ID
 * deleteRideUseCase(rideId = 123L)
 *
 * // Delete by Ride object
 * deleteRideUseCase(ride = ride)
 * ```
 *
 * @property rideRepository Repository for ride data access
 */
class DeleteRideUseCase @Inject constructor(
    private val rideRepository: RideRepository
) {
    /**
     * Delete a ride by its ID.
     *
     * @param rideId Unique identifier for the ride to delete
     * @throws Exception if ride doesn't exist or deletion fails
     */
    suspend operator fun invoke(rideId: Long) {
        val ride = rideRepository.getRideById(rideId)
            ?: throw IllegalArgumentException("Ride with ID $rideId not found")
        rideRepository.deleteRide(ride)
    }

    /**
     * Delete a ride by its domain model.
     *
     * @param ride Ride object to delete
     * @throws Exception if deletion fails
     */
    suspend operator fun invoke(ride: Ride) {
        rideRepository.deleteRide(ride)
    }
}
