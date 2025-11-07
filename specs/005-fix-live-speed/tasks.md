# Tasks: Fix Live Current Speed Display Bug

**Input**: Design documents from `/specs/005-fix-live-speed/`
**Prerequisites**: plan.md (required), spec.md (required for user stories), research.md, data-model.md, contracts/

**Tests**: This bug fix includes unit and UI tests as required by BikeRedlights constitution (80%+ coverage for non-UI layers).

**Organization**: Tasks are organized by architectural layer (Domain → Data → Service → UI) following Clean Architecture principles. This is a single user story (P1) bug fix.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[US1]**: User Story 1 - See Real-Time Speed While Riding
- Include exact file paths in descriptions

## Path Conventions

- **Android project structure**: `app/src/main/java/com/example/bikeredlights/`
- **Test structure**: `app/src/test/java/` (unit tests), `app/src/androidTest/java/` (instrumented tests)

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Update documentation to track this bug fix

- [ ] T001 Update TODO.md moving "Live Current Speed Bug" from Planned to In Progress
- [ ] T002 Update RELEASE.md adding bug fix to Unreleased section

**Checkpoint**: Documentation updated - ready to start implementation

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: No foundational work needed - all infrastructure exists (StateFlow, Hilt DI, Clean Architecture layers are already in place)

**⚠️ SKIP**: This bug fix works within existing architecture. Proceed directly to User Story implementation.

---

## Phase 3: User Story 1 - See Real-Time Speed While Riding (Priority: P1) 🎯 Bug Fix

**Goal**: Fix hardcoded 0.0 speed display by wiring GPS speed through Service → Repository → ViewModel → UI layers.

**Independent Test**: Start ride, move (walk/cycle), verify current speed displays real-time values (not 0.0) and updates every 1-4 seconds.

### Implementation for User Story 1

**Layer 1: Domain (Repository Interface)**

- [ ] T003 [US1] Add getCurrentSpeed() method to RideRecordingStateRepository interface in app/src/main/java/com/example/bikeredlights/domain/repository/RideRecordingStateRepository.kt

**Layer 2: Data (Repository Implementation)**

- [ ] T004 [US1] Add private _currentSpeed MutableStateFlow(0.0) to RideRecordingStateRepositoryImpl in app/src/main/java/com/example/bikeredlights/data/repository/RideRecordingStateRepositoryImpl.kt
- [ ] T005 [US1] Implement getCurrentSpeed() returning _currentSpeed.asStateFlow() in RideRecordingStateRepositoryImpl
- [ ] T006 [US1] Add internal updateCurrentSpeed(speedMps: Double) method with require(speedMps >= 0.0) validation in RideRecordingStateRepositoryImpl
- [ ] T007 [US1] Add internal resetCurrentSpeed() method setting _currentSpeed.value = 0.0 in RideRecordingStateRepositoryImpl
- [ ] T008 [US1] Commit layer 1-2 changes with message "feat(data): add current speed StateFlow to repository"

**Layer 3: Service (GPS Speed Emission)**

- [ ] T009 [US1] In RideRecordingService.startLocationTracking() (line ~430), add speed emission: val currentSpeed = maxOf(0.0, (locationData.speedMps ?: 0f).toDouble()) in app/src/main/java/com/example/bikeredlights/service/RideRecordingService.kt
- [ ] T010 [US1] Call rideRecordingStateRepository.updateCurrentSpeed(currentSpeed) after speed calculation in startLocationTracking()
- [ ] T011 [US1] In stopRideRecording() method, call rideRecordingStateRepository.resetCurrentSpeed() to reset speed to 0.0
- [ ] T012 [US1] In pauseRecording() method, call rideRecordingStateRepository.resetCurrentSpeed() to reset speed to 0.0
- [ ] T013 [US1] Commit service layer changes with message "feat(service): emit current speed to repository on GPS updates"

**Layer 4: UI (ViewModel Exposure)**

- [ ] T014 [US1] Add currentSpeed: StateFlow<Double> property to RideRecordingViewModel using stateIn with WhileSubscribed(5000) in app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt
- [ ] T015 [US1] Wire currentSpeed to repository.getCurrentSpeed().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)
- [ ] T016 [US1] Commit ViewModel changes with message "feat(viewmodel): expose current speed StateFlow to UI"

**Layer 5: UI (Screen Integration)**

- [ ] T017 [US1] In LiveRideScreen composable, add: val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle() (line ~66) in app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt
- [ ] T018 [US1] Replace hardcoded currentSpeed = 0.0 with currentSpeed = currentSpeed variable in RideStatistics call (line 350)
- [ ] T019 [US1] Commit UI changes with message "fix(ui): wire current speed from ViewModel to LiveRideScreen display"

**Checkpoint**: At this point, current speed should display real-time GPS values on Live tab instead of hardcoded 0.0

---

## Phase 4: Testing & Validation

**Purpose**: Verify bug fix works correctly across all scenarios

### Unit Tests

- [ ] T020 [P] [US1] Create RideRecordingStateRepositoryImplTest.kt with test: getCurrentSpeed returns initial value of 0.0 in app/src/test/java/com/example/bikeredlights/data/repository/RideRecordingStateRepositoryImplTest.kt
- [ ] T021 [P] [US1] Add test: updateCurrentSpeed updates StateFlow value in RideRecordingStateRepositoryImplTest.kt
- [ ] T022 [P] [US1] Add test: updateCurrentSpeed throws on negative value in RideRecordingStateRepositoryImplTest.kt
- [ ] T023 [P] [US1] Add test: resetCurrentSpeed sets value to 0.0 in RideRecordingStateRepositoryImplTest.kt
- [ ] T024 [P] [US1] Add test: getCurrentSpeed emits to multiple collectors in RideRecordingStateRepositoryImplTest.kt
- [ ] T025 [US1] Modify RideRecordingViewModelTest.kt adding test: currentSpeed exposes repository StateFlow in app/src/test/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModelTest.kt
- [ ] T026 [US1] Add test: convertSpeed converts m/s to km/h correctly in RideRecordingViewModelTest.kt
- [ ] T027 [US1] Add test: convertSpeed converts m/s to mph correctly in RideRecordingViewModelTest.kt
- [ ] T028 [US1] Add test: convertSpeed throws on negative value in RideRecordingViewModelTest.kt
- [ ] T029 [US1] Commit unit tests with message "test(repository,viewmodel): add unit tests for current speed StateFlow"

### UI Tests

- [ ] T030 [P] [US1] Create LiveRideScreenTest.kt with test: displays current speed during ride in app/src/androidTest/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreenTest.kt
- [ ] T031 [P] [US1] Add test: speed updates when StateFlow changes in LiveRideScreenTest.kt
- [ ] T032 [P] [US1] Add test: speed displays 0.0 when ride not started in LiveRideScreenTest.kt
- [ ] T033 [P] [US1] Add test: speed displays in correct units (km/h vs mph) in LiveRideScreenTest.kt
- [ ] T034 [US1] Commit UI tests with message "test(ui): add Compose tests for current speed display"

### Emulator Testing (MANDATORY per constitution)

- [ ] T035 [US1] Build debug APK: ./gradlew assembleDebug
- [ ] T036 [US1] Start Android emulator (API 34+): emulator -avd Pixel_6_API_34
- [ ] T037 [US1] Install debug build: adb install app/build/outputs/apk/debug/app-debug.apk
- [ ] T038 [US1] Test Scenario 1: Start ride, simulate GPS movement (Extended Controls → Location), verify speed displays non-zero value
- [ ] T039 [US1] Test Scenario 2: Load test-route.gpx (from quickstart.md), play route, verify speed updates in real-time
- [ ] T040 [US1] Test Scenario 3: During ride, pause manually, verify speed resets to 0.0
- [ ] T041 [US1] Test Scenario 4: Resume ride, verify speed updates resume
- [ ] T042 [US1] Test Scenario 5: Rotate screen during ride, verify speed value persists (no reset to 0.0)
- [ ] T043 [US1] Test Scenario 6: Change units (Settings → Metric/Imperial), verify speed converts correctly
- [ ] T044 [US1] Document emulator test results (all scenarios pass/fail) for PR

### Physical Device Testing (RECOMMENDED per constitution)

- [ ] T045 [US1] Install on physical device: ./gradlew installDebug && adb install app/build/outputs/apk/debug/app-debug.apk
- [ ] T046 [US1] Walking test: Start ride, walk outdoors, verify speed displays 1-5 km/h and drops to 0.0 when stopped
- [ ] T047 [US1] Cycling test: Cycle at normal pace, verify speed displays 15-25 km/h and updates dynamically
- [ ] T048 [US1] Pause/resume test: Pause during movement, verify speed resets to 0.0, resume and verify updates resume
- [ ] T049 [US1] Configuration change test: Rotate screen during ride, verify speed persists across rotation
- [ ] T050 [US1] Units test: Switch between metric/imperial, verify speed converts correctly (20 km/h ≈ 12.4 mph)
- [ ] T051 [US1] Document physical device test results for PR

**Checkpoint**: All tests passing, bug fix validated on emulator and physical device

---

## Phase 5: Documentation & Release Preparation

**Purpose**: Finalize documentation and prepare for release

- [ ] T052 Update TODO.md moving "Live Current Speed Bug" from In Progress to Completed with completion date
- [ ] T053 Update RELEASE.md finalizing bug fix entry in Unreleased section with detailed description
- [ ] T054 Verify all commits follow conventional commit format (feat/fix/test)
- [ ] T055 Run ./gradlew test to verify all unit tests pass
- [ ] T056 Run ./gradlew connectedDebugAndroidTest to verify all UI tests pass
- [ ] T057 Review code changes for code quality (lint warnings addressed)
- [ ] T058 Prepare PR description with: bug summary, fix approach, testing results, emulator/device validation

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: SKIPPED - no foundational work needed
- **User Story 1 (Phase 3)**: Can start immediately (depends on existing architecture)
  - **Layer sequence**: Domain → Data → Service → ViewModel → UI (strict order, no parallelization)
- **Testing (Phase 4)**: Depends on Phase 3 implementation completion
  - Unit tests (T020-T029): Can run in parallel after repository/ViewModel implemented
  - UI tests (T030-T034): Can run in parallel after LiveRideScreen modified
  - Emulator tests (T035-T044): Sequential execution (manual testing)
  - Physical device tests (T045-T051): Sequential execution (manual testing)
- **Documentation (Phase 5)**: Depends on all testing completion

### User Story 1 Internal Dependencies

**Layer 1 (Domain)**: T003
- No dependencies, must complete before Layer 2

**Layer 2 (Data)**: T004 → T005 → T006 → T007 → T008
- T004: Add private StateFlow (depends on T003 interface)
- T005: Implement getter (depends on T004)
- T006: Add updateCurrentSpeed() (depends on T004)
- T007: Add resetCurrentSpeed() (depends on T004)
- T008: Commit (depends on T004-T007 completion)

**Layer 3 (Service)**: T009 → T010 → T011 → T012 → T013
- T009: Calculate current speed (depends on T006 existing)
- T010: Emit to repository (depends on T009)
- T011: Reset on stop (depends on T007)
- T012: Reset on pause (depends on T007)
- T013: Commit (depends on T009-T012 completion)

**Layer 4 (UI ViewModel)**: T014 → T015 → T016
- T014: Add StateFlow property (depends on T005 repository method)
- T015: Wire to repository (depends on T014)
- T016: Commit (depends on T014-T015 completion)

**Layer 5 (UI Screen)**: T017 → T018 → T019
- T017: Collect from ViewModel (depends on T014)
- T018: Pass to RideStatistics (depends on T017)
- T019: Commit (depends on T017-T018 completion)

### Parallel Opportunities

**Setup Phase (Phase 1)**:
- T001 and T002 can run in parallel (different files)

**User Story Phase (Phase 3)**:
- No parallelization possible - strict layer dependency (Domain → Data → Service → ViewModel → UI)
- Each layer must complete before next layer starts

**Testing Phase (Phase 4)**:
- T020-T024: All repository tests can run in parallel (test different methods)
- T025-T028: All ViewModel tests can run in parallel (test different methods)
- T030-T033: All UI tests can run in parallel (test different scenarios)
- T020-T034: Repository, ViewModel, and UI test suites can run in parallel (different test files)

**Documentation Phase (Phase 5)**:
- No parallelization (sequential review process)

---

## Parallel Example: Testing Phase

```bash
# Launch all repository tests together (T020-T024):
Task: "Test getCurrentSpeed returns initial value of 0.0"
Task: "Test updateCurrentSpeed updates StateFlow value"
Task: "Test updateCurrentSpeed throws on negative value"
Task: "Test resetCurrentSpeed sets value to 0.0"
Task: "Test getCurrentSpeed emits to multiple collectors"

# Launch all ViewModel tests together (T025-T028):
Task: "Test currentSpeed exposes repository StateFlow"
Task: "Test convertSpeed converts m/s to km/h correctly"
Task: "Test convertSpeed converts m/s to mph correctly"
Task: "Test convertSpeed throws on negative value"

# Launch all UI tests together (T030-T033):
Task: "Test displays current speed during ride"
Task: "Test speed updates when StateFlow changes"
Task: "Test speed displays 0.0 when ride not started"
Task: "Test speed displays in correct units"
```

---

## Implementation Strategy

### MVP (Single User Story Bug Fix)

1. Complete Phase 1: Setup (documentation start)
2. Skip Phase 2: Foundational (not needed)
3. Complete Phase 3: User Story 1 implementation
   - Domain layer (T003)
   - Data layer (T004-T008)
   - Service layer (T009-T013)
   - ViewModel layer (T014-T016)
   - UI layer (T017-T019)
4. Complete Phase 4: Testing & validation
   - Unit tests (T020-T029)
   - UI tests (T030-T034)
   - Emulator testing (T035-T044)
   - Physical device testing (T045-T051) - RECOMMENDED
5. Complete Phase 5: Documentation & release prep
6. **STOP and VALIDATE**: Bug is fixed, speed displays real-time values

### Incremental Delivery (Layer by Layer with Commits)

1. Complete T001-T002 (setup) → Commit documentation updates
2. Complete T003 (domain interface) → Commit domain changes
3. Complete T004-T008 (data layer) → Commit repository implementation
4. Complete T009-T013 (service layer) → Commit service GPS emission
5. Complete T014-T016 (ViewModel layer) → Commit ViewModel exposure
6. Complete T017-T019 (UI layer) → Commit UI screen integration
7. **TEST END-TO-END**: Bug should be fixed at this point
8. Complete T020-T034 (automated tests) → Commit test coverage
9. Complete T035-T051 (manual testing) → Validate on devices
10. Complete T052-T058 (documentation) → Prepare PR

### Single Developer Strategy

1. Work sequentially through phases (Setup → Implementation → Testing → Documentation)
2. Commit after each layer completion (5 commits for 5 layers)
3. Run automated tests after implementation complete
4. Perform emulator testing (mandatory)
5. Perform physical device testing (recommended)
6. Prepare PR with comprehensive testing results

---

## Notes

- [P] tasks = different files, no dependencies within phase
- [US1] label = all tasks belong to single user story (bug fix)
- Layer sequence is strict: Domain → Data → Service → ViewModel → UI (no parallelization possible)
- Commit after each layer for clean git history
- Emulator testing is MANDATORY per BikeRedlights constitution before merge
- Physical device testing is RECOMMENDED but not required for bug fixes
- All automated tests must pass before PR submission
- Verify GPS simulation works correctly (see quickstart.md for setup)
- Avoid: skipping tests, skipping emulator validation, combining layers in single commit
