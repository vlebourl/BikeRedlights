# Quickstart: Stop Detection & Recording

**Feature**: 009-stop-detection
**Date**: 2025-11-18
**For**: Developers implementing this feature

## What This Feature Does

Automatically detects when a cyclist stops during a ride by monitoring GPS speed. When speed drops below a configurable threshold (default 3 km/h) for a configurable duration (default 15 seconds), the system:
1. Records stop data to database (location, timestamps, sequential number)
2. Displays a live popup showing "🛑 Stop #N" with duration counter
3. Updates "Stops: N" counter on Live tab
4. Ends stop when movement resumes (speed > threshold for 3 seconds)

This provides the raw data for future clustering (Feature 010) to infer red light locations.

---

## Prerequisites

Before implementing Feature 009, ensure:

- ✅ **Feature 008 (Stop Detection Settings)** is complete
  - Settings UI exists for: speed threshold, duration threshold, clustering radius
  - SettingsRepository exposes DataStore values via Flows
- ✅ **Feature 002-007 (Ride Recording)** infrastructure exists
  - RideRecordingService runs in foreground during rides
  - LocationRepository provides GPS location updates (Flow)
  - RideViewModel manages ride state (recording, paused, stopped)
  - Room database with `rides` and `track_points` tables
- ✅ **Development Environment** is set up
  - Android Studio with Kotlin 2.0.21, Java 17
  - Android emulator with GPS simulation capability
  - Room Database Inspector plugin for debugging

---

## Implementation Roadmap (High-Level)

### Phase 1: Database Layer (Data)
1. Create `StopEntity.kt` (Room entity)
2. Create `StopDao.kt` (database operations)
3. Implement Room migration (v4 → v5) adding `stops` table
4. Create `StopRepository.kt` interface and implementation
5. Update `AppModule.kt` Hilt bindings

**Validation**: Use Database Inspector to verify table creation, foreign keys, indexes.

### Phase 2: Domain Logic (Domain)
1. Create `Stop.kt` domain model
2. Create `StopDetectionState.kt` runtime state class
3. Create `StopDetectionStateMachine.kt` (state transitions)
4. Modify `TrackLocationUseCase.kt` to integrate stop detection
5. Create `StopDetectionUtils.kt` (consecutive seconds filtering, duration formatting)

**Validation**: Unit tests for state machine transitions, consecutive seconds logic.

### Phase 3: Service Integration (Service)
1. Modify `RideRecordingService.kt` to own stop detection state
2. Add stop state to service scope (survives backgrounding)
3. Expose stop events via SharedFlow to ViewModel
4. Handle stop cleanup on ride pause/stop

**Validation**: Test app backgrounding, service survival during long stops (30+ min).

### Phase 4: UI Layer (UI)
1. Create `StopPopup.kt` composable (semi-transparent card)
2. Modify `RideStatsRow.kt` to add "Stops: N" display
3. Modify `LiveRideScreen.kt` to show StopPopup when stop active
4. Update `RideViewModel.kt` to collect stop events from service

**Validation**: Emulator testing with GPX route playback, verify popup appearance/dismissal.

### Phase 5: Testing
1. Unit tests: `StopDetectionStateMachineTest.kt`, `StopRepositoryTest.kt`
2. Instrumented tests: `StopDaoTest.kt` (CASCADE delete, UNIQUE constraints)
3. Integration tests: Full ride with multiple stops, verify database records
4. Manual emulator tests: Real GPS simulation, edge cases (signal loss, app kill)

**Validation**: All tests passing, emulator ride with 5+ stops shows correct data.

---

## Key Files to Create/Modify

### NEW Files (Create)

**Data Layer**:
- `app/src/main/java/com/example/bikeredlights/data/local/entity/StopEntity.kt`
- `app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt`
- `app/src/main/java/com/example/bikeredlights/data/repository/StopRepositoryImpl.kt`

**Domain Layer**:
- `app/src/main/java/com/example/bikeredlights/domain/model/Stop.kt`
- `app/src/main/java/com/example/bikeredlights/domain/model/StopDetectionState.kt`
- `app/src/main/java/com/example/bikeredlights/domain/repository/StopRepository.kt` (interface)
- `app/src/main/java/com/example/bikeredlights/domain/util/StopDetectionStateMachine.kt`
- `app/src/main/java/com/example/bikeredlights/domain/util/StopDetectionUtils.kt`

**UI Layer**:
- `app/src/main/java/com/example/bikeredlights/ui/components/ride/StopPopup.kt`

**Tests**:
- `app/src/test/java/com/example/bikeredlights/domain/util/StopDetectionStateMachineTest.kt`
- `app/src/test/java/com/example/bikeredlights/data/repository/StopRepositoryTest.kt`
- `app/src/androidTest/java/com/example/bikeredlights/data/local/dao/StopDaoTest.kt`

### MODIFIED Files

**Data Layer**:
- `app/src/main/java/com/example/bikeredlights/data/local/AppDatabase.kt` (add stops table, migration)
- `app/src/main/java/com/example/bikeredlights/di/AppModule.kt` (Hilt bindings)

**Domain Layer**:
- `app/src/main/java/com/example/bikeredlights/domain/usecase/TrackLocationUseCase.kt` (add stop detection)

**Service Layer**:
- `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt` (stop state management)

**UI Layer**:
- `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatsRow.kt` (add stop count)
- `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt` (show popup)
- `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideViewModel.kt` (stop state)

---

## Critical Implementation Details

### 1. Consecutive Seconds Filtering (GPS Noise)

**Problem**: GPS speed is extremely unreliable at low speeds (can show 200+ mph when stationary).

**Solution**: Require 3 consecutive GPS readings below/above threshold before confirming state change.

**Code Pattern** (from research.md):
```kotlin
fun Flow<Float>.filterConsecutiveSeconds(
    predicate: (Float) -> Boolean,
    requiredCount: Int = 3
): Flow<Boolean> = scan(0 to false) { (count, _), speed ->
    val matches = predicate(speed)
    val newCount = if (matches) (count + 1).coerceAtMost(requiredCount) else 0
    newCount to (newCount >= requiredCount)
}.map { it.second }.distinctUntilChanged()
```

**Usage**:
```kotlin
speedFlow
    .filterConsecutiveSeconds(predicate = { it < speedThreshold })
    .collect { isStoppedConfirmed -> /* handle state change */ }
```

### 2. State Machine in Service, NOT ViewModel

**Rationale** (from research.md):
- Service survives Doze mode, process death, long background periods
- ViewModel is UI-scoped and gets destroyed when app backgrounded
- Stop detection must continue during 30+ minute stops even when app in background

**Architecture**:
```
RideRecordingService
    ├── owns StopDetectionStateMachine
    ├── collects LocationRepository.locationUpdates
    ├── processes speed via state machine
    ├── calls StopRepository (insert/update)
    └── emits stop events via SharedFlow

RideViewModel
    └── collects stop events from Service
        └── updates UI state (popup visibility, stop count)
```

### 3. Database Migration Pattern

**Version Bump**: v4 → v5

**Migration Code** (in AppDatabase.kt):
```kotlin
val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        // Create stops table with foreign keys
        database.execSQL("""
            CREATE TABLE stops (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                ride_id INTEGER NOT NULL,
                stop_number INTEGER NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                start_timestamp INTEGER NOT NULL,
                end_timestamp INTEGER,
                duration_seconds INTEGER,
                cluster_id INTEGER,
                FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE,
                UNIQUE (ride_id, stop_number)
            )
        """)

        // Critical: Create indexes for foreign keys (avoids performance warnings)
        database.execSQL("CREATE INDEX idx_stops_ride_id ON stops(ride_id)")
        database.execSQL("CREATE INDEX idx_stops_cluster_id ON stops(cluster_id)")
        database.execSQL("CREATE INDEX idx_stops_start_timestamp ON stops(start_timestamp)")
    }
}
```

**Testing Migration**:
```kotlin
@Test
fun testMigration4To5() {
    val helper = MigrationTestHelper(/* ... */)

    // Create v4 database
    helper.createDatabase(TEST_DB, 4).apply { close() }

    // Migrate to v5
    helper.runMigrationsAndValidate(TEST_DB, 5, true, MIGRATION_4_5)

    // Verify stops table exists
    val db = helper.runMigrationsAndValidate(TEST_DB, 5, true)
    val cursor = db.query("SELECT * FROM stops")
    assertEquals(0, cursor.count) // Empty table, no errors
}
```

### 4. Stop Popup UI Pattern

**Design** (from roadmap UI/UX guidelines):
- Semi-transparent card (Material 3 elevated card, 80% opacity)
- Top-center positioning (doesn't block map or speed)
- Auto-dismiss on movement (fade-out 200ms animation)
- Updates every 1 second (stop duration counter)

**Compose Pattern**:
```kotlin
@Composable
fun StopPopup(
    stopNumber: Int,
    durationSeconds: Int,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = true,
        exit = fadeOut(animationSpec = tween(200))
    ) {
        Card(
            modifier = modifier
                .padding(top = 16.dp)
                .alpha(0.8f),
            elevation = CardDefaults.elevatedCardElevation(8.dp)
        ) {
            Row(/* ... */) {
                Icon(Icons.Default.Stop, tint = Color.Red)
                Text("Stop #$stopNumber", style = MaterialTheme.typography.titleMedium)
                Text(formatDuration(durationSeconds), style = MaterialTheme.typography.bodyLarge)
            }
        }
    }
}
```

---

## Testing Strategy

### Unit Tests (Fast, No Android Framework)

**StopDetectionStateMachineTest.kt**:
- Test all state transitions (Moving → Detecting → Confirmed → Moving)
- Test consecutive seconds logic (below/above threshold)
- Test edge cases (exactly 3 seconds, GPS dropout, rapid oscillation)

**Test Pattern**:
```kotlin
@Test
fun `when speed below threshold for 3 seconds, transitions to Confirmed`() = runTest {
    val stateMachine = StopDetectionStateMachine(speedThreshold = 3f, durationThreshold = 15)

    // Emit 3 consecutive speeds below threshold
    repeat(3) { stateMachine.processSpeed(2.5f, currentTime = it * 1000L) }

    // Wait for duration threshold
    advanceTimeBy(15_000)

    assertEquals(StopState.Confirmed, stateMachine.currentState)
}
```

### Instrumented Tests (Android Framework Required)

**StopDaoTest.kt**:
- Test insert stop (verify foreign key constraint)
- Test CASCADE delete (delete ride → stops auto-deleted)
- Test UNIQUE constraint (duplicate stopNumber per ride fails)
- Test Flow reactivity (getStopCountByRideId emits on insert)

**Test Pattern**:
```kotlin
@Test
fun testCascadeDelete() = runTest {
    val rideId = rideDao.insertRide(testRide)
    val stopId = stopDao.insertStop(testStop.copy(rideId = rideId))

    // Verify stop exists
    assertNotNull(stopDao.getStopById(stopId))

    // Delete ride
    rideDao.deleteRide(rideId)

    // Verify stop auto-deleted (CASCADE)
    assertNull(stopDao.getStopById(stopId))
}
```

### Manual Emulator Testing

**GPX Route Playback**:
1. Load GPX file in emulator (Extended Controls → Location → Load GPX)
2. Start ride recording in app
3. Play route at cycling speed (10-20 km/h)
4. Pause route to simulate stop (speed drops to 0)
5. Wait 15+ seconds, verify popup appears
6. Resume route, verify popup dismisses after 3 seconds of movement
7. Check database (Database Inspector): verify stop record exists

**Edge Case Testing**:
- App backgrounding during stop (send to background, wait 5 min, resume)
- App kill during stop (force stop, restart app, verify partial stop data saved)
- GPS signal loss (disable location in emulator settings mid-stop)
- Rapid speed oscillations (manually adjust GPX file to oscillate around threshold)

---

## Common Pitfalls & Solutions

### Pitfall 1: GPS Speed Unreliability
**Problem**: `Location.getSpeed()` returns 0.0 when GPS has no fix, can show 200+ km/h when stationary.
**Solution**: Always check `location.hasSpeed()` before using speed value. Use 3-second consecutive filtering.

### Pitfall 2: Memory Leaks in Service
**Problem**: Storing `Location` objects in service state causes memory leaks (Location holds Context).
**Solution**: Extract primitives only (speed, lat, long) into lightweight data class. Never store Location references.

### Pitfall 3: Stop Detection During Pause
**Problem**: If ride is paused manually, stop detection still runs, records stops while stationary.
**Solution**: Check `rideState == RideState.RECORDING` before processing speed in state machine. Ignore updates during PAUSED state.

### Pitfall 4: Foreign Key Not Enforced
**Problem**: Room doesn't enforce foreign keys by default, stops can reference non-existent rides.
**Solution**: Enable foreign keys in AppDatabase: `@Database(exportSchema = true, enableForeignKeyConstraints = true)` (already set in existing database).

### Pitfall 5: UI Recomposition Lag
**Problem**: Stop popup flickers or lags when updating duration every second.
**Solution**: Use `LaunchedEffect` with 1-second delay instead of continuous Flow collection. Only update UI when value changes.

---

## Definition of Done

Feature 009 is complete when:

- ✅ Database: `stops` table created with foreign keys, indexes, migration tested
- ✅ Domain: Stop detection state machine handles all transitions correctly
- ✅ Service: Stop detection runs reliably in background for 30+ minute stops
- ✅ UI: Stop popup displays correctly, auto-dismisses on movement
- ✅ UI: "Stops: N" counter updates in real-time on Live tab
- ✅ Settings: Speed/duration thresholds from Feature 008 are consumed correctly
- ✅ Tests: All unit tests pass (state machine, repository)
- ✅ Tests: All instrumented tests pass (DAO, CASCADE delete)
- ✅ Emulator: Manual test ride with 5+ stops shows correct data in database
- ✅ Emulator: App survives backgrounding during active stop (no data loss)
- ✅ Code Review: All code follows CLAUDE.md standards (Kotlin-first, immutability, null safety)
- ✅ Documentation: TODO.md and RELEASE.md updated (per constitution)
- ✅ Release: APK built, tagged v0.9.0, GitHub release created

---

## Next Steps After Feature 009

Once stop detection is complete:

1. **Feature 010: Stop Clustering & Statistics** (Roadmap Feature 5)
   - DBSCAN-like clustering algorithm using Haversine distance
   - Populates `cluster_id` field in existing stop records
   - Calculates median/min/max/avg duration per cluster
   - Creates global `stop_clusters` table

2. **Feature 011: Stop Clusters Map Visualization** (Roadmap Feature 6)
   - New "Stops" tab in bottom navigation
   - Google Maps with red light markers (color-coded by duration)
   - Marker size scales with stop frequency
   - Bottom sheet showing cluster statistics

Feature 009 provides the raw data foundation for these future features. Clustering cannot happen without stop detection working correctly first.

---

## Support & Resources

- **Spec**: [spec.md](spec.md) - Full feature specification
- **Research**: [research.md](research.md) - Technical research findings
- **Data Model**: [data-model.md](data-model.md) - Entity definitions, relationships
- **Contract**: [contracts/StopDao.kt](contracts/StopDao.kt) - Database interface
- **Roadmap**: [../../docs/roadmap.md](../../docs/roadmap.md) - Overall project roadmap
- **Standards**: [CLAUDE.md](../../CLAUDE.md) - Android development standards

For questions or blockers, refer to research.md Section 7 (Testing Strategy) or consult existing similar features (Feature 002-007 ride recording infrastructure).
