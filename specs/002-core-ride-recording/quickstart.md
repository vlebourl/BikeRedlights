# Quickstart Guide: Core Ride Recording

**Feature**: F1A - Core Ride Recording
**Version**: v0.3.0
**Branch**: 002-core-ride-recording
**Date**: 2025-11-04

---

## Overview

This quickstart guide provides a condensed roadmap for implementing GPS-based ride recording with Room persistence, foreground service architecture, and manual/auto-pause functionality.

**Target Audience**: Developers implementing this feature
**Prerequisites**: Feature spec (`spec.md`), research (`research.md`), and data model (`data-model.md`) reviewed

---

## Implementation Checklist

### Phase 0: Database Foundation (Day 1)

- [ ] **Create Room Entities**
  - [ ] Define `Ride` entity with all fields (data-model.md §1)
  - [ ] Define `TrackPoint` entity with foreign key CASCADE (data-model.md §2)
  - [ ] Add indices: startTime, rideId, timestamp

- [ ] **Create DAOs**
  - [ ] `RideDao` with insert/update/delete/query methods
  - [ ] `TrackPointDao` with batch insert optimization
  - [ ] Add Flow-based queries for reactive updates

- [ ] **Build Database Class**
  - [ ] `BikeRedlightsDatabase` with version = 1
  - [ ] Configure schema export (already in build.gradle.kts)
  - [ ] Use `.fallbackToDestructiveMigration()` for v1

- [ ] **Unit Test DAOs**
  - [ ] In-memory database tests
  - [ ] Verify cascade delete behavior
  - [ ] Test incomplete ride queries

**Files Created**:
- `app/src/main/java/com/example/bikeredlights/data/local/entity/Ride.kt`
- `app/src/main/java/com/example/bikeredlights/data/local/entity/TrackPoint.kt`
- `app/src/main/java/com/example/bikeredlights/data/local/dao/RideDao.kt`
- `app/src/main/java/com/example/bikeredlights/data/local/dao/TrackPointDao.kt`
- `app/src/main/java/com/example/bikeredlights/data/local/BikeRedlightsDatabase.kt`

---

### Phase 1: Repository Layer (Day 2)

- [ ] **Create Repository Interfaces**
  - [ ] `RideRepository` interface (contracts/repository-contracts.md §1)
  - [ ] `TrackPointRepository` interface (§2)
  - [ ] `LocationRepository` interface (§3)
  - [ ] `RideRecordingStateRepository` interface (§4)

- [ ] **Implement Repositories**
  - [ ] `RideRepositoryImpl` with DAO integration
  - [ ] `TrackPointRepositoryImpl` with validation
  - [ ] `LocationRepositoryImpl` with FusedLocationProviderClient
  - [ ] `RideRecordingStateRepositoryImpl` with MutableStateFlow

- [ ] **Add Error Handling**
  - [ ] Wrap SQL exceptions in domain errors
  - [ ] Validate TrackPoint accuracy < 50m
  - [ ] Handle SecurityException for location permissions

- [ ] **Unit Test Repositories**
  - [ ] Mock DAOs, test business logic
  - [ ] Verify StateFlow emissions (use Turbine)
  - [ ] Test error handling paths

**Files Created**:
- `app/src/main/java/com/example/bikeredlights/domain/repository/*.kt` (interfaces)
- `app/src/main/java/com/example/bikeredlights/data/repository/*RepositoryImpl.kt` (implementations)

---

### Phase 2: Foreground Service (Day 3)

- [ ] **Create Service**
  - [ ] `RideRecordingService` extends `LifecycleService`
  - [ ] Implement `onStartCommand()` with action handling (research.md §1)
  - [ ] Build notification with Pause/Stop actions
  - [ ] Call `startForeground()` within 5 seconds

- [ ] **Location Tracking**
  - [ ] Configure `LocationRequest` with High Accuracy (1000ms)
  - [ ] Implement `LocationCallback` for GPS updates
  - [ ] Calculate distance using Haversine formula
  - [ ] Insert TrackPoints to database

- [ ] **Service Actions**
  - [ ] ACTION_START_RECORDING: Start GPS, show notification
  - [ ] ACTION_PAUSE_RECORDING: Stop GPS, update notification
  - [ ] ACTION_RESUME_RECORDING: Resume GPS
  - [ ] ACTION_STOP_RECORDING: Stop GPS, finish ride, stop service

- [ ] **Process Death Handling**
  - [ ] Return `START_STICKY` from onStartCommand
  - [ ] On service restart, check for incomplete rides
  - [ ] Resume recording if incomplete ride found

- [ ] **Update Manifest**
  - [ ] Add service declaration with `foregroundServiceType="location"`
  - [ ] Add permissions: FOREGROUND_SERVICE, FOREGROUND_SERVICE_LOCATION, POST_NOTIFICATIONS

- [ ] **Test Service**
  - [ ] Instrumented test: Start/stop service
  - [ ] Verify notification appears
  - [ ] Test action handling (pause/resume/stop)

**Files Created**:
- `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`
- `app/src/main/AndroidManifest.xml` (updated)

---

### Phase 3: ViewModel & State Management (Day 4)

- [ ] **Create ViewModel**
  - [ ] `RideRecordingViewModel` with StateFlow-based UI state
  - [ ] Inject repositories via constructor (manual DI)
  - [ ] Observe `RideRecordingStateRepository` for live updates

- [ ] **Implement Actions**
  - [ ] `startRide()`: Create ride in DB, start service
  - [ ] `pauseRide()`: Send ACTION_PAUSE_RECORDING intent
  - [ ] `resumeRide()`: Send ACTION_RESUME_RECORDING intent
  - [ ] `stopRide()`: Show save/discard dialog
  - [ ] `saveRide()`: Finish ride in DB, navigate to Review
  - [ ] `discardRide()`: Delete ride, navigate to Live tab

- [ ] **Auto-Pause Logic**
  - [ ] Read `AutoPauseConfig` from SettingsRepository (F2A)
  - [ ] Monitor speed < 1 km/h for threshold minutes
  - [ ] Trigger auto-pause, update state
  - [ ] Auto-resume on speed > 1 km/h

- [ ] **Units Conversion**
  - [ ] Read `UnitsSystem` from SettingsRepository
  - [ ] Convert m/s → km/h or mph for display
  - [ ] Convert meters → km or miles for display

- [ ] **Unit Test ViewModel**
  - [ ] Mock repositories
  - [ ] Verify state transitions (idle → recording → paused → stopped)
  - [ ] Test manual pause/resume coordination with auto-pause
  - [ ] Test units conversion

**Files Created**:
- `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`

---

### Phase 4: UI Implementation (Day 5)

- [ ] **Live Ride Screen**
  - [ ] Display duration (HH:MM:SS format)
  - [ ] Display distance (with units)
  - [ ] Display current speed (with units)
  - [ ] Display average speed (with units)
  - [ ] Display max speed (with units)
  - [ ] Show "Ride Paused" banner when paused

- [ ] **Control Buttons**
  - [ ] Start Ride button (FAB, visible when idle)
  - [ ] Pause button (bottom action bar, left position)
  - [ ] Resume button (replaces pause when paused)
  - [ ] Stop Ride button (bottom action bar, right position)

- [ ] **Wake Lock**
  - [ ] Implement `KeepScreenOn` composable (research.md §3)
  - [ ] Apply when `isRecording = true`
  - [ ] Release when backgrounded or stopped

- [ ] **Save/Discard Dialog**
  - [ ] Show when Stop tapped
  - [ ] "Save" button → finish ride, navigate to Review
  - [ ] "Discard" button → delete ride, return to Live tab

- [ ] **Review Screen**
  - [ ] Display final stats: duration, distance, avg speed, max speed
  - [ ] Respect user's units preference
  - [ ] Show map placeholder: "Map visualization coming in v0.4.0"
  - [ ] Back button returns to Live tab

- [ ] **Recovery Dialog**
  - [ ] On app launch, check for incomplete rides
  - [ ] Show dialog: "Recover incomplete ride?" [Discard] [Recover]
  - [ ] Recover: Mark ride complete, show Review screen
  - [ ] Discard: Delete ride

- [ ] **UI Tests**
  - [ ] Start ride → duration counts up
  - [ ] Pause ride → banner appears, stats freeze
  - [ ] Resume ride → banner disappears, stats resume
  - [ ] Stop ride → dialog appears
  - [ ] Save → navigate to Review screen

**Files Created**:
- `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt`
- `app/src/main/java/com/example/bikeredlights/ui/screens/ride/RideReviewScreen.kt`
- `app/src/main/java/com/example/bikeredlights/ui/screens/ride/SaveRideDialog.kt`
- `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatistics.kt`
- `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideControls.kt`

---

### Phase 5: Integration & Polish (Day 6-7)

- [ ] **Navigation Updates**
  - [ ] Update `AppNavigation.kt` to include RideReviewScreen
  - [ ] Pass `rideId` as navigation argument
  - [ ] Handle back navigation properly

- [ ] **Settings Integration**
  - [ ] Read GPS accuracy from SettingsRepository
  - [ ] Pass to LocationRequest (1000ms or 4000ms)
  - [ ] Update service when settings change mid-ride

- [ ] **Edge Case Handling**
  - [ ] GPS signal loss: Show "GPS Signal Lost" indicator
  - [ ] Storage full: Catch SQLiteFullException, stop recording
  - [ ] Permissions revoked: Stop service, show notification
  - [ ] Rapid start/stop: Enforce 5-second minimum duration

- [ ] **Emulator Testing** (MANDATORY)
  - [ ] Location simulation: Load GPX route, set 15-20 km/h playback
  - [ ] Start ride, verify duration/distance/speed update
  - [ ] Pause ride, verify GPS stops, stats freeze
  - [ ] Resume ride, verify GPS resumes
  - [ ] Background app, verify notification and tracking continues
  - [ ] Lock screen, verify tracking continues
  - [ ] Kill app process, verify service restarts
  - [ ] Revoke permissions mid-ride, verify graceful handling
  - [ ] Test dark mode, rotation, back navigation

- [ ] **Performance Validation**
  - [ ] Battery drain < 10%/hour with High Accuracy
  - [ ] GPS updates processed < 100ms
  - [ ] UI maintains 60fps
  - [ ] Database inserts non-blocking

- [ ] **Commit & Push**
  - [ ] Small, frequent commits (every feature/fix)
  - [ ] Push to GitHub regularly
  - [ ] Final commit: "feat: complete Core Ride Recording (F1A)"

---

## Key Technical Decisions

| Decision Area | Choice | Rationale |
|--------------|--------|-----------|
| Service Type | LifecycleService | Lifecycle-aware coroutines |
| Communication | StateFlow | Modern reactive pattern |
| Database | Room 2.6.1 + KSP | Fast annotation processing |
| Location API | FusedLocationProviderClient | Battery-efficient GPS |
| Wake Lock | FLAG_KEEP_SCREEN_ON | No permission required |
| Units Storage | Base units (meters, m/s) | Convert at UI layer |
| DI | Manual | Hilt deferred per Constitution |

---

## Testing Strategy

### Unit Tests (Target: 90%+ coverage)

**Test Files**:
- `RideRepositoryImplTest.kt`
- `LocationRepositoryImplTest.kt`
- `RideRecordingViewModelTest.kt`
- `CalculateDistanceUseCaseTest.kt`

**Key Tests**:
- Ride creation and finishing
- TrackPoint cascade delete
- Manual pause/resume state transitions
- Auto-pause detection and auto-resume
- Units conversion accuracy
- Error handling (storage full, permissions revoked)

### Instrumented Tests

**Test Files**:
- `RideRecordingServiceTest.kt`
- `LiveRideScreenTest.kt`
- `RideReviewScreenTest.kt`

**Key Tests**:
- Service lifecycle (start/stop)
- Notification creation and updates
- Location updates trigger UI recomposition
- Pause/resume button interactions
- Save/discard dialog flow

### Emulator Testing Checklist

Use GPX file with realistic cycling route:
```bash
# Extended Controls (...) → Location → Load GPX
# Set playback speed to 15-20 km/h
# Start ride in app, verify all functionality
```

---

## Dependencies

### Existing (Already in Project)

- Room 2.6.1
- Play Services Location 21.3.0
- Jetpack Compose BOM 2024.11.00
- Kotlin Coroutines 1.9.0
- DataStore Preferences 1.1.1 (F2A)

### New Permissions (Add to AndroidManifest.xml)

```xml
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
<uses-permission android:name="android.permission.WAKE_LOCK" />
```

**Note**: WAKE_LOCK permission added for completeness, but FLAG_KEEP_SCREEN_ON doesn't require it.

---

## Common Pitfalls & Solutions

### Pitfall 1: Service Killed by Android

**Symptom**: Recording stops when app backgrounded on some devices
**Solution**:
- Use `START_STICKY` (requests restart)
- Persist state continuously to database
- Show user how to disable battery optimization for app

### Pitfall 2: Wake Lock Not Released

**Symptom**: Screen stays on after stopping recording
**Solution**: Use `DisposableEffect` which guarantees cleanup

### Pitfall 3: Race Condition on Pause

**Symptom**: Manual pause and auto-pause both trigger simultaneously
**Solution**: Priority logic - manual pause disables auto-pause detection

### Pitfall 4: TrackPoint Validation Crash

**Symptom**: App crashes on TrackPoint insert with poor GPS
**Solution**: Validate accuracy < 50m before insert, skip invalid points

### Pitfall 5: Notification Doesn't Appear

**Symptom**: Service starts but no notification shows
**Solution**:
- Call `startForeground()` within 5 seconds of service start
- Request POST_NOTIFICATIONS permission on Android 13+

---

## Debugging Commands

```bash
# View service status
adb shell dumpsys activity services | grep RideRecordingService

# Monitor location updates
adb logcat | grep -E "Location|FusedLocation"

# Check notification channel
adb shell dumpsys notification | grep BikeRedlights

# Force Doze mode (test battery optimization)
adb shell dumpsys battery unplug
adb shell dumpsys deviceidle force-idle

# Kill app process (test service restart)
adb shell am kill com.example.bikeredlights

# Check database file
adb shell "run-as com.example.bikeredlights cat /data/data/com.example.bikeredlights/databases/bike_redlights.db" | xxd
```

---

## Success Criteria (From Spec)

- [SC-001] User can start ride and see duration counting within 10 seconds
- [SC-002] Recording survives 5+ minutes with screen locked
- [SC-003] Ride statistics accurate within 5% vs known routes
- [SC-004] Units conversion accurate within 1%
- [SC-005] Auto-pause triggers within 10 seconds of threshold
- [SC-006] Service survives 30+ minute rides when backgrounded
- [SC-007] 90%+ test coverage for ViewModels/UseCases/Repositories
- [SC-008] Battery drain < 10%/hour with High Accuracy GPS
- [SC-009] Database writes complete in < 100ms (non-blocking)
- [SC-010] Stop button responds within 1 second
- [SC-011] GPS signal loss handled gracefully (no crashes)
- [SC-012] Incomplete ride recovery works correctly

---

## Final Deliverables

- [ ] All code committed and pushed to GitHub
- [ ] Unit tests passing (90%+ coverage)
- [ ] Instrumented tests passing
- [ ] Emulator testing complete (all scenarios validated)
- [ ] TODO.md updated with completed feature
- [ ] RELEASE.md updated with v0.3.0 entry
- [ ] CLAUDE.md updated with new technology (Room database)
- [ ] Pull request created with detailed description
- [ ] APK built and tested on emulator

---

**Ready for Implementation**: All design artifacts complete. Begin Phase 0 (Database Foundation).
