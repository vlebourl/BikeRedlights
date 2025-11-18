# Stop Detection Research: Best Practices & Patterns

**Feature:** BikeRedlights Feature 009 - Stop Detection & Recording
**Date:** November 18, 2025
**Technology Stack:** Android (Kotlin 2.0.21), Room 2.6.1, Jetpack Compose, Hilt, Coroutines

---

## Executive Summary

This research document provides comprehensive guidance for implementing reliable stop detection in the BikeRedlights Android ride tracking application. The feature must detect when a cyclist stops during a ride (speed below threshold for a configurable duration), filter GPS noise effectively, persist stops to Room database, and provide live UI feedback while continuing to work reliably in a foreground service background.

### Critical Findings

1. **GPS Speed Reliability**: `Location.getSpeed()` is unreliable at low speeds (<5 km/h) and requires consecutive measurement filtering
2. **State Machine Pattern**: Sealed classes with `when` expressions provide type-safe state transitions
3. **Consecutive Filtering**: Custom Flow operator using `scan` + `transform` is superior to `debounce` for counting consecutive seconds
4. **Database Migration**: Room foreign key CASCADE requires creating new table + data copy (SQLite limitation)
5. **Foreground Service**: Stop detection state should live in Service, not ViewModel, for reliable long-stop handling
6. **Memory Management**: Service-based architecture prevents ViewModel memory leaks during long rides
7. **Testing Strategy**: Mock location providers with synthetic Flow streams for deterministic tests

---

## Question 1: GPS Speed Accuracy for Stop Detection

### Decision

**Use `Location.getSpeed()` with consecutive measurement filtering + Activity Recognition validation for reliability in the 0-5 km/h range.**

### Rationale

#### GPS Speed Reliability Issues

Based on research and Android developer community findings:

- **Low Speed Unreliability**: `Location.getSpeed()` is massively unreliable at low speeds, especially when GPS coverage is not optimal. Devices can report ridiculously high speeds (200+ mph) even when stationary.
- **Zero Speed Issues**: Some devices (particularly Android 6.0.1+) always return `getSpeed() = 0.0` even when `hasSpeed() = true` and the device is moving.
- **Accuracy Dependency**: Speed accuracy correlates with location accuracy. Below 15m accuracy, speed values become increasingly unreliable.

#### Best Practices from Research

1. **High Accuracy + Fast Updates**: `getSpeed()` works best with:
   - `PRIORITY_HIGH_ACCURACY` location request
   - 1-second update interval (not 3-5 seconds)
   - Current BikeRedlights config: 1000ms interval ✅

2. **Reality Check with Activity Recognition**: Google Play Services recommendation:
   ```
   if (DetectedActivity == STILL) → true speed is probably 0
   if (DetectedActivity == ON_FOOT) → low speed (~1 m/s)
   if (DetectedActivity == ON_BICYCLE) → trust Location.getSpeed()
   ```

3. **Consecutive Measurement Filtering**: Never trust a single GPS reading for stop detection. Require multiple consecutive measurements below threshold.

#### Recommended Stop Detection Strategy

```kotlin
// Threshold Configuration (from Feature 008 settings)
val speedThresholdMps = 2.0  // ~7.2 km/h (configurable)
val consecutiveSecondsRequired = 3  // Configurable in settings

// Detection Logic
fun isStopConfirmed(
    speedHistory: List<Double>,  // Last N speed readings
    accuracyHistory: List<Float>  // Corresponding accuracies
): Boolean {
    // Require 3+ consecutive readings
    if (speedHistory.size < consecutiveSecondsRequired) return false

    // All readings must be below threshold
    val allBelowThreshold = speedHistory.all { it < speedThresholdMps }

    // All readings must have good accuracy (<= 25m for stop detection)
    val allGoodAccuracy = accuracyHistory.all { it <= 25.0f }

    return allBelowThreshold && allGoodAccuracy
}
```

### Code Pattern

```kotlin
// LocationData already includes speed from FusedLocationProvider
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,        // meters/second from Location.getSpeed()
    val accuracy: Float,      // meters
    val timestamp: Long,      // milliseconds
    val hasSpeed: Boolean     // Location.hasSpeed()
)

// In StopDetectionUseCase
class DetectStopsUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<StopDetectionState> {
        return combine(
            locationRepository.getLocationUpdates(),
            settingsRepository.stopDetectionSettings
        ) { location, settings ->
            // Filter unreliable speed readings
            if (!location.hasSpeed || location.accuracy > 25f) {
                return@combine StopDetectionState.InsufficientData
            }

            // Speed in m/s, threshold configurable via settings
            StopDetectionReading(
                speed = location.speed,
                accuracy = location.accuracy,
                timestamp = location.timestamp,
                threshold = settings.stopSpeedThresholdMps
            )
        }
        .consecutiveBelow(
            threshold = { reading -> reading.threshold },
            duration = { settingsRepository.stopDetectionSettings.first().consecutiveSeconds }
        )
    }
}
```

### Alternatives Considered

**Alternative 1: Calculate Speed from Position Delta**
```kotlin
// REJECTED: Higher computational cost, no better accuracy
fun calculateSpeed(prev: Location, current: Location): Double {
    val distance = prev.distanceTo(current)
    val timeDelta = (current.time - prev.time) / 1000.0
    return distance / timeDelta
}
```
- **Why Rejected**: Research shows this approach is no more accurate than `Location.getSpeed()` at low speeds and adds complexity. Android's FusedLocationProvider already uses sophisticated algorithms internally.

**Alternative 2: Use Only Activity Recognition**
```kotlin
// REJECTED: Activity Recognition alone has significant latency
DetectedActivity.STILL → Assume stopped
```
- **Why Rejected**: Activity Recognition updates have 2-5 second latency and don't provide precise stop timing. Better as validation layer, not primary detection.

**Alternative 3: Single Threshold Check**
```kotlin
// REJECTED: Too noisy, false positives at traffic lights
if (location.speed < threshold) → Stopped
```
- **Why Rejected**: GPS noise causes speed to fluctuate. Research shows single readings are unreliable. Consecutive filtering is essential.

### Android Gotchas

1. **FusedLocationProvider Latency**: Even with 1-second interval, there's typically 100-300ms latency between actual movement and speed update emission.

2. **Device Variability**: Some manufacturers (Huawei, Xiaomi) aggressively throttle GPS in background. Foreground service notification is mandatory.

3. **Speed Accuracy Field Missing**: `Location` class has no `getSpeedAccuracy()` method (unlike `getAccuracy()` for position). Must validate via position accuracy as proxy.

4. **Zero Speed When Stationary**: When device is completely still, `hasSpeed()` may return `false` instead of reporting `speed = 0.0`. Handle this case:
   ```kotlin
   val effectiveSpeed = if (location.hasSpeed) location.speed else {
       // Assume stationary if no speed but high accuracy
       if (location.accuracy < 20f) 0.0 else null
   }
   ```

5. **Battery Saver Mode**: Android 9+ battery saver mode can reduce GPS update frequency even with foreground service. Document this limitation.

---

## Question 2: State Machine Pattern for Stop Detection

### Decision

**Implement a sealed class-based state machine with `when` expressions for type-safe state transitions. Place state machine logic in a dedicated `StopDetectionStateMachine` class (domain layer), not directly in UseCase or Service.**

### Rationale

#### Why Sealed Classes?

Kotlin sealed classes provide compile-time exhaustive checking for state transitions:

```kotlin
sealed class StopState {
    data object Moving : StopState()
    data class Detecting(
        val consecutiveSecondsBelow: Int,
        val threshold: Double
    ) : StopState()
    data class Confirmed(
        val stopStartTime: Long,
        val location: LocationData
    ) : StopState()
    data class Ended(
        val stopDurationMillis: Long
    ) : StopState()
}
```

**Benefits:**
- Compiler enforces exhaustive `when` handling (missing state = compile error)
- Clear data associated with each state (type-safe)
- Easy to extend with new states without breaking existing code
- Works perfectly with Kotlin Flow emissions

#### State Machine Architecture

**Separation of Concerns:**

```
┌──────────────────────────────────────────────────┐
│ Service Layer (RideRecordingService)            │
│ - Owns state machine instance                   │
│ - Drives state machine with location updates    │
│ - Persists stops to database on confirm/end     │
└─────────────────┬────────────────────────────────┘
                  │
┌─────────────────▼────────────────────────────────┐
│ Domain Layer (StopDetectionStateMachine)        │
│ - Pure Kotlin class (no Android deps)           │
│ - Handles state transitions                     │
│ - Validates transition rules                    │
│ - Emits new states immutably                    │
└─────────────────┬────────────────────────────────┘
                  │
┌─────────────────▼────────────────────────────────┐
│ Domain Layer (DetectStopsUseCase)               │
│ - Provides Flow of StopDetectionState           │
│ - Combines location + settings                  │
│ - Filters unreliable GPS readings               │
└──────────────────────────────────────────────────┘
```

### Code Pattern

```kotlin
// domain/model/StopDetectionState.kt
sealed class StopDetectionState {
    /**
     * Normal riding state - speed above threshold
     */
    data object Moving : StopDetectionState()

    /**
     * Potential stop detected - counting consecutive seconds below threshold
     * @param consecutiveCount Number of consecutive seconds below threshold (1-based)
     * @param requiredCount Total seconds needed to confirm stop (e.g., 3)
     */
    data class Detecting(
        val consecutiveCount: Int,
        val requiredCount: Int,
        val thresholdMps: Double
    ) : StopDetectionState()

    /**
     * Stop confirmed - cyclist has been stopped for required duration
     * @param stopStartTime Timestamp when stop was first detected (first reading below threshold)
     * @param currentDuration Current duration of stop in milliseconds
     * @param location Location where stop occurred
     */
    data class Confirmed(
        val stopStartTime: Long,
        val currentDuration: Long,
        val location: LocationData
    ) : StopDetectionState()

    /**
     * Insufficient GPS data for reliable detection
     */
    data object InsufficientData : StopDetectionState()
}

// domain/StopDetectionStateMachine.kt
class StopDetectionStateMachine(
    private val consecutiveSecondsRequired: Int = 3
) {
    private var currentState: StopState = StopState.Moving

    sealed class StopState {
        data object Moving : StopState()
        data class Detecting(
            val startTime: Long,
            val consecutiveCount: Int
        ) : StopState()
        data class Confirmed(
            val stopStartTime: Long,
            val location: LocationData
        ) : StopState()
    }

    sealed class StopEvent {
        data class SpeedBelowThreshold(
            val location: LocationData,
            val threshold: Double
        ) : StopEvent()
        data class SpeedAboveThreshold(
            val location: LocationData
        ) : StopEvent()
        data object InsufficientData : StopEvent()
    }

    fun transition(event: StopEvent): StopDetectionState {
        currentState = when (currentState) {
            is StopState.Moving -> handleMovingState(event)
            is StopState.Detecting -> handleDetectingState(event)
            is StopState.Confirmed -> handleConfirmedState(event)
        }

        return mapToExternalState(currentState)
    }

    private fun handleMovingState(event: StopEvent): StopState {
        return when (event) {
            is StopEvent.SpeedBelowThreshold -> {
                StopState.Detecting(
                    startTime = event.location.timestamp,
                    consecutiveCount = 1
                )
            }
            is StopEvent.SpeedAboveThreshold -> StopState.Moving
            is StopEvent.InsufficientData -> StopState.Moving
        }
    }

    private fun handleDetectingState(event: StopEvent): StopState {
        val detecting = currentState as StopState.Detecting

        return when (event) {
            is StopEvent.SpeedBelowThreshold -> {
                val newCount = detecting.consecutiveCount + 1
                if (newCount >= consecutiveSecondsRequired) {
                    // Transition to confirmed
                    StopState.Confirmed(
                        stopStartTime = detecting.startTime,
                        location = event.location
                    )
                } else {
                    // Still detecting
                    detecting.copy(consecutiveCount = newCount)
                }
            }
            is StopEvent.SpeedAboveThreshold -> {
                // Reset to moving - not enough consecutive readings
                StopState.Moving
            }
            is StopEvent.InsufficientData -> {
                // Keep current detecting state - don't reset on single bad reading
                detecting
            }
        }
    }

    private fun handleConfirmedState(event: StopEvent): StopState {
        val confirmed = currentState as StopState.Confirmed

        return when (event) {
            is StopEvent.SpeedBelowThreshold -> {
                // Remain confirmed, update location if needed
                confirmed
            }
            is StopEvent.SpeedAboveThreshold -> {
                // Stop ended - transition to moving
                // Caller should persist stop before this transition
                StopState.Moving
            }
            is StopEvent.InsufficientData -> {
                // Remain confirmed - don't end stop on bad GPS reading
                confirmed
            }
        }
    }

    private fun mapToExternalState(internalState: StopState): StopDetectionState {
        return when (internalState) {
            is StopState.Moving -> StopDetectionState.Moving
            is StopState.Detecting -> StopDetectionState.Detecting(
                consecutiveCount = internalState.consecutiveCount,
                requiredCount = consecutiveSecondsRequired,
                thresholdMps = 2.0  // Get from settings
            )
            is StopState.Confirmed -> StopDetectionState.Confirmed(
                stopStartTime = internalState.stopStartTime,
                currentDuration = System.currentTimeMillis() - internalState.stopStartTime,
                location = internalState.location
            )
        }
    }

    fun getCurrentState(): StopDetectionState = mapToExternalState(currentState)

    fun reset() {
        currentState = StopState.Moving
    }
}
```

### Alternatives Considered

**Alternative 1: Tinder StateMachine Library**
```kotlin
// REJECTED: Overkill for simple 3-state machine
val stateMachine = StateMachine.create<State, Event, SideEffect> {
    initialState(State.Moving)
    state<State.Moving> { ... }
}
```
- **Why Rejected**: Adds external dependency for simple logic. Sealed classes + `when` is sufficient and more explicit.

**Alternative 2: Enum-Based State Machine**
```kotlin
// REJECTED: Can't associate data with states
enum class StopState { MOVING, DETECTING, CONFIRMED }
```
- **Why Rejected**: Enums can't carry state-specific data (e.g., `consecutiveCount` in Detecting). Would require separate data holders.

**Alternative 3: State Machine in UseCase Directly**
```kotlin
// REJECTED: Violates Single Responsibility Principle
class DetectStopsUseCase {
    private var state: StopState = Moving
    private var consecutiveCount = 0

    operator fun invoke(): Flow<...> {
        // Complex state logic mixed with Flow operations
    }
}
```
- **Why Rejected**: Makes UseCase harder to test. State machine logic should be isolated for unit testing without Flow/coroutine complexity.

### Android Gotchas

1. **State Persistence Across Process Death**: If foreground service is killed by system, state machine state is lost. Must persist critical state (current stop start time) to DataStore.

2. **Race Conditions**: If multiple coroutines call `transition()` concurrently, state could become inconsistent. Solution: Make state machine a `@Singleton` in Hilt and ensure single-threaded access.

3. **Memory Leaks**: State machine holding `Location` objects indefinitely. Solution: Only store essential data (timestamp, lat/lng, speed) in states, not entire `Location` instances.

4. **Configuration Changes**: ViewModel recreation doesn't affect state machine if it lives in Service. Good design.

---

## Question 3: Consecutive Seconds Filtering with Kotlin Flows

### Decision

**Implement a custom Flow operator `consecutiveBelow()` using `scan` operator for accumulation + `distinctUntilChanged` for emission control. Do NOT use `debounce` or `buffer` for this use case.**

### Rationale

#### Why NOT `debounce`?

`debounce` waits for a period of *silence* before emitting:

```kotlin
// WRONG APPROACH
flow.debounce(3000)  // Waits 3s of no emissions before emitting last value
```

**Problem**: GPS updates arrive every second. There's no 3-second silence period. `debounce` would never emit or would only emit when location updates stop (wrong behavior).

#### Why NOT `buffer`?

`buffer` controls backpressure, not value filtering:

```kotlin
// WRONG APPROACH
flow.buffer(capacity = 3)  // Just buffering, not filtering consecutive values
```

**Problem**: Buffer stores emissions to prevent slow collectors from blocking fast producers. Doesn't implement "count consecutive values below threshold" logic.

#### Why `scan` + Custom Logic?

The `scan` operator maintains accumulator state across emissions:

```kotlin
data class ConsecutiveState(
    val count: Int = 0,
    val allBelow: Boolean = false
)

flow.scan(ConsecutiveState()) { state, reading ->
    if (reading.speed < threshold) {
        ConsecutiveState(
            count = state.count + 1,
            allBelow = true
        )
    } else {
        ConsecutiveState(count = 0, allBelow = false)
    }
}
```

**Benefits:**
- Explicitly tracks consecutive count
- Emits every intermediate state (useful for UI progress)
- Easy to test (pure function)
- No timing dependencies (unlike debounce)

### Code Pattern

```kotlin
// domain/util/FlowExtensions.kt

/**
 * Custom Flow operator that detects when values remain below a threshold
 * for a specified number of consecutive emissions.
 *
 * @param threshold Function to extract threshold value for comparison
 * @param consecutiveRequired Number of consecutive emissions below threshold needed
 * @param getValue Function to extract value to compare against threshold
 * @return Flow that emits detection state changes
 */
fun <T> Flow<T>.consecutiveBelow(
    threshold: suspend (T) -> Double,
    consecutiveRequired: Int,
    getValue: suspend (T) -> Double
): Flow<ConsecutiveDetectionState<T>> = flow {
    data class AccumulatorState(
        val count: Int = 0,
        val firstValue: T? = null,
        val lastValue: T? = null
    )

    this@consecutiveBelow
        .scan(AccumulatorState()) { acc, value ->
            val currentThreshold = threshold(value)
            val currentValue = getValue(value)

            if (currentValue < currentThreshold) {
                // Below threshold - increment or start counting
                AccumulatorState(
                    count = acc.count + 1,
                    firstValue = acc.firstValue ?: value,
                    lastValue = value
                )
            } else {
                // Above threshold - reset
                AccumulatorState(count = 0, firstValue = null, lastValue = null)
            }
        }
        .distinctUntilChangedBy { it.count >= consecutiveRequired }
        .collect { acc ->
            if (acc.count > 0 && acc.count < consecutiveRequired) {
                emit(ConsecutiveDetectionState.Detecting(
                    count = acc.count,
                    required = consecutiveRequired,
                    firstValue = acc.firstValue!!
                ))
            } else if (acc.count >= consecutiveRequired) {
                emit(ConsecutiveDetectionState.Confirmed(
                    firstValue = acc.firstValue!!,
                    lastValue = acc.lastValue!!
                ))
            } else {
                emit(ConsecutiveDetectionState.AboveThreshold)
            }
        }
}

sealed class ConsecutiveDetectionState<T> {
    class Detecting<T>(
        val count: Int,
        val required: Int,
        val firstValue: T
    ) : ConsecutiveDetectionState<T>()

    class Confirmed<T>(
        val firstValue: T,
        val lastValue: T
    ) : ConsecutiveDetectionState<T>()

    class AboveThreshold<T> : ConsecutiveDetectionState<T>()
}

// Usage in DetectStopsUseCase
class DetectStopsUseCase @Inject constructor(
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository
) {
    operator fun invoke(): Flow<StopDetectionState> {
        return locationRepository.getLocationUpdates()
            .filter { it.hasSpeed && it.accuracy <= 25f }
            .consecutiveBelow(
                threshold = { settingsRepository.getStopSpeedThresholdMps() },
                consecutiveRequired = 3,
                getValue = { it.speed }
            )
            .map { detectionState ->
                when (detectionState) {
                    is ConsecutiveDetectionState.Detecting ->
                        StopDetectionState.Detecting(
                            consecutiveCount = detectionState.count,
                            requiredCount = detectionState.required,
                            thresholdMps = settingsRepository.getStopSpeedThresholdMps()
                        )
                    is ConsecutiveDetectionState.Confirmed ->
                        StopDetectionState.Confirmed(
                            stopStartTime = detectionState.firstValue.timestamp,
                            currentDuration = 0L,
                            location = detectionState.firstValue
                        )
                    is ConsecutiveDetectionState.AboveThreshold ->
                        StopDetectionState.Moving
                }
            }
    }
}
```

### Alternatives Considered

**Alternative 1: Sliding Window with `windowed()`**
```kotlin
// REJECTED: Requires collecting all values in memory
flow.windowed(size = 3, step = 1) { window ->
    window.all { it.speed < threshold }
}
```
- **Why Rejected**: `windowed()` is for Sequences, not Flows. Would need to convert Flow→List→Sequence, losing streaming benefits.

**Alternative 2: StateFlow-based Accumulator**
```kotlin
// REJECTED: More complex than scan, no benefits
val consecutiveCount = MutableStateFlow(0)
flow.onEach { reading ->
    if (reading.speed < threshold) {
        consecutiveCount.update { it + 1 }
    } else {
        consecutiveCount.value = 0
    }
}
```
- **Why Rejected**: Requires managing separate StateFlow. `scan` accomplishes same with functional approach.

**Alternative 3: Manual State in collect { }**
```kotlin
// REJECTED: Imperative, not testable as operator
var count = 0
flow.collect { reading ->
    if (reading.speed < threshold) {
        count++
        if (count >= 3) emit(Confirmed)
    } else {
        count = 0
    }
}
```
- **Why Rejected**: Can't chain with other operators. Hard to test. Not reusable.

### Android Gotchas

1. **Flow Cancellation**: If Flow is cancelled mid-detection (app backgrounds), consecutive count is lost. Must persist state if detection should resume.

2. **Hot vs Cold Flows**: `scan` on a cold Flow creates new accumulator per collector. If multiple collectors exist, each gets independent state (usually desired).

3. **Backpressure**: If database writes are slow (persisting stops), use `buffer()` *after* `consecutiveBelow()` to prevent blocking upstream location updates.

4. **Timestamp Precision**: GPS timestamps are milliseconds. For "consecutive seconds" counting, verify timestamps are actually ~1000ms apart:
   ```kotlin
   .scan(State()) { state, reading ->
       val timeDelta = reading.timestamp - (state.lastTimestamp ?: 0)
       if (timeDelta < 500 || timeDelta > 2000) {
           // GPS hiccup - ignore this reading
           state
       } else {
           // Process normally
       }
   }
   ```

---

## Question 4: Room Database Migration with Foreign Key CASCADE

### Decision

**Use Room Migration with manual table recreation strategy. Bump database version, create new `stops` table with foreign key CASCADE to `rides` table, and implement migration class. Do NOT use AutoMigration due to foreign key constraints.**

### Rationale

#### SQLite Foreign Key Limitation

SQLite does not support `ALTER TABLE` to add or modify foreign key constraints. Only these operations are supported:
- `RENAME TABLE`
- `ADD COLUMN` (with restrictions)
- `RENAME COLUMN` (SQLite 3.25.0+)

To add a table with foreign key to existing database, you must:
1. Create new table with foreign key definition
2. No data migration needed (new table starts empty)
3. Update database version number

#### Room Migration Strategy

```kotlin
// data/local/BikeRedlightsDatabase.kt
@Database(
    entities = [
        Ride::class,
        TrackPoint::class,
        Stop::class  // NEW
    ],
    version = 5,  // Increment from 4 → 5
    exportSchema = true
)
abstract class BikeRedlightsDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
    abstract fun trackPointDao(): TrackPointDao
    abstract fun stopDao(): StopDao  // NEW
}

// Migration 4 → 5: Add stops table
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Create stops table with foreign key CASCADE
        db.execSQL("""
            CREATE TABLE IF NOT EXISTS stops (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                ride_id INTEGER NOT NULL,
                start_time INTEGER NOT NULL,
                end_time INTEGER,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                FOREIGN KEY(ride_id) REFERENCES rides(id) ON DELETE CASCADE
            )
        """)

        // Create index on foreign key (Room requirement for performance)
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_stops_ride_id
            ON stops(ride_id)
        """)

        // Create index on start_time for query performance
        db.execSQL("""
            CREATE INDEX IF NOT EXISTS idx_stops_start_time
            ON stops(start_time)
        """)
    }
}

// In Hilt module
@Provides
@Singleton
fun provideDatabase(@ApplicationContext context: Context): BikeRedlightsDatabase {
    return Room.databaseBuilder(
        context,
        BikeRedlightsDatabase::class,
        "bikeredlights_db"
    )
    .addMigrations(MIGRATION_4_5)  // Add migration
    .build()
}
```

### Code Pattern

```kotlin
// data/local/entity/Stop.kt
@Entity(
    tableName = "stops",
    foreignKeys = [
        ForeignKey(
            entity = Ride::class,
            parentColumns = ["id"],
            childColumns = ["ride_id"],
            onDelete = ForeignKey.CASCADE  // Auto-delete stops when ride deleted
        )
    ],
    indices = [
        Index(value = ["ride_id"], name = "idx_stops_ride_id"),  // Required by Room
        Index(value = ["start_time"], name = "idx_stops_start_time")  // Query performance
    ]
)
data class Stop(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ride_id")
    val rideId: Long,

    @ColumnInfo(name = "start_time")
    val startTime: Long,  // Unix timestamp in milliseconds

    @ColumnInfo(name = "end_time")
    val endTime: Long?,   // Nullable for ongoing stops

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double
)

// data/local/dao/StopDao.kt
@Dao
interface StopDao {
    @Insert
    suspend fun insertStop(stop: Stop): Long

    @Update
    suspend fun updateStop(stop: Stop)

    @Query("SELECT * FROM stops WHERE ride_id = :rideId ORDER BY start_time ASC")
    fun getStopsForRide(rideId: Long): Flow<List<Stop>>

    @Query("SELECT * FROM stops WHERE ride_id = :rideId AND end_time IS NULL LIMIT 1")
    suspend fun getOngoingStop(rideId: Long): Stop?

    @Delete
    suspend fun deleteStop(stop: Stop)
}
```

#### Testing the Migration

```kotlin
// androidTest/data/local/MigrationTest.kt
@RunWith(AndroidJUnit4::class)
class MigrationTest {
    private lateinit var migrationTestHelper: MigrationTestHelper

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        BikeRedlightsDatabase::class.java,
        AutoMigrationSpec::class.java
    )

    @Test
    fun migrate4To5() {
        // Create database at version 4
        val db = helper.createDatabase(TEST_DB, 4).apply {
            // Insert test ride
            execSQL("""
                INSERT INTO rides (id, name, start_time, end_time,
                    elapsed_duration_millis, moving_duration_millis,
                    manual_paused_duration_millis, auto_paused_duration_millis,
                    distance_meters, avg_speed_mps, max_speed_mps)
                VALUES (1, 'Test Ride', 1700000000000, NULL,
                    0, 0, 0, 0, 0.0, 0.0, 0.0)
            """)
            close()
        }

        // Run migration to version 5
        val migratedDb = helper.runMigrationsAndValidate(
            TEST_DB,
            5,
            true,
            MIGRATION_4_5
        )

        // Verify stops table exists
        val cursor = migratedDb.query("SELECT * FROM stops")
        assertThat(cursor.count).isEqualTo(0)
        cursor.close()

        // Verify foreign key constraint
        migratedDb.execSQL("""
            INSERT INTO stops (ride_id, start_time, latitude, longitude)
            VALUES (1, 1700000000000, 37.7749, -122.4194)
        """)

        // Delete parent ride - should CASCADE delete stop
        migratedDb.execSQL("DELETE FROM rides WHERE id = 1")

        val stopsAfterDelete = migratedDb.query("SELECT * FROM stops")
        assertThat(stopsAfterDelete.count).isEqualTo(0)  // Stop deleted via CASCADE
        stopsAfterDelete.close()
    }

    companion object {
        private const val TEST_DB = "migration-test"
    }
}
```

### Alternatives Considered

**Alternative 1: AutoMigration**
```kotlin
// REJECTED: AutoMigration cannot infer foreign key constraints
@Database(
    version = 5,
    autoMigrations = [AutoMigration(from = 4, to = 5)]
)
```
- **Why Rejected**: Room's AutoMigration requires `@AutoMigrationSpec` for foreign keys, which is more complex than manual migration for this simple case.

**Alternative 2: Destructive Migration**
```kotlin
// REJECTED: Loses user ride data
.fallbackToDestructiveMigration()
```
- **Why Rejected**: Unacceptable for production app with user data. Users would lose all saved rides.

**Alternative 3: Recreate Entire Database Schema**
```kotlin
// REJECTED: Unnecessary complexity, risk of data loss
// Drop rides table, recreate with stops table
```
- **Why Rejected**: Existing `rides` and `track_points` tables don't need changes. Creating new table is sufficient.

### Android Gotchas

1. **Identity Hash Mismatch**: Room generates identity hash for each schema version. Must increment version number even if using `fallbackToDestructiveMigration()`, or app will crash with `IllegalStateException`.

2. **Missing Index Warning**: Room prints `MISSING_INDEX_ON_FOREIGN_KEY_CHILD` warning if foreign key column lacks index. Always create index on foreign key columns:
   ```kotlin
   indices = [Index(value = ["ride_id"])]
   ```

3. **Foreign Key Enforcement**: SQLite foreign key constraints are disabled by default. Room enables them automatically, but if accessing database directly via `SupportSQLiteDatabase`, must enable:
   ```kotlin
   db.execSQL("PRAGMA foreign_keys=ON")
   ```

4. **Cascade Delete in Tests**: When testing CASCADE behavior, ensure foreign key enforcement is enabled. Room's test helpers may not enable it automatically.

5. **Schema Export**: Always export schema to `app/schemas/` directory for version control and migration testing:
   ```kotlin
   room {
       schemaDirectory("$projectDir/schemas")
   }
   ```

6. **Version Bump Conflicts**: If multiple features add migrations simultaneously, coordinate version numbers to avoid conflicts (e.g., feature branches should use temporary high version numbers, then normalize on merge).

---

## Question 5: Foreground Service Integration for Stop Detection

### Decision

**Stop detection logic MUST run in `RideRecordingService` (existing foreground service), NOT in ViewModel. State machine instance lives in Service with lifecycle tied to ride recording session. Use Service → ViewModel → UI Flow for state propagation.**

### Rationale

#### Why Service, Not ViewModel?

**Requirement**: Stop detection must continue reliably during long stops (30+ minutes) even when:
- App is in background
- Screen is off
- Device is in Doze mode
- Activity/ViewModel is destroyed

**ViewModel Limitations:**
- ViewModel lifecycle tied to Activity/Fragment (destroyed on process death)
- Coroutines in `viewModelScope` cancelled when ViewModel cleared
- Cannot survive long background periods

**Foreground Service Benefits:**
- Runs independently of UI lifecycle
- Protected from aggressive battery optimization (with notification)
- Survives Doze mode (foreground service exemption)
- Already exists in BikeRedlights for ride recording

#### Service Architecture

```
┌─────────────────────────────────────────────────┐
│ RideRecordingService (Foreground)               │
│ ├─ LocationRepository.getLocationUpdates()      │
│ ├─ StopDetectionStateMachine (instance)         │
│ ├─ StopRepository (persist to Room)             │
│ └─ _stopDetectionState: MutableStateFlow        │
└──────────────┬──────────────────────────────────┘
               │ Exposes StateFlow
┌──────────────▼──────────────────────────────────┐
│ RideRecordingViewModel                          │
│ └─ stopDetectionState: StateFlow (from Service) │
└──────────────┬──────────────────────────────────┘
               │ Collects in Composable
┌──────────────▼──────────────────────────────────┐
│ RideRecordingScreen (@Composable)               │
│ └─ Display stop detection UI                    │
└──────────────────────────────────────────────────┘
```

### Code Pattern

```kotlin
// service/RideRecordingService.kt
@HiltAndroidService
class RideRecordingService : Service() {

    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var rideRepository: RideRepository
    @Inject lateinit var stopRepository: StopRepository
    @Inject lateinit var settingsRepository: SettingsRepository

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val stateMachine = StopDetectionStateMachine(consecutiveSecondsRequired = 3)

    private val _stopDetectionState = MutableStateFlow<StopDetectionState>(
        StopDetectionState.Moving
    )
    val stopDetectionState: StateFlow<StopDetectionState> = _stopDetectionState.asStateFlow()

    private var currentRideId: Long? = null
    private var currentStopId: Long? = null

    override fun onCreate() {
        super.onCreate()

        // Start location tracking + stop detection
        serviceScope.launch {
            locationRepository.getLocationUpdates()
                .filter { it.hasSpeed && it.accuracy <= 25f }
                .collect { location ->
                    val settings = settingsRepository.stopDetectionSettings.first()

                    // Determine event
                    val event = when {
                        !location.hasSpeed -> StopDetectionStateMachine.StopEvent.InsufficientData
                        location.speed < settings.stopSpeedThresholdMps ->
                            StopDetectionStateMachine.StopEvent.SpeedBelowThreshold(
                                location = location,
                                threshold = settings.stopSpeedThresholdMps
                            )
                        else -> StopDetectionStateMachine.StopEvent.SpeedAboveThreshold(location)
                    }

                    // Transition state machine
                    val newState = stateMachine.transition(event)

                    // Handle state-specific logic
                    handleStopDetectionState(newState, location)

                    // Emit to UI
                    _stopDetectionState.value = newState
                }
        }
    }

    private suspend fun handleStopDetectionState(
        state: StopDetectionState,
        location: LocationData
    ) {
        when (state) {
            is StopDetectionState.Moving -> {
                // End current stop if exists
                currentStopId?.let { stopId ->
                    val stop = stopRepository.getStopById(stopId)
                    if (stop != null && stop.endTime == null) {
                        stopRepository.updateStop(
                            stop.copy(endTime = System.currentTimeMillis())
                        )
                        currentStopId = null
                    }
                }
            }

            is StopDetectionState.Confirmed -> {
                // Create new stop in database (once)
                if (currentStopId == null) {
                    val rideId = currentRideId ?: return

                    val stop = Stop(
                        rideId = rideId,
                        startTime = state.stopStartTime,
                        endTime = null,  // Ongoing
                        latitude = state.location.latitude,
                        longitude = state.location.longitude
                    )

                    currentStopId = stopRepository.insertStop(stop)
                }
            }

            is StopDetectionState.Detecting -> {
                // No database action during detection
            }

            is StopDetectionState.InsufficientData -> {
                // No database action
            }
        }
    }

    override fun onDestroy() {
        serviceScope.cancel()
        super.onDestroy()
    }

    // Binder for ViewModel to access service state
    inner class LocalBinder : Binder() {
        fun getService(): RideRecordingService = this@RideRecordingService
    }

    override fun onBind(intent: Intent?): IBinder = LocalBinder()
}

// ui/screens/recording/RideRecordingViewModel.kt
@HiltViewModel
class RideRecordingViewModel @Inject constructor(
    // No stop detection logic here - all in Service
) : ViewModel() {

    // Bound service instance
    private var service: RideRecordingService? = null

    val stopDetectionState: StateFlow<StopDetectionState>
        get() = service?.stopDetectionState ?: MutableStateFlow(StopDetectionState.Moving)

    fun bindService(service: RideRecordingService) {
        this.service = service
    }

    fun unbindService() {
        this.service = null
    }
}

// ui/screens/recording/RideRecordingScreen.kt
@Composable
fun RideRecordingScreen(
    viewModel: RideRecordingViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val stopDetectionState by viewModel.stopDetectionState.collectAsStateWithLifecycle()

    // Service binding
    DisposableEffect(Unit) {
        val serviceConnection = object : ServiceConnection {
            override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                val service = (binder as? RideRecordingService.LocalBinder)?.getService()
                viewModel.bindService(service!!)
            }

            override fun onServiceDisconnected(name: ComponentName?) {
                viewModel.unbindService()
            }
        }

        val intent = Intent(context, RideRecordingService::class.java)
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)

        onDispose {
            context.unbindService(serviceConnection)
        }
    }

    // UI based on stop detection state
    when (stopDetectionState) {
        is StopDetectionState.Moving -> {
            Text("Riding...")
        }
        is StopDetectionState.Detecting -> {
            val detecting = stopDetectionState as StopDetectionState.Detecting
            Text("Detecting stop: ${detecting.consecutiveCount}/${detecting.requiredCount}s")
        }
        is StopDetectionState.Confirmed -> {
            val confirmed = stopDetectionState as StopDetectionState.Confirmed
            val durationSec = (System.currentTimeMillis() - confirmed.stopStartTime) / 1000
            Text("Stopped for ${durationSec}s")
        }
        is StopDetectionState.InsufficientData -> {
            Text("GPS signal weak...")
        }
    }
}
```

### Alternatives Considered

**Alternative 1: Stop Detection in ViewModel**
```kotlin
// REJECTED: ViewModel doesn't survive background
class RideRecordingViewModel {
    init {
        viewModelScope.launch {
            detectStopsUseCase().collect { ... }
        }
    }
}
```
- **Why Rejected**: `viewModelScope` cancelled when ViewModel cleared (Activity destroyed, config change). Would miss stops during background periods.

**Alternative 2: WorkManager for Stop Detection**
```kotlin
// REJECTED: WorkManager not suitable for real-time detection
class StopDetectionWorker : CoroutineWorker() {
    override suspend fun doWork(): Result { ... }
}
```
- **Why Rejected**: WorkManager is for deferred background tasks, not real-time location tracking. Minimum execution delay is ~15 minutes in Doze mode.

**Alternative 3: Separate Stop Detection Service**
```kotlin
// REJECTED: Unnecessary complexity, two foreground notifications
class StopDetectionService : Service() { ... }
class RideRecordingService : Service() { ... }
```
- **Why Rejected**: Ride recording already requires foreground service. Adding second service would show two persistent notifications (poor UX).

### Android Gotchas

1. **Doze Mode Restrictions**: Even foreground services have location access restricted in Doze mode (Android 6+). Solution: Request `ACCESS_BACKGROUND_LOCATION` permission (required for Android 10+).

2. **Manufacturer Killing**: Some manufacturers (Huawei, Xiaomi) kill foreground services after extended periods. Solution: Educate users to whitelist app in battery optimization settings.

3. **Service Process Death**: System can still kill service under extreme memory pressure. Solution: Return `START_STICKY` from `onStartCommand()` to auto-restart, and persist critical state (current stop start time) to DataStore.

4. **Notification Requirement**: Foreground service MUST show persistent notification within 5 seconds of start (Android 8+). Failure = immediate service death.

5. **StateFlow Subscription Leak**: If ViewModel doesn't unbind from Service on `onCleared()`, service holds reference to ViewModel = memory leak. Solution: Use `DisposableEffect` in Composable for binding lifecycle.

6. **Concurrent Modification**: Multiple coroutines accessing state machine concurrently. Solution: Ensure state machine operations happen on single dispatcher (`Dispatchers.Default.limitedParallelism(1)`).

---

## Question 6: Memory Management for Stop Detection State

### Decision

**Stop detection state (consecutive counters, stop timer, state machine) should live in Service-scoped singleton managed by Hilt. Avoid storing state in ViewModel. Use DataStore for state persistence across service restarts.**

### Rationale

#### Memory Leak Risks

**ViewModel Memory Leaks - Common Causes:**
1. **Holding Activity/Context References**: ViewModel outlives Activity during config changes
2. **Repository Singleton + Callbacks**: Repository holds ViewModel reference indefinitely
3. **Long-lived Coroutines**: `viewModelScope.launch` with infinite loops

**Service Memory Leaks - Common Causes:**
1. **Static References**: Service holding static references to Locations/Bitmaps
2. **Location Callbacks Not Removed**: FusedLocationProvider callbacks leak service instance
3. **Notification Builders**: Holding PendingIntents with Activity context

#### Recommended Memory Architecture

```
┌────────────────────────────────────────────────┐
│ RideRecordingService (@HiltAndroidService)     │
│ ├─ serviceScope: CoroutineScope(SupervisorJob)│
│ ├─ stateMachine: StopDetectionStateMachine    │
│ │    (created in onCreate, reset in onDestroy)│
│ ├─ _stopState: MutableStateFlow               │
│ └─ consecutiveCounter: Int (local variable)   │
└────────────────────────────────────────────────┘
              ↓ (inject)
┌────────────────────────────────────────────────┐
│ @Singleton Repositories (Hilt-managed)         │
│ ├─ LocationRepository (stateless)             │
│ ├─ StopRepository (stateless)                 │
│ └─ SettingsRepository (stateless)             │
└────────────────────────────────────────────────┘
              ↓ (Room/DataStore)
┌────────────────────────────────────────────────┐
│ Persistent Storage (survives process death)    │
│ ├─ Room Database (stops table)                │
│ └─ DataStore (current stop start time)        │
└────────────────────────────────────────────────┘
```

**Key Principles:**
1. **Service owns transient state**: State machine, consecutive counters live in Service instance
2. **Repositories are stateless**: Just data access, no cached state
3. **Critical state persisted**: Current stop start time saved to DataStore
4. **StateFlow for UI**: Service exposes StateFlow, ViewModel collects it (no state duplication)

### Code Pattern

```kotlin
// service/RideRecordingService.kt
@HiltAndroidService
class RideRecordingService : Service() {

    @Inject lateinit var locationRepository: LocationRepository
    @Inject lateinit var stopRepository: StopRepository
    @Inject lateinit var stateRepository: RideRecordingStateRepository  // DataStore

    // Service-scoped CoroutineScope (cancelled in onDestroy)
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // State machine instance (reset on service destroy)
    private lateinit var stateMachine: StopDetectionStateMachine

    // StateFlow for UI consumption (lightweight, no Location object retention)
    private val _stopDetectionState = MutableStateFlow<StopDetectionState>(
        StopDetectionState.Moving
    )
    val stopDetectionState: StateFlow<StopDetectionState> = _stopDetectionState.asStateFlow()

    // Current ride/stop IDs (primitive types, no leak risk)
    private var currentRideId: Long? = null
    private var currentStopId: Long? = null

    override fun onCreate() {
        super.onCreate()

        // Initialize state machine
        stateMachine = StopDetectionStateMachine(consecutiveSecondsRequired = 3)

        // Restore persisted state if service restarted
        serviceScope.launch {
            val persistedState = stateRepository.getCurrentStopStartTime()
            if (persistedState != null) {
                // TODO: Restore state machine to Confirmed state
            }
        }

        startLocationTracking()
    }

    private fun startLocationTracking() {
        serviceScope.launch {
            locationRepository.getLocationUpdates()
                .filter { it.hasSpeed && it.accuracy <= 25f }
                .collect { location ->
                    // Extract minimal data (don't store entire Location object)
                    val locationData = LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        speed = location.speed,
                        accuracy = location.accuracy,
                        timestamp = System.currentTimeMillis(),
                        hasSpeed = location.hasSpeed
                    )

                    processLocationUpdate(locationData)
                }
        }
    }

    private suspend fun processLocationUpdate(location: LocationData) {
        // State machine transition (lightweight, no heavy objects)
        val event = createStopEvent(location)
        val newState = stateMachine.transition(event)

        // Persist critical state changes
        when (newState) {
            is StopDetectionState.Confirmed -> {
                if (currentStopId == null) {
                    // Persist stop start time to DataStore (survives service restart)
                    stateRepository.setCurrentStopStartTime(newState.stopStartTime)

                    // Insert stop to database
                    currentStopId = persistStop(newState)
                }
            }
            is StopDetectionState.Moving -> {
                // Clear persisted state
                stateRepository.clearCurrentStopStartTime()
            }
            else -> { /* No persistence needed */ }
        }

        // Emit lightweight state to UI
        _stopDetectionState.value = newState
    }

    override fun onDestroy() {
        // Critical: Cancel all coroutines to prevent leaks
        serviceScope.cancel()

        // Clear state machine (releases any held references)
        stateMachine.reset()

        super.onDestroy()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Return START_STICKY for auto-restart after process death
        return START_STICKY
    }
}

// data/repository/RideRecordingStateRepository.kt
@Singleton
class RideRecordingStateRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>
) : RideRecordingStateRepository {

    private val CURRENT_STOP_START_TIME = longPreferencesKey("current_stop_start_time")

    override suspend fun setCurrentStopStartTime(timestamp: Long) {
        dataStore.edit { prefs ->
            prefs[CURRENT_STOP_START_TIME] = timestamp
        }
    }

    override suspend fun getCurrentStopStartTime(): Long? {
        return dataStore.data.first()[CURRENT_STOP_START_TIME]
    }

    override suspend fun clearCurrentStopStartTime() {
        dataStore.edit { prefs ->
            prefs.remove(CURRENT_STOP_START_TIME)
        }
    }
}

// domain/model/LocationData.kt (lightweight value class)
data class LocationData(
    val latitude: Double,
    val longitude: Double,
    val speed: Double,
    val accuracy: Float,
    val timestamp: Long,
    val hasSpeed: Boolean
)
// Do NOT store android.location.Location objects - they contain
// references to System services and can leak memory
```

#### Memory Leak Prevention Checklist

**Service-Level:**
- ✅ Cancel `serviceScope` in `onDestroy()`
- ✅ Remove location callbacks before service death
- ✅ Don't store `Location` objects (use data classes)
- ✅ Don't hold static references to Service instance
- ✅ Use ApplicationContext for Notification builders

**ViewModel-Level:**
- ✅ Don't reference Activity/Fragment/View
- ✅ Don't store service reference directly (use bound service pattern)
- ✅ Use `viewModelScope` for coroutines (auto-cancels on clear)
- ✅ Unbind from service in `onCleared()`

**Repository-Level:**
- ✅ Repositories should be stateless (no cached data)
- ✅ Return Flows, not LiveData (better for coroutines)
- ✅ Don't hold callbacks/listeners indefinitely

### Alternatives Considered

**Alternative 1: State in ViewModel**
```kotlin
// REJECTED: Memory leak risk, doesn't survive background
class RideRecordingViewModel {
    private val stateMachine = StopDetectionStateMachine()
    private var consecutiveCount = 0
}
```
- **Why Rejected**: ViewModel lifecycle too short for long rides. State lost on config change/process death.

**Alternative 2: State in Repository Singleton**
```kotlin
// REJECTED: Repository should be stateless
@Singleton
class StopDetectionRepository {
    private val stateMachine = StopDetectionStateMachine()  // Lives forever
}
```
- **Why Rejected**: Violates Repository pattern (should be stateless data access). State survives ride end, causing incorrect behavior on next ride.

**Alternative 3: All State in DataStore**
```kotlin
// REJECTED: Excessive I/O for transient state
dataStore.edit {
    it[CONSECUTIVE_COUNT] = count  // Written every second
}
```
- **Why Rejected**: Writing to DataStore every second is I/O overhead. DataStore should only persist critical state that must survive crashes.

### Android Gotchas

1. **LeakCanary False Positives**: LeakCanary may report Service as leak during normal operation (foreground service is supposed to live). Ignore if service is properly bound and UI released.

2. **StateFlow Never Completes**: `StateFlow` doesn't complete like regular Flows. If ViewModel collects via `viewModelScope.launch`, ensure scope is cancelled on `onCleared()`.

3. **DataStore Write Blocking**: DataStore writes are async. Don't assume write completes before next line. Use `edit { }` suspend function.

4. **Location Object Size**: `android.location.Location` objects are ~1KB each. Storing 1000 locations = 1MB memory. Always convert to lightweight data class.

5. **Service Singleton Lifecycle**: Hilt `@Singleton` instances live for entire app lifetime. Service instances live only while service is running. Don't inject service instance into singletons.

6. **Process Death Recovery**: If system kills service process, Hilt singletons are recreated but in-memory state is lost. Critical state (current stop start time) MUST be in DataStore.

---

## Question 7: Testing Strategy for GPS-Based Stop Detection

### Decision

**Use synthetic location streams with Kotlin Flow test utilities for unit tests. Use mock `FusedLocationProviderClient` for repository tests. Use Espresso + mock locations for integration tests on emulator.**

### Rationale

#### Testing Challenges for GPS Logic

1. **Non-deterministic GPS**: Real GPS has variable accuracy, timing, and noise
2. **Long test duration**: Consecutive 3-second detection requires real-time waiting (unacceptable)
3. **Device dependency**: Different devices have different GPS chips
4. **Location permission**: Tests would require runtime permissions

#### Three-Layer Testing Strategy

**Layer 1: Unit Tests (State Machine & Use Cases)**
- Test with synthetic Flow streams (no actual GPS)
- Instant test execution (no delays)
- 100% deterministic behavior
- No Android framework dependencies

**Layer 2: Instrumented Tests (Repository)**
- Mock `FusedLocationProviderClient`
- Test LocationRepository integration
- Verify callback registration/unregistration
- Requires Android emulator

**Layer 3: Manual Testing (Full E2E)**
- Emulator with GPX route playback
- Physical device with real GPS
- User acceptance testing

### Code Pattern

#### Unit Tests (State Machine)

```kotlin
// test/domain/StopDetectionStateMachineTest.kt
class StopDetectionStateMachineTest {

    private lateinit var stateMachine: StopDetectionStateMachine

    @Before
    fun setup() {
        stateMachine = StopDetectionStateMachine(consecutiveSecondsRequired = 3)
    }

    @Test
    fun `transition from Moving to Detecting on speed below threshold`() {
        // Given
        val location = createLocationData(speed = 1.5)
        val event = StopEvent.SpeedBelowThreshold(location, threshold = 2.0)

        // When
        val newState = stateMachine.transition(event)

        // Then
        assertThat(newState).isInstanceOf<StopDetectionState.Detecting>()
        val detecting = newState as StopDetectionState.Detecting
        assertThat(detecting.consecutiveCount).isEqualTo(1)
        assertThat(detecting.requiredCount).isEqualTo(3)
    }

    @Test
    fun `transition to Confirmed after 3 consecutive below-threshold readings`() {
        // Given - Simulate 3 consecutive readings below threshold
        val locations = listOf(
            createLocationData(speed = 1.0, timestamp = 1000L),
            createLocationData(speed = 1.5, timestamp = 2000L),
            createLocationData(speed = 0.5, timestamp = 3000L)
        )

        // When - Process all 3 locations
        lateinit var finalState: StopDetectionState
        locations.forEach { location ->
            val event = StopEvent.SpeedBelowThreshold(location, threshold = 2.0)
            finalState = stateMachine.transition(event)
        }

        // Then - Should be confirmed
        assertThat(finalState).isInstanceOf<StopDetectionState.Confirmed>()
        val confirmed = finalState as StopDetectionState.Confirmed
        assertThat(confirmed.stopStartTime).isEqualTo(1000L)
    }

    @Test
    fun `reset to Moving if speed exceeds threshold during detection`() {
        // Given - Two readings below threshold
        val event1 = StopEvent.SpeedBelowThreshold(
            createLocationData(speed = 1.0),
            threshold = 2.0
        )
        val event2 = StopEvent.SpeedBelowThreshold(
            createLocationData(speed = 1.5),
            threshold = 2.0
        )
        stateMachine.transition(event1)
        stateMachine.transition(event2)

        // When - Third reading above threshold
        val event3 = StopEvent.SpeedAboveThreshold(
            createLocationData(speed = 5.0)
        )
        val newState = stateMachine.transition(event3)

        // Then - Should reset to Moving
        assertThat(newState).isEqualTo(StopDetectionState.Moving)
    }

    private fun createLocationData(
        speed: Double = 0.0,
        timestamp: Long = System.currentTimeMillis()
    ) = LocationData(
        latitude = 37.7749,
        longitude = -122.4194,
        speed = speed,
        accuracy = 10f,
        timestamp = timestamp,
        hasSpeed = true
    )
}
```

#### Flow Operator Tests (Consecutive Filtering)

```kotlin
// test/domain/util/FlowExtensionsTest.kt
@OptIn(ExperimentalCoroutinesApi::class)
class ConsecutiveBelowFlowTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `consecutiveBelow emits Detecting state during accumulation`() = runTest {
        // Given - Flow of speed readings
        val speedFlow = flow {
            emit(LocationData(speed = 1.0, timestamp = 1000L))
            emit(LocationData(speed = 1.5, timestamp = 2000L))
        }

        // When - Apply consecutiveBelow operator
        val results = speedFlow
            .consecutiveBelow(
                threshold = { 2.0 },
                consecutiveRequired = 3,
                getValue = { it.speed }
            )
            .toList()

        // Then - Should emit Detecting(1) then Detecting(2)
        assertThat(results).hasSize(2)
        assertThat(results[0]).isInstanceOf<ConsecutiveDetectionState.Detecting>()
        val detecting1 = results[0] as ConsecutiveDetectionState.Detecting
        assertThat(detecting1.count).isEqualTo(1)

        assertThat(results[1]).isInstanceOf<ConsecutiveDetectionState.Detecting>()
        val detecting2 = results[1] as ConsecutiveDetectionState.Detecting
        assertThat(detecting2.count).isEqualTo(2)
    }

    @Test
    fun `consecutiveBelow emits Confirmed after threshold met`() = runTest {
        // Given - Flow with 3 consecutive below-threshold readings
        val speedFlow = flow {
            emit(LocationData(speed = 1.0, timestamp = 1000L))
            emit(LocationData(speed = 1.2, timestamp = 2000L))
            emit(LocationData(speed = 0.8, timestamp = 3000L))
        }

        // When
        val results = speedFlow
            .consecutiveBelow(
                threshold = { 2.0 },
                consecutiveRequired = 3,
                getValue = { it.speed }
            )
            .toList()

        // Then - Last emission should be Confirmed
        assertThat(results.last()).isInstanceOf<ConsecutiveDetectionState.Confirmed>()
    }

    @Test
    fun `consecutiveBelow resets on above-threshold reading`() = runTest {
        // Given - Two below, one above, two below again
        val speedFlow = flow {
            emit(LocationData(speed = 1.0, timestamp = 1000L))  // Below
            emit(LocationData(speed = 1.5, timestamp = 2000L))  // Below
            emit(LocationData(speed = 5.0, timestamp = 3000L))  // Above - RESET
            emit(LocationData(speed = 1.0, timestamp = 4000L))  // Below
            emit(LocationData(speed = 1.2, timestamp = 5000L))  // Below
        }

        // When
        val results = speedFlow
            .consecutiveBelow(
                threshold = { 2.0 },
                consecutiveRequired = 3,
                getValue = { it.speed }
            )
            .toList()

        // Then - Should reset and restart counting
        assertThat(results).containsAtLeast(
            ConsecutiveDetectionState.AboveThreshold(),
            ConsecutiveDetectionState.Detecting(count = 1, required = 3)
        )
    }
}
```

#### Repository Integration Tests (Mocked FusedLocationProvider)

```kotlin
// androidTest/data/repository/LocationRepositoryTest.kt
@RunWith(AndroidJUnit4::class)
class LocationRepositoryTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    private lateinit var mockFusedClient: FusedLocationProviderClient
    private lateinit var repository: LocationRepository

    @Before
    fun setup() {
        mockFusedClient = mockk<FusedLocationProviderClient>(relaxed = true)
        repository = LocationRepositoryImpl(mockFusedClient)
    }

    @Test
    fun getLocationUpdates_emitsLocationData() = runTest {
        // Given - Mock location provider returns locations
        val mockLocation = createMockLocation(
            latitude = 37.7749,
            longitude = -122.4194,
            speed = 5.0f,
            accuracy = 10f
        )

        // Mock location callback invocation
        every {
            mockFusedClient.requestLocationUpdates(any(), any<LocationCallback>(), any())
        } answers {
            val callback = secondArg<LocationCallback>()
            callback.onLocationResult(LocationResult.create(listOf(mockLocation)))
            Tasks.forResult(null)
        }

        // When - Collect from repository
        val locations = repository.getLocationUpdates()
            .take(1)
            .toList()

        // Then
        assertThat(locations).hasSize(1)
        assertThat(locations[0].latitude).isEqualTo(37.7749)
        assertThat(locations[0].speed).isEqualTo(5.0)
    }

    @Test
    fun getLocationUpdates_removesCallbackOnCancellation() = runTest {
        // Given
        val locationCallbackSlot = slot<LocationCallback>()
        every {
            mockFusedClient.requestLocationUpdates(
                any(),
                capture(locationCallbackSlot),
                any()
            )
        } returns Tasks.forResult(null)

        // When - Collect then cancel
        val job = launch {
            repository.getLocationUpdates().collect { }
        }
        delay(100)
        job.cancel()

        // Then - Should remove location callback
        verify {
            mockFusedClient.removeLocationUpdates(locationCallbackSlot.captured)
        }
    }

    private fun createMockLocation(
        latitude: Double,
        longitude: Double,
        speed: Float,
        accuracy: Float
    ): Location {
        return Location("test").apply {
            this.latitude = latitude
            this.longitude = longitude
            this.speed = speed
            this.accuracy = accuracy
            this.time = System.currentTimeMillis()
        }
    }
}
```

#### Manual Testing with GPX Routes

```kotlin
// For manual emulator testing, create GPX file with stop scenario

// test-resources/stop_detection_scenario.gpx
<?xml version="1.0" encoding="UTF-8"?>
<gpx version="1.1">
  <trk>
    <name>Stop Detection Test Route</name>
    <trkseg>
      <!-- Moving segment: 10 m/s (~22 mph) -->
      <trkpt lat="37.7749" lon="-122.4194"><time>2024-01-01T10:00:00Z</time></trkpt>
      <trkpt lat="37.7750" lon="-122.4193"><time>2024-01-01T10:00:01Z</time></trkpt>
      <trkpt lat="37.7751" lon="-122.4192"><time>2024-01-01T10:00:02Z</time></trkpt>

      <!-- Deceleration -->
      <trkpt lat="37.7752" lon="-122.4191"><time>2024-01-01T10:00:03Z</time></trkpt>

      <!-- Stop segment: Same coordinates for 5 seconds -->
      <trkpt lat="37.7752" lon="-122.4191"><time>2024-01-01T10:00:04Z</time></trkpt>
      <trkpt lat="37.7752" lon="-122.4191"><time>2024-01-01T10:00:05Z</time></trkpt>
      <trkpt lat="37.7752" lon="-122.4191"><time>2024-01-01T10:00:06Z</time></trkpt>
      <trkpt lat="37.7752" lon="-122.4191"><time>2024-01-01T10:00:07Z</time></trkpt>
      <trkpt lat="37.7752" lon="-122.4191"><time>2024-01-01T10:00:08Z</time></trkpt>

      <!-- Resume movement -->
      <trkpt lat="37.7753" lon="-122.4190"><time>2024-01-01T10:00:09Z</time></trkpt>
      <trkpt lat="37.7754" lon="-122.4189"><time>2024-01-01T10:00:10Z</time></trkpt>
    </trkseg>
  </trk>
</gpx>
```

### Alternatives Considered

**Alternative 1: Real GPS Testing Only**
```kotlin
// REJECTED: Flaky, slow, environment-dependent
@Test
fun testStopDetection_withRealGPS() {
    // Wait for actual GPS signal...
    // Walk to location, stop, wait...
    // Tests take minutes, fail in bad weather
}
```
- **Why Rejected**: Impossible to run in CI/CD. Flaky (weather, building interference). Too slow for TDD workflow.

**Alternative 2: Robolectric for Location Mocking**
```kotlin
// REJECTED: Limited FusedLocationProvider support
@RunWith(RobolectricTestRunner::class)
class LocationTest {
    // Robolectric shadows for FusedLocationProvider are incomplete
}
```
- **Why Rejected**: Robolectric's location mocking is incomplete. Better to use real Android instrumentation tests with mocked client.

**Alternative 3: Thread.sleep() for Timing Tests**
```kotlin
// REJECTED: Slow tests, brittle timing
@Test
fun consecutiveSecondsTest() {
    emit(location1)
    Thread.sleep(1000)  // Wait 1 second
    emit(location2)
    Thread.sleep(1000)  // Wait 1 second
    emit(location3)
    // Test takes 2+ seconds
}
```
- **Why Rejected**: Tests should execute instantly. Use `runTest` with virtual time instead.

### Android Gotchas

1. **TestDispatcher for Flow Tests**: Always use `runTest { }` from `kotlinx-coroutines-test` for Flow testing. Provides virtual time (instant test execution).

2. **MockK Relaxed Mode**: Use `relaxed = true` for FusedLocationProviderClient mocking to avoid stubbing every method:
   ```kotlin
   val mockClient = mockk<FusedLocationProviderClient>(relaxed = true)
   ```

3. **Location Object Creation**: `Location` class has no public constructor with all fields. Must create via `Location(provider)` then set fields:
   ```kotlin
   Location("test").apply { latitude = 37.7; speed = 5.0f }
   ```

4. **Emulator Mock Locations**: Emulator mock locations require `ACCESS_MOCK_LOCATION` permission (security risk to enable in production builds). Only enable in debug builds.

5. **GPX Playback Speed**: Emulator GPX playback speed is configurable. Set to 1x speed for realistic testing (default is often 10x).

6. **Turbine for Flow Testing**: Use Turbine library for easier Flow testing with `awaitItem()`, `awaitComplete()`:
   ```kotlin
   flow.test {
       assertThat(awaitItem()).isEqualTo(expected)
       awaitComplete()
   }
   ```

7. **Flow Never Completes**: StateFlow never calls `onCompletion`. Use `take(n)` or `first()` in tests:
   ```kotlin
   stateFlow.take(3).test { ... }
   ```

---

## Summary of Decisions

| Question | Decision | Rationale |
|----------|----------|-----------|
| **1. GPS Speed Accuracy** | Use `Location.getSpeed()` with 3-second consecutive filtering + accuracy validation | Single readings unreliable; consecutive filtering eliminates GPS noise |
| **2. State Machine Pattern** | Sealed classes + `when` expressions in dedicated `StopDetectionStateMachine` class | Type-safe, compile-time exhaustive checking, testable without Android deps |
| **3. Consecutive Filtering** | Custom Flow operator using `scan` + `distinctUntilChanged` | `debounce` doesn't fit (no silence period), `scan` explicitly tracks count |
| **4. Database Migration** | Manual Migration 4→5, create `stops` table with CASCADE foreign key | SQLite doesn't support ALTER TABLE for foreign keys; new table = clean migration |
| **5. Foreground Service** | Stop detection logic in `RideRecordingService`, not ViewModel | Service survives background/Doze mode; ViewModel lifecycle too short |
| **6. Memory Management** | State in Service + critical state in DataStore, avoid Location object retention | Service-scoped state prevents leaks; DataStore for crash recovery |
| **7. Testing Strategy** | Unit tests with synthetic Flows + instrumented tests with mock client | Fast, deterministic, no GPS dependency; GPX testing for manual validation |

---

## Implementation Checklist

- [ ] **Domain Layer**
  - [ ] Create `StopDetectionState` sealed class (Moving, Detecting, Confirmed, InsufficientData)
  - [ ] Implement `StopDetectionStateMachine` class with state transition logic
  - [ ] Create `DetectStopsUseCase` for Flow of detection states
  - [ ] Implement `consecutiveBelow()` custom Flow operator
  - [ ] Write unit tests for state machine (100% coverage target)
  - [ ] Write unit tests for Flow operator with Turbine

- [ ] **Data Layer**
  - [ ] Create `Stop` Room entity with foreign key CASCADE to `Ride`
  - [ ] Implement `StopDao` with insert/update/query operations
  - [ ] Create `StopRepository` interface and implementation
  - [ ] Implement Room Migration 4→5 for stops table
  - [ ] Add DataStore methods for persisting current stop start time
  - [ ] Write migration test with `MigrationTestHelper`
  - [ ] Write repository tests with mocked Room

- [ ] **Service Layer**
  - [ ] Integrate stop detection in `RideRecordingService`
  - [ ] Add `StateFlow<StopDetectionState>` exposed to ViewModels
  - [ ] Implement stop persistence on Confirmed state
  - [ ] Implement stop end on Moving state transition
  - [ ] Add DataStore persistence for crash recovery
  - [ ] Ensure `serviceScope` cancellation in `onDestroy()`

- [ ] **UI Layer**
  - [ ] Bind `RideRecordingViewModel` to Service
  - [ ] Collect `stopDetectionState` in `RideRecordingScreen`
  - [ ] Display UI for Moving, Detecting, Confirmed states
  - [ ] Show stop duration timer during Confirmed state
  - [ ] Add visual feedback (icon, color) for stop state
  - [ ] Handle InsufficientData state with "weak GPS" message

- [ ] **Settings Integration**
  - [ ] Use `stopSpeedThresholdMps` setting from Feature 008
  - [ ] Use `consecutiveSecondsRequired` setting
  - [ ] React to settings changes during active ride

- [ ] **Testing**
  - [ ] Unit tests for state machine (all transitions)
  - [ ] Unit tests for Flow operator (consecutive logic)
  - [ ] Instrumented tests for LocationRepository with mocks
  - [ ] Integration tests for Service + Repository
  - [ ] Manual testing with GPX route on emulator
  - [ ] Manual testing on physical device with real cycling

- [ ] **Documentation**
  - [ ] Update TODO.md with feature progress
  - [ ] Update RELEASE.md with stop detection entry
  - [ ] Document state persistence strategy
  - [ ] Create GPX test routes for QA

- [ ] **Code Review**
  - [ ] Verify no memory leaks (LeakCanary check)
  - [ ] Verify no blocking operations on main thread
  - [ ] Verify database migration tested
  - [ ] Verify all states handled exhaustively
  - [ ] Verify emulator testing completed
  - [ ] Verify commit message follows conventional commits

---

## References

### Android Developer Documentation
- [FusedLocationProviderClient](https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient)
- [Room Database Migrations](https://developer.android.com/training/data-storage/room/migrating-db-versions)
- [Foreground Services](https://developer.android.com/develop/background-work/services/foreground-services)
- [Doze Mode Optimization](https://developer.android.com/training/monitoring-device-state/doze-standby)

### Kotlin Documentation
- [Kotlin Flows](https://kotlinlang.org/docs/flow.html)
- [Sealed Classes](https://kotlinlang.org/docs/sealed-classes.html)
- [Flow Operators](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/)

### Community Resources
- Stack Overflow: "Location.getSpeed() reliability at low speeds"
- Medium: "Android State Machine with Sealed Classes"
- GitHub: Tinder StateMachine library (reference, not used)

### BikeRedlights Project Documentation
- `/specs/008-stop-detection-settings/` - Settings infrastructure
- `/app/src/main/java/com/example/bikeredlights/data/local/entity/Ride.kt` - Existing schema
- `/app/build.gradle.kts` - Current dependencies and configuration

---

**End of Research Document**
