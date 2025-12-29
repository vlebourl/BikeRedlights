package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Stop
import com.google.android.gms.maps.model.LatLng
import javax.inject.Inject

/**
 * Calculates cluster center as arithmetic mean of GPS coordinates.
 *
 * Created by Feature 011: Stop Cluster Visualization.
 * Used to determine marker placement on Google Maps for cluster visualization.
 *
 * Business Rules:
 * - Center = (mean latitude, mean longitude)
 * - Arithmetic mean is sufficient for small clusters (30m radius per Feature 008)
 * - Input must not be empty (throws IllegalArgumentException)
 *
 * Algorithm:
 * 1. Extract all latitudes from stops
 * 2. Extract all longitudes from stops
 * 3. Calculate arithmetic mean of each
 * 4. Return LatLng with mean coordinates
 *
 * Complexity: O(n) where n = number of stops in cluster
 * Expected execution time: <1ms for 50 stops
 *
 * @throws IllegalArgumentException if stops list is empty
 */
class CalculateClusterCenterUseCase @Inject constructor() {
    /**
     * Calculate cluster center from list of stops.
     *
     * @param stops List of stops in cluster (must be non-empty)
     * @return LatLng representing cluster center for marker placement
     * @throws IllegalArgumentException if stops list is empty
     */
    operator fun invoke(stops: List<Stop>): LatLng {
        require(stops.isNotEmpty()) { "Cannot calculate center of empty cluster" }

        val avgLat = stops.map { it.latitude }.average()
        val avgLng = stops.map { it.longitude }.average()

        return LatLng(avgLat, avgLng)
    }
}
