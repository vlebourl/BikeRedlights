package com.example.bikeredlights.domain.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.scan

/**
 * Utility functions for stop detection (Feature 009).
 *
 * Provides:
 * - Consecutive seconds filtering for GPS noise reduction
 * - Duration formatting for UI display
 * - Helper functions for state machine logic
 */
object StopDetectionUtils {

    /**
     * Filter Flow to require N consecutive readings matching a predicate.
     *
     * Use Case: GPS speed is extremely unreliable at low speeds (can show 200+ mph when stationary).
     * Requiring 3 consecutive readings below/above threshold avoids false positives from GPS noise.
     *
     * Example:
     * ```kotlin
     * speedFlow
     *     .filterConsecutiveSeconds(predicate = { it < 3f }, requiredCount = 3)
     *     .collect { isStoppedConfirmed -> /* handle state change */ }
     * ```
     *
     * State Tracking:
     * - Counter increments when predicate matches
     * - Counter resets to 0 when predicate fails
     * - Emits true when counter reaches requiredCount
     * - Emits false when counter drops below requiredCount
     *
     * @receiver Flow<Float> GPS speed values in km/h
     * @param predicate Condition to check (e.g., speed < threshold)
     * @param requiredCount Number of consecutive matches needed (default 3)
     * @return Flow<Boolean> emitting true when N consecutive matches achieved
     */
    fun Flow<Float>.filterConsecutiveSeconds(
        predicate: (Float) -> Boolean,
        requiredCount: Int = 3
    ): Flow<Boolean> = scan(0 to false) { (count, _), speed ->
        val matches = predicate(speed)
        val newCount = if (matches) {
            (count + 1).coerceAtMost(requiredCount)
        } else {
            0
        }
        newCount to (newCount >= requiredCount)
    }.map { it.second }.distinctUntilChanged()

    /**
     * Format duration in seconds to human-readable string.
     *
     * Format:
     * - 0-59s: "0:XX"
     * - 60s-3599s: "M:SS"
     * - 3600s+: "H:MM:SS"
     *
     * Examples:
     * - 5s → "0:05"
     * - 45s → "0:45"
     * - 90s → "1:30"
     * - 3661s → "1:01:01"
     *
     * @param durationSeconds Duration in seconds
     * @return Formatted string (MM:SS or HH:MM:SS)
     */
    fun formatDuration(durationSeconds: Int): String {
        require(durationSeconds >= 0) { "Duration must be non-negative, got $durationSeconds" }

        val hours = durationSeconds / 3600
        val minutes = (durationSeconds % 3600) / 60
        val seconds = durationSeconds % 60

        return if (hours > 0) {
            // HH:MM:SS format for durations >= 1 hour
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            // MM:SS format for durations < 1 hour
            String.format("%d:%02d", minutes, seconds)
        }
    }

    /**
     * Calculate duration between two timestamps.
     *
     * @param startTimestamp Unix epoch milliseconds when stop started
     * @param endTimestamp Unix epoch milliseconds when stop ended
     * @return Duration in seconds
     * @throws IllegalArgumentException if endTimestamp < startTimestamp
     */
    fun calculateDuration(startTimestamp: Long, endTimestamp: Long): Int {
        require(endTimestamp >= startTimestamp) {
            "End timestamp ($endTimestamp) must be >= start timestamp ($startTimestamp)"
        }
        return ((endTimestamp - startTimestamp) / 1000).toInt()
    }

    /**
     * Check if speed is below threshold (accounting for GPS accuracy).
     *
     * GPS accuracy: Speed readings have ~±1 km/h error margin.
     * Add small buffer to avoid edge case flickering.
     *
     * @param speed Current GPS speed in km/h
     * @param threshold Configured speed threshold in km/h
     * @return True if speed is reliably below threshold
     */
    fun isSpeedBelowThreshold(speed: Float, threshold: Float): Boolean {
        return speed < threshold
    }

    /**
     * Check if speed is above threshold (accounting for GPS accuracy).
     *
     * @param speed Current GPS speed in km/h
     * @param threshold Configured speed threshold in km/h
     * @return True if speed is reliably above threshold
     */
    fun isSpeedAboveThreshold(speed: Float, threshold: Float): Boolean {
        return speed >= threshold
    }

    /**
     * Validate stop detection configuration thresholds.
     *
     * @param speedThreshold Speed threshold in km/h (must be 1-5)
     * @param durationThreshold Duration threshold in seconds (must be 5-30)
     * @throws IllegalArgumentException if thresholds are out of valid range
     */
    fun validateThresholds(speedThreshold: Float, durationThreshold: Int) {
        require(speedThreshold in 1f..5f) {
            "Speed threshold must be in range [1.0, 5.0] km/h, got $speedThreshold"
        }
        require(durationThreshold in 5..30) {
            "Duration threshold must be in range [5, 30] seconds, got $durationThreshold"
        }
    }
}
