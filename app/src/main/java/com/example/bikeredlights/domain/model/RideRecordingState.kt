package com.example.bikeredlights.domain.model

/**
 * Domain model representing the current state of ride recording.
 *
 * **Design Principles**:
 * - Sealed class for exhaustive when() checks
 * - Type-safe state machine
 * - Persisted to DataStore for process death recovery
 *
 * **State Transitions**:
 * ```
 * Idle
 *   → Recording (start button pressed)
 *
 * Recording
 *   → ManuallyPaused (pause button pressed)
 *   → AutoPaused (speed dropped below threshold)
 *   → Stopped (stop button pressed)
 *
 * ManuallyPaused
 *   → Recording (resume button pressed)
 *   → Stopped (stop button pressed)
 *
 * AutoPaused
 *   → Recording (speed increased above threshold)
 *   → Stopped (stop button pressed)
 *
 * Stopped
 *   → Idle (after save/discard dialog)
 * ```
 *
 * **State Hierarchy**:
 * - Idle: No active ride
 * - Recording: Active GPS tracking, updating distance/speed
 * - ManuallyPaused: User-initiated pause, GPS still tracking
 * - AutoPaused: Speed-based pause, GPS still tracking
 * - Stopped: Ride ended, awaiting save/discard decision
 */
sealed class RideRecordingState {

    /**
     * No active ride recording.
     */
    data object Idle : RideRecordingState()

    /**
     * Active ride recording in progress.
     *
     * @property rideId Database ID of the current ride
     */
    data class Recording(val rideId: Long) : RideRecordingState()

    /**
     * Ride manually paused by user.
     *
     * @property rideId Database ID of the current ride
     */
    data class ManuallyPaused(val rideId: Long) : RideRecordingState()

    /**
     * Ride auto-paused due to low speed.
     *
     * @property rideId Database ID of the current ride
     */
    data class AutoPaused(val rideId: Long) : RideRecordingState()

    /**
     * Ride stopped, awaiting save/discard decision.
     *
     * @property rideId Database ID of the current ride
     */
    data class Stopped(val rideId: Long) : RideRecordingState()

    /**
     * Check if recording is active (not idle or stopped).
     */
    val isActive: Boolean
        get() = this is Recording || this is ManuallyPaused || this is AutoPaused

    /**
     * Check if ride is paused (manual or auto).
     */
    val isPaused: Boolean
        get() = this is ManuallyPaused || this is AutoPaused

    /**
     * Get current ride ID if available.
     */
    val currentRideId: Long?
        get() = when (this) {
            is Recording -> rideId
            is ManuallyPaused -> rideId
            is AutoPaused -> rideId
            is Stopped -> rideId
            is Idle -> null
        }
}
