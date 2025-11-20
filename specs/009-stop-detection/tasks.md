# Tasks: Stop Detection & Recording

**Input**: Design documents from `/specs/009-stop-detection/`
**Prerequisites**: spec.md, plan.md, research.md, data-model.md, contracts/StopDao.kt, quickstart.md

**Tests**: Tests are included for critical components (state machine, DAO, repository) as this is a safety-critical feature dealing with location data and database integrity.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. Feature 009 has 5 user stories (all P1) that build incrementally:
- **US1**: Real-time stop detection during rides (core detection engine)
- **US2**: Live stop status UI popup (user feedback)
- **US3**: Stop count display on Live tab (at-a-glance metric)
- **US4**: Stop data persistence in database (long-term storage)
- **US5**: Integration with settings thresholds (Feature 008 consumption)

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Android single-module app structure:
- `app/src/main/java/com/example/bikeredlights/` - main source
- `app/src/test/java/com/example/bikeredlights/` - unit tests
- `app/src/androidTest/java/com/example/bikeredlights/` - instrumented tests

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Database schema and foundational data structures that all user stories depend on

- [X] T001 Create StopEntity.kt Room entity in app/src/main/java/com/example/bikeredlights/data/local/entity/StopEntity.kt
- [X] T002 [P] Create Stop.kt domain model in app/src/main/java/com/example/bikeredlights/domain/model/Stop.kt
- [X] T003 [P] Create StopDao.kt interface in app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt
- [X] T004 Implement Room migration MIGRATION_1_2 in app/src/main/java/com/example/bikeredlights/data/local/BikeRedlightsDatabase.kt (add stops table, foreign keys, indexes)
- [X] T005 Update BikeRedlightsDatabase.kt version from 1 to 2 and add StopDao provider method
- [X] T006 [P] Create StopRepository.kt interface in app/src/main/java/com/example/bikeredlights/domain/repository/StopRepository.kt
- [X] T007 [P] Create StopRepositoryImpl.kt in app/src/main/java/com/example/bikeredlights/data/repository/StopRepositoryImpl.kt
- [X] T008 Update DatabaseModule.kt Hilt bindings in app/src/main/java/com/example/bikeredlights/di/DatabaseModule.kt (provide StopDao, bind StopRepository)

**Checkpoint**: Database layer ready - stops table created, repository accessible via Hilt

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core stop detection logic and state machine that all user stories require

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [X] T009 Create StopDetectionState.kt data class in app/src/main/java/com/example/bikeredlights/domain/model/StopDetectionState.kt (runtime state: counters, timers, active stop ID)
- [X] T010 [P] Create StopDetectionUtils.kt in app/src/main/java/com/example/bikeredlights/domain/util/StopDetectionUtils.kt (consecutive seconds filtering, duration formatting)
- [X] T011 Create StopDetectionStateMachine.kt in app/src/main/java/com/example/bikeredlights/domain/util/StopDetectionStateMachine.kt (state transitions: Moving → Detecting → Confirmed)
- [X] T012 Write unit tests for StopDetectionStateMachine in app/src/test/java/com/example/bikeredlights/domain/util/StopDetectionStateMachineTest.kt (test all state transitions, consecutive seconds logic, edge cases)
- [X] T013 Write instrumented tests for StopDao in app/src/androidTest/java/com/example/bikeredlights/data/local/dao/StopDaoTest.kt (test insert, updateStopEnd, CASCADE delete, UNIQUE constraint, Flow reactivity)
- [X] T014 Write unit tests for StopRepository in app/src/test/java/com/example/bikeredlights/data/repository/StopRepositoryTest.kt (test CRUD operations, domain/entity mapping)
- [X] T015 Verify database migration 4→5 using Database Inspector (check stops table schema, foreign keys, indexes)

**Checkpoint**: Foundation ready - state machine tested, database validated, user story implementation can now begin

---

## Phase 3: User Story 1 - Real-Time Stop Detection During Active Ride (Priority: P1) 🎯 MVP CORE

**Goal**: Detect when rider stops during active ride by monitoring GPS speed against thresholds, with 3-second consecutive filtering to avoid GPS noise false positives. Record stop start timestamp and location when duration threshold is met.

**Independent Test**: Start a ride, ride at normal speed (10 km/h), then stop completely for 15+ seconds. Verify stop is detected (state machine transitions to Confirmed), and stop record is inserted into database with correct rideId, stopNumber, latitude, longitude, startTimestamp.

### Implementation for User Story 1

- [X] T016 [US1] Integrate stop detection in RideRecordingService location callback (call stopDetectionStateMachine.processSpeed() on each GPS update)
- [X] T017 [US1] Modify RideRecordingService.kt to own StopDetectionStateMachine instance in service scope (survives backgrounding)
- [X] T018 [US1] Add stop detection initialization in RideRecordingService.startRide() (load thresholds from settings, create state machine)
- [X] T019 [US1] Add stop detection cleanup in RideRecordingService.stopRide() (end active stop if exists, reset state)
- [X] T020 [US1] Add stop event emissions/state exposure to RideRecordingService using StateFlow (expose currentStopNumber and currentStopDuration to UI)
  - ✅ Added getCurrentStopNumber() and getCurrentStopDuration() to RideRecordingStateRepository interface
  - ✅ Implemented StateFlows with validation in RideRecordingStateRepositoryImpl
  - ✅ Service collects state machine events and updates StateFlows
  - ✅ Stop duration updates every 100ms during active stop
  - ✅ Stop state resets on stop end or ride end
- [X] T021 [US1] Consecutive seconds filtering handled by StopDetectionStateMachine (speedBelowThresholdCount, speedAboveThresholdCount)
- [X] T022 [US1] Stop confirmation logic in StopDetectionStateMachine (when duration threshold met, calls StopRepository.insertStop internally)
- [X] T023 [US1] Stop end detection in StopDetectionStateMachine (when speed > threshold for 3 seconds, calls StopRepository.updateStopEnd internally)
- [X] T024 [US1] Manual pause during active stop handled by StopDetectionStateMachine.endCurrentStop() (called internally on stopRide())
- [X] T025 [US1] FR-028 data persistence on stop confirmation (StopDetectionStateMachine saves start time, location immediately to database)

**Checkpoint**: User Story 1 complete - stop detection works during rides, data is persisted to database, can be tested independently by recording a ride with stops and verifying database records

---

## Phase 4: User Story 2 - Live Stop Status UI During Ride (Priority: P1)

**Goal**: Display semi-transparent popup showing "🛑 Stop #N" with live duration counter when stop is detected. Auto-dismiss with fade-out animation when rider starts moving again. Provides visual feedback for user trust.

**Independent Test**: Start a ride, stop for 15+ seconds, verify popup appears showing stop number and duration counter updating every second. Start moving again, verify popup auto-dismisses with fade-out animation after 3 seconds of movement.

### Implementation for User Story 2

- [X] T026 [P] [US2] Create StopPopup.kt composable in app/src/main/java/com/example/bikeredlights/ui/components/ride/StopPopup.kt (semi-transparent card, stop number, duration counter, fade-out animation)
  - ✅ Material 3 Card with semi-transparent surface (alpha 0.9)
  - ✅ Stop emoji 🛑 + stop number display
  - ✅ formatDuration() helper (MM:SS or HH:MM:SS format)
  - ✅ AnimatedVisibility with fadeIn/fadeOut animations
  - ✅ 4 preview states for testing
- [X] T027 [US2] Add stop popup state to RideRecordingViewModel (currentStopNumber: StateFlow<Int?>, stopDurationSeconds: StateFlow<Int?>)
  - ✅ Exposed from RideRecordingStateRepository
  - ✅ WhileSubscribed(5000) for battery optimization
- [X] T028 [US2] Collect stop events from RideRecordingService in RideViewModel (automatic via StateFlow from repository)
- [X] T029 [US2] Implement 1-second duration counter (handled in RideRecordingService every 100ms)
- [X] T030 [US2] Modify LiveRideScreen.kt to display StopPopup composable when currentStopNumber is not null
  - ✅ Collect currentStopNumber and currentStopDuration
  - ✅ Pass to StopPopup component
- [X] T031 [US2] Position StopPopup at top-center of LiveRideScreen (doesn't block map or GPS indicator)
- [X] T032 [US2] Add AnimatedVisibility with fadeOut to StopPopup (built-in to StopPopup composable)
- [X] T033 [US2] Test popup visibility logic (AnimatedVisibility handles this based on stopNumber != null)

**Checkpoint**: User Story 2 complete - popup displays during stops with live counter, auto-dismisses on movement, can be tested independently by starting a ride and observing UI during stop/resume cycles

---

## Phase 5: User Story 3 - Stop Count Display on Live Tab (Priority: P2)

**Goal**: Display "Stops: N" counter on Live tab statistics row showing cumulative stop count during current ride. Updates in real-time as stops are detected. Resets to 0 when new ride starts.

**Independent Test**: Start a ride, make 3 stops (each 15+ seconds), verify "Stops: 3" appears on Live tab statistics row. Stop and save ride, start new ride, verify counter resets to "Stops: 0".

### Implementation for User Story 3

- [ ] T034 [US3] Modify RideStatsRow.kt in app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatsRow.kt to add stop count display (format: "Stops: N")
- [ ] T035 [US3] Add stopCount: StateFlow<Int> to RideViewModel.kt (collect from StopRepository.getStopCountByRideId Flow)
- [ ] T036 [US3] Wire stop count Flow in RideViewModel.startRide() (collect from repository, emit to UI)
- [ ] T037 [US3] Add stop count to RideStatsRow layout (position below map, alongside duration and distance)
- [ ] T038 [US3] Handle stop count display states (0 when no ride, N when recording, persist during pause, reset on new ride)
- [ ] T039 [US3] Add subtle animation or color change when stop count increments (Material 3 animated counter)
- [ ] T040 [US3] Test stop count reactivity (verify Flow emits immediately when stop inserted via StopDao.insertStop)

**Checkpoint**: User Story 3 complete - stop count displays correctly on Live tab, updates in real-time, can be tested independently by recording a ride with multiple stops and verifying counter accuracy

---

## Phase 6: User Story 4 - Stop Data Persistence in Database (Priority: P1)

**Goal**: Persist all stop data permanently to Room database with CASCADE delete on ride deletion. Ensure data integrity (unique stopNumber per ride, valid foreign keys, correct timestamps/durations). Support future clustering (Feature 010) with cluster_id field initialized to NULL.

**Independent Test**: Record a ride with 4 stops, save ride, query database directly (Database Inspector or via StopDao.getStopsByRideId) to verify 4 stop records exist with correct data. Delete ride from history, verify all 4 stop records are auto-deleted via CASCADE.

### Implementation for User Story 4

- [ ] T041 [US4] Implement StopDao.insertStop() to return database ID (used as activeStopId in state machine)
- [ ] T042 [US4] Implement StopDao.updateStopEnd() to set endTimestamp and durationSeconds when stop ends
- [ ] T043 [US4] Implement StopDao.getStopsByRideId() to retrieve all stops for a ride ordered by stopNumber
- [ ] T044 [US4] Implement StopDao.getStopCountByRideId() as Flow for reactive stop count updates
- [ ] T045 [US4] Implement StopDao.getUnclusteredStops() for Feature 010 clustering (cluster_id IS NULL)
- [ ] T046 [US4] Implement StopRepository.insertStop() with domain/entity mapping (Stop → StopEntity)
- [ ] T047 [US4] Implement StopRepository.updateStopEnd() with timestamp/duration calculation
- [ ] T048 [US4] Implement StopRepository.getStopsByRideId() with entity/domain mapping (StopEntity → Stop)
- [ ] T049 [US4] Add database validation in RideRecordingService after stop insert (verify stopId returned, handle constraint violations)
- [ ] T050 [US4] Test CASCADE delete behavior (create ride with stops, delete ride, verify stops auto-deleted via StopDaoTest)
- [ ] T051 [US4] Test UNIQUE constraint on (ride_id, stop_number) (attempt duplicate stopNumber, verify SQLiteConstraintException)
- [ ] T052 [US4] Test foreign key constraint (attempt insert with invalid rideId, verify SQLiteConstraintException)
- [ ] T053 [US4] Verify cluster_id field initialized as NULL for all new stop records (Feature 009 never sets this field)

**Checkpoint**: User Story 4 complete - all stop data persists correctly with foreign keys, CASCADE delete works, database constraints enforced, can be tested independently via Database Inspector and instrumented tests

---

## Phase 7: User Story 5 - Integration with Settings Thresholds (Priority: P1)

**Goal**: Read speed threshold and duration threshold from SettingsRepository (DataStore) when ride starts. Use thresholds for stop detection throughout ride. Support defaults (3 km/h, 15s) if not configured. Apply new thresholds only to future rides, not mid-ride.

**Independent Test**: Configure custom thresholds in Settings (speed=2 km/h, duration=10s), start ride, stop for exactly 10 seconds at 1.5 km/h, verify stop is detected. Change thresholds mid-ride, verify current ride continues using original thresholds. Start new ride, verify new thresholds are active.

### Implementation for User Story 5

- [ ] T054 [US5] Read stopDetectionConfig from SettingsRepository in RideRecordingService.startRide() (collect latest values from DataStore)
- [ ] T055 [US5] Pass speed threshold to StopDetectionStateMachine constructor (used for speed < threshold checks)
- [ ] T056 [US5] Pass duration threshold to StopDetectionStateMachine constructor (used for timer confirmation)
- [ ] T057 [US5] Apply default thresholds if SettingsRepository returns null (3 km/h, 15s per Feature 008 defaults)
- [ ] T058 [US5] Freeze thresholds at ride start (store in service scope, ignore DataStore changes mid-ride)
- [ ] T059 [US5] Add logging for threshold values in RideRecordingService.startRide() (debug: "Stop detection using speed=X km/h, duration=Y seconds")
- [ ] T060 [US5] Test threshold consumption (set custom values, verify state machine uses them correctly)
- [ ] T061 [US5] Test default threshold fallback (fresh install, no settings configured, verify 3 km/h and 15s are used)
- [ ] T062 [US5] Test mid-ride threshold changes don't affect current ride (change settings during ride, verify detection behavior unchanged)

**Checkpoint**: User Story 5 complete - settings thresholds are consumed correctly, defaults work, mid-ride changes don't affect active ride, can be tested independently by configuring various threshold combinations and verifying detection behavior

---

## Phase 8: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories, documentation, validation

- [ ] T063 [P] Add comprehensive logging for stop detection events in RideRecordingService (FR-001 to FR-028 coverage)
- [ ] T064 [P] Add error handling for database constraint violations (foreign key, UNIQUE, invalid timestamps)
- [ ] T065 [P] Add memory leak check (verify no Location objects stored in service state, only primitives extracted)
- [ ] T066 [P] Test app backgrounding during active stop (background app for 5 minutes, resume, verify stop continues, data not lost)
- [ ] T067 [P] Test GPS signal loss during active stop (disable location in emulator, re-enable, verify stop continues)
- [ ] T068 [P] Test rapid speed oscillations around threshold (GPS noise simulation, verify consecutive seconds filtering works)
- [ ] T069 [P] Test extremely long stops (30+ minutes, verify duration counter doesn't overflow, database stores correctly)
- [ ] T070 [P] Test stop detection while ride paused (verify no stops detected during PAUSED state, detection only active during RECORDING)
- [ ] T071 Manual emulator testing with GPX route playback (load route, ride, stop, verify popup and database)
- [ ] T072 Update TODO.md with Feature 009 status (move to "In Progress" with start date, task checklist)
- [ ] T073 Update RELEASE.md Unreleased section with Feature 009 entry (Stop Detection & Recording with user story summary)
- [ ] T074 Run quickstart.md validation (verify all implementation roadmap phases completed, Definition of Done checklist satisfied)
- [ ] T075 Code review against CLAUDE.md standards (Kotlin-first, immutability, null safety, MVVM architecture, Material 3 UI)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
  - Creates database schema, entities, DAOs, repositories
  - BLOCKS all other phases (database must exist before logic can use it)

- **Foundational (Phase 2)**: Depends on Setup (Phase 1) completion
  - Creates state machine, utils, writes tests for infrastructure
  - BLOCKS all user story phases (logic must be tested before integration)

- **User Story 1 (Phase 3)**: Depends on Foundational (Phase 2) completion
  - Core stop detection during rides
  - **MVP CORE** - All other user stories depend on this working
  - BLOCKS US2, US3 (they need stop events from US1)
  - US4 and US5 could theoretically run in parallel with US1, but recommend sequential for clarity

- **User Story 2 (Phase 4)**: Depends on US1 (Phase 3) completion
  - Displays stop popup based on events from US1
  - Can run in parallel with US3 if team capacity allows (different files)

- **User Story 3 (Phase 5)**: Depends on US1 (Phase 3) completion
  - Displays stop count based on database updates from US1
  - Can run in parallel with US2 if team capacity allows (different files)

- **User Story 4 (Phase 6)**: Depends on Setup (Phase 1) completion
  - Database persistence layer (DAO implementation, repository)
  - US1 depends on this for stop insertion/updates
  - Could theoretically run in parallel with Foundational (Phase 2), but recommend after US1 for validation

- **User Story 5 (Phase 7)**: Depends on US1 (Phase 3) completion
  - Settings integration (reads thresholds from DataStore)
  - Final integration piece, touches US1 service code
  - Must be sequential after US1

- **Polish (Phase 8)**: Depends on all user stories being complete
  - Cross-cutting logging, error handling, testing
  - Can proceed when all 5 user stories are functional

### Critical Path (Must Be Sequential)

1. **Phase 1 (Setup)** → Database schema MUST exist first
2. **Phase 2 (Foundational)** → State machine MUST be tested before use
3. **Phase 3 (US1)** → Core detection MUST work before UI/display features
4. **Phase 4-7 (US2-US5)** → Can proceed after US1 is stable
5. **Phase 8 (Polish)** → Final validation and documentation

### Recommended Execution Order (Solo Developer)

1. Phase 1: Setup (database layer) - ~2-3 hours
2. Phase 2: Foundational (state machine + tests) - ~3-4 hours
3. **VALIDATE**: Run unit tests, verify state machine works
4. Phase 3: User Story 1 (core detection) - ~4-5 hours
5. **VALIDATE**: Test on emulator with GPX route, verify database records
6. Phase 6: User Story 4 (database persistence) - ~2-3 hours
7. **VALIDATE**: Test CASCADE delete, UNIQUE constraints
8. Phase 7: User Story 5 (settings integration) - ~2 hours
9. **VALIDATE**: Test with custom thresholds
10. Phase 4: User Story 2 (stop popup UI) - ~2-3 hours
11. **VALIDATE**: Test popup appearance/dismissal
12. Phase 5: User Story 3 (stop count display) - ~1-2 hours
13. **VALIDATE**: Test counter updates
14. Phase 8: Polish (logging, edge cases, docs) - ~3-4 hours
15. **FINAL VALIDATION**: Run all tests, manual emulator testing, quickstart.md checklist

**Total Estimated Time**: 25-30 hours of focused development

### Parallel Opportunities (Multiple Developers)

**After Foundational (Phase 2) completes**:

- **Developer A**: Phase 3 (US1) + Phase 7 (US5) - Core detection + settings (sequential, 6-7 hours)
- **Developer B**: Phase 4 (US2) + Phase 5 (US3) - UI components (parallel, 3-5 hours)
- **Developer C**: Phase 6 (US4) - Database persistence + tests (parallel, 2-3 hours)
- **Team**: Phase 8 (Polish) - Cross-cutting concerns (after all stories complete, 3-4 hours)

**Within Each Phase**:

- Phase 1: T001-T003 marked [P] can run in parallel (different files)
- Phase 2: T010, T012-T014 marked [P] can run in parallel (tests, utils)
- Phase 4 (US2): T026 (StopPopup) can start while T027-T029 (ViewModel) in progress
- Phase 5 (US3): T034 (RideStatsRow) can start while T035-T036 (ViewModel) in progress
- Phase 8: Most polish tasks marked [P] can run in parallel (logging, tests, docs)

---

## Parallel Example: User Story 1 (Core Detection)

```bash
# These tasks CANNOT be parallelized (sequential dependencies):
T016 [US1] Modify TrackLocationUseCase.kt (integrate state machine)
  ↓ (depends on T016)
T017 [US1] Modify RideRecordingService.kt (own state machine instance)
  ↓ (depends on T017)
T018 [US1] Add stop detection initialization in startRide()
  ↓ (depends on T018)
T021 [US1] Implement consecutive seconds filtering in location callback
  ↓ (depends on T021)
T022 [US1] Implement stop confirmation logic (insert to database)
```

**Rationale**: These tasks all modify the same file (RideRecordingService.kt) or have direct dependencies. Must be done sequentially.

---

## Parallel Example: User Story 2 (Stop Popup UI)

```bash
# Launch these tasks in parallel (different files):
Task T026 [P] [US2]: "Create StopPopup.kt composable"
  (works in: ui/components/ride/StopPopup.kt)

# Then launch these together (same file, but different sections):
Task T027 [US2]: "Add stop popup state to RideViewModel.kt"
Task T028 [US2]: "Collect stop events from Service in RideViewModel"
  (both work in: ui/viewmodel/RideViewModel.kt - can be done in one edit)
```

**Rationale**: T026 creates new file, can proceed immediately. T027-T028 modify same file, should be grouped together but separate from T026.

---

## Implementation Strategy

### MVP First (User Stories 1, 4, 5 Only)

**Minimum Viable Product for Feature 009**:

1. Complete Phase 1: Setup (database layer) - ~2-3 hours
2. Complete Phase 2: Foundational (state machine + tests) - ~3-4 hours
3. Complete Phase 3: User Story 1 (core detection) - ~4-5 hours
4. Complete Phase 6: User Story 4 (database persistence) - ~2-3 hours
5. Complete Phase 7: User Story 5 (settings integration) - ~2 hours
6. **STOP and VALIDATE**: Test on emulator with GPX route, verify database records
7. Skip US2 (popup) and US3 (count display) initially
8. **MVP READY**: Stop detection works, data is persisted, thresholds are configurable

**MVP Delivers**:
- ✅ Stops are detected during rides (FR-001 to FR-010)
- ✅ Stops are saved to database (FR-011, FR-012, FR-022, FR-024)
- ✅ Settings thresholds are used (FR-019 to FR-021)
- ❌ No visual feedback (no popup) - users won't know stops are detected
- ❌ No live counter (no stop count display) - users can't see how many stops

**MVP Limitations**: Without US2 and US3, users have no visual confirmation that stop detection is working. They must check the database manually or wait for Feature 010 (clustering) to see stop data. This is acceptable for internal testing but not for production release.

**Recommendation**: Implement US2 (popup) as part of MVP - only adds 2-3 hours but provides critical user trust.

### Incremental Delivery (Recommended)

1. Complete Setup + Foundational (Phases 1-2) → Foundation ready (~5-7 hours)
2. **Increment 1**: Add US1 + US4 + US5 → Test independently → Core detection working (~8-10 hours)
3. **Increment 2**: Add US2 → Test independently → Popup feedback working (~2-3 hours)
4. **Increment 3**: Add US3 → Test independently → Stop count display working (~1-2 hours)
5. **Increment 4**: Polish (Phase 8) → Final validation (~3-4 hours)
6. **TOTAL**: 19-26 hours, each increment adds value and can be tested

### Parallel Team Strategy

With 3 developers (after Foundational phase completes):

1. **Team completes Setup + Foundational together** (Phases 1-2) → ~5-7 hours
2. **Once Foundational is done, split work**:
   - **Developer A**: US1 (core detection) → US5 (settings) → Sequential, 6-7 hours
   - **Developer B**: US2 (popup UI) → US3 (stop count) → Sequential, 3-5 hours
   - **Developer C**: US4 (database persistence) → Tests → Parallel, 2-3 hours
3. **Team merges and validates together** → ~2 hours
4. **Team completes Polish together** (Phase 8) → ~3-4 hours

**Total Time (3 developers)**: ~17-24 hours calendar time (vs 25-30 hours solo)

---

## Notes

- **[P] tasks**: Different files, no dependencies, can run in parallel
- **[Story] label**: Maps task to specific user story for traceability (US1-US5)
- **Each user story is independently testable**: Can validate each story in isolation before moving to next
- **Tests are included**: This is a safety-critical feature dealing with location data and database integrity
- **Commit strategy**: Commit after each task or logical group (small, frequent commits per CLAUDE.md)
- **Checkpoints**: Stop at each checkpoint to validate story independently
- **MVP**: US1 + US4 + US5 provide core detection, but US2 is recommended for user trust
- **Database migration**: MUST test migration 4→5 thoroughly before production (no rollback possible)
- **GPS noise filtering**: 3-second consecutive filtering is CRITICAL - do not skip this logic
- **Memory safety**: NEVER store Location objects in service state (Context leak risk)
- **Service scope**: State machine MUST live in RideRecordingService, NOT ViewModel (survives backgrounding)
- **Foreign keys**: Verify CASCADE delete works correctly (instrumented tests required)
- **Performance targets**: <1s stop detection latency, <100ms database insert, 1Hz UI update frequency
- **Avoid**: Hardcoded thresholds (always read from settings), Location object storage (memory leaks), state in ViewModel (lost on background)

---

## Post-Implementation Validation

Before marking Feature 009 as complete, verify:

- ✅ All 75 tasks completed (T001-T075)
- ✅ All unit tests pass (state machine, repository, utils)
- ✅ All instrumented tests pass (DAO, CASCADE delete, UNIQUE constraints)
- ✅ Manual emulator test with GPX route shows correct stop detection
- ✅ Database Inspector shows stops table with correct foreign keys and indexes
- ✅ App survives backgrounding during active stop (no data loss)
- ✅ GPS signal loss handled gracefully (no crashes)
- ✅ Settings thresholds are consumed correctly (test custom values)
- ✅ Popup appears/dismisses correctly with smooth animation
- ✅ Stop count updates in real-time on Live tab
- ✅ TODO.md updated (moved to "In Progress" with checklist)
- ✅ RELEASE.md updated (added to "Unreleased" section)
- ✅ Code review passed (follows CLAUDE.md standards)
- ✅ All FR-001 to FR-028 functional requirements implemented
- ✅ All SC-001 to SC-012 success criteria met

**Final Step**: Run quickstart.md "Definition of Done" checklist before creating PR and release.
