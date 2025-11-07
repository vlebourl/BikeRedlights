# Research: Auto-Resume Bug Fix (Feature 004)

**Date**: 2025-11-07
**Feature**: Fix Auto-Resume Not Working After Auto-Pause
**Spec**: [spec.md](./spec.md)

## Executive Summary

Auto-resume functionality is **structurally broken** due to a logic placement error in `RideRecordingService.kt`. The auto-resume logic exists (lines 530-565) but is **unreachable** during the `AutoPaused` state due to a conditional gate that prevents its execution. This is a **high-severity architectural bug** requiring logic extraction and relocation, not a complex algorithmic fix.

---

## 1. Root Cause Analysis

### The Bug

**File**: `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`
**Lines**: 433-435 (conditional gate), 530-565 (unreachable auto-resume logic)

**Problem**: Auto-resume logic lives inside `updateRideDistance()` function, which is only called when ride is **not** auto-paused. This creates a Catch-22:

```kotlin
// Line 433-435: The problematic gate
if (!isManuallyPaused && !isAutoPaused) {
    updateRideDistance(rideId)  // Auto-resume logic is inside here!
}
```

**Why Auto-Pause Works But Auto-Resume Doesn't**:

| State | `isAutoPaused` | `updateRideDistance()` Called? | Auto-Pause Logic Executes? | Auto-Resume Logic Executes? |
|-------|----------------|--------------------------------|----------------------------|------------------------------|
| Recording | `false` | ✅ Yes | ✅ Yes (can detect low speed and pause) | N/A (not auto-paused yet) |
| AutoPaused | `true` | ❌ **No** | N/A (already paused) | ❌ **No** (unreachable!) |

**Smoking Gun Code** (lines 530-565):
```kotlin
is RideRecordingState.AutoPaused -> {
    // Check if speed went above resume threshold
    if (currentSpeed >= resumeThreshold) {
        // [... auto-resume logic ...]
    }
}
```
This code is inside `updateRideDistance()`, which is never called during `AutoPaused` state.

### Evidence from Real-World Testing

- User reported: Auto-pause triggers correctly (speed < 1 km/h) ✅
- User reported: Auto-resume **does not** trigger when moving again ❌
- User must manually tap Resume button while cycling (safety hazard) ⚠️

This confirms the code path analysis: auto-pause logic is reachable, auto-resume logic is not.

---

## 2. Technical Context

### Platform & Dependencies

| Component | Version | Notes |
|-----------|---------|-------|
| **Compile SDK** | 35 (Android 15) | Latest stable |
| **Target SDK** | 35 | Android 15 |
| **Min SDK** | 34 (Android 14+) | Conservative minimum |
| **Kotlin** | 2.0.21 | Latest stable |
| **Java** | 17 (OpenJDK 17) | Required for Kotlin 2.0+ |
| **Gradle** | 8.13 | Build system |
| **AGP** | 8.13.0 | Android Gradle Plugin |

**Key Libraries**:
- Jetpack Compose BOM: 2024.11.00 (UI framework)
- Room: 2.6.1 (local database with auto-generated SQL)
- Hilt: 2.51.1 (dependency injection)
- Kotlin Coroutines: 1.9.0 (async operations, Flow)
- DataStore: 1.1.1 (settings persistence)
- Play Services Location: 21.3.0 (GPS tracking via FusedLocationProviderClient)
- Lifecycle: 2.8.7 (ViewModel, StateFlow, lifecycle-aware components)

**Testing Stack**:
- JUnit 4.13.2 (unit test runner)
- MockK 1.13.13 (Kotlin mocking library)
- Turbine 1.2.0 (Flow testing utilities)
- Truth 1.4.4 (fluent assertions)
- AndroidX Test (instrumented tests)
- Compose UI Test (UI testing framework)

### Architecture Pattern

**MVVM + Clean Architecture** (4 layers):

```
┌─────────────────────────────────────┐
│ UI Layer (Jetpack Compose)          │
│ - LiveRideScreen.kt                 │  Shows "Auto-Paused" UI
│ - RideStatistics.kt                 │  Displays metrics
│ - Notification (via Service)        │  Shows "Auto-paused" text
└──────────────┬──────────────────────┘
               │ StateFlow<UiState>
┌──────────────▼──────────────────────┐
│ ViewModel Layer                     │
│ - RideRecordingViewModel.kt        │  Maps service state to UI state
│   * resumeRide() - manual resume   │
│   * updateUiStateFromRecordingState │
└──────────────┬──────────────────────┘
               │ StateFlow<RideRecordingState>
┌──────────────▼──────────────────────┐
│ Service Layer (Foreground Service)  │
│ - RideRecordingService.kt           │  GPS tracking + state machine
│   * startLocationTracking()         │  Collects GPS updates (1s or 4s)
│   * updateRideDistance()             │  🐛 BUG LOCATION
│   * checkAutoResume() [MISSING]     │  ❌ Needs to be extracted
└──────────────┬──────────────────────┘
               │ Calls use cases
┌──────────────▼──────────────────────┐
│ Data Layer (Repositories)           │
│ - RideRecordingStateRepository      │  Manages RideRecordingState
│ - RideRepository                    │  CRUD for Ride entities
│ - LocationRepository                │  GPS location updates (Flow)
└─────────────────────────────────────┘
```

### State Machine

**States** (`RideRecordingState` sealed class):
- `Idle` - No ride active
- `Recording(rideId: Long)` - Actively recording
- `ManuallyPaused(rideId: Long)` - User pressed Pause button
- `AutoPaused(rideId: Long)` - System detected low speed (< 1 km/h for threshold seconds)
- `Stopped(rideId: Long)` - User pressed Stop Ride

**Transitions**:
- `Idle → Recording` (user taps Start Ride)
- `Recording → ManuallyPaused` (user taps Pause)
- `Recording → AutoPaused` (speed < 1 km/h for 5-60 seconds) ✅ **Works**
- `AutoPaused → Recording` (speed > 1 km/h) ❌ **Broken**
- `ManuallyPaused → Recording` (user taps Resume) ✅ **Works**

### GPS Update Flow

**Location Collection** (lines 407-438 in RideRecordingService.kt):
```kotlin
locationRepository.getLocationUpdates()
    .collect { locationData ->
        val state = rideRecordingStateRepository.getCurrentState()
        val isManuallyPaused = state is RideRecordingState.ManuallyPaused
        val isAutoPaused = state is RideRecordingState.AutoPaused

        // 1. Record track point (works during auto-pause ✅)
        recordTrackPointUseCase(
            rideId = rideId,
            locationData = locationData,
            isManuallyPaused = isManuallyPaused,
            isAutoPaused = isAutoPaused
        )

        // 2. Calculate distance (BUG: skipped during auto-pause ❌)
        if (!isManuallyPaused && !isAutoPaused) {
            updateRideDistance(rideId)  // Auto-resume logic trapped inside!
        }
    }
```

**GPS Update Frequency**:
- **High Accuracy**: 1-second intervals (priority: `PRIORITY_HIGH_ACCURACY`)
- **Battery Saver**: 4-second intervals (priority: `PRIORITY_BALANCED_POWER_ACCURACY`)

**Data Persistence**:
- Track points are recorded **even during auto-pause** with `isAutoPaused=true` flag ✅
- This ensures continuous GPS breadcrumbs for later analysis
- Auto-pause duration is accumulated in `Ride.autoPausedDurationMillis`

---

## 3. Solution Design

### Decision: Extract Auto-Resume Logic

**Rationale**:
1. **Separation of Concerns**: Auto-resume detection is a state transition concern, not a distance calculation concern
2. **Execution Guarantee**: Extracting logic before the pause gate ensures it executes during `AutoPaused` state
3. **Code Clarity**: Separates "check if we should resume" from "calculate distance if recording"
4. **Minimal Risk**: Logic extraction preserves existing behavior, only changes execution order

**Alternatives Considered**:

| Alternative | Why Rejected |
|-------------|--------------|
| Modify conditional gate to allow `updateRideDistance()` during auto-pause | Violates single responsibility principle; risks breaking distance calculation invariants; harder to test |
| Add auto-resume check inside `recordTrackPointUseCase` | Wrong layer (use case shouldn't manage service state); creates tight coupling |
| Poll for auto-resume in separate coroutine | Unnecessary complexity; GPS updates already provide trigger events; wastes resources |

### Implementation Strategy

**New Function**: `checkAutoResume(rideId: Long, currentSpeed: Double)`

**Location**: `RideRecordingService.kt` (new private suspend function)

**Responsibilities**:
1. Check if auto-pause is enabled in settings
2. Compare current speed against resume threshold (> 1 km/h = 0.278 m/s)
3. Accumulate auto-paused duration before resuming
4. Transition state from `AutoPaused` to `Recording`
5. Update notification text from "Auto-paused" to "Recording..."
6. Reset state tracking variables (autoPauseStartTime, lowSpeedStartTime)
7. Log auto-resume event for debugging

**Integration Point** (line 436):
```kotlin
.collect { locationData ->
    // ... existing track point recording ...

    // NEW: Check for auto-resume FIRST (before pause gate)
    if (isAutoPaused) {
        checkAutoResume(rideId, locationData.speedMetersPerSec)
    }

    // EXISTING: Calculate distance if not paused
    if (!isManuallyPaused && !isAutoPaused) {
        updateRideDistance(rideId)  // Now only handles distance + auto-pause
    }
}
```

**Hysteresis Strategy** (to prevent flapping):
- Auto-pause: Requires speed < 1 km/h for **threshold seconds** (5-60s configurable) ✅ **Already implemented**
- Auto-resume: Requires speed > 1 km/h for **1 GPS reading** (immediate trigger)
- Rationale: Asymmetric thresholds prevent rapid pause/resume cycles
  - Starting movement is intentional (low false positive risk)
  - Stopping can be gradual (need time-based confirmation)

### Grace Period Interaction

**Grace Period** (30 seconds after manual resume):
- Purpose: Prevent auto-pause from immediately triggering after user manually resumes
- Implementation: `lastManualResumeTime` variable tracks last manual resume
- Auto-pause logic checks: `timeSinceManualResume < 30000` → suppress auto-pause

**Auto-Resume Compatibility**:
- Grace period **only affects auto-pause detection**, not auto-resume
- If auto-pause somehow triggers during grace period (edge case), auto-resume still works normally
- No code changes needed for grace period compatibility

---

## 4. Testing Strategy

### Unit Tests (Instrumented)

**File**: `app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt`

**Test Cases**:
1. **Auto-resume triggers on speed increase**
   - Setup: Ride in `AutoPaused` state, auto-pause enabled
   - Action: Send location update with speed > 1 km/h
   - Assert: State transitions to `Recording` within 2 seconds

2. **Auto-resume accumulates auto-pause duration**
   - Setup: Auto-pause for 10 seconds
   - Action: Trigger auto-resume
   - Assert: `Ride.autoPausedDurationMillis` increased by ~10,000ms

3. **Auto-resume updates notification**
   - Setup: Notification shows "Auto-paused"
   - Action: Trigger auto-resume
   - Assert: Notification text changes to "Recording..."

4. **Auto-resume doesn't trigger on GPS drift (< 1 km/h)**
   - Setup: Stationary with GPS reporting 0.5 km/h noise
   - Action: Send location updates for 30 seconds
   - Assert: State remains `AutoPaused`

5. **Manual resume takes precedence over auto-resume**
   - Setup: Ride auto-paused
   - Action: User taps Resume button before auto-resume trigger
   - Assert: Manual resume succeeds, grace period starts

6. **Auto-resume works in Battery Saver mode (4s intervals)**
   - Setup: GPS accuracy = Battery Saver
   - Action: Send location updates every 4 seconds with speed > 1 km/h
   - Assert: Auto-resume triggers within 8 seconds (2 GPS updates)

### Integration Tests (Emulator)

**Test Scenario**: Stop-and-go traffic simulation

1. Enable auto-pause with 5-second threshold
2. Start ride recording
3. Simulate GPS route with speed sequence:
   - 5 km/h (moving) → 10s
   - 0.5 km/h (stopped at red light) → 7s → auto-pause triggers
   - 2 km/h (accelerating) → auto-resume should trigger within 2s
   - 15 km/h (riding) → 20s
4. Verify:
   - Auto-pause triggered after 7s of low speed ✅
   - Auto-resume triggered within 2s of movement ✅
   - Total auto-paused duration = ~7 seconds ✅
   - Ride statistics exclude auto-paused time ✅

**Tools**:
- Android emulator mock location provider
- GPX file with stop-and-go route
- adb shell commands to inject GPS coordinates

### Physical Device Testing (Field Validation)

**Required**: Minimum 5 real bike rides with auto-pause enabled

**Test Conditions**:
1. Urban stop-and-go (traffic lights, stop signs)
2. High Accuracy GPS mode (1s intervals)
3. Battery Saver GPS mode (4s intervals)
4. Poor GPS signal areas (tunnels, urban canyons)
5. Rapid speed fluctuations (slow hill climbing, coasting)

**Success Criteria**:
- Auto-resume success rate > 95% (at least 19/20 stops auto-resume correctly)
- Auto-resume latency < 2s (High Accuracy) or < 8s (Battery Saver) in 95% of cases
- Zero false auto-resumes during stationary periods

---

## 5. Performance & Constraints

### GPS Update Latency

| GPS Mode | Update Interval | Auto-Resume Detection Latency |
|----------|-----------------|-------------------------------|
| High Accuracy | 1 second | < 2 seconds (2 GPS readings) |
| Battery Saver | 4 seconds | < 8 seconds (2 GPS readings) |

**Rationale for "2 GPS readings" requirement**:
- First reading: Speed increases above threshold → trigger auto-resume
- Second reading: Confirm movement is sustained (hysteresis)
- Prevents false triggers from GPS noise or single-spike errors

### Battery Impact

**No additional power consumption** from this fix:
- GPS tracking already continues during auto-pause (foreground service runs)
- Track points are already recorded during auto-pause (`isAutoPaused=true` flag)
- Fix only relocates existing logic execution, doesn't add new work

### Foreground Service Guarantees

**Service Protection**:
- Foreground service with persistent notification (Android won't kill it)
- Wake lock acquired during recording (screen can sleep, service continues)
- Process death recovery via DataStore (current state persisted)

**Auto-Resume Resilience**:
- If app process dies during auto-pause, service restarts automatically
- Service reads current state from DataStore → resumes in `AutoPaused` state
- Next GPS update triggers auto-resume check → normal operation resumes

---

## 6. Files to Modify

### Primary Changes

1. **`app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`**
   - Extract `checkAutoResume()` function (new, ~40 lines)
   - Relocate auto-resume logic from `updateRideDistance()` to `startLocationTracking()`
   - Update: Lines 436-438 (add `checkAutoResume()` call before pause gate)
   - Update: Lines 530-565 (remove auto-resume logic, keep only auto-pause logic)
   - Estimated lines changed: ~50 lines (additions + deletions)

### Testing Changes

2. **`app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt`**
   - Add 6 new test cases for auto-resume scenarios
   - Estimated lines added: ~200 lines (comprehensive test coverage)

### Documentation Updates (Automatic per Constitution)

3. **`TODO.md`**
   - Move "Bug: Auto-Resume Not Working" from "Planned" to "In Progress" → "Completed"

4. **`RELEASE.md`**
   - Add entry to "Unreleased" section: "Bug Fix: Auto-resume now works after auto-pause"

---

## 7. Risks & Mitigations

### High Risk: Auto-Resume Still Doesn't Work After Fix

**Scenario**: Fix is deployed but bug persists due to misidentified root cause

**Mitigation**:
1. **Pre-Implementation Validation**: Add comprehensive logging to current codebase to confirm root cause analysis before writing fix
2. **Incremental Testing**: Test fix on emulator with GPS simulation before physical device testing
3. **Logging Strategy**: Add debug logs at every state transition to isolate exact failure point if regression occurs

### Medium Risk: Auto-Resume Introduces New Bugs in Manual Pause/Resume

**Scenario**: Extracting logic breaks manual resume or grace period functionality

**Mitigation**:
1. **Regression Testing**: Extensive manual pause/resume tests before release
2. **Code Isolation**: Keep auto-resume logic separate from manual resume logic (no shared code paths)
3. **Test Coverage**: Add tests for manual resume → auto-pause → manual resume sequences

### Medium Risk: GPS Noise Causes Auto-Resume Flapping

**Scenario**: Stationary GPS drift (0.5-1.5 km/h) triggers false auto-resumes

**Mitigation**:
1. **Hysteresis**: Require 2 consecutive GPS readings > 1 km/h before auto-resume
2. **Distance Threshold**: Alternative: require movement distance > 10 meters over 2 readings
3. **Field Testing**: Physical device testing in various GPS conditions (urban canyon, open road, forest)

### Low Risk: Different GPS Accuracy Modes Behave Inconsistently

**Scenario**: Auto-resume works in High Accuracy but fails in Battery Saver mode

**Mitigation**:
1. **Dual Mode Testing**: Test both High Accuracy and Battery Saver modes with identical scenarios
2. **Timing Validation**: Document expected latency differences (2s vs 8s) in user-facing documentation
3. **Emulator Testing**: Use emulator to simulate both GPS update frequencies

---

## 8. Success Criteria Validation

### How We'll Measure Success

**From spec.md (SC-001 to SC-007)**:

| Success Criterion | Measurement Method | Target |
|-------------------|-------------------|--------|
| SC-001: Auto-resume works 100% of time when speed > 1 km/h | Field testing: 20+ stop-and-go scenarios | 100% success rate |
| SC-002: Auto-resume triggers within latency targets | Stopwatch timing during tests | 95% within 2s (HA) / 8s (BS) |
| SC-003: GPS drift doesn't cause false auto-resume | Stationary testing: 100+ stationary periods | <1% false positive rate |
| SC-004: 90% reduction in manual phone interactions | User feedback: before vs after patch | 90% reduction reported |
| SC-005: Zero "auto-resume not working" bug reports | GitHub issues tracking | 0 reports in 30 days |
| SC-006: Auto-paused duration accurate within 1s | Database validation: compare expected vs actual | ±1 second accuracy |
| SC-007: 95% real-world success rate | Physical device testing: 20+ rides | 95% success (19/20 stops) |

### Pre-Release Validation Checklist

Before releasing v0.4.1:
- [ ] All 6 unit tests pass
- [ ] Emulator integration test (10 stops, 10 resumes) passes
- [ ] Manual regression testing (manual pause/resume still works)
- [ ] Physical device testing: 5 rides with auto-pause enabled, all auto-resumes successful
- [ ] Code review completed
- [ ] RELEASE.md updated with bug fix entry
- [ ] TODO.md moved to "Completed" section

---

## 9. Timeline & Effort Estimate

| Phase | Tasks | Estimated Time |
|-------|-------|----------------|
| **Implementation** | Extract `checkAutoResume()`, relocate call, remove old logic | 1-2 hours |
| **Unit Testing** | Write 6 test cases, debug failures | 1 hour |
| **Emulator Testing** | GPS simulation, route playback | 30 minutes |
| **Physical Device Testing** | 5 real bike rides with validation | 3-5 hours (spread over 3-5 days) |
| **Code Review & Cleanup** | Review, address feedback, finalize docs | 1 hour |
| **Total** | | **6.5-9.5 hours** |

**Target Release**: v0.4.1 (patch release)
**Release Timeline**: 3-7 days after implementation starts (depends on field testing weather/availability)

---

## 10. Post-Release Monitoring

### Metrics to Track

1. **Bug Reports**: Monitor GitHub issues for "auto-resume" mentions (target: 0 in 30 days)
2. **User Reviews**: Track Play Store reviews mentioning auto-pause/resume (target: >80% positive sentiment)
3. **Telemetry** (if implemented): Auto-resume success rate in production (target: >95%)
4. **Support Tickets**: Track support volume for pause/resume issues (target: 50% reduction vs v0.4.0)

### Rollback Plan

If critical issues are discovered post-release:
1. Revert commit with auto-resume fix
2. Release v0.4.2 with revert
3. Document issue in TODO.md as "Deferred"
4. Re-investigate root cause with additional logging

---

## Conclusion

This bug is **high-severity** (safety-critical) but **low-complexity** (logic extraction). The root cause is clear, the solution is straightforward, and the risk is manageable with comprehensive testing. Estimated effort is 6.5-9.5 hours including field validation.

**Recommended Action**: Proceed to Phase 1 (Design & Contracts) to finalize implementation plan and generate task breakdown.
