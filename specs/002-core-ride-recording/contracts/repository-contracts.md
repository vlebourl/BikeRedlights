# Repository Contracts: Core Ride Recording

**Feature**: F1A - Core Ride Recording
**Date**: 2025-11-04
**Purpose**: Define interfaces and contracts for data layer repositories

---

## Overview

This document specifies the contracts (interfaces) for repositories in the data layer. These contracts define how the domain layer interacts with data sources (Room database, location services) without depending on implementation details.

---

## 1. RideRepository Contract

### Purpose
Provides access to persistent ride data and statistics.

### Interface Definition

```kotlin
interface RideRepository {
    /**
     * Start a new ride with generated name.
     *
     * @param startTime Unix timestamp (milliseconds) when ride started
     * @return Generated ride ID
     * @throws SQLException if database operation fails
     */
    suspend fun startRide(startTime: Long): Long

    /**
     * Finish ride with final statistics.
     *
     * @param rideId Ride to finish
     * @param endTime Unix timestamp when ride ended
     * @param elapsedDurationMillis Total time including pauses
     * @param movingDurationMillis Time excluding all pauses
     * @param manualPausedDurationMillis Time spent manually paused
     * @param autoPausedDurationMillis Time spent auto-paused
     * @param distanceMeters Total distance in meters
     * @param avgSpeedMps Average speed in meters/second
     * @param maxSpeedMps Maximum speed in meters/second
     * @throws IllegalArgumentException if rideId not found
     */
    suspend fun finishRide(
        rideId: Long,
        endTime: Long,
        elapsedDurationMillis: Long,
        movingDurationMillis: Long,
        manualPausedDurationMillis: Long,
        autoPausedDurationMillis: Long,
        distanceMeters: Double,
        avgSpeedMps: Double,
        maxSpeedMps: Double
    )

    /**
     * Get ride by ID.
     *
     * @param rideId Ride identifier
     * @return Ride if found, null otherwise
     */
    suspend fun getRideById(rideId: Long): Ride?

    /**
     * Get all incomplete rides (endTime = null).
     * Used for recovery on app launch.
     *
     * @return List of incomplete rides, empty if none
     */
    suspend fun getIncompleteRides(): List<Ride>

    /**
     * Observe all rides in real-time.
     * For future history screen (F3).
     *
     * @return Flow of all rides, sorted by startTime descending
     */
    fun getAllRidesFlow(): Flow<List<Ride>>

    /**
     * Delete ride and all associated track points (cascade).
     *
     * @param ride Ride to delete
     * @throws IllegalArgumentException if ride doesn't exist
     */
    suspend fun deleteRide(ride: Ride)
}
```

### Error Handling

**Expected Exceptions**:
- `SQLException`: Database full, connection error
- `IllegalArgumentException`: Invalid rideId, constraint violation
- `IllegalStateException`: Attempting to finish already-finished ride

**Repository Responsibility**: Catch SQL exceptions and wrap in domain-specific errors.

---

## 2. TrackPointRepository Contract

### Purpose
Manages GPS track points for rides.

### Interface Definition

```kotlin
interface TrackPointRepository {
    /**
     * Add single track point during recording.
     *
     * @param trackPoint Track point to insert
     * @return Generated track point ID
     * @throws SQLException if database full or other error
     * @throws IllegalArgumentException if accuracy > 50m (invalid per spec FR-022)
     */
    suspend fun addTrackPoint(trackPoint: TrackPoint): Long

    /**
     * Batch insert track points (performance optimization).
     *
     * @param trackPoints List of track points
     * @throws SQLException if database operation fails
     */
    suspend fun addAllTrackPoints(trackPoints: List<TrackPoint>)

    /**
     * Get all track points for a ride.
     *
     * @param rideId Ride identifier
     * @return List of track points ordered by timestamp ascending
     */
    suspend fun getTrackPointsForRide(rideId: Long): List<TrackPoint>

    /**
     * Observe track points for a ride in real-time.
     *
     * @param rideId Ride identifier
     * @return Flow of track points, ordered by timestamp ascending
     */
    fun getTrackPointsForRideFlow(rideId: Long): Flow<List<TrackPoint>>

    /**
     * Get last track point for a ride.
     * Used for distance calculation.
     *
     * @param rideId Ride identifier
     * @return Last track point if exists, null otherwise
     */
    suspend fun getLastTrackPoint(rideId: Long): TrackPoint?

    /**
     * Count track points for a ride.
     * Useful for debugging and statistics.
     *
     * @param rideId Ride identifier
     * @return Number of track points
     */
    suspend fun getTrackPointCount(rideId: Long): Int
}
```

### Validation Rules

Track points must satisfy:
- `latitude in -90.0..90.0`
- `longitude in -180.0..180.0`
- `accuracy <= 50.0` (meters)
- `speedMetersPerSec >= 0.0`
- NOT (`isManuallyPaused` AND `isAutoPaused`)

**Implementation enforces** these rules before database insert.

---

## 3. LocationRepository Contract

### Purpose
Provides access to device GPS location updates.

### Interface Definition

```kotlin
interface LocationRepository {
    /**
     * Observe real-time location updates.
     *
     * @return Flow of locations, emits when new GPS fix received
     */
    fun getLocationUpdatesFlow(): Flow<Location>

    /**
     * Start requesting location updates.
     *
     * @param intervalMillis Update interval (1000ms for High Accuracy, 4000ms for Battery Saver)
     * @throws SecurityException if location permissions not granted
     */
    suspend fun startLocationUpdates(intervalMillis: Long)

    /**
     * Stop requesting location updates.
     * Used during manual pause to save battery.
     */
    suspend fun stopLocationUpdates()

    /**
     * Check if location updates are currently running.
     *
     * @return True if actively receiving updates, false otherwise
     */
    fun isReceivingUpdates(): Boolean

    /**
     * Get last known location (may be stale).
     *
     * @return Last location if available, null otherwise
     * @throws SecurityException if location permissions not granted
     */
    suspend fun getLastKnownLocation(): Location?
}
```

### Location Object Contract

```kotlin
data class Location(
    val latitude: Double,
    val longitude: Double,
    val speed: Float,      // Meters per second
    val accuracy: Float,   // Meters
    val timestamp: Long,   // Unix milliseconds
    val altitude: Double?, // Optional, not used in v0.3.0
    val bearing: Float?    // Optional, not used in v0.3.0
)
```

**Note**: This wraps Android's `android.location.Location` class for domain layer abstraction.

---

## 4. RideRecordingStateRepository Contract

### Purpose
Manages runtime recording state shared between service and ViewModel.

### Interface Definition

```kotlin
interface RideRecordingStateRepository {
    /**
     * Observe current recording state.
     *
     * @return Flow of recording state, emits on any state change
     */
    fun getRecordingStateFlow(): Flow<RideRecordingState?>

    /**
     * Update recording state.
     * Called by service to notify ViewModel of changes.
     *
     * @param state New recording state, null if no active recording
     */
    suspend fun updateRecordingState(state: RideRecordingState?)

    /**
     * Get current recording state (non-reactive).
     *
     * @return Current state, null if no active recording
     */
    suspend fun getCurrentRecordingState(): RideRecordingState?

    /**
     * Clear recording state.
     * Called when ride is stopped/saved.
     */
    suspend fun clearRecordingState()
}
```

### RideRecordingState Data Class

```kotlin
data class RideRecordingState(
    val rideId: Long,
    val startTime: Instant,

    // Manual pause
    val isManuallyPaused: Boolean = false,
    val manualPauseStartTime: Instant? = null,
    val accumulatedManualPausedDuration: Duration = Duration.ZERO,

    // Auto-pause
    val isAutoPaused: Boolean = false,
    val autoPauseStartTime: Instant? = null,
    val accumulatedAutoPausedDuration: Duration = Duration.ZERO,

    // Live stats
    val currentDistanceMeters: Double = 0.0,
    val currentSpeedMetersPerSec: Float = 0f,
    val maxSpeedMetersPerSec: Float = 0f,
    val lastTrackPoint: TrackPoint? = null
) {
    val effectivelyPaused: Boolean get() = isManuallyPaused || isAutoPaused
    val elapsedDuration: Duration get() = Clock.System.now() - startTime
    val movingDuration: Duration get() = elapsedDuration - accumulatedManualPausedDuration - accumulatedAutoPausedDuration
    val avgSpeedMetersPerSec: Float get() = if (movingDuration.inWholeSeconds > 0) {
        (currentDistanceMeters / movingDuration.inWholeSeconds).toFloat()
    } else 0f
}
```

---

## 5. Service Actions Contract

### Purpose
Defines intent actions for controlling RideRecordingService.

### Action Constants

```kotlin
object RideRecordingServiceActions {
    const val ACTION_START_RECORDING = "com.example.bikeredlights.ACTION_START_RECORDING"
    const val ACTION_STOP_RECORDING = "com.example.bikeredlights.ACTION_STOP_RECORDING"
    const val ACTION_PAUSE_RECORDING = "com.example.bikeredlights.ACTION_PAUSE_RECORDING"
    const val ACTION_RESUME_RECORDING = "com.example.bikeredlights.ACTION_RESUME_RECORDING"
}
```

### Intent Extras

```kotlin
object RideRecordingServiceExtras {
    const val EXTRA_RIDE_ID = "extra_ride_id"
    const val EXTRA_START_TIME = "extra_start_time"
}
```

### Usage Example

```kotlin
// Start recording
val intent = Intent(context, RideRecordingService::class.java).apply {
    action = RideRecordingServiceActions.ACTION_START_RECORDING
    putExtra(RideRecordingServiceExtras.EXTRA_START_TIME, System.currentTimeMillis())
}
if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
    context.startForegroundService(intent)
} else {
    context.startService(intent)
}

// Pause recording
val pauseIntent = Intent(context, RideRecordingService::class.java).apply {
    action = RideRecordingServiceActions.ACTION_PAUSE_RECORDING
}
context.startService(pauseIntent)
```

---

## 6. Repository Communication Patterns

### Service → Repository → ViewModel

```
[RideRecordingService]
         ↓ writes
[RideRecordingStateRepository] (StateFlow)
         ↓ observes
[RideRecordingViewModel]
         ↓ exposes
[UI (Composable)]
```

**Example Flow**:
1. Service receives location update from `FusedLocationProviderClient`
2. Service calculates new distance, updates recording state
3. Service calls `rideRecordingStateRepository.updateRecordingState(newState)`
4. Repository emits new state via StateFlow
5. ViewModel's `stateIn()` collects and transforms for UI
6. Composable's `collectAsStateWithLifecycle()` triggers recomposition
7. UI displays updated stats

### ViewModel → Service (Actions)

```
[UI Button Click]
         ↓
[ViewModel.onPauseClick()]
         ↓ creates Intent
[RideRecordingService]
         ↓ executes action
[LocationRepository.stopLocationUpdates()]
```

---

## 7. Error Handling Patterns

### Sealed Result Type

```kotlin
sealed class RepositoryResult<out T> {
    data class Success<T>(val data: T) : RepositoryResult<T>()
    data class Error(val exception: Throwable, val message: String) : RepositoryResult<Nothing>()
}

// Usage in repository
suspend fun startRideSafe(startTime: Long): RepositoryResult<Long> {
    return try {
        val rideId = startRide(startTime)
        RepositoryResult.Success(rideId)
    } catch (e: SQLException) {
        RepositoryResult.Error(e, "Database full - cannot start ride")
    } catch (e: Exception) {
        RepositoryResult.Error(e, "Failed to start ride: ${e.message}")
    }
}

// Usage in ViewModel
viewModelScope.launch {
    when (val result = repository.startRideSafe(startTime)) {
        is RepositoryResult.Success -> handleSuccess(result.data)
        is RepositoryResult.Error -> showError(result.message)
    }
}
```

---

## 8. Threading Contracts

### Repository Methods

**All suspend functions execute on Dispatchers.IO**:
```kotlin
class RideRepositoryImpl(
    private val rideDao: RideDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : RideRepository {

    override suspend fun getRideById(rideId: Long): Ride? = withContext(ioDispatcher) {
        rideDao.getRideById(rideId)
    }
}
```

**Flow emissions observe on caller's dispatcher**:
```kotlin
// ViewModel collects on main thread via viewModelScope
val rides = repository.getAllRidesFlow()
    .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

---

## 9. Testing Contracts

### Test Doubles

```kotlin
// Fake implementation for testing
class FakeRideRepository : RideRepository {
    private val rides = mutableListOf<Ride>()

    override suspend fun startRide(startTime: Long): Long {
        val ride = Ride(id = rides.size + 1L, startTime = startTime, ...)
        rides.add(ride)
        return ride.id
    }

    override suspend fun getRideById(rideId: Long): Ride? {
        return rides.find { it.id == rideId }
    }

    // ... other methods
}

// Usage in ViewModel tests
@Test
fun `startRide creates ride and updates UI state`() = runTest {
    val repository = FakeRideRepository()
    val viewModel = RideRecordingViewModel(repository)

    viewModel.startRide()

    val state = viewModel.uiState.value
    assertThat(state.isRecording).isTrue()
    assertThat(state.currentRideId).isNotNull()
}
```

---

## 10. Contract Guarantees

### RideRepository Guarantees

- **Atomicity**: `finishRide()` updates ride atomically (transaction)
- **Cascade Delete**: Deleting ride auto-deletes all TrackPoints
- **Ordering**: `getAllRidesFlow()` always returns rides sorted by startTime descending
- **Null Safety**: All suspend functions return non-null or throw exceptions (no silent failures)

### LocationRepository Guarantees

- **Lifecycle**: `startLocationUpdates()` must be paired with `stopLocationUpdates()`
- **Permissions**: Throws `SecurityException` if permissions not granted (caller's responsibility to check first)
- **Battery**: Updates stop automatically when app is killed (OS behavior)

### RideRecordingStateRepository Guarantees

- **Single Source of Truth**: Only one recording state at a time
- **Thread-Safe**: All operations safe from multiple coroutines
- **Reactive**: State changes emit immediately to all collectors

---

**Contract Specification Complete**: Ready for implementation and use case development.
