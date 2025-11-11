package com.example.bikeredlights.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.bikeredlights.domain.model.RideRecordingState
import com.example.bikeredlights.domain.repository.RideRecordingStateRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * DataStore extension for recording state preferences.
 */
private val Context.recordingStateDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "recording_state"
)

/**
 * Implementation of RideRecordingStateRepository using DataStore Preferences.
 *
 * **Design Principles**:
 * - In-memory StateFlow for immediate UI updates
 * - DataStore persistence for process death recovery
 * - Thread-safe operations (StateFlow + DataStore)
 * - Singleton to ensure single source of truth
 *
 * **State Persistence**:
 * - Stores current state type (Idle, Recording, Paused, Stopped)
 * - Stores current ride ID
 * - Restores state on app restart
 *
 * **Flow Behavior**:
 * - Hot StateFlow (always has value)
 * - Emits immediately on collect
 * - Updates all collectors on state change
 *
 * @property context Application context for DataStore
 */
@Singleton
class RideRecordingStateRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : RideRecordingStateRepository {

    /**
     * In-memory state for immediate access.
     */
    private val _recordingState = MutableStateFlow<RideRecordingState>(RideRecordingState.Idle)

    /**
     * In-memory current speed for real-time GPS updates.
     *
     * **Design Rationale**:
     * - Ephemeral state (not persisted to DataStore)
     * - Resets to 0.0 on pause/stop
     * - Real-time updates from Service GPS location callbacks
     * - Thread-safe StateFlow for concurrent access
     */
    private val _currentSpeed = MutableStateFlow(0.0)

    /**
     * In-memory current bearing (heading direction) for real-time GPS updates (Feature 007 - v0.6.1).
     *
     * **Design Rationale**:
     * - Ephemeral state (not persisted to DataStore)
     * - Retains last known bearing on pause (doesn't reset like speed)
     * - Resets to null on stop
     * - Real-time updates from Service GPS location callbacks
     * - Thread-safe StateFlow for concurrent access
     */
    private val _currentBearing = MutableStateFlow<Float?>(null)

    /**
     * DataStore preference keys.
     */
    private companion object {
        val STATE_TYPE_KEY = stringPreferencesKey("state_type")
        val RIDE_ID_KEY = longPreferencesKey("ride_id")

        // State type constants
        const val STATE_IDLE = "idle"
        const val STATE_RECORDING = "recording"
        const val STATE_MANUALLY_PAUSED = "manually_paused"
        const val STATE_AUTO_PAUSED = "auto_paused"
        const val STATE_STOPPED = "stopped"
    }

    init {
        // Restore state from DataStore on initialization
        // This will be called when repository is first injected
        CoroutineScope(Dispatchers.IO).launch {
            restoreStateFromDataStore()
        }
    }

    override fun getRecordingState(): Flow<RideRecordingState> {
        return _recordingState.asStateFlow()
    }

    override suspend fun updateRecordingState(state: RideRecordingState) {
        // Update in-memory state
        _recordingState.value = state

        // Persist to DataStore
        context.recordingStateDataStore.edit { preferences ->
            when (state) {
                is RideRecordingState.Idle -> {
                    preferences[STATE_TYPE_KEY] = STATE_IDLE
                    preferences.remove(RIDE_ID_KEY)
                }
                is RideRecordingState.Recording -> {
                    preferences[STATE_TYPE_KEY] = STATE_RECORDING
                    preferences[RIDE_ID_KEY] = state.rideId
                }
                is RideRecordingState.ManuallyPaused -> {
                    preferences[STATE_TYPE_KEY] = STATE_MANUALLY_PAUSED
                    preferences[RIDE_ID_KEY] = state.rideId
                }
                is RideRecordingState.AutoPaused -> {
                    preferences[STATE_TYPE_KEY] = STATE_AUTO_PAUSED
                    preferences[RIDE_ID_KEY] = state.rideId
                }
                is RideRecordingState.Stopped -> {
                    preferences[STATE_TYPE_KEY] = STATE_STOPPED
                    preferences[RIDE_ID_KEY] = state.rideId
                }
            }
        }
    }

    override suspend fun clearRecordingState() {
        // Update in-memory state
        _recordingState.value = RideRecordingState.Idle

        // Clear DataStore
        context.recordingStateDataStore.edit { preferences ->
            preferences.clear()
        }
    }

    override suspend fun getCurrentState(): RideRecordingState {
        return _recordingState.value
    }

    override fun getCurrentSpeed(): StateFlow<Double> {
        return _currentSpeed.asStateFlow()
    }

    override fun getCurrentBearing(): StateFlow<Float?> {
        return _currentBearing.asStateFlow()
    }

    /**
     * Update current speed from GPS location updates.
     *
     * **Internal Method**: Called by RideRecordingService on each GPS update.
     *
     * **Preconditions**:
     * - speedMps must be >= 0.0 (negative speeds are invalid)
     *
     * **Side Effects**:
     * - Updates _currentSpeed StateFlow
     * - Emits new speed value to all collectors
     *
     * @param speedMps Current speed in meters per second
     * @throws IllegalArgumentException if speedMps < 0.0
     */
    internal suspend fun updateCurrentSpeed(speedMps: Double) {
        require(speedMps >= 0.0) { "Speed must be non-negative, got: $speedMps m/s" }
        _currentSpeed.value = speedMps
    }

    /**
     * Reset current speed to 0.0.
     *
     * **Internal Method**: Called by RideRecordingService on pause/stop.
     *
     * **Use Cases**:
     * - Manual pause: Speed should show 0.0 during pause
     * - Auto-pause: Speed should show 0.0 during pause
     * - Stop ride: Speed should reset to 0.0
     * - Discard ride: Speed should reset to 0.0
     *
     * **Side Effects**:
     * - Updates _currentSpeed StateFlow to 0.0
     * - Emits 0.0 to all collectors
     */
    internal suspend fun resetCurrentSpeed() {
        _currentSpeed.value = 0.0
    }

    /**
     * Update current bearing from GPS location updates (Feature 007 - v0.6.1).
     *
     * **Internal Method**: Called by RideRecordingService on each GPS update.
     *
     * **Preconditions**:
     * - bearingDegrees must be in range 0.0-360.0 or null
     * - null indicates bearing is unavailable (stationary, poor GPS signal)
     *
     * **Side Effects**:
     * - Updates _currentBearing StateFlow
     * - Emits new bearing value to all collectors
     *
     * **Note**: Unlike speed, bearing is retained during pause (not reset).
     * This allows map to maintain orientation when rider is temporarily stopped.
     *
     * @param bearingDegrees Current GPS bearing in degrees (0-360 or null)
     * @throws IllegalArgumentException if bearingDegrees is not in valid range
     */
    internal suspend fun updateCurrentBearing(bearingDegrees: Float?) {
        if (bearingDegrees != null) {
            require(bearingDegrees in 0.0f..360.0f) {
                "Bearing must be in range 0-360 degrees, got: $bearingDegrees"
            }
        }
        _currentBearing.value = bearingDegrees
    }

    /**
     * Reset current bearing to null (Feature 007 - v0.6.1).
     *
     * **Internal Method**: Called by RideRecordingService on stop.
     *
     * **Use Cases**:
     * - Stop ride: Bearing should reset to null (north-up fallback)
     * - Discard ride: Bearing should reset to null
     *
     * **Note**: NOT called on pause - bearing is retained during pause.
     *
     * **Side Effects**:
     * - Updates _currentBearing StateFlow to null
     * - Emits null to all collectors
     */
    internal suspend fun resetCurrentBearing() {
        _currentBearing.value = null
    }

    /**
     * Restore state from DataStore on initialization.
     */
    private suspend fun restoreStateFromDataStore() {
        val preferences = context.recordingStateDataStore.data.first()
        val stateType = preferences[STATE_TYPE_KEY] ?: STATE_IDLE
        val rideId = preferences[RIDE_ID_KEY]

        val restoredState = when (stateType) {
            STATE_IDLE -> RideRecordingState.Idle
            STATE_RECORDING -> {
                if (rideId != null) {
                    RideRecordingState.Recording(rideId)
                } else {
                    RideRecordingState.Idle  // Fallback if data corrupted
                }
            }
            STATE_MANUALLY_PAUSED -> {
                if (rideId != null) {
                    RideRecordingState.ManuallyPaused(rideId)
                } else {
                    RideRecordingState.Idle
                }
            }
            STATE_AUTO_PAUSED -> {
                if (rideId != null) {
                    RideRecordingState.AutoPaused(rideId)
                } else {
                    RideRecordingState.Idle
                }
            }
            STATE_STOPPED -> {
                if (rideId != null) {
                    RideRecordingState.Stopped(rideId)
                } else {
                    RideRecordingState.Idle
                }
            }
            else -> RideRecordingState.Idle  // Unknown state
        }

        _recordingState.value = restoredState
    }
}
