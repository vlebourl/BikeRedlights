# Tasks: Fix Auto-Resume After Auto-Pause

**Feature**: 004-fix-auto-resume
**Branch**: `004-fix-auto-resume`
**Spec**: [spec.md](./spec.md)
**Plan**: [plan.md](./plan.md)

---

## Task Overview

**Total Estimated Effort**: 6.5-9.5 hours active development + 3-7 days for physical testing (5 real bike rides)

**User Stories**:
- **US1 (P1)**: Automatic Resume After Stop - Core auto-resume functionality
- **US2 (P2)**: Grace Period Respects Movement - Manual resume behavior preserved
- **US3 (P3)**: GPS Accuracy Modes Work - Battery Saver GPS compatibility

**Organization**: Tasks grouped by user story for independent implementation/testing

---

## Phase 1: Setup & Prerequisites

### Environment Setup

- [x] T001 [P] Setup: Create feature branch `004-fix-auto-resume` from main
  - `git checkout -b 004-fix-auto-resume`

- [x] T002 [P] Setup: Verify development environment (Java 17, Kotlin 2.0.21)
  - Run `java -version` and `./gradlew --version`

- [x] T003 [P] Setup: Update TODO.md to mark feature as "In Progress"
  - File: `TODO.md`

---

## Phase 2: Foundational Implementation (Blocking)

### Core Service Logic

- [x] T004 [US1] Implement: Extract `checkAutoResume()` function in RideRecordingService
  - File: `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`
  - Location: After `startLocationTracking()` function (line 440)
  - Signature: `private suspend fun checkAutoResume(rideId: Long, currentSpeed: Double)`
  - Implementation:
    - Check `autoPauseConfig.enabled` feature toggle
    - Check speed threshold (0.278 m/s = 1 km/h)
    - Accumulate auto-paused duration to `Ride.autoPausedDurationMillis`
    - Transition state from `AutoPaused` → `Recording`
    - Reset tracking variables (`autoPauseStartTime`, `lastManualResumeTime`, `lowSpeedStartTime`)
    - Update notification to "Recording..."
    - **Log debug event (FR-012)**: Include timestamp, speed (m/s), GPS accuracy (m), rideId in log message
      - Example: `Log.d("RideRecordingService", "Auto-resume triggered: rideId=$rideId speed=${currentSpeed}m/s >= threshold=0.278m/s")`
  - Reference: `specs/004-fix-auto-resume/quickstart.md` lines 78-125

- [x] T005 [US1] Integrate: Call `checkAutoResume()` before pause gate in `startLocationTracking()`
  - File: `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`
  - Location: Inside `locationRepository.getLocationUpdates().collect` block (line 434-436)
  - Code change:
    ```kotlin
    // After recording track point
    recordTrackPointUseCase(...)

    // NEW: Check for auto-resume (before pause gate)
    if (isAutoPaused) {
        val currentSpeed = (locationData.speedMps ?: 0f).toDouble()
        checkAutoResume(rideId, currentSpeed)
    }

    // Existing distance calculation (pause gate)
    if (!isManuallyPaused && !isAutoPaused) {
        updateRideDistance(rideId)
    }
    ```
  - Reference: `specs/004-fix-auto-resume/contracts/service-interface.md` lines 166-191

- [x] T006 [US1] Cleanup: Remove unreachable auto-resume logic from `updateRideDistance()`
  - File: `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`
  - Location: Lines 598-633 (inside `updateRideDistance()`)
  - Action: Deleted entire `is RideRecordingState.AutoPaused` case (now in `checkAutoResume()`)
  - Rationale: Logic was structurally unreachable; moved to proper location in T005

- [x] T007 [P] Build: Compile project and verify no compilation errors
  - Command: `./gradlew assembleDebug`
  - Expected: Build succeeds with 0 errors ✅ BUILD SUCCESSFUL

---

## Phase 3: User Story 1 - Automatic Resume After Stop (P1)

### Unit Testing

- [ ] T008 [US1] Test: Auto-resume triggers when speed exceeds 1 km/h
  - File: `app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt`
  - Test name: `when speed exceeds 1 kmh during auto-pause, auto-resume triggers`
  - Setup: Start ride, trigger auto-pause
  - Action: Send location update with speed = 1.5 m/s (> 1 km/h)
  - Assert: State transitions to `Recording` within 2 seconds
  - Reference: `specs/004-fix-auto-resume/contracts/service-interface.md` lines 295-312

- [ ] T009 [US1] Test: Auto-paused duration accumulates correctly
  - File: `app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt`
  - Test name: `when auto-resume triggers, auto-paused duration is accumulated`
  - Setup: Start ride, trigger auto-pause
  - Action: Wait 10 seconds, send speed = 2.0 m/s to trigger auto-resume
  - Assert: `Ride.autoPausedDurationMillis` is 9900-10100ms (±100ms tolerance)
  - Reference: `specs/004-fix-auto-resume/contracts/service-interface.md` lines 314-331

- [ ] T010 [US1] Test: Auto-resume does NOT trigger on GPS drift
  - File: `app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt`
  - Test name: `when speed is below 1 kmh during auto-pause, auto-resume does not trigger`
  - Setup: Start ride, trigger auto-pause
  - Action: Send 10 location updates with speed = 0.5 m/s (< 1 km/h) over 10 seconds
  - Assert: State remains `AutoPaused` (no auto-resume)
  - Reference: `specs/004-fix-auto-resume/contracts/service-interface.md` lines 333-352

### Emulator Validation (US1)

- [ ] T011 [US1] Emulator: Test basic auto-resume with GPS simulation
  - Platform: Android Emulator (API 34, Pixel 6)
  - Setup: Install debug build, enable High Accuracy GPS mode
  - Test Steps:
    1. Start ride
    2. Simulate movement at 15 km/h for 30 seconds (GPX playback)
    3. Simulate stop at 0.5 km/h for 10 seconds → auto-pause triggers after 5s
    4. Simulate movement at 15 km/h again → auto-resume should trigger within 2s
  - Success Criteria:
    - Notification text changes from "Auto-paused" to "Recording..." ✅
    - Live tab removes "Auto-Paused" indicator ✅
    - Distance calculation resumes (distance increases) ✅
  - Reference: `specs/004-fix-auto-resume/contracts/service-interface.md` lines 376-386

---

## Phase 4: User Story 2 - Grace Period Respects Movement (P2)

### Unit Testing

- [ ] T012 [US2] Test: Manual resume overrides auto-resume
  - File: `app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt`
  - Test name: `when user manually resumes during auto-pause, manual resume overrides auto-resume`
  - Setup: Start ride, trigger auto-pause
  - Action: Call `service.resumeRide()` (user taps Resume button)
  - Assert: State transitions to `Recording`, `lastManualResumeTime > 0` (grace period active)
  - Reference: `specs/004-fix-auto-resume/contracts/service-interface.md` lines 354-372

- [ ] T013 [US2] Test: Grace period prevents immediate re-auto-pause after manual resume
  - File: `app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt`
  - Test name: `when user manually resumes, grace period prevents auto-pause for N seconds`
  - Setup: Start ride, trigger auto-pause, manually resume
  - Action: Send low-speed location updates (< 1 km/h) during grace period
  - Assert: State remains `Recording` (auto-pause blocked by grace period)
  - Reference: `specs/004-fix-auto-resume/spec.md` lines 120-131 (FR-007)

### Emulator Validation (US2)

- [ ] T014 [US2] Emulator: Test manual resume during auto-pause
  - Platform: Android Emulator (API 34)
  - Setup: Install debug build
  - Test Steps:
    1. Start ride, simulate movement until auto-pause triggers
    2. Tap "Resume" button while still stationary
    3. Send low-speed GPS updates for grace period duration
    4. Verify auto-pause does NOT re-trigger during grace period
  - Success Criteria:
    - Manual resume takes precedence ✅
    - Grace period prevents immediate re-auto-pause ✅
    - Auto-pause works normally after grace period expires ✅

---

## Phase 5: User Story 3 - GPS Accuracy Modes Work (P3)

### Unit Testing

- [ ] T015 [US3] Test: Auto-resume works with Battery Saver GPS mode
  - File: `app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt`
  - Test name: `when GPS in battery saver mode, auto-resume still triggers within latency target`
  - Setup: Start ride with Battery Saver GPS priority, trigger auto-pause
  - Action: Send location update with 4-second interval (battery saver cadence), speed = 2.0 m/s
  - Assert: Auto-resume triggers within 8 seconds (95th percentile target)
  - Reference: `specs/004-fix-auto-resume/contracts/service-interface.md` lines 391-397

### Emulator Validation (US3)

- [ ] T016 [US3] Emulator: Test with Battery Saver GPS mode
  - Platform: Android Emulator (API 34)
  - Setup: Install debug build, configure Battery Saver GPS in settings
  - Test Steps:
    1. Start ride
    2. Simulate movement → auto-pause → movement cycle
    3. Monitor auto-resume latency (should be < 8 seconds)
  - Success Criteria:
    - Auto-resume triggers despite slower GPS update interval ✅
    - Latency within 8-second target ✅
  - Reference: `specs/004-fix-auto-resume/spec.md` lines 175-181 (SC-003)

---

## Phase 6: Physical Device Testing (All User Stories)

### Real-World Validation

- [ ] T017 Physical: Test Ride 1 - Basic auto-resume functionality
  - Device: Physical Android phone (production environment)
  - Scenario: Ride at normal speed → stop at traffic light → resume riding
  - Duration: 15-20 minutes with 2-3 stop cycles
  - Success Criteria:
    - Auto-pause triggers after 5 seconds of stopping ✅
    - Auto-resume triggers within 2 seconds of starting movement ✅
    - No false auto-resumes while stationary ✅
  - Reference: `specs/004-fix-auto-resume/quickstart.md` lines 197-208

- [ ] T018 Physical: Test Ride 2 - Multiple auto-pause/resume cycles
  - Device: Physical Android phone
  - Scenario: Urban ride with 5+ traffic lights
  - Duration: 30 minutes
  - Success Criteria:
    - All auto-pause/resume cycles work correctly ✅
    - Total auto-paused duration accumulates accurately ✅
    - Distance calculation resumes after each auto-resume ✅
  - Reference: `specs/004-fix-auto-resume/spec.md` lines 147-153 (SC-001)

- [ ] T019 Physical: Test Ride 3 - Manual resume during auto-pause
  - Device: Physical Android phone
  - Scenario: Stop at light, manually resume before movement starts
  - Duration: 20 minutes
  - Success Criteria:
    - Manual resume works immediately (no delay) ✅
    - Grace period prevents re-auto-pause when still slow ✅
    - Auto-pause resumes normal operation after grace period ✅
  - Reference: `specs/004-fix-auto-resume/spec.md` lines 191-202 (EC-002)

- [ ] T020 Physical: Test Ride 4 - Poor GPS accuracy conditions
  - Device: Physical Android phone
  - Scenario: Ride through areas with poor GPS (urban canyons, tunnels)
  - Duration: 25 minutes
  - Success Criteria:
    - No false auto-resumes due to GPS noise ✅
    - Auto-resume still works when signal recovers ✅
    - App handles GPS loss gracefully (no crashes) ✅
  - Reference: `specs/004-fix-auto-resume/spec.md` lines 227-234 (EC-005)

- [ ] T021 Physical: Test Ride 5 - Battery Saver GPS mode
  - Device: Physical Android phone with Battery Saver GPS enabled
  - Scenario: Normal ride with auto-pause/resume cycles
  - Duration: 30 minutes
  - Success Criteria:
    - Auto-resume triggers within 8-second latency target ✅
    - Distance accuracy acceptable (within 5% of High Accuracy) ✅
    - Battery usage reasonable (no excessive drain) ✅
  - Reference: `specs/004-fix-auto-resume/spec.md` lines 175-181 (SC-003)

---

## Phase 7: Documentation & Release

### Documentation Updates

- [ ] T022 [P] Docs: Update TODO.md to mark feature as completed
  - File: `TODO.md`
  - Action: Move "Feature 004: Fix Auto-Resume" from "In Progress" to "Completed"
  - Add completion date and summary of changes

- [ ] T023 [P] Docs: Update RELEASE.md with bug fix entry
  - File: `RELEASE.md`
  - Section: "Unreleased" → "Bugs Fixed"
  - Entry:
    ```markdown
    - **Fix auto-resume not working after auto-pause** (Critical bug fix)
      - Auto-resume now triggers within 2s (High Accuracy GPS) when movement detected
      - Manual resume during auto-pause works correctly with grace period
      - Compatible with Battery Saver GPS mode (8s latency)
      - Fixes safety hazard requiring manual phone interaction while cycling
    ```

### Code Review & Merge Preparation

- [ ] T024 [P] Review: Self-review code changes against checklist
  - Verify all constitution requirements met:
    - ✅ Follows Kotlin coding conventions
    - ✅ MVVM architecture preserved
    - ✅ Small, logical commits (T025-T027)
    - ✅ Tests written and passing (T008-T016)
    - ✅ Emulator testing completed (T011, T014, T016)
    - ✅ Physical device testing completed (T017-T021)
    - ✅ TODO.md and RELEASE.md updated (T022-T023)
  - Reference: `CLAUDE.md` lines 321-343 (Code Review Checklist)

- [ ] T025 [P] Commit: Service implementation changes
  - Files: `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt`
  - Message:
    ```
    fix(service): implement auto-resume after auto-pause

    - Extract checkAutoResume() function with speed threshold detection
    - Relocate auto-resume call before pause gate in location flow
    - Remove unreachable auto-resume logic from updateRideDistance()
    - Accumulate auto-paused duration correctly on state transition

    Fixes critical bug where auto-resume never triggered, requiring
    manual phone interaction while cycling (safety hazard).

    Refs: specs/004-fix-auto-resume/spec.md (US1, US2, US3)
    ```

- [ ] T026 [P] Commit: Test implementation
  - Files: `app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt`
  - Message:
    ```
    test(service): add auto-resume instrumented tests

    - Test auto-resume triggers on speed increase (US1)
    - Test auto-paused duration accumulation (US1)
    - Test GPS drift does not cause false auto-resume (US1)
    - Test manual resume override (US2)
    - Test grace period behavior (US2)
    - Test Battery Saver GPS mode (US3)

    All tests passing. Validated on emulator and 5 physical rides.
    ```

- [ ] T027 [P] Commit: Documentation updates
  - Files: `TODO.md`, `RELEASE.md`
  - Message:
    ```
    docs: update TODO.md and RELEASE.md for auto-resume fix

    - Mark Feature 004 as completed in TODO.md
    - Add bug fix entry to RELEASE.md Unreleased section
    - Document testing completion (emulator + 5 physical rides)
    ```

- [ ] T028 [P] Push: Push branch to GitHub
  - Command: `git push -u origin 004-fix-auto-resume`
  - Verify: Branch appears on GitHub remote

### Pull Request & Release

- [ ] T029 [P] PR: Create pull request to main
  - Title: "Fix: Auto-Resume Not Working After Auto-Pause (P0 - Critical)"
  - Description:
    ```markdown
    ## Summary
    Fixes critical bug where auto-resume does not trigger after auto-pause, forcing cyclists to manually interact with phone while riding (safety hazard).

    ## Root Cause
    Auto-resume logic (RideRecordingService.kt:530-565) was structurally unreachable due to conditional gate preventing execution during AutoPaused state.

    ## Solution
    - Extracted `checkAutoResume()` function
    - Relocated call to execute BEFORE pause gate in location update flow
    - Removed unreachable dead code

    ## Changes
    - Modified: `app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt` (~50 LOC)
    - Added: 6 instrumented tests in `RideRecordingServiceTest.kt` (~200 LOC)
    - Updated: `TODO.md`, `RELEASE.md`

    ## Testing
    - ✅ All unit tests passing (6/6)
    - ✅ Emulator testing completed (3 scenarios)
    - ✅ Physical device testing completed (5 real bike rides)

    ## User Stories Implemented
    - US1 (P1): Automatic Resume After Stop
    - US2 (P2): Grace Period Respects Movement
    - US3 (P3): GPS Accuracy Modes Work

    ## Specification
    See `specs/004-fix-auto-resume/spec.md` for full details.
    ```
  - Link to spec: `specs/004-fix-auto-resume/spec.md`

- [ ] T030 Release: Version bump and release preparation
  - After PR approval and merge, follow release workflow per `CLAUDE.md` lines 463-539:
    1. Update RELEASE.md: Move "Unreleased" to "v0.4.1" section
    2. Update `app/build.gradle.kts`:
       - `versionCode = 401` (0 * 10000 + 4 * 100 + 1)
       - `versionName = "0.4.1"`
    3. Commit: `git commit -m "chore: bump version to v0.4.1"`
    4. Tag: `git tag -a v0.4.1 -m "Release v0.4.1: Fix Auto-Resume Bug"`
    5. Push tag: `git push origin v0.4.1`
    6. Build APK: `./gradlew assembleRelease`
    7. Create GitHub Release with APK attachment

---

## Task Dependencies

**Critical Path** (must complete in order):
1. T001-T003 (Setup) → T004-T007 (Core Implementation) → T008-T010 (US1 Tests) → T011 (US1 Emulator)
2. T012-T014 (US2) can proceed after T007 (parallel to US1 validation)
3. T015-T016 (US3) can proceed after T007 (parallel to US1/US2)
4. T017-T021 (Physical Testing) requires T007 + at least one emulator test passing
5. T022-T030 (Docs & Release) requires ALL testing phases complete

**Parallelizable Tasks**:
- T002 (Environment Check) parallel to T001 (Branch Creation)
- T012-T014 (US2) parallel to T011 (US1 Emulator)
- T015-T016 (US3) parallel to T011, T014
- T022-T023 (Docs) parallel to T024-T027 (Commits)

---

## Completion Criteria

Feature is **DONE** when:
- ✅ All 30 tasks completed
- ✅ All unit tests passing (6/6)
- ✅ All emulator tests passing (3 scenarios)
- ✅ All physical device tests passing (5 rides)
- ✅ TODO.md and RELEASE.md updated
- ✅ Pull request approved and merged
- ✅ Release v0.4.1 published to GitHub with signed APK

**Estimated Completion**: 6.5-9.5 hours active development + 3-7 days physical testing (excluding PR review wait time)
