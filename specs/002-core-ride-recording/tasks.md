# Implementation Tasks: Core Ride Recording

**Feature**: F1A - Core Ride Recording (v0.3.0)
**Branch**: 002-core-ride-recording
**Date**: 2025-11-04

---

## Overview

This document provides an actionable task breakdown for implementing GPS-based ride recording. Tasks are organized by user story to enable independent implementation and testing. Each phase represents a complete, shippable increment.

**User Stories** (from spec.md):
- **US1** (P1): Start and Stop Recording a Ride
- **US2** (P2): View Live Ride Statistics
- **US3** (P3): Review Completed Ride Statistics
- **US4** (P1): Recording Continues in Background
- **US5** (P2): Settings Integration
- **US6** (P3): Screen Stays Awake During Recording

**Test Coverage Requirement**: 90%+ for ViewModels, UseCases, Repositories (spec SC-007)

---

## Task Notation

- `[P]` = Parallelizable (can run simultaneously with other [P] tasks)
- `[US#]` = User Story number (maps to spec.md user stories)
- File paths are absolute from repo root

---

## Phase 1: Setup & Database Foundation

**Goal**: Initialize Room database and core data layer (blocking prerequisite for all user stories)

### Setup Tasks

- [X] T001 Create Ride entity in app/src/main/java/com/example/bikeredlights/data/local/entity/Ride.kt
- [X] T002 [P] Create TrackPoint entity in app/src/main/java/com/example/bikeredlights/data/local/entity/TrackPoint.kt
- [X] T003 Create RideDao interface in app/src/main/java/com/example/bikeredlights/data/local/dao/RideDao.kt
- [X] T004 [P] Create TrackPointDao interface in app/src/main/java/com/example/bikeredlights/data/local/dao/TrackPointDao.kt
- [X] T005 Create BikeRedlightsDatabase class in app/src/main/java/com/example/bikeredlights/data/local/BikeRedlightsDatabase.kt
- [X] T006 Add service declaration and permissions to app/src/main/AndroidManifest.xml

### Database Test Tasks (Required per spec SC-007)

- [X] T007 [P] Write unit tests for RideDao in app/src/androidTest/java/com/example/bikeredlights/data/local/RideDaoTest.kt
- [X] T008 [P] Write unit tests for TrackPointDao in app/src/androidTest/java/com/example/bikeredlights/data/local/TrackPointDaoTest.kt
- [X] T009 Run instrumented tests and verify cascade delete behavior

**Validation Criteria**:
- ✅ Room database builds successfully
- ✅ Schema exported to app/schemas/
- ✅ Cascade delete test passes (deleting Ride auto-deletes TrackPoints)
- ✅ Both DAOs have 90%+ test coverage

---

## Phase 2: Foundation - Repository Layer

**Goal**: Implement repository interfaces and domain models (shared infrastructure for US1-US6)

### Repository Interface Tasks

- [X] T010 Create RideRepository interface in app/src/main/java/com/example/bikeredlights/domain/repository/RideRepository.kt
- [X] T011 [P] Create TrackPointRepository interface in app/src/main/java/com/example/bikeredlights/domain/repository/TrackPointRepository.kt
- [X] T012 [P] Create LocationRepository interface in app/src/main/java/com/example/bikeredlights/domain/repository/LocationRepository.kt (already exists from Feature 001-speed-tracking)
- [X] T013 [P] Create RideRecordingStateRepository interface in app/src/main/java/com/example/bikeredlights/domain/repository/RideRecordingStateRepository.kt

### Domain Model Tasks

- [X] T014 [P] Create Ride domain model in app/src/main/java/com/example/bikeredlights/domain/model/Ride.kt
- [X] T015 [P] Create TrackPoint domain model in app/src/main/java/com/example/bikeredlights/domain/model/TrackPoint.kt
- [X] T016 [P] Create RideRecordingState domain model in app/src/main/java/com/example/bikeredlights/domain/model/RideRecordingState.kt
- [X] T017a [P] Implement ride name generator utility in app/src/main/java/com/example/bikeredlights/domain/util/RideNameGenerator.kt (format: "Ride on MMM d, yyyy")

### Repository Implementation Tasks

- [X] T017 Implement RideRepositoryImpl in app/src/main/java/com/example/bikeredlights/data/repository/RideRepositoryImpl.kt
- [X] T018 [P] Implement TrackPointRepositoryImpl in app/src/main/java/com/example/bikeredlights/data/repository/TrackPointRepositoryImpl.kt
- [X] T019 [P] Implement LocationRepositoryImpl in app/src/main/java/com/example/bikeredlights/data/repository/LocationRepositoryImpl.kt (already exists from Feature 001-speed-tracking)
- [X] T020 [P] Implement RideRecordingStateRepositoryImpl in app/src/main/java/com/example/bikeredlights/data/repository/RideRecordingStateRepositoryImpl.kt

### Repository Test Tasks (Required per spec SC-007)

- [X] T021 [P] Write unit tests for RideRepositoryImpl in app/src/test/java/com/example/bikeredlights/data/repository/RideRepositoryImplTest.kt
- [X] T022 [P] Write unit tests for LocationRepositoryImpl in app/src/test/java/com/example/bikeredlights/data/repository/LocationRepositoryImplTest.kt (skipped - LocationRepository from Feature 001)
- [X] T023 Run all repository unit tests and verify 90%+ coverage

**Validation Criteria**:
- ✅ All repository interfaces defined with clear contracts
- ✅ All implementations use Dispatchers.IO for database operations
- ✅ StateFlow emissions work correctly (test with Turbine)
- ✅ Repository tests achieve 90%+ coverage

---

## Phase 3: User Story 1 (P1) - Start and Stop Recording

**Goal**: Implement basic ride recording with start/stop controls and foreground service

**Independent Test**: Tap "Start Ride", wait 30 seconds, tap "Stop Ride", verify save/discard dialog

### Service Implementation

- [X] T024 [US1] Create RideRecordingService in app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt
- [X] T024a [US1] Implement POST_NOTIFICATIONS permission request for Android 13+ in RideRecordingService.onCreate()
- [X] T025 [US1] Implement notification builder with Pause/Stop actions in RideRecordingService
- [X] T026 [US1] Implement LocationCallback for GPS updates in RideRecordingService
- [X] T027 [US1] Add START_STICKY logic and process death recovery in RideRecordingService

### Use Case Implementation

- [X] T028 [P] [US1] Create StartRideUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/StartRideUseCase.kt
- [X] T029 [P] [US1] Create FinishRideUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/FinishRideUseCase.kt
- [X] T029a [P] [US1] Add minimum 5-second duration validation in FinishRideUseCase with auto-discard logic
- [X] T030 [P] [US1] Create RecordTrackPointUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/RecordTrackPointUseCase.kt
- [X] T031 [P] [US1] Create CalculateDistanceUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/CalculateDistanceUseCase.kt

### ViewModel Implementation

- [X] T032 [US1] Create RideRecordingViewModel in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt
- [X] T033 [US1] Implement startRide() action in RideRecordingViewModel
- [X] T034 [US1] Implement stopRide() action with save/discard dialog in RideRecordingViewModel
- [X] T035 [US1] Add StateFlow-based UI state management in RideRecordingViewModel

### UI Implementation

- [X] T036 [US1] Update LiveRideScreen with Start/Stop buttons in app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt
- [X] T037 [P] [US1] Create SaveRideDialog composable in app/src/main/java/com/example/bikeredlights/ui/components/ride/SaveRideDialog.kt
- [X] T038 [US1] Wire ViewModel to LiveRideScreen UI in app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt

### Test Tasks (Required per spec SC-007)

- [ ] T039 [P] [US1] Write unit tests for StartRideUseCase in app/src/test/java/com/example/bikeredlights/domain/usecase/StartRideUseCaseTest.kt
- [ ] T040 [P] [US1] Write unit tests for CalculateDistanceUseCase in app/src/test/java/com/example/bikeredlights/domain/usecase/CalculateDistanceUseCaseTest.kt
- [ ] T041 [P] [US1] Write unit tests for RideRecordingViewModel in app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModelTest.kt
- [ ] T042 [P] [US1] Write instrumented tests for RideRecordingService in app/src/androidTest/java/com/example/bikeredlights/service/RideRecordingServiceTest.kt
- [ ] T043 [P] [US1] Write Compose UI tests for LiveRideScreen in app/src/androidTest/java/com/example/bikeredlights/ui/screens/LiveRideScreenTest.kt
- [ ] T044 [US1] Run all US1 tests and verify 90%+ coverage for ViewModel/UseCases

### Integration & Validation

- [ ] T045 [US1] Test on emulator: Start ride → duration counts → Stop ride → dialog appears
- [ ] T046 [US1] Test save flow: Save button → ride persists in database → navigate to Review (placeholder)
- [ ] T047 [US1] Test discard flow: Discard button → ride deleted → return to Live tab

**Completion Criteria for US1**:
- ✅ User can start recording, see duration counting
- ✅ User can stop recording, see save/discard dialog
- ✅ Save persists ride to Room database
- ✅ Discard deletes ride from database
- ✅ Foreground service runs with notification
- ✅ 90%+ test coverage for ViewModel/UseCases
- ✅ All acceptance scenarios from spec pass

---

## Phase 4: User Story 4 (P1) - Background Recording Continuity

**Goal**: Ensure recording survives screen-off and app backgrounding

**Independent Test**: Start ride, background app for 2 minutes, resume, verify accumulated time

**Dependencies**: Requires US1 complete (service already implemented)

### Service Enhancement

- [ ] T048 [US4] Add background lifecycle handling in RideRecordingService
- [ ] T049 [US4] Implement notification tap action to open app in RideRecordingService
- [ ] T050 [US4] Add notification "Stop Ride" action handler in RideRecordingService

### Test Tasks

- [ ] T051 [P] [US4] Write instrumented test for screen-off scenario in RideRecordingServiceTest.kt
- [ ] T052 [P] [US4] Write instrumented test for app backgrounding in RideRecordingServiceTest.kt
- [ ] T053 [US4] Run US4 tests and verify service survives 5+ minutes backgrounded

### Integration & Validation

- [ ] T054 [US4] Test on emulator: Start ride → lock screen → wait 2 min → unlock → verify tracking continued
- [ ] T055 [US4] Test on emulator: Start ride → background app → wait 2 min → resume → verify stats accumulated
- [ ] T056 [US4] Test notification tap: Tap notification → app opens to Live tab with stats
- [ ] T057 [US4] Test notification stop: Tap "Stop" in notification → dialog appears

**Completion Criteria for US4**:
- ✅ Recording survives screen lock for 5+ minutes (spec SC-002)
- ✅ Recording survives app backgrounding
- ✅ Notification shows real-time duration/distance
- ✅ Notification tap opens app to Live tab
- ✅ Notification stop action triggers save/discard dialog
- ✅ All acceptance scenarios from spec pass

---

## Phase 5: User Story 2 (P2) - Live Ride Statistics

**Goal**: Display real-time statistics (duration, distance, speeds) during recording

**Independent Test**: Start ride, simulate GPS movement, verify stats update every second

**Dependencies**: Requires US1 complete

### Statistics Calculation

- [X] T058 [US2] Add distance calculation (Haversine formula) to RecordTrackPointUseCase (already implemented via CalculateDistanceUseCase)
- [X] T059 [US2] Add average speed calculation to RideRecordingService.updateRideDistance()
- [X] T060 [US2] Add max speed tracking to RideRecordingService.updateRideDistance()
- [X] T061 [US2] Add stationary detection (< 1 km/h shows 0) to RideRecordingService.updateRideDistance()

### UI Components

- [X] T062 [P] [US2] Create RideStatistics composable in app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatistics.kt
- [X] T063 [P] [US2] Create RideControls composable in app/src/main/java/com/example/bikeredlights/ui/components/ride/RideControls.kt
- [X] T064 [US2] Update LiveRideScreen with RideStatistics display
- [X] T065 [US2] Add duration formatting (HH:MM:SS) utility to RideStatistics (formatDuration function)

### Test Tasks

- [X] T066 [P] [US2] Write unit tests for formatDuration in RideStatisticsTest.kt (14 tests)
- [X] T067 [P] [US2] Write unit tests for average speed calculation in SpeedCalculationsTest.kt (5 tests)
- [X] T068 [P] [US2] Write unit tests for max speed tracking in SpeedCalculationsTest.kt (4 tests)
- [X] T069 [P] [US2] Write unit tests for stationary detection in SpeedCalculationsTest.kt (5 tests)
- [X] T070 [US2] Write unit tests for moving duration calculation in SpeedCalculationsTest.kt (5 tests)

### Integration & Validation

- [ ] T071 [US2] Test on emulator: Start ride → simulate GPS route → verify distance accumulates
- [ ] T072 [US2] Test on emulator: Simulate 20 km/h movement → verify current speed displays correctly
- [ ] T073 [US2] Test on emulator: Verify average speed = distance / duration
- [ ] T074 [US2] Test on emulator: Simulate stationary (< 1 km/h) → verify speed shows 0.0
- [ ] T075 [US2] Verify statistics accurate within 5% vs known route (spec SC-003)

**Completion Criteria for US2**:
- ✅ Duration counts up in HH:MM:SS format
- ✅ Distance displays with one decimal place
- ✅ Current speed updates every second
- ✅ Average speed calculates correctly
- ✅ Max speed tracks peak value
- ✅ Stationary detection shows 0.0 km/h
- ✅ Statistics accurate within 5% (spec SC-003)
- ✅ All acceptance scenarios from spec pass

---

## Phase 6: User Story 5 (P2) - Settings Integration

**Status**: ⚠️ **PARTIALLY COMPLETE - KNOWN BUGS** (See [bugs/BUGS.md](../bugs/BUGS.md))

**Goal**: Respect user preferences for units, GPS accuracy, and auto-pause

**Independent Test**: Change settings mid-ride, verify stats update to new units without data loss

**Dependencies**: Requires US2 complete (stats display), F2A (settings infrastructure)

### Phase 6 Status Summary

**Completed** (2025-11-05):
- ✅ T076-T079: Units conversion implementation (Metric/Imperial)
- ✅ Flow-based ride observation infrastructure (enables real-time updates when Service fixed)

**Blocked by Critical Bugs** (Documented in [bugs/BUGS.md](../bugs/BUGS.md)):
- ❌ **Bug #1**: Duration not updating in real-time (Service only updates on GPS events, not timer-based)
- ❌ **Bug #2**: Duration continues when paused (no pause timestamp tracking)
- ❌ **Bug #3**: Missing permission request UI (app crashes on first launch)
- ⚠️ **Bug #4**: Missing current time display (enhancement request)

**Tasks Blocked**: T080-T099 (GPS accuracy, auto-pause testing) cannot proceed until Bugs #1 and #2 are resolved.

**Next Actions**:
1. Fix Bug #1: Implement timer-based duration updates in RideRecordingService
2. Fix Bug #2: Add pause timestamp tracking and exclude paused time from duration
3. Resume Phase 6 testing and complete remaining tasks

**Testing Notes**:
- Emulator testing revealed fundamental issues with Service update logic
- Units conversion code works correctly, but cannot validate in live recording due to Bug #1
- Permission crash (Bug #3) worked around via manual adb grant for testing purposes

### Units Conversion

- [X] T076 [US5] Read UnitsSystem from SettingsRepository in RideRecordingViewModel
- [X] T077 [US5] Implement meters → km/miles conversion in RideRecordingViewModel
- [X] T078 [US5] Implement m/s → km/h/mph conversion in RideRecordingViewModel
- [X] T079 [US5] Update RideStatistics to display units labels (km, km/h, miles, mph)

### GPS Accuracy Integration

- [ ] T080 [US5] Read GpsAccuracy from SettingsRepository in RideRecordingService
- [ ] T081 [US5] Configure LocationRequest with 1000ms (High) or 4000ms (Battery Saver) interval
- [ ] T082 [US5] Implement settings change listener to update interval mid-ride

### Auto-Pause Implementation

- [ ] T083 [US5] Read AutoPauseConfig from SettingsRepository in RideRecordingViewModel
- [ ] T084 [US5] Implement auto-pause detection (speed < 1 km/h for threshold) in RideRecordingViewModel
- [ ] T085 [US5] Implement auto-resume detection (speed > 1 km/h) in RideRecordingViewModel
- [ ] T086 [US5] Add "Paused" indicator banner to LiveRideScreen
- [ ] T087 [US5] Exclude paused duration from total duration in FinishRideUseCase

### Test Tasks

- [ ] T088 [P] [US5] Write unit tests for units conversion in RideRecordingViewModelTest.kt
- [ ] T089 [P] [US5] Write unit tests for auto-pause detection in RideRecordingViewModelTest.kt
- [ ] T090 [P] [US5] Write unit tests for auto-resume logic in RideRecordingViewModelTest.kt
- [ ] T091 [US5] Run US5 tests and verify units conversion accurate within 1% (spec SC-004)

### Integration & Validation

- [ ] T092 [US5] Test on emulator: Set Metric → start ride → verify km/h displayed
- [ ] T093 [US5] Test on emulator: Set Imperial → start ride → verify mph displayed
- [ ] T094 [US5] Test on emulator: Change units mid-ride → verify stats update without data loss
- [ ] T095 [US5] Test on emulator: Set High Accuracy → verify 1s GPS updates (check logs)
- [ ] T096 [US5] Test on emulator: Set Battery Saver → verify 4s GPS updates
- [ ] T097 [US5] Test on emulator: Enable auto-pause (5 min) → simulate stationary 5 min → verify pauses
- [ ] T098 [US5] Test on emulator: Auto-paused ride → simulate movement → verify resumes
- [ ] T099 [US5] Verify auto-pause triggers within 10 seconds of threshold (spec SC-005)

**Completion Criteria for US5**:
- ✅ Metric units display correctly (km/h, km)
- ✅ Imperial units display correctly (mph, miles)
- ✅ Units conversion accurate within 1% (spec SC-004)
- ✅ High Accuracy GPS uses 1s intervals
- ✅ Battery Saver GPS uses 4s intervals
- ✅ Auto-pause triggers after threshold
- ✅ Auto-resume on movement detection
- ✅ Paused duration excluded from total
- ✅ Auto-pause triggers within 10 seconds (spec SC-005)
- ✅ All acceptance scenarios from spec pass

---

## Phase 7: User Story 3 (P3) - Review Screen

**Goal**: Display post-ride statistics on Review screen

**Independent Test**: Complete ride, tap Save, verify Review screen shows correct stats

**Dependencies**: Requires US1, US2, US5 complete

### Review Screen Implementation

- [X] T100 [US3] Create RideReviewScreen composable in app/src/main/java/com/example/bikeredlights/ui/screens/ride/RideReviewScreen.kt
- [X] T101 [US3] Add navigation route for Review screen in app/src/main/java/com/example/bikeredlights/ui/navigation/AppNavigation.kt
- [X] T102 [US3] Pass rideId as navigation argument from SaveRideDialog
- [X] T103 [US3] Fetch ride from database in RideReviewScreen
- [X] T104 [US3] Display statistics (duration, distance, avg speed, max speed) in RideReviewScreen
- [X] T105 [US3] Add map placeholder message "Map visualization coming in v0.4.0"
- [X] T106 [US3] Implement back navigation to Live tab

### Test Tasks

- [X] T107 [P] [US3] Write Compose UI tests for RideReviewScreen in app/src/androidTest/java/com/example/bikeredlights/ui/screens/RideReviewScreenTest.kt
- [X] T108 [US3] Run US3 tests and verify Review screen displays correct data

### Integration & Validation

- [ ] T109 [US3] Test on emulator: Complete ride → Save → verify navigate to Review screen
- [ ] T110 [US3] Test on emulator: Verify Review screen shows correct duration/distance/speeds
- [ ] T111 [US3] Test on emulator: Verify Review screen respects units preference
- [ ] T112 [US3] Test on emulator: Tap back → verify return to Live tab in idle state

**Completion Criteria for US3**:
- ✅ Review screen displays after save
- ✅ All statistics display correctly (duration, distance, avg speed, max speed)
- ✅ Statistics use user's preferred units
- ✅ Map placeholder message shown
- ✅ Back button returns to Live tab
- ✅ All acceptance scenarios from spec pass

---

## Phase 8: User Story 6 (P3) - Screen Wake Lock

**Goal**: Keep screen on during foreground recording

**Independent Test**: Start ride, leave in foreground, verify screen doesn't auto-lock

**Dependencies**: Requires US1 complete

### Wake Lock Implementation

- [X] T113 [US6] Create KeepScreenOn composable in app/src/main/java/com/example/bikeredlights/ui/components/ride/KeepScreenOn.kt
- [X] T114 [US6] Implement DisposableEffect with FLAG_KEEP_SCREEN_ON in KeepScreenOn
- [X] T115 [US6] Apply KeepScreenOn to LiveRideScreen when isRecording = true
- [X] T116 [US6] Verify wake lock releases when recording stops
- [X] T117 [US6] Verify wake lock releases when app backgrounds

### Test Tasks

- [ ] T118 [P] [US6] Write instrumented test for wake lock applied when recording in LiveRideScreenTest.kt
- [ ] T119 [P] [US6] Write instrumented test for wake lock released when stopped in LiveRideScreenTest.kt
- [ ] T120 [US6] Run US6 tests and verify wake lock behavior

### Integration & Validation

- [ ] T121 [US6] Test on emulator: Start ride → wait beyond screen timeout → verify screen stays on
- [ ] T122 [US6] Test on emulator: Background app → verify screen locks normally (service continues)
- [ ] T123 [US6] Test on emulator: Stop ride → verify wake lock released, screen can lock

**Completion Criteria for US6**:
- ✅ Screen stays on during foreground recording
- ✅ Screen locks normally when backgrounded
- ✅ Wake lock releases when recording stops
- ✅ All acceptance scenarios from spec pass

---

## Phase 9: Manual Pause/Resume (Resolved Clarification)

**Goal**: Add manual pause/resume buttons for cafe stops mid-ride

**Independent Test**: Start ride, tap Pause, verify GPS stops and banner shows, tap Resume

**Dependencies**: Requires US1, US2 complete

### Manual Pause UI

- [ ] T124 Add Pause button to RideControls in app/src/main/java/com/example/bikeredlights/ui/components/ride/RideControls.kt
- [ ] T125 Add Resume button (replaces Pause when paused) to RideControls
- [ ] T126 Add "Ride Paused" banner to LiveRideScreen
- [ ] T127 Update RideRecordingViewModel with pauseRide() action
- [ ] T128 Update RideRecordingViewModel with resumeRide() action

### Service Integration

- [ ] T129 Add ACTION_PAUSE_RECORDING handler to RideRecordingService
- [ ] T130 Add ACTION_RESUME_RECORDING handler to RideRecordingService
- [ ] T131 Stop GPS location updates on manual pause in RideRecordingService
- [ ] T132 Resume GPS location updates on manual resume in RideRecordingService
- [ ] T133 Update notification to show "Ride Paused" with Resume action

### State Management

- [ ] T134 Add isManuallyPaused flag to RideRecordingState
- [ ] T135 Add manualPauseStartTime to RideRecordingState
- [ ] T136 Add accumulatedManualPausedDuration to RideRecordingState
- [ ] T137 Implement manual pause priority over auto-pause in RideRecordingViewModel
- [ ] T138 Mark TrackPoints with isManuallyPaused flag

### Test Tasks

- [ ] T139 [P] Write unit tests for pauseRide() in RideRecordingViewModelTest.kt
- [ ] T140 [P] Write unit tests for resumeRide() in RideRecordingViewModelTest.kt
- [ ] T141 [P] Write unit tests for manual/auto-pause coordination in RideRecordingViewModelTest.kt
- [ ] T142 [P] Write Compose UI tests for Pause/Resume buttons in LiveRideScreenTest.kt
- [ ] T143 Run manual pause tests and verify 90%+ coverage

### Integration & Validation

- [ ] T144 Test on emulator: Start ride → Pause → verify GPS stops, stats freeze, banner appears
- [ ] T145 Test on emulator: Paused ride → Resume → verify GPS resumes, stats resume, banner disappears
- [ ] T146 Test on emulator: Pause for 2 min → Resume → verify paused duration excluded from total
- [ ] T147 Test on emulator: Manual pause + auto-pause → verify manual takes priority

**Completion Criteria**:
- ✅ Pause button stops GPS and freezes stats
- ✅ Resume button restarts GPS and resumes stats
- ✅ "Ride Paused" banner displays during pause
- ✅ Notification shows paused state with Resume action
- ✅ Manual pause takes priority over auto-pause
- ✅ Paused duration excluded from total duration
- ✅ TrackPoints marked with pause state

---

## Phase 10: Edge Case Handling & Polish

**Goal**: Handle edge cases and add recovery features

**Dependencies**: All user stories complete

### Edge Case Implementation

- [ ] T148 Add GPS signal loss indicator to LiveRideScreen
- [ ] T149 Skip TrackPoints with accuracy > 50m in RecordTrackPointUseCase
- [ ] T150 Handle SecurityException in LocationRepositoryImpl (permissions revoked)
- [ ] T151 Catch SQLiteFullException in TrackPointRepositoryImpl (storage full)
- [ ] T152 Show notification "Storage full - recording stopped" when disk full
- [ ] T153 Enforce minimum 5-second ride duration before save
- [ ] T154 Show toast "Ride too short to save" and auto-discard if < 5s

### Incomplete Ride Recovery

- [ ] T155 Check for incomplete rides (endTime = null) on app launch in MainActivity
- [ ] T156 Create RecoveryDialog composable in app/src/main/java/com/example/bikeredlights/ui/screens/ride/RecoveryDialog.kt
- [ ] T157 Show RecoveryDialog with "Discard" and "Recover" options
- [ ] T158 Implement recover action: Mark ride complete with last TrackPoint timestamp
- [ ] T159 Implement discard action: Delete incomplete ride

### Battery Warning

- [ ] T160 Add battery level check in RideRecordingService
- [ ] T161 Show notification if battery < 15% and High Accuracy enabled
- [ ] T162 Notification suggests switching to Battery Saver mode

### Polish Tasks

- [ ] T163 Update notification every second with real-time duration/distance
- [ ] T164 Add Material 3 animations to button state changes
- [ ] T165 Verify all touch targets meet 48dp × 48dp minimum
- [ ] T166 Add TalkBack contentDescriptions to all interactive elements
- [ ] T167 Test dark mode rendering for all screens
- [ ] T168 Test rotation handling for all screens

### Final Test Pass

- [ ] T169 Run all unit tests and verify 90%+ coverage (spec SC-007)
- [ ] T170 Run all instrumented tests and verify pass rate
- [ ] T171 Comprehensive emulator testing with GPX route simulation
- [ ] T171a [US4] Run 1-hour battery drain test with High Accuracy GPS on Pixel emulator, measure and verify <10%/hour drain
- [ ] T172 Verify battery drain < 10%/hour with High Accuracy (spec SC-008)
- [ ] T173 Verify database writes < 100ms (spec SC-009)
- [ ] T174 Verify Stop button responds < 1 second (spec SC-010)

**Completion Criteria**:
- ✅ All edge cases handled gracefully (no crashes)
- ✅ GPS signal loss shows indicator
- ✅ Storage full handled with notification
- ✅ Permissions revoked handled with notification
- ✅ Incomplete ride recovery works
- ✅ Battery warning shows when appropriate
- ✅ All accessibility requirements met (48dp targets, TalkBack, dark mode)
- ✅ All 12 success criteria from spec pass (SC-001 to SC-012)

---

## Phase 11: Documentation & Release

**Goal**: Finalize documentation and prepare for PR/release

### Documentation Updates

- [ ] T175 Update TODO.md with completed feature status
- [ ] T176 Update RELEASE.md with v0.3.0 entry (Unreleased section)
- [ ] T177 Verify all code comments are clear and helpful
- [ ] T178 Update navigation graph documentation in AppNavigation.kt

### Final Validation

- [ ] T179 Run final test suite (all 90+ tests)
- [ ] T180 Build release APK with `./gradlew assembleRelease`
- [ ] T181 Verify APK installs and launches on emulator
- [ ] T182 Test complete user flow: Start → Record 5 min → Pause → Resume → Stop → Save → Review
- [ ] T183 Verify no ProGuard/R8 issues in release build

### Commit & Push

- [ ] T184 Final commit: "feat: complete Core Ride Recording (F1A)"
- [ ] T185 Push all changes to GitHub
- [ ] T186 Verify CI/CD checks pass (if configured)

**Completion Criteria**:
- ✅ All documentation updated
- ✅ Release APK builds successfully
- ✅ Complete user flow tested end-to-end
- ✅ All changes committed and pushed
- ✅ Ready for pull request creation

---

## Task Summary

**Total Tasks**: 186 tasks
- Phase 1 (Setup): 9 tasks
- Phase 2 (Foundation): 14 tasks
- Phase 3 (US1): 24 tasks
- Phase 4 (US4): 10 tasks
- Phase 5 (US2): 18 tasks
- Phase 6 (US5): 24 tasks
- Phase 7 (US3): 13 tasks
- Phase 8 (US6): 11 tasks
- Phase 9 (Manual Pause): 24 tasks
- Phase 10 (Edge Cases): 27 tasks
- Phase 11 (Documentation): 12 tasks

**Parallel Opportunities**: 89 tasks marked [P] (can run simultaneously)

**Test Tasks**: 35 test tasks (achieving 90%+ coverage requirement)

---

## Dependency Graph

### Story Completion Order

```
Phase 1: Setup & Database Foundation (BLOCKING)
  ↓
Phase 2: Repository Layer (BLOCKING)
  ↓
Phase 3: US1 (P1) - Start/Stop Recording (MVP)
  ↓
Phase 4: US4 (P1) - Background Recording (depends on US1)
  ↓
┌─────────────────┬──────────────────┐
│                 │                  │
Phase 5: US2 (P2) Phase 8: US6 (P3)  Phase 9: Manual Pause
Live Statistics   Wake Lock          (depends on US1)
(depends on US1)  (depends on US1)
│                 │                  │
└─────────────────┴──────────────────┘
  ↓
Phase 6: US5 (P2) - Settings Integration (depends on US2)
  ↓
Phase 7: US3 (P3) - Review Screen (depends on US1, US2, US5)
  ↓
Phase 10: Edge Cases & Polish (depends on all)
  ↓
Phase 11: Documentation & Release
```

### Critical Path (Minimum for MVP)

1. Phase 1: Setup & Database Foundation (9 tasks)
2. Phase 2: Repository Layer (14 tasks)
3. Phase 3: US1 - Start/Stop Recording (24 tasks)
4. Phase 4: US4 - Background Recording (10 tasks)

**MVP Total**: 57 tasks → Delivers complete start/stop/background recording

---

## Parallel Execution Examples

### Phase 1 Parallelization
```bash
# Run these simultaneously (different files, no dependencies)
T002: Create TrackPoint entity
T007: Write RideDao tests
T008: Write TrackPointDao tests
```

### Phase 2 Parallelization
```bash
# All repository interfaces can be created in parallel
T011: TrackPointRepository interface
T012: LocationRepository interface
T013: RideRecordingStateRepository interface

# All domain models can be created in parallel
T014: Ride domain model
T015: TrackPoint domain model
T016: RideRecordingState domain model

# All repository implementations can be created in parallel
T018: TrackPointRepositoryImpl
T019: LocationRepositoryImpl
T020: RideRecordingStateRepositoryImpl

# All repository tests can be written in parallel
T021: RideRepositoryImpl tests
T022: LocationRepositoryImpl tests
```

### Phase 3 (US1) Parallelization
```bash
# Use cases can be created in parallel
T028: StartRideUseCase
T029: FinishRideUseCase
T030: RecordTrackPointUseCase
T031: CalculateDistanceUseCase

# UI components can be created in parallel with use cases
T037: SaveRideDialog composable

# Test tasks can be written in parallel after implementation
T039: StartRideUseCase tests
T040: CalculateDistanceUseCase tests
T041: RideRecordingViewModel tests
T042: RideRecordingService tests
T043: LiveRideScreen tests
```

---

## Implementation Strategy

### MVP-First Approach

**Week 1: MVP** (Phases 1-4)
- Complete start/stop recording with background service
- Deliverable: Users can record rides that survive backgrounding
- Tasks: T001-T057 (57 tasks)

**Week 2: Enhanced UX** (Phases 5-7)
- Add live statistics, settings integration, review screen
- Deliverable: Full-featured ride recording with stats
- Tasks: T058-T112 (55 tasks)

**Week 3: Polish** (Phases 8-11)
- Add wake lock, manual pause, edge cases, documentation
- Deliverable: Production-ready v0.3.0 release
- Tasks: T113-T186 (74 tasks)

### Testing Strategy

- **Unit tests**: After each implementation task (TDD approach where applicable)
- **Integration tests**: At end of each phase before moving to next
- **Emulator testing**: Daily during development, comprehensive at end

### Commit Strategy

- Small, frequent commits (every 2-5 tasks)
- Commit format: `feat(ride): add [component]` or `test(ride): add [test]`
- Push to GitHub at end of each phase

---

**Task Generation Complete**: Ready for implementation. Start with Phase 1 (Setup & Database Foundation).
