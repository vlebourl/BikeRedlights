package com.example.bikeredlights.domain.model

/**
 * Transient runtime state for stop detection logic (Feature 009).
 *
 * Purpose: Track stop detection state machine progress during ride recording.
 * NOT persisted to database - lives in RideRecordingService scope.
 *
 * Lifecycle:
 * - Created when ride starts
 * - Reset when ride stops
 * - Survives app backgrounding (service scope)
 * - Destroyed when service stops
 *
 * State Machine States:
 * - **Moving**: Speed > threshold, no stop in progress
 * - **Detecting**: Speed < threshold, counting consecutive seconds (0-3)
 * - **Confirmed**: Duration threshold met, stop persisted, popup visible
 *
 * Memory Safety:
 * - No Location objects stored (prevents Context leaks)
 * - Only primitives and IDs
 * - ~200 bytes total size
 *
 * @property currentSpeed Latest GPS speed in km/h (from Location.getSpeed() * 3.6)
 * @property speedBelowThresholdCount Consecutive seconds speed < threshold (0-3)
 * @property speedAboveThresholdCount Consecutive seconds speed > threshold during active stop (0-3)
 * @property stopTimer Duration in milliseconds since stop confirmed (null when not stopped)
 * @property currentStopNumber Next stop number to assign (1, 2, 3... per ride)
 * @property activeStopId Database ID of current in-progress stop (null when not stopped)
 * @property isStopConfirmed True when duration threshold met, false during detection phase
 * @property detectionStartTime Timestamp when speed first dropped below threshold (used for threshold checking only)
 * @property stopConfirmedTime Timestamp when stop was confirmed (used for UI duration calculation)
 * @property hasStartedMoving True once rider has reached movement threshold (prevents false stops at ride start)
 */
data class StopDetectionState(
    val currentSpeed: Float = 0f,
    val speedBelowThresholdCount: Int = 0,
    val speedAboveThresholdCount: Int = 0,
    val stopTimer: Long? = null,
    val currentStopNumber: Int = 1,
    val activeStopId: Long? = null,
    val isStopConfirmed: Boolean = false,
    val detectionStartTime: Long? = null,
    val stopConfirmedTime: Long? = null,
    val hasStartedMoving: Boolean = false
) {
    /**
     * Check if currently in Moving state (not detecting, not stopped).
     */
    val isMoving: Boolean
        get() = !isDetecting && !isStopConfirmed

    /**
     * Check if currently in Detecting state (speed below threshold, counting consecutive seconds).
     */
    val isDetecting: Boolean
        get() = speedBelowThresholdCount > 0 && !isStopConfirmed

    /**
     * Check if currently in Confirmed state (stop active, popup visible).
     */
    val isStopped: Boolean
        get() = isStopConfirmed && activeStopId != null

    /**
     * Get current stop duration in seconds (only valid when stopped).
     *
     * Uses stopConfirmedTime (not detectionStartTime) to show accurate duration
     * from when stop was confirmed, not from when detection started.
     *
     * @return Duration in seconds, or 0 if not stopped
     */
    fun getCurrentStopDuration(): Int {
        if (!isStopped || stopConfirmedTime == null) return 0
        val currentTime = System.currentTimeMillis()
        return ((currentTime - stopConfirmedTime) / 1000).toInt()
    }

    /**
     * Reset state for new ride (called on ride start).
     */
    fun reset(): StopDetectionState {
        return StopDetectionState(
            currentSpeed = 0f,
            speedBelowThresholdCount = 0,
            speedAboveThresholdCount = 0,
            stopTimer = null,
            currentStopNumber = 1,
            activeStopId = null,
            isStopConfirmed = false,
            detectionStartTime = null,
            stopConfirmedTime = null,
            hasStartedMoving = false
        )
    }

    companion object {
        /**
         * Create initial state for new ride.
         */
        fun initial(): StopDetectionState = StopDetectionState()

        /**
         * Maximum consecutive seconds counter value (caps at 3).
         */
        const val MAX_CONSECUTIVE_SECONDS = 3

        /**
         * Movement threshold in km/h to determine if rider has actually started moving.
         * Must be higher than stop threshold to avoid false positives from GPS noise.
         * Default: 5 km/h (typical walking pace - ensures rider is cycling)
         */
        const val MOVEMENT_THRESHOLD_KMH = 5f
    }
}
