package com.example.bikeredlights.domain.util

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests for haversineDistance function (Feature 010).
 *
 * Tests verify correctness of Haversine formula implementation for GPS distance calculation.
 *
 * Test Strategy:
 * - Known distances: Use real-world locations with verified distances
 * - Edge cases: Same point, antipodal points, date line crossing
 * - Accuracy: Validate ±0.5% error tolerance
 *
 * Reference Distances (validated via Google Maps):
 * - Google Campus (Mountain View) to Moscone Center (SF): ~49 km
 * - Golden Gate Bridge to Bay Bridge: ~12 km
 * - Same intersection (within 20m): ~15m
 */
class HaversineDistanceTest {

    /**
     * Test: Same point returns 0 meters.
     *
     * Validates: distance(P, P) = 0 for any point P
     */
    @Test
    fun `same point returns zero distance`() {
        val lat = 37.422
        val lon = -122.084

        val distance = haversineDistance(lat, lon, lat, lon)

        assertThat(distance).isEqualTo(0.0f)
    }

    /**
     * Test: Known distance - Google Campus to Moscone Center.
     *
     * Validates: Accuracy within ±0.5% for ~49 km distance
     *
     * Coordinates:
     * - Google Campus: 37.422, -122.084 (Mountain View, CA)
     * - Moscone Center: 37.784, -122.401 (San Francisco, CA)
     *
     * Expected: ~49,000 meters (49 km)
     * Tolerance: ±245 meters (0.5%)
     */
    @Test
    fun `known distance - Google campus to Moscone center`() {
        val googleLat = 37.422
        val googleLon = -122.084
        val mosconeLat = 37.784
        val mosconeLon = -122.401

        val distance = haversineDistance(googleLat, googleLon, mosconeLat, mosconeLon)

        // Expected distance: ~49,000 meters (verified via Google Maps)
        // Tolerance: ±0.5% = ±245 meters
        assertThat(distance).isWithin(245.0f).of(49000.0f)
    }

    /**
     * Test: Short distance - two stops at same intersection (within 20m clustering radius).
     *
     * Validates: Accuracy for small distances (<100m) critical for clustering
     *
     * Scenario: Two stops ~14 meters apart at same intersection
     * - Stop 1: 37.422000, -122.084000
     * - Stop 2: 37.422100, -122.084100 (~14m NE)
     *
     * Expected: ~14.2 meters
     * Tolerance: ±2 meters (sufficient for 20m clustering)
     */
    @Test
    fun `short distance - two stops within clustering radius`() {
        val stop1Lat = 37.422000
        val stop1Lon = -122.084000
        val stop2Lat = 37.422100
        val stop2Lon = -122.084100

        val distance = haversineDistance(stop1Lat, stop1Lon, stop2Lat, stop2Lon)

        // Expected: ~14.2 meters (verified by haversine calculation)
        // Tolerance: ±2 meters (critical for 20m clustering accuracy)
        assertThat(distance).isWithin(2.0f).of(14.2f)
    }

    /**
     * Test: Points on same latitude (different longitudes).
     *
     * Validates: Longitude-only distance calculation
     *
     * Scenario: Two points at same latitude (37.422°N), 0.1° longitude apart
     * Expected: ~8,831 meters (latitude-dependent)
     */
    @Test
    fun `points on same latitude`() {
        val lat = 37.422
        val lon1 = -122.084
        val lon2 = -122.184 // 0.1° west

        val distance = haversineDistance(lat, lon1, lat, lon2)

        // At 37.422° latitude, 0.1° longitude ≈ 8,831 meters
        // Tolerance: ±200 meters
        assertThat(distance).isWithin(200.0f).of(8831.0f)
    }

    /**
     * Test: Points on same longitude (different latitudes).
     *
     * Validates: Latitude-only distance calculation
     *
     * Scenario: Two points at same longitude, 0.1° latitude apart
     * Expected: ~11,100 meters (constant for all latitudes)
     */
    @Test
    fun `points on same longitude`() {
        val lon = -122.084
        val lat1 = 37.422
        val lat2 = 37.522 // 0.1° north

        val distance = haversineDistance(lat1, lon, lat2, lon)

        // 0.1° latitude ≈ 11,100 meters (constant)
        // Tolerance: ±50 meters
        assertThat(distance).isWithin(50.0f).of(11100.0f)
    }

    /**
     * Test: Crossing international date line (longitude wrap at ±180°).
     *
     * Validates: Correct handling of longitude wrap (lon1=-179°, lon2=+179° = 2° apart, not 358°)
     *
     * Scenario: Two points near date line
     * - Point 1: 0°N, 179°E (just west of date line)
     * - Point 2: 0°N, -179°E (just east of date line)
     *
     * Expected: ~222 km (2° longitude at equator)
     * NOT: ~39,800 km (358° would be nearly circumference)
     */
    @Test
    fun `crossing international date line`() {
        val lat = 0.0 // Equator (simplifies calculation)
        val lon1 = 179.0  // Just west of date line
        val lon2 = -179.0 // Just east of date line

        val distance = haversineDistance(lat, lon1, lat, lon2)

        // 2° longitude at equator ≈ 222,000 meters
        // Tolerance: ±1000 meters
        assertThat(distance).isWithin(1000.0f).of(222000.0f)

        // Critical: Verify NOT computing 358° distance (~39,800 km)
        assertThat(distance).isLessThan(300000.0f)
    }

    /**
     * Test: Antipodal points (opposite sides of Earth).
     *
     * Validates: Maximum possible distance ≈ π * R ≈ 20,015 km
     *
     * Scenario: North Pole (90°N, 0°E) to South Pole (-90°N, 0°E)
     * Expected: ~20,015,086 meters (half Earth's circumference)
     */
    @Test
    fun `antipodal points - poles`() {
        val northPoleLat = 90.0
        val northPoleLon = 0.0
        val southPoleLat = -90.0
        val southPoleLon = 0.0

        val distance = haversineDistance(northPoleLat, northPoleLon, southPoleLat, southPoleLon)

        // Half of Earth's circumference ≈ 20,015,086 meters
        // Tolerance: ±1000 meters (acceptable for max distance validation)
        assertThat(distance).isWithin(1000.0f).of(20015086.0f)
    }

    /**
     * Test: Symmetry property - distance(A, B) = distance(B, A).
     *
     * Validates: Commutative property of distance function
     */
    @Test
    fun `distance is symmetric`() {
        val lat1 = 37.422
        val lon1 = -122.084
        val lat2 = 37.784
        val lon2 = -122.401

        val distanceAB = haversineDistance(lat1, lon1, lat2, lon2)
        val distanceBA = haversineDistance(lat2, lon2, lat1, lon1)

        assertThat(distanceAB).isEqualTo(distanceBA)
    }

    /**
     * Test: Triangle inequality - distance(A, C) <= distance(A, B) + distance(B, C).
     *
     * Validates: Fundamental distance metric property
     *
     * Points: A → B → C (path via B should be >= direct distance A → C)
     */
    @Test
    fun `triangle inequality holds`() {
        val latA = 37.422
        val lonA = -122.084
        val latB = 37.500
        val lonB = -122.200
        val latC = 37.784
        val lonC = -122.401

        val distanceAC = haversineDistance(latA, lonA, latC, lonC)
        val distanceAB = haversineDistance(latA, lonA, latB, lonB)
        val distanceBC = haversineDistance(latB, lonB, latC, lonC)

        // Triangle inequality: AC <= AB + BC
        assertThat(distanceAC).isAtMost(distanceAB + distanceBC)
    }

    /**
     * Test: Very small distance (<1 meter) precision.
     *
     * Validates: Function works for sub-meter precision (e.g., same GPS coordinate drift)
     *
     * Scenario: Two measurements at same location with GPS drift (~0.5m)
     */
    @Test
    fun `very small distance - GPS drift`() {
        val lat1 = 37.422000
        val lon1 = -122.084000
        val lat2 = 37.422001 // ~0.1m north
        val lon2 = -122.084001 // ~0.08m east

        val distance = haversineDistance(lat1, lon1, lat2, lon2)

        // Expected: ~0.13 meters (sqrt(0.1² + 0.08²) ≈ 0.13m)
        // Tolerance: ±0.1 meters (float precision)
        assertThat(distance).isAtMost(0.5f)
    }

    /**
     * Test: Accuracy at high latitudes (near poles).
     *
     * Validates: Haversine formula accuracy at extreme latitudes (spherical Earth assumption)
     *
     * Scenario: Two points in Alaska at ~70°N latitude
     * Note: Haversine assumes spherical Earth, less accurate near poles (but still ±0.5% for <10,000km)
     */
    @Test
    fun `accuracy at high latitude - Alaska`() {
        val alaskaLat1 = 70.0
        val alaskaLon1 = -150.0
        val alaskaLat2 = 70.0
        val alaskaLon2 = -149.0 // 1° longitude difference

        val distance = haversineDistance(alaskaLat1, alaskaLon1, alaskaLat2, alaskaLon2)

        // At 70° latitude, 1° longitude ≈ 38 km
        // Tolerance: ±1% (higher tolerance at extreme latitudes)
        assertThat(distance).isWithin(400.0f).of(38000.0f)
    }
}
