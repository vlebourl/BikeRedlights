package com.example.bikeredlights.domain.util

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for UnitConversions utility.
 *
 * Tests cover:
 * - km/h → mph conversion with 2 decimal precision
 * - mph → km/h conversion
 * - km → miles conversion
 * - miles → km conversion
 * - Edge cases: 0.0, 0.1, 100.0, 999.9
 * - Conversion factor accuracy (0.621371)
 * - Formatting functions
 */
class UnitConversionsTest {

    companion object {
        private const val DELTA = 0.01f // Precision tolerance for float comparisons
    }

    @Test
    fun `toMph converts 0 kmh correctly`() {
        val result = UnitConversions.toMph(0.0f)
        assertEquals(0.0f, result, DELTA)
    }

    @Test
    fun `toMph converts 0_1 kmh correctly`() {
        val result = UnitConversions.toMph(0.1f)
        assertEquals(0.06f, result, DELTA)
    }

    @Test
    fun `toMph converts 100 kmh correctly`() {
        val result = UnitConversions.toMph(100.0f)
        // 100 * 0.621371 = 62.1371, rounded to 2 decimals = 62.14
        assertEquals(62.14f, result, DELTA)
    }

    @Test
    fun `toMph converts 999_9 kmh correctly`() {
        val result = UnitConversions.toMph(999.9f)
        // 999.9 * 0.621371 = 621.309, rounded to 2 decimals = 621.31
        assertEquals(621.31f, result, DELTA)
    }

    @Test
    fun `toMph uses correct conversion factor 0_621371`() {
        // Test with 25 km/h (common cycling speed)
        val kmh = 25.0f
        val expectedMph = 15.53f // 25 * 0.621371 = 15.534, rounded = 15.53
        val result = UnitConversions.toMph(kmh)
        assertEquals(expectedMph, result, DELTA)
    }

    @Test
    fun `toMph rounds to 2 decimal places`() {
        // Test rounding precision
        val result = UnitConversions.toMph(25.5f) // 15.844...
        assertEquals(15.84f, result, DELTA)
    }

    @Test
    fun `toKmh converts mph correctly`() {
        val result = UnitConversions.toKmh(62.14f)
        // Should be close to 100 km/h
        assertEquals(100.0f, result, 0.1f)
    }

    @Test
    fun `toMiles converts 0 km correctly`() {
        val result = UnitConversions.toMiles(0.0f)
        assertEquals(0.0f, result, DELTA)
    }

    @Test
    fun `toMiles converts 0_1 km correctly`() {
        val result = UnitConversions.toMiles(0.1f)
        assertEquals(0.06f, result, DELTA)
    }

    @Test
    fun `toMiles converts 100 km correctly`() {
        val result = UnitConversions.toMiles(100.0f)
        assertEquals(62.14f, result, DELTA)
    }

    @Test
    fun `toMiles converts 999_9 km correctly`() {
        val result = UnitConversions.toMiles(999.9f)
        assertEquals(621.31f, result, DELTA)
    }

    @Test
    fun `toMiles converts marathon distance 42_195 km correctly`() {
        val result = UnitConversions.toMiles(42.195f)
        // 42.195 * 0.621371 = 26.219, rounded = 26.22
        assertEquals(26.22f, result, DELTA)
    }

    @Test
    fun `toKm converts miles correctly`() {
        val result = UnitConversions.toKm(62.14f)
        // Should be close to 100 km
        assertEquals(100.0f, result, 0.1f)
    }

    @Test
    fun `toMph and toKmh are inverse operations`() {
        val originalKmh = 50.0f
        val mph = UnitConversions.toMph(originalKmh)
        val backToKmh = UnitConversions.toKmh(mph)
        assertEquals(originalKmh, backToKmh, 0.1f)
    }

    @Test
    fun `toMiles and toKm are inverse operations`() {
        val originalKm = 20.0f
        val miles = UnitConversions.toMiles(originalKm)
        val backToKm = UnitConversions.toKm(miles)
        assertEquals(originalKm, backToKm, 0.1f)
    }

    @Test
    fun `formatSpeed with metric shows kmh`() {
        val result = UnitConversions.formatSpeed(25.5f, isMetric = true)
        assertEquals("25.5 km/h", result)
    }

    @Test
    fun `formatSpeed with imperial shows mph`() {
        val result = UnitConversions.formatSpeed(15.8f, isMetric = false)
        assertEquals("15.8 mph", result)
    }

    @Test
    fun `formatDistance with metric shows km`() {
        val result = UnitConversions.formatDistance(10.5f, isMetric = true)
        assertEquals("10.50 km", result)
    }

    @Test
    fun `formatDistance with imperial shows mi`() {
        val result = UnitConversions.formatDistance(6.52f, isMetric = false)
        assertEquals("6.52 mi", result)
    }
}
