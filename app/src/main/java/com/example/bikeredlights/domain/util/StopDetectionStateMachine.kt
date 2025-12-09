package com.example.bikeredlights.domain.util

import com.example.bikeredlights.domain.model.Stop
import com.example.bikeredlights.domain.model.StopDetectionState
import com.example.bikeredlights.domain.repository.StopRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * State machine for stop detection logic (Feature 009).
 *
 * Responsibilities:
 * - Manage state transitions: Moving → Detecting → Confirmed
 * - Apply consecutive seconds filtering for GPS noise reduction
 * - Persist stops to database when confirmed
 * - Emit events to ViewModel for UI updates
 *
 * Lifecycle:
 * - Created when ride starts (lives in RideRecordingService scope)
 * - Survives app backgrounding (service scope)
 * - Destroyed when ride stops
 *
 * Thread Safety:
 * - All methods are synchronous (called from main thread or service scope)
 * - Database writes are async but non-blocking (launch in scope)
 *
 * Memory Safety:
 * - No Location objects stored (prevents Context leaks)
 * - Only primitives and IDs in state
 *
 * @param stopRepository Repository for database operations
 * @param speedThresholdKmh Speed threshold in km/h (from settings)
 * @param durationThresholdSeconds Duration threshold in seconds (from settings)
 * @param scope CoroutineScope for async operations (service scope)
 */
class StopDetectionStateMachine @Inject constructor(
    private val stopRepository: StopRepository,
    private val speedThresholdKmh: Float,
    private val durationThresholdSeconds: Int,
    private val scope: CoroutineScope
) {
    /**
     * Current runtime state (not persisted to database).
     */
    private var state = StopDetectionState.initial()

    /**
     * Current ride ID (set when ride starts).
     */
    private var currentRideId: Long? = null

    /**
     * Events emitted to ViewModel for UI updates.
     */
    private val _events = MutableSharedFlow<StopEvent>(extraBufferCapacity = 10)
    val events: SharedFlow<StopEvent> = _events.asSharedFlow()

    init {
        // Validate thresholds at initialization
        StopDetectionUtils.validateThresholds(speedThresholdKmh, durationThresholdSeconds)
    }

    /**
     * Start stop detection for a new ride.
     *
     * @param rideId Database ID of the current ride
     */
    fun startRide(rideId: Long) {
        currentRideId = rideId
        state = StopDetectionState.initial()
    }

    /**
     * Stop detection and cleanup state.
     *
     * If a stop is active when ride ends, delete it (not a traffic stop, it's the destination).
     *
     * **Rationale**: When ending a ride during an active stop:
     * - The stop is the rider's final destination, not a traffic light/stop sign
     * - We want to track traffic delays, not destination stops
     * - Prevents spurious stop records at ride end locations
     *
     * **Edge Case**: If rider stops at a red light for 30s then ends ride, we lose that stop.
     * This is acceptable - the stop wasn't the "ride-ending event", just coincidental timing.
     */
    fun stopRide() {
        if (state.isStopped) {
            // Delete the active stop instead of ending it
            // (it's the ride destination, not a traffic stop)
            val stopId = state.activeStopId
            if (stopId != null && stopId > 0) {
                android.util.Log.i("StopDetection", "🗑️ Deleting stop ID:$stopId (ride ended during stop - destination, not traffic stop)")
                scope.launch {
                    stopRepository.deleteStop(stopId)
                }
            }
        }
        state = StopDetectionState.initial()
        currentRideId = null
    }

    /**
     * Process a new speed reading from GPS.
     *
     * State transitions:
     * - **Moving → Detecting**: Speed drops below threshold (start counting consecutive seconds)
     * - **Detecting → Moving**: Speed rises above threshold before 3 consecutive seconds (reset counter)
     * - **Detecting → Confirmed**: 3 consecutive seconds below threshold + duration threshold met (insert to database)
     * - **Confirmed → Moving**: 3 consecutive seconds above threshold (end stop, update database)
     *
     * @param speedKmh Current GPS speed in km/h (from Location.getSpeed() * 3.6)
     * @param latitude Current latitude (for stop location)
     * @param longitude Current longitude (for stop location)
     * @param timestamp Current timestamp in milliseconds
     */
    fun processSpeed(
        speedKmh: Float,
        latitude: Double,
        longitude: Double,
        timestamp: Long
    ) {
        val rideId = currentRideId ?: return // No ride active, ignore

        // Update current speed
        state = state.copy(currentSpeed = speedKmh)

        when {
            // Case 1: Speed below threshold
            StopDetectionUtils.isSpeedBelowThreshold(speedKmh, speedThresholdKmh) -> {
                if (state.isStopConfirmed) {
                    // Already stopped - reset resume counter
                    android.util.Log.d("StopDetection", "Speed below threshold during active stop: ${speedKmh}km/h (still stopped)")
                    state = state.copy(speedAboveThresholdCount = 0)
                } else {
                    // Increment below-threshold counter
                    val newCount = state.speedBelowThresholdCount + 1

                    if (newCount == 1) {
                        // First second below threshold - start detection
                        android.util.Log.d("StopDetection", "⚠️ DETECTION START: Speed dropped below ${speedThresholdKmh}km/h (current: ${speedKmh}km/h)")
                        state = state.copy(
                            speedBelowThresholdCount = 1,
                            speedAboveThresholdCount = 0,
                            detectionStartTime = timestamp
                        )
                    } else {
                        // Update counter (caps at 3)
                        val cappedCount = newCount.coerceAtMost(StopDetectionState.MAX_CONSECUTIVE_SECONDS)
                        android.util.Log.d("StopDetection", "Detection ongoing: ${cappedCount}/3 consecutive seconds below threshold (${speedKmh}km/h)")
                        state = state.copy(speedBelowThresholdCount = cappedCount)

                        // Check duration threshold if we've reached 3 consecutive seconds
                        if (cappedCount >= StopDetectionState.MAX_CONSECUTIVE_SECONDS) {
                            val detectionStart = state.detectionStartTime ?: timestamp
                            val durationSoFar = ((timestamp - detectionStart) / 1000).toInt()

                            android.util.Log.d("StopDetection", "3 consecutive seconds confirmed! Duration so far: ${durationSoFar}s (threshold: ${durationThresholdSeconds}s)")

                            if (durationSoFar >= durationThresholdSeconds) {
                                // Duration threshold met - confirm stop
                                android.util.Log.i("StopDetection", "🛑 STOP CONFIRMED! Duration ${durationSoFar}s >= threshold ${durationThresholdSeconds}s")
                                confirmStop(rideId, latitude, longitude, detectionStart)
                            }
                        }
                    }
                }
            }

            // Case 2: Speed above threshold
            StopDetectionUtils.isSpeedAboveThreshold(speedKmh, speedThresholdKmh) -> {
                if (state.isStopConfirmed) {
                    // Currently stopped - increment resume counter
                    val newCount = state.speedAboveThresholdCount + 1

                    android.util.Log.d("StopDetection", "✅ Speed above threshold during stop: ${speedKmh}km/h - resume counter ${newCount}/3")

                    if (newCount >= StopDetectionState.MAX_CONSECUTIVE_SECONDS) {
                        // 3 consecutive seconds above threshold - end stop
                        android.util.Log.i("StopDetection", "🚀 STOP ENDED! 3 consecutive seconds above threshold - rider moving again")
                        endCurrentStop()
                    } else {
                        state = state.copy(speedAboveThresholdCount = newCount)
                    }
                } else {
                    // Was detecting or moving - reset to moving
                    if (state.speedBelowThresholdCount > 0) {
                        android.util.Log.d("StopDetection", "❌ Detection cancelled: Speed back above threshold (${speedKmh}km/h) before stop confirmed")
                    }
                    state = state.copy(
                        speedBelowThresholdCount = 0,
                        speedAboveThresholdCount = 0,
                        detectionStartTime = null
                    )
                }
            }
        }
    }

    /**
     * Confirm stop and persist to database.
     *
     * Called when:
     * - 3 consecutive seconds below threshold
     * - Duration threshold met
     *
     * @param rideId Current ride ID
     * @param latitude Stop latitude
     * @param longitude Stop longitude
     * @param startTimestamp Stop start timestamp
     */
    private fun confirmStop(
        rideId: Long,
        latitude: Double,
        longitude: Double,
        startTimestamp: Long
    ) {
        if (state.isStopConfirmed) return // Already confirmed

        val stopNumber = state.currentStopNumber
        val confirmationTime = System.currentTimeMillis()

        // Update state immediately (synchronous) with temporary stopId
        // This ensures tests and UI see state change right away
        // IMPORTANT: Set stopConfirmedTime to NOW (not startTimestamp) for accurate UI duration
        state = state.copy(
            isStopConfirmed = true,
            activeStopId = -1L, // Temporary until database insert completes
            speedBelowThresholdCount = 0,
            speedAboveThresholdCount = 0,
            stopConfirmedTime = confirmationTime // Used for UI duration calculation
        )

        // Create stop entity
        val stop = Stop(
            id = 0L, // Will be assigned by database
            rideId = rideId,
            stopNumber = stopNumber,
            latitude = latitude,
            longitude = longitude,
            startTimestamp = startTimestamp,
            endTimestamp = null, // Still active
            clusterId = null // For Feature 010
        )

        // Insert to database (async) and update with real stopId
        scope.launch {
            android.util.Log.i("StopDetection", "💾 Inserting stop #${stopNumber} to database - Location: (${latitude}, ${longitude})")
            val stopId = stopRepository.insertStop(stop)

            // Update state with real database ID
            state = state.copy(activeStopId = stopId)

            android.util.Log.i("StopDetection", "✅ Stop #${stopNumber} persisted with ID: $stopId")

            // Emit event to ViewModel
            _events.emit(StopEvent.StopDetected(stopNumber, stopId))
        }
    }

    /**
     * End current stop and update database.
     *
     * Called when:
     * - 3 consecutive seconds above threshold
     * - Manual pause during active stop
     * - Ride stops
     */
    private fun endCurrentStop() {
        val stopId = state.activeStopId ?: return // No active stop
        val startTime = state.stopConfirmedTime ?: return // Invalid state - use confirmed time, not detection time
        val endTime = System.currentTimeMillis()
        val duration = StopDetectionUtils.calculateDuration(startTime, endTime)

        android.util.Log.i("StopDetection", "📊 Ending stop ID:$stopId - Duration: ${duration}s")

        // Update database (async)
        scope.launch {
            stopRepository.updateStopEnd(stopId, endTime, duration)

            // Increment stop number for next stop
            val nextStopNumber = state.currentStopNumber + 1

            // Reset state to moving
            state = state.copy(
                isStopConfirmed = false,
                activeStopId = null,
                currentStopNumber = nextStopNumber,
                speedBelowThresholdCount = 0,
                speedAboveThresholdCount = 0,
                detectionStartTime = null,
                stopConfirmedTime = null
            )

            // Emit event to ViewModel
            _events.emit(StopEvent.StopEnded(stopId))
        }
    }

    /**
     * Handle manual pause during active stop.
     *
     * Ends the current stop immediately (user action overrides automatic detection).
     */
    fun handleManualPause() {
        if (state.isStopped) {
            endCurrentStop()
        } else {
            // Reset detection state
            state = state.copy(
                speedBelowThresholdCount = 0,
                speedAboveThresholdCount = 0,
                detectionStartTime = null
            )
        }
    }

    /**
     * Get current state (for testing/debugging).
     */
    fun getCurrentState(): StopDetectionState = state
}

/**
 * Events emitted by state machine for ViewModel consumption.
 */
sealed class StopEvent {
    /**
     * Stop detected and persisted to database.
     *
     * @property stopNumber Stop number within current ride (1, 2, 3...)
     * @property stopId Database ID of the stop
     */
    data class StopDetected(val stopNumber: Int, val stopId: Long) : StopEvent()

    /**
     * Stop ended (rider resumed movement).
     *
     * @property stopId Database ID of the stop
     */
    data class StopEnded(val stopId: Long) : StopEvent()
}
