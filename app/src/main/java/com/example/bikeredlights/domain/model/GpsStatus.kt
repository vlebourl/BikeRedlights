package com.example.bikeredlights.domain.model

/**
 * Represents the current state of GPS signal availability and quality.
 *
 * This sealed interface enables type-safe state management and exhaustive when expressions
 * for GPS status handling. Status transitions based on accuracy thresholds:
 * - Unavailable: accuracy > 50m or no location for >10s
 * - Acquiring: accuracy 10-50m or first location fix
 * - Active: accuracy ≤ 10m with regular updates
 */
sealed interface GpsStatus {

    /**
     * GPS is unavailable (no permission, disabled, or severe signal loss).
     *
     * UI should display red indicator with error message.
     */
    data object Unavailable : GpsStatus

    /**
     * GPS is acquiring signal (no fix yet or low accuracy).
     *
     * UI should display yellow/orange indicator with "Acquiring..." message.
     */
    data object Acquiring : GpsStatus

    /**
     * GPS signal is active with acceptable accuracy.
     *
     * UI should display green indicator with accuracy value.
     *
     * @property accuracy Horizontal accuracy in meters (≤10m for good signal)
     */
    data class Active(val accuracy: Float) : GpsStatus
}
