package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.display.RideDetailData
import com.example.bikeredlights.domain.model.display.toDetailData
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for fetching a single ride by ID with formatted display data.
 *
 * **Business Logic**:
 * - Fetches ride from repository by ID
 * - Transforms to RideDetailData with unit-aware formatting
 * - Returns Flow for reactive updates
 *
 * **Architecture**:
 * - Part of domain layer (pure business logic)
 * - No Android dependencies
 * - Injected via Hilt
 *
 * **Usage**:
 * ```kotlin
 * val rideDetail: Flow<RideDetailData?> = getRideByIdUseCase(
 *     rideId = 123L,
 *     unitsSystem = UnitsSystem.METRIC
 * )
 * ```
 *
 * @property rideRepository Repository for accessing ride data
 */
class GetRideByIdUseCase @Inject constructor(
    private val rideRepository: RideRepository
) {
    /**
     * Fetch ride by ID with formatted display data.
     *
     * @param rideId Unique identifier for the ride
     * @param unitsSystem Unit system for formatting (METRIC or IMPERIAL)
     * @return Flow emitting RideDetailData or null if ride not found
     */
    operator fun invoke(
        rideId: Long,
        unitsSystem: UnitsSystem
    ): Flow<RideDetailData?> {
        return rideRepository.getRideById(rideId).map { ride ->
            ride?.toDetailData(unitsSystem)
        }
    }
}
