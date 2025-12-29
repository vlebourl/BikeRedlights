/**
 * CONTRACT: HaversineDistance (Feature 010)
 *
 * This file documents the contract for geographic distance calculation.
 * Implementation: app/src/main/java/com/example/bikeredlights/domain/util/HaversineDistance.kt
 *
 * Purpose: Calculate accurate distance between GPS coordinates on Earth's surface.
 */

package com.example.bikeredlights.domain.util

import kotlin.math.*

/**
 * Calculate distance between two GPS coordinates using Haversine formula.
 *
 * **Formula**:
 * ```
 * a = sin²(Δlat/2) + cos(lat1) * cos(lat2) * sin²(Δlon/2)
 * c = 2 * atan2(√a, √(1−a))
 * distance = R * c
 * ```
 * where R = Earth's radius = 6,371,000 meters
 *
 * **Accuracy**:
 * - Assumes spherical Earth (not ellipsoidal)
 * - Error: ±0.5% for most distances (<10,000 km)
 * - For 20m clustering radius: error is ±0.1m (negligible)
 * - More accurate than flat Euclidean distance (which has up to 30% error at high latitudes)
 * - Less accurate than Vincenty formula (which has ±0.001% error but 10x slower)
 *
 * **Contract**:
 * - MUST be a pure function (no side effects)
 * - MUST return distance in meters
 * - MUST handle same point (lat1=lat2, lon1=lon2) → 0.0m
 * - MUST handle antipodal points (opposite sides of Earth) → ~20,000km
 * - MUST handle longitude wrap at ±180° (e.g., lon1=-179°, lon2=+179° = 2° apart, not 358°)
 * - SHOULD be accurate to ±0.5% for distances up to 10,000 km
 * - SHOULD NOT validate latitude/longitude ranges (caller's responsibility)
 *
 * **Performance**:
 * - Time: O(1) constant time (6 trig operations + 1 square root)
 * - Expected: ~10-15 CPU cycles per call
 * - Typical: 1M calls in 10-20ms on modern mobile CPU
 *
 * **Edge Cases**:
 * - Same point: distance = 0.0m (sin(0) = 0, atan2(0, 1) = 0)
 * - Points on same latitude: distance = cos(lat) * Δlon * R
 * - Points on same longitude: distance = Δlat * R
 * - Antipodal points: distance ≈ π * R ≈ 20,037 km
 * - Crossing date line (lon1=-179°, lon2=+179°): correctly calculates 2° not 358°
 *
 * **Earth Radius**:
 * - Mean radius: 6,371,000 m (used by this implementation)
 * - Equatorial radius: 6,378,137 m
 * - Polar radius: 6,356,752 m
 * - For clustering (20m epsilon), ±7km radius variation is negligible (<0.1% error)
 *
 * **Example Usage**:
 * ```kotlin
 * // Google campus to Moscone Center, San Francisco
 * val distance = haversineDistance(
 *     lat1 = 37.422, lon1 = -122.084,  // Mountain View
 *     lat2 = 37.784, lon2 = -122.401   // San Francisco
 * )
 * // distance ≈ 49,000 meters (49 km)
 *
 * // Two stops at same intersection (within 20m)
 * val distance2 = haversineDistance(
 *     lat1 = 37.422000, lon1 = -122.084000,
 *     lat2 = 37.422100, lon2 = -122.084100  // ~15m away
 * )
 * // distance2 ≈ 15.7 meters
 * ```
 *
 * **Alternatives Considered**:
 * - **Vincenty formula**: More accurate (±0.001%) but 10x slower - overkill for 20m clustering
 * - **Flat Euclidean distance**: Fast but inaccurate (up to 30% error at high latitudes) - REJECTED
 * - **Android Location.distanceBetween()**: Requires Android framework, prevents pure domain testing - REJECTED
 *
 * **References**:
 * - Haversine formula: https://en.wikipedia.org/wiki/Haversine_formula
 * - Movable Type Scripts: https://www.movable-type.co.uk/scripts/latlong.html
 * - Aviation Formulary: http://www.edwilliams.org/avform147.htm
 *
 * @param lat1 Latitude of first point in decimal degrees [-90.0, 90.0]
 * @param lon1 Longitude of first point in decimal degrees [-180.0, 180.0]
 * @param lat2 Latitude of second point in decimal degrees [-90.0, 90.0]
 * @param lon2 Longitude of second point in decimal degrees [-180.0, 180.0]
 * @return Distance between points in meters (always >= 0.0)
 */
fun haversineDistance(
    lat1: Double,
    lon1: Double,
    lat2: Double,
    lon2: Double
): Float {
    // Earth's mean radius in meters
    val R = 6371000.0

    // Convert decimal degrees to radians
    val lat1Rad = Math.toRadians(lat1)
    val lat2Rad = Math.toRadians(lat2)
    val deltaLatRad = Math.toRadians(lat2 - lat1)
    val deltaLonRad = Math.toRadians(lon2 - lon1)

    // Haversine formula
    val a = sin(deltaLatRad / 2).pow(2) +
            cos(lat1Rad) * cos(lat2Rad) * sin(deltaLonRad / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))

    // Distance in meters
    return (R * c).toFloat()
}

/**
 * Optimized Haversine distance for small distances (<1 km).
 *
 * **Contract**:
 * - MUST be faster than full Haversine for distances <1 km
 * - SHOULD have same accuracy as full Haversine (±0.5%)
 * - MAY use flat Earth approximation if justified by performance gain
 *
 * **Status**: DEFERRED - Use full Haversine for MVP
 * Only implement if profiling shows haversineDistance() is a bottleneck.
 *
 * @param lat1 Latitude of first point in decimal degrees
 * @param lon1 Longitude of first point in decimal degrees
 * @param lat2 Latitude of second point in decimal degrees
 * @param lon2 Longitude of second point in decimal degrees
 * @return Distance between points in meters
 */
// fun haversineDistanceOptimized(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Float {
//     // FUTURE: Flat Earth approximation for small distances
//     // x = Δlon * cos(avgLat) * R
//     // y = Δlat * R
//     // distance = sqrt(x² + y²)
//     // Only accurate for distances <1 km, but 2-3x faster
// }
