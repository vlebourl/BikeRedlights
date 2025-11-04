package com.example.bikeredlights.domain.util

import kotlin.math.round

/**
 * Utility functions for converting between metric and imperial units.
 *
 * Conversion factors:
 * - 1 km/h = 0.621371 mph
 * - 1 km = 0.621371 miles
 *
 * All conversions preserve precision to 2 decimal places for display.
 */
object UnitConversions {
    /**
     * Conversion factor from kilometers per hour to miles per hour.
     * Source: NIST (National Institute of Standards and Technology)
     */
    private const val KMH_TO_MPH = 0.621371

    /**
     * Conversion factor from kilometers to miles.
     * Same as KMH_TO_MPH since units ratio is identical.
     */
    private const val KM_TO_MILES = 0.621371

    /**
     * Convert speed from kilometers per hour to miles per hour.
     *
     * @param kmh Speed in kilometers per hour
     * @return Speed in miles per hour, rounded to 2 decimal places
     *
     * Examples:
     * - 0.0 km/h → 0.0 mph
     * - 100.0 km/h → 62.14 mph
     * - 25.5 km/h → 15.84 mph
     */
    fun toMph(kmh: Float): Float {
        return (round(kmh * KMH_TO_MPH * 100) / 100).toFloat()
    }

    /**
     * Convert speed from miles per hour to kilometers per hour.
     *
     * @param mph Speed in miles per hour
     * @return Speed in kilometers per hour, rounded to 2 decimal places
     */
    fun toKmh(mph: Float): Float {
        return (round(mph / KMH_TO_MPH * 100) / 100).toFloat()
    }

    /**
     * Convert distance from kilometers to miles.
     *
     * @param km Distance in kilometers
     * @return Distance in miles, rounded to 2 decimal places
     *
     * Examples:
     * - 0.0 km → 0.0 mi
     * - 10.0 km → 6.21 mi
     * - 42.195 km → 26.22 mi (marathon distance)
     */
    fun toMiles(km: Float): Float {
        return (round(km * KM_TO_MILES * 100) / 100).toFloat()
    }

    /**
     * Convert distance from miles to kilometers.
     *
     * @param miles Distance in miles
     * @return Distance in kilometers, rounded to 2 decimal places
     */
    fun toKm(miles: Float): Float {
        return (round(miles / KM_TO_MILES * 100) / 100).toFloat()
    }

    /**
     * Format speed with appropriate unit label.
     *
     * @param speed Speed value (already converted to target units)
     * @param isMetric true for km/h, false for mph
     * @return Formatted string like "25.5 km/h" or "15.8 mph"
     */
    fun formatSpeed(speed: Float, isMetric: Boolean): String {
        val unit = if (isMetric) "km/h" else "mph"
        return "%.1f %s".format(speed, unit)
    }

    /**
     * Format distance with appropriate unit label.
     *
     * @param distance Distance value (already converted to target units)
     * @param isMetric true for km, false for miles
     * @return Formatted string like "10.5 km" or "6.5 mi"
     */
    fun formatDistance(distance: Float, isMetric: Boolean): String {
        val unit = if (isMetric) "km" else "mi"
        return "%.2f %s".format(distance, unit)
    }
}
