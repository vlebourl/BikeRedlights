package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.domain.model.display.RideListItem
import com.example.bikeredlights.domain.model.display.toListItem
import com.example.bikeredlights.domain.model.history.SortPreference
import com.example.bikeredlights.domain.repository.RideRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Use case for retrieving all rides with optional sorting.
 *
 * Transforms domain Ride models into display-ready RideListItem models.
 * Applies user's unit preference for consistent formatting.
 *
 * **Use Cases**:
 * - Ride history list display
 * - Sorted ride browsing
 * - Unit-aware ride summaries
 *
 * @property rideRepository Repository for ride data access
 */
class GetAllRidesUseCase @Inject constructor(
    private val rideRepository: RideRepository
) {
    /**
     * Get all rides with default sort (newest first).
     *
     * @param unitsSystem User's preferred unit system
     * @return Flow emitting list of display-ready ride items
     */
    operator fun invoke(unitsSystem: UnitsSystem): Flow<List<RideListItem>> {
        return rideRepository.getAllRidesFlow().map { rides ->
            rides.map { it.toListItem(unitsSystem) }
        }
    }

    /**
     * Get all rides with custom sort order.
     *
     * @param sortPreference Desired sort order
     * @param unitsSystem User's preferred unit system
     * @return Flow emitting sorted list of display-ready ride items
     */
    operator fun invoke(
        sortPreference: SortPreference,
        unitsSystem: UnitsSystem
    ): Flow<List<RideListItem>> {
        return rideRepository.getAllRidesSorted(sortPreference).map { rides ->
            rides.map { it.toListItem(unitsSystem) }
        }
    }
}
