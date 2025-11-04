package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.Ride
import com.example.bikeredlights.domain.repository.RideRepository
import javax.inject.Inject

/**
 * Use case for finishing a ride recording session.
 *
 * **Business Logic**:
 * - Sets endTime to current timestamp
 * - Calculates final elapsedDuration
 * - Validates minimum 5-second duration
 * - Updates ride in database
 * - Returns FinishRideResult (Success or TooShort)
 *
 * **Minimum Duration Validation**:
 * - Rides < 5 seconds are considered invalid
 * - Returns TooShort result (caller should auto-discard)
 * - Prevents accidental "tap Start, immediately tap Stop" scenarios
 *
 * **Use Case Flow**:
 * 1. User taps "Stop Ride" button
 * 2. ViewModel calls FinishRideUseCase(rideId)
 * 3. Use case retrieves current ride from database
 * 4. Sets endTime and calculates elapsedDuration
 * 5. Validates duration >= 5 seconds
 * 6. Updates ride in database
 * 7. Returns result to ViewModel
 * 8. ViewModel shows save/discard dialog (if valid) or auto-discards (if too short)
 *
 * **Error Handling**:
 * - Returns RideNotFound if ride doesn't exist
 * - Returns TooShort if duration < 5 seconds
 * - Database errors propagate as exceptions
 *
 * @property rideRepository Repository for ride persistence
 */
class FinishRideUseCase @Inject constructor(
    private val rideRepository: RideRepository
) {
    /**
     * Finish a ride recording session.
     *
     * @param rideId Database ID of the ride to finish
     * @return FinishRideResult indicating success or validation failure
     */
    suspend operator fun invoke(rideId: Long): FinishRideResult {
        // Retrieve current ride
        val ride = rideRepository.getRideById(rideId)
            ?: return FinishRideResult.RideNotFound

        // Set end time
        val endTime = System.currentTimeMillis()

        // Calculate elapsed duration
        val elapsedDuration = endTime - ride.startTime

        // Validate minimum duration (5 seconds = 5000ms)
        if (elapsedDuration < MIN_RIDE_DURATION_MILLIS) {
            return FinishRideResult.TooShort(elapsedDuration)
        }

        // Update ride with end time and elapsed duration
        val finishedRide = ride.copy(
            endTime = endTime,
            elapsedDurationMillis = elapsedDuration
        )

        rideRepository.updateRide(finishedRide)

        return FinishRideResult.Success(finishedRide)
    }

    companion object {
        /**
         * Minimum ride duration in milliseconds (5 seconds).
         *
         * Rides shorter than this are considered invalid and should be discarded.
         */
        const val MIN_RIDE_DURATION_MILLIS = 5000L
    }
}

/**
 * Result of finishing a ride.
 */
sealed class FinishRideResult {
    /**
     * Ride finished successfully.
     *
     * @property ride Finished ride with endTime set
     */
    data class Success(val ride: Ride) : FinishRideResult()

    /**
     * Ride is too short (< 5 seconds).
     *
     * Caller should auto-discard the ride.
     *
     * @property durationMillis Actual duration in milliseconds
     */
    data class TooShort(val durationMillis: Long) : FinishRideResult()

    /**
     * Ride not found in database.
     */
    data object RideNotFound : FinishRideResult()
}
