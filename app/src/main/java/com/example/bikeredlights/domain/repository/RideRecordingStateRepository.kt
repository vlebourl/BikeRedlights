package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.RideRecordingState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Repository interface for managing ride recording state.
 *
 * Provides operations to track the current recording session state
 * across app lifecycle and process death scenarios.
 *
 * **Design Principles**:
 * - Domain layer interface (independent of DataStore)
 * - Flow-based state emissions for reactive UI
 * - In-memory + persistent storage (DataStore)
 * - Single source of truth for recording state
 *
 * **State Management**:
 * - Tracks current ride ID
 * - Tracks recording/paused/stopped states
 * - Tracks manual vs auto-pause
 * - Persists across app restarts
 *
 * **Use Cases**:
 * - Service tracks recording state
 * - UI observes state for button updates
 * - App restart recovery uses persisted state
 */
interface RideRecordingStateRepository {

    /**
     * Get current recording state as a Flow.
     *
     * **Flow Behavior**:
     * - Hot Flow (StateFlow)
     * - Always has a value (defaults to Idle)
     * - Emits on state changes
     *
     * @return Flow emitting RideRecordingState updates
     */
    fun getRecordingState(): Flow<RideRecordingState>

    /**
     * Update the current recording state.
     *
     * **Side Effects**:
     * - Updates in-memory state (StateFlow)
     * - Persists to DataStore for recovery
     * - Emits new state to all collectors
     *
     * @param state New recording state
     */
    suspend fun updateRecordingState(state: RideRecordingState)

    /**
     * Clear recording state (called when ride is finished/discarded).
     *
     * Resets state to Idle and clears DataStore.
     */
    suspend fun clearRecordingState()

    /**
     * Get current recording state as a one-time snapshot.
     *
     * @return Current RideRecordingState
     */
    suspend fun getCurrentState(): RideRecordingState

    /**
     * Get current speed in meters per second as a StateFlow.
     *
     * **Flow Behavior**:
     * - Hot Flow (StateFlow)
     * - Always has a value (defaults to 0.0)
     * - Emits on speed updates from GPS
     * - Resets to 0.0 on pause/stop
     *
     * **Lifecycle**:
     * - 0.0 when no ride is recording
     * - Real-time GPS speed during active recording
     * - 0.0 when ride is paused (manual or auto-pause)
     * - 0.0 when ride is stopped
     *
     * @return StateFlow emitting current speed in m/s (0.0 when not recording)
     */
    fun getCurrentSpeed(): StateFlow<Double>

    /**
     * Get current GPS bearing (heading direction) in degrees as a StateFlow (Feature 007 - v0.6.1).
     *
     * **Flow Behavior**:
     * - Hot Flow (StateFlow)
     * - Always has a value (defaults to null)
     * - Emits on bearing updates from GPS
     * - Retains last known bearing on pause
     * - Resets to null on stop
     *
     * **Lifecycle**:
     * - null when no ride is recording
     * - Real-time GPS bearing (0-360 degrees) during active recording
     * - Retains last known bearing when ride is paused
     * - null when ride is stopped
     * - null if GPS bearing unavailable (stationary, poor signal)
     *
     * **Bearing Values**:
     * - 0° = North
     * - 90° = East
     * - 180° = South
     * - 270° = West
     *
     * @return StateFlow emitting current bearing in degrees (null when unavailable)
     */
    fun getCurrentBearing(): StateFlow<Float?>
}
