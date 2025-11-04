package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.RideRecordingState
import kotlinx.coroutines.flow.Flow

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
}
