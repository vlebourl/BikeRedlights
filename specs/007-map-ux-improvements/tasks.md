# Tasks: Map UX Improvements (v0.6.1 Patch)

**Input**: Design documents from `/specs/007-map-ux-improvements/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Tests are NOT explicitly requested in the spec. Implementation tasks only.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story. User stories are ordered by priority (P1 > P2 > P3).

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Android project**: `app/src/main/java/com/example/bikeredlights/`
- **Tests**: `app/src/test/java/com/example/bikeredlights/`
- All paths are relative to repository root

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: No new infrastructure needed - all changes build on existing v0.6.0 components

**Status**: ✅ Already complete - v0.6.0 has Maps Compose, DataStore, ViewModels infrastructure

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core data models and shared state management that multiple user stories depend on

**⚠️ CRITICAL**: These tasks MUST be complete before ANY user story work begins

- [X] T001 [P] Add bearing field to MapViewState data class in `app/src/main/java/com/example/bikeredlights/domain/model/MapViewState.kt`
- [X] T002 [P] Add currentBearing StateFlow to RideRecordingViewModel in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`
- [X] T003 Update RideRecordingViewModel.onLocationUpdate to extract and emit bearing from Location in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`

**Checkpoint**: Foundation ready - User Story 1 & 2 (map/marker) can now begin in parallel

---

## Phase 3: User Story 4 - Granular Auto-Pause Timing Options (Priority: P3) 🎯 MVP

**Goal**: Allow users to configure auto-pause sensitivity with 6 timing options (1s, 2s, 5s, 10s, 15s, 30s) instead of current limited set

**Why P3 is MVP**: Most independent story with zero dependencies on bearing/map changes. Can be implemented, tested, and delivered first to validate release pipeline and settings infrastructure.

**Independent Test**: Navigate to Settings > Auto-Pause Timing, select each of the 6 options (1s, 2s, 5s, 10s, 15s, 30s), verify selection persists after app restart, start ride and verify auto-pause triggers at selected threshold

### Implementation for User Story 4

- [X] T004 [P] [US4] Add AUTO_PAUSE_THRESHOLD_KEY and accessor methods to SettingsRepository in `app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepository.kt`
- [X] T005 [P] [US4] Add autoPauseThreshold StateFlow to SettingsViewModel in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/SettingsViewModel.kt`
- [X] T006 [P] [US4] Add setAutoPauseThreshold method to SettingsViewModel with validation in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/SettingsViewModel.kt`
- [X] T007 [US4] Create AutoPauseSettingsScreen composable with 6 radio button options in `app/src/main/java/com/example/bikeredlights/ui/screens/settings/AutoPauseSettingsScreen.kt`
- [X] T008 [US4] Add navigation route for AutoPauseSettingsScreen in navigation graph (location depends on existing nav structure)
- [X] T009 [US4] Add navigation call from SettingsHomeScreen to AutoPauseSettingsScreen in `app/src/main/java/com/example/bikeredlights/ui/screens/settings/SettingsHomeScreen.kt`

**Checkpoint**: User Story 4 complete and independently testable. Settings persist, UI displays 6 options, auto-pause respects selected threshold.

---

## Phase 4: User Story 3 - Real-Time Pause Counter (Priority: P2)

**Goal**: Display pause duration that updates every second while paused, instead of showing static counter

**Independent Test**: Start ride, trigger auto-pause by remaining stationary, observe counter incrementing every second (0:01, 0:02, 0:03...), lock device screen, unlock and verify counter shows accurate elapsed time, resume riding and verify final pause time matches what was displayed

### Implementation for User Story 3

- [X] T010 [P] [US3] Add _pauseStartTime MutableStateFlow to RideRecordingViewModel in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`
- [X] T011 [US3] Create pausedDuration StateFlow with Flow.flatMapLatest that emits every second in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`
- [X] T012 [US3] Update RideRecordingViewModel.pauseRide to set _pauseStartTime to Instant.now() in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`
- [X] T013 [US3] Update RideRecordingViewModel.resumeRide to reset _pauseStartTime and accumulate total paused time in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`
- [X] T014 [US3] Add pausedDuration parameter to RideStatistics composable in `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatistics.kt`
- [X] T015 [US3] Add real-time pause counter display with MM:SS formatting in RideStatistics composable in `app/src/main/java/com/example/bikeredlights/ui/components/ride/RideStatistics.kt`
- [X] T016 [US3] Update LiveRideScreen to collect pausedDuration and pass to RideStatistics in `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt`

**Checkpoint**: User Story 3 complete and independently testable. Pause counter updates in real-time, survives screen lock, final value matches displayed value on resume.

---

## Phase 5: User Story 1 - Directional Map Orientation (Priority: P1)

**Goal**: Rotate map to follow rider's heading direction instead of always pointing north, making navigation intuitive

**Dependencies**: Phase 2 (bearing StateFlow) must be complete

**Independent Test**: Start ride with GPS enabled, move in different directions (N, S, E, W, NE, SW), verify map rotates smoothly to keep your direction pointing upward, stand still and verify map maintains last bearing or reverts to north-up

### Implementation for User Story 1

- [ ] T017 [US1] Update RideRecordingViewModel to sync MapViewState.bearing with currentBearing in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`
- [ ] T018 [US1] Add currentBearing parameter to BikeMap composable signature in `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`
- [ ] T019 [US1] Update CameraPositionState initialization to use mapViewState.bearing in BikeMap in `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`
- [ ] T020 [US1] Add LaunchedEffect to animate CameraPositionState bearing changes with 300ms duration in `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`
- [ ] T021 [US1] Add bearing debouncing logic (only animate if delta > 5 degrees) in `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`
- [ ] T022 [US1] Update LiveRideScreen to collect currentBearing and pass to BikeMap in `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt`

**Checkpoint**: User Story 1 complete and independently testable. Map rotates smoothly to follow heading, debouncing prevents jitter, fallback to north-up when bearing unavailable.

---

## Phase 6: User Story 2 - Directional Location Marker (Priority: P1)

**Goal**: Replace generic pin with directional arrow/bike icon that rotates to show heading direction

**Dependencies**: Phase 2 (bearing StateFlow) must be complete

**Independent Test**: Start ride, observe blue arrow marker pointing in direction of travel, turn 90 degrees and verify arrow rotates, stand still and verify marker switches to pin or static arrow, resume movement and verify arrow returns. View completed ride and verify start/end markers are pins (not arrows).

### Implementation for User Story 2

- [ ] T023 [P] [US2] Create LocationMarker composable with bearing and isMoving parameters in `app/src/main/java/com/example/bikeredlights/ui/components/map/LocationMarker.kt`
- [ ] T024 [P] [US2] Implement directional arrow drawing using Canvas API in LocationMarker in `app/src/main/java/com/example/bikeredlights/ui/components/map/LocationMarker.kt`
- [ ] T025 [P] [US2] Apply graphicsLayer rotation transformation based on bearing in LocationMarker in `app/src/main/java/com/example/bikeredlights/ui/components/map/LocationMarker.kt`
- [ ] T026 [P] [US2] Implement pin icon fallback when isMoving is false in LocationMarker in `app/src/main/java/com/example/bikeredlights/ui/components/map/LocationMarker.kt`
- [ ] T027 [US2] Integrate LocationMarker into BikeMap using MarkerInfoWindowContent or overlay in `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`
- [ ] T028 [US2] Update BikeMap to pass currentBearing and isMoving (bearing != null) to LocationMarker in `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`
- [ ] T029 [US2] Verify RideDetailScreen and RideReviewScreen still display pin markers (not directional arrows) for completed rides in `app/src/main/java/com/example/bikeredlights/ui/screens/history/RideDetailScreen.kt` and `app/src/main/java/com/example/bikeredlights/ui/screens/ride/RideReviewScreen.kt`

**Checkpoint**: User Story 2 complete and independently testable. Directional marker rotates with heading, falls back to pin when stationary, completed ride maps still show pins.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentation, validation, and final integration testing across all user stories

- [ ] T030 [P] Update TODO.md with v0.6.1 feature status in `/Users/vlb/AndroidStudioProjects/BikeRedlights/TODO.md`
- [ ] T031 [P] Update RELEASE.md with v0.6.1 patch notes and all 4 feature additions in `/Users/vlb/AndroidStudioProjects/BikeRedlights/RELEASE.md`
- [ ] T032 [P] Update CLAUDE.md Active Technologies section with v0.6.1 additions in `/Users/vlb/AndroidStudioProjects/BikeRedlights/CLAUDE.md`
- [ ] T033 Run emulator integration test: Settings → Pause Counter → Map Bearing → Marker Rotation as described in quickstart.md
- [ ] T034 Verify all 4 user stories work together harmoniously (no conflicts or performance issues)
- [ ] T035 Run lint checks and address any warnings introduced by changes
- [ ] T036 Verify app builds successfully with `./gradlew assembleRelease`

**Checkpoint**: All polish tasks complete. Ready for pull request creation and release tagging.

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: ✅ Already complete (v0.6.0 infrastructure)
- **Foundational (Phase 2)**: T001-T003 MUST complete before user stories - BLOCKS all stories
- **User Stories (Phase 3-6)**: All depend on Phase 2 completion
  - **US4 (P3)**: Zero dependencies on other stories - BEST MVP candidate
  - **US3 (P2)**: Independent, no dependencies on bearing/map changes
  - **US1 (P1)**: Depends on Phase 2 bearing StateFlow
  - **US2 (P1)**: Depends on Phase 2 bearing StateFlow
- **Polish (Phase 7)**: Depends on desired user stories being complete

### User Story Dependencies

**Visual Dependency Graph**:
```
Phase 2: Foundational (T001-T003)
    ├─→ US4 (P3) [T004-T009] ← FULLY INDEPENDENT (Best MVP)
    ├─→ US3 (P2) [T010-T016] ← FULLY INDEPENDENT
    ├─→ US1 (P1) [T017-T022] ← Depends on Phase 2 bearing
    └─→ US2 (P1) [T023-T029] ← Depends on Phase 2 bearing
```

- **User Story 4 (P3)**: Can start immediately after Phase 2 - ZERO dependencies on other stories, all settings changes
- **User Story 3 (P2)**: Can start immediately after Phase 2 - ZERO dependencies on other stories, all pause counter changes
- **User Story 1 (P1)**: Can start immediately after Phase 2 - Uses bearing StateFlow for map rotation
- **User Story 2 (P1)**: Can start immediately after Phase 2 - Uses bearing StateFlow for marker rotation, can work in parallel with US1

**Key Insight**: Despite US1 and US2 being P1 priority, US4 (P3) is the best MVP candidate because it has ZERO cross-dependencies and validates the release pipeline.

### Within Each User Story

- **US4**: All tasks T004-T009 follow SettingsRepository → ViewModel → UI → Navigation pattern
  - T004-T006 can run in parallel (different concerns)
  - T007 depends on T005-T006 completion
  - T008-T009 are sequential navigation wiring

- **US3**: Tasks follow ViewModel Flow → UI integration pattern
  - T010-T013 are sequential in ViewModel (build pause timer Flow)
  - T014-T015 can run in parallel with T010-T013 (UI changes)
  - T016 is final integration

- **US1**: Tasks follow state → map component → screen integration pattern
  - T017 is prerequisite for all others
  - T018-T021 are sequential in BikeMap (bearing animation logic)
  - T022 is final integration

- **US2**: Tasks follow marker component → map integration pattern
  - T023-T026 can run in parallel (different LocationMarker aspects)
  - T027-T028 are sequential integration into BikeMap
  - T029 is validation of completed ride screens

### Parallel Opportunities

**Within Phase 2 (Foundational)**:
- T001 and T002 can run in parallel (different files)
- T003 depends on T002 completion

**Within User Stories**:
- **US4**: T004, T005, T006 can run in parallel (marked [P])
- **US3**: T010-T013 (ViewModel) can run in parallel with T014-T015 (UI) (marked [P])
- **US2**: T023, T024, T025, T026 can run in parallel (marked [P])

**Across User Stories** (after Phase 2 complete):
- US4 and US3 can run fully in parallel (zero conflicts)
- US1 and US2 can run in parallel if different developers work on them
- All 4 stories CAN be worked on simultaneously by 4 developers after Phase 2

---

## Parallel Example: Phase 2 (Foundational)

```bash
# Task Agent 1
Task: "Add bearing field to MapViewState data class in app/src/main/java/.../domain/model/MapViewState.kt"

# Task Agent 2 (parallel)
Task: "Add currentBearing StateFlow to RideRecordingViewModel in app/src/main/java/.../ui/viewmodel/RideRecordingViewModel.kt"

# Task Agent 3 (after Agent 2 completes)
Task: "Update RideRecordingViewModel.onLocationUpdate to extract and emit bearing from Location in app/src/main/java/.../ui/viewmodel/RideRecordingViewModel.kt"
```

---

## Parallel Example: User Story 4

```bash
# Launch in parallel (different concerns, can be done together):
Task: "Add AUTO_PAUSE_THRESHOLD_KEY and accessor methods to SettingsRepository"
Task: "Add autoPauseThreshold StateFlow to SettingsViewModel"
Task: "Add setAutoPauseThreshold method to SettingsViewModel with validation"

# Then sequentially:
Task: "Create AutoPauseSettingsScreen composable"
Task: "Add navigation route for AutoPauseSettingsScreen"
Task: "Add navigation call from SettingsHomeScreen"
```

---

## Parallel Example: User Story 2

```bash
# Launch in parallel (different LocationMarker aspects):
Task: "Create LocationMarker composable with bearing and isMoving parameters"
Task: "Implement directional arrow drawing using Canvas API in LocationMarker"
Task: "Apply graphicsLayer rotation transformation based on bearing in LocationMarker"
Task: "Implement pin icon fallback when isMoving is false in LocationMarker"

# Then integrate:
Task: "Integrate LocationMarker into BikeMap using MarkerInfoWindowContent or overlay"
Task: "Update BikeMap to pass currentBearing and isMoving to LocationMarker"
Task: "Verify RideDetailScreen and RideReviewScreen still display pin markers for completed rides"
```

---

## Implementation Strategy

### MVP First (User Story 4 Only) - RECOMMENDED

**Why US4 as MVP**: Most independent story, zero dependencies on bearing/map changes, validates entire release pipeline (DataStore → ViewModel → UI → Navigation → Settings persistence)

1. Complete Phase 2: Foundational (T001-T003)
2. Complete Phase 3: User Story 4 (T004-T009)
3. **STOP and VALIDATE**: Test US4 independently
   - Navigate to Settings > Auto-Pause
   - Select each of 6 options
   - Restart app, verify persistence
   - Start ride, verify auto-pause threshold works
4. Commit small, frequent commits per quickstart.md guidance
5. Create PR for v0.6.1 MVP with just US4
6. Deploy/demo to validate release process

### Incremental Delivery (Recommended Order)

**Based on independence, not priority**:

1. Complete Phase 2: Foundational → Bearing data available
2. Add US4 (P3) → Settings complete → Deploy/Demo (MVP!)
3. Add US3 (P2) → Pause counter complete → Deploy/Demo
4. Add US1 (P1) → Map rotation complete → Deploy/Demo
5. Add US2 (P1) → Directional marker complete → Deploy/Demo
6. Polish phase → Full v0.6.1 release

**Rationale**: US4 and US3 are fully independent and can be delivered early to gain confidence. US1 and US2 both touch BikeMap.kt, so doing them sequentially reduces merge conflicts.

### Parallel Team Strategy

With 2-4 developers after Phase 2 completion:

**2 Developers**:
- Dev A: US4 → US3 (both independent)
- Dev B: US1 → US2 (both map-related)

**3 Developers**:
- Dev A: US4 (settings)
- Dev B: US3 (pause counter)
- Dev C: US1 + US2 (map + marker, sequential to avoid BikeMap.kt conflicts)

**4 Developers** (maximum parallelism):
- Dev A: US4
- Dev B: US3
- Dev C: US1
- Dev D: US2 (coordinate with Dev C on BikeMap.kt integration)

---

## Task Summary

**Total Tasks**: 36 tasks (T001-T036)

**Tasks per User Story**:
- Phase 1 (Setup): 0 tasks (already complete)
- Phase 2 (Foundational): 3 tasks (T001-T003)
- Phase 3 (US4 - Auto-Pause Settings): 6 tasks (T004-T009)
- Phase 4 (US3 - Pause Counter): 7 tasks (T010-T016)
- Phase 5 (US1 - Map Bearing): 6 tasks (T017-T022)
- Phase 6 (US2 - Directional Marker): 7 tasks (T023-T029)
- Phase 7 (Polish): 7 tasks (T030-T036)

**Parallel Opportunities Identified**:
- Phase 2: 2 tasks can run in parallel (T001, T002)
- US4: 3 tasks can run in parallel (T004, T005, T006)
- US3: 2 task groups can run in parallel (T010-T013 with T014-T015)
- US2: 4 tasks can run in parallel (T023-T026)
- Polish: 3 tasks can run in parallel (T030, T031, T032)
- **Cross-story parallelism**: All 4 user stories can run in parallel after Phase 2

**MVP Scope**: Phase 2 + Phase 3 (US4 only) = 9 tasks for validated MVP

**Full Feature Scope**: All 36 tasks for complete v0.6.1 patch release

---

## Format Validation: ✅ PASSED

All tasks follow the required checklist format:
- ✅ Every task starts with `- [ ]` (checkbox)
- ✅ Every task has sequential ID (T001-T036)
- ✅ [P] marker present for parallelizable tasks
- ✅ [Story] label present for all user story phase tasks (US1, US2, US3, US4)
- ✅ Exact file paths included in descriptions
- ✅ Tasks organized by user story for independent implementation
- ✅ No setup/foundational/polish tasks have [Story] labels (correctly omitted)

---

## Notes

- **No tests included**: Tests not explicitly requested in feature specification
- **Commit strategy**: Follow quickstart.md guidance for small commits (<200 LOC per commit)
- **Emulator testing**: GPS simulation required for US1 and US2 (bearing-dependent features)
- **Settings validation**: US4 requires testing on physical device or emulator with app restart
- **Pause counter validation**: US3 requires testing with device screen lock to verify background behavior
- **MVP recommendation**: Start with US4 (most independent) to validate release pipeline before tackling map/marker changes
- **Avoid**: Working on US1 and US2 simultaneously in BikeMap.kt (will cause merge conflicts)
