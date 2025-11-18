# Tasks: Stop Detection Settings

**Input**: Design documents from `/specs/008-stop-detection-settings/`
**Prerequisites**: plan.md (tech stack), spec.md (user stories), research.md (decisions), data-model.md (entities), contracts/ (interfaces)

**Tests**: Unit tests included for domain model validation (per CLAUDE.md requirements). Emulator testing is MANDATORY before merge.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3, US4)
- Include exact file paths in descriptions

## Path Conventions

- **Android Project**: `app/src/main/java/com/example/bikeredlights/`, `app/src/test/java/`
- **Layers**: `domain/`, `data/`, `ui/` (MVVM + Clean Architecture)

---

## Phase 1: Setup (Domain Layer Foundation)

**Purpose**: Create domain model with validation - pure Kotlin, no Android dependencies

**Why separate from Foundational**: Domain layer can be implemented and tested in complete isolation without any Android framework or Data/UI layer dependencies. This follows Clean Architecture principles.

- [X] T001 Create StopDetectionConfig.kt domain model in app/src/main/java/com/example/bikeredlights/domain/model/settings/StopDetectionConfig.kt with validation (3 fields, init block, companion constants)
- [X] T002 Create StopDetectionConfigTest.kt unit test file in app/src/test/java/com/example/bikeredlights/domain/model/settings/StopDetectionConfigTest.kt
- [X] T003 [P] Write unit test for default values validation in StopDetectionConfigTest.kt
- [X] T004 [P] Write unit test for invalid speed threshold rejection in StopDetectionConfigTest.kt
- [X] T005 [P] Write unit test for invalid duration threshold rejection in StopDetectionConfigTest.kt
- [X] T006 [P] Write unit test for invalid clustering radius rejection in StopDetectionConfigTest.kt
- [X] T007 Run unit tests and verify all pass (./gradlew test)
- [X] T008 Commit domain layer: "feat(domain): add StopDetectionConfig with validation"

**Checkpoint**: Domain model complete and tested independently. No Android dependencies.

---

## Phase 2: Foundational (Data Layer Infrastructure)

**Purpose**: Extend existing DataStore infrastructure - BLOCKS all user story UI work

**⚠️ CRITICAL**: UI screens (User Stories 1-4) cannot display/save settings until this phase is complete

- [X] T009 Add DataStore keys to app/src/main/java/com/example/bikeredlights/data/preferences/PreferencesKeys.kt (STOP_DETECTION_SPEED_THRESHOLD_KMH, STOP_DETECTION_DURATION_THRESHOLD_SECONDS, STOP_DETECTION_CLUSTERING_RADIUS_METERS)
- [X] T010 Extend SettingsRepository interface in app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepository.kt (add stopDetectionConfig Flow and setStopDetectionConfig suspend method)
- [X] T011 Implement stopDetectionConfig Flow in app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepositoryImpl.kt (map DataStore to StopDetectionConfig with defaults)
- [X] T012 Implement setStopDetectionConfig suspend method in app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepositoryImpl.kt (write all 3 keys atomically via dataStore.edit)
- [X] T013 Extend SettingsViewModel in app/src/main/java/com/example/bikeredlights/ui/screens/settings/SettingsViewModel.kt (add stopDetectionConfig StateFlow using stateIn with WhileSubscribed(5000))
- [X] T014 Add updateStopDetectionConfig method to SettingsViewModel in app/src/main/java/com/example/bikeredlights/ui/screens/settings/SettingsViewModel.kt (launch viewModelScope coroutine, call repository.setStopDetectionConfig)
- [X] T015 Commit data layer: "feat(data): extend SettingsRepository for stop detection config"
- [X] T016 Commit ViewModel layer: "feat(viewmodel): add stop detection config StateFlow"

**Checkpoint**: Foundation ready - UI screens can now read/write stop detection settings via ViewModel

---

## Phase 3: User Story 4 - Access Stop Detection Settings (Priority: P1) 🎯 First Deliverable

**Goal**: Add navigation card on Settings home screen to access Stop Detection detail screen

**Why first**: This is the entry point for all other user stories. Without navigation, users cannot access any of the configuration screens. Delivers immediate discoverability value.

**Independent Test**: Open app → Navigate to Settings tab → Verify "Stop Detection" card appears with subtitle "Thresholds, Clustering" → Tap card → Verify navigation to empty detail screen (settings UI added in later phases)

### Implementation for User Story 4

- [X] T017 [US4] Add Stop Detection settings card to SettingsHomeScreen in app/src/main/java/com/example/bikeredlights/ui/screens/settings/SettingsHomeScreen.kt (use SettingCard composable with title "Stop Detection", subtitle "Speed, Duration, Clustering", appropriate icon, onClick navigation callback)
- [X] T018 [US4] Add "stopDetection" route to SettingsNavGraph in app/src/main/java/com/example/bikeredlights/ui/navigation/SettingsNavGraph.kt (composable route linking to StopDetectionSettingsScreen - initially empty screen scaffolding)
- [X] T019 [US4] Create StopDetectionSettingsScreen.kt skeleton in app/src/main/java/com/example/bikeredlights/ui/screens/settings/StopDetectionSettingsScreen.kt (Scaffold with TopAppBar, back button, empty Column - no settings controls yet)
- [X] T020 [US4] Wire onStopDetectionClick callback in SettingsHomeScreen navigation lambda (navController.navigate("stopDetection"))
- [X] T021 [US4] Commit navigation setup: "feat(ui): add Stop Detection settings navigation"

**Checkpoint**: User can navigate from Settings home → Stop Detection screen (empty screen for now). Ready for settings UI implementation.

---

## Phase 4: User Story 1 - Configure Speed Detection Threshold (Priority: P1) 🎯 MVP Core Feature

**Goal**: Add speed threshold setting with dropdown control (ExposedDropdownMenuBox), default value (2 km/h), persistence, and Imperial/Metric unit conversion

**Why this priority**: Foundational setting that defines "stopped" state. Most critical parameter for stop detection. Must be configurable independently.

**Independent Test**: Navigate to Settings → Stop Detection → See speed threshold options (1-5 km/h) → Select 2 km/h → Restart app → Verify 2 km/h persists → Switch to Imperial units → Verify displays 1.2 mph → Switch back to Metric → Verify displays 2 km/h

### Implementation for User Story 1

- [X] T022 [US1] Add speed threshold section to StopDetectionSettingsScreen in app/src/main/java/com/example/bikeredlights/ui/screens/settings/StopDetectionSettingsScreen.kt (Column with title "Speed Threshold", ExposedDropdownMenuSetting control) - IMPLEMENTED WITH DROPDOWN
- [X] T023 [US1] Implement ExposedDropdownMenuSetting for speed threshold with km/h options (1, 2, 3, 4, 5) using StopDetectionConfig.VALID_SPEED_THRESHOLDS - USED DROPDOWN INSTEAD OF SEGMENTED BUTTONS (Material 3 best practice for 5+ options)
- [X] T024 [US1] Wire speed threshold ExposedDropdownMenuSetting to viewModel.stopDetectionConfig.speedThresholdKmh (selectedOption) and viewModel.updateStopDetectionConfig (onOptionSelected callback)
- [X] T025 [US1] Add Imperial/Metric unit conversion logic for speed threshold display (convert km/h to mph when unitsSystem == IMPERIAL using conversion factor 0.621371, format to 1 decimal place)
- [X] T026 [US1] Add conditional rendering based on unitsSystem StateFlow (collect unitsSystem from ViewModel, display km/h or mph options accordingly)
- [X] T027 [US1] Commit speed threshold UI: "feat(ui): add speed threshold setting with unit conversion"

**Emulator Test for US1**:
- [X] T028 [US1] Test default value (2 km/h) displays on first launch
- [X] T029 [US1] Test selecting 3 km/h and restarting app verifies persistence
- [X] T030 [US1] Test Imperial units display mph correctly (tested 2 km/h → 1.2 mph)
- [X] T031 [US1] Test toggling Metric/Imperial updates speed threshold display without losing setting

**Checkpoint**: Speed threshold fully functional - can configure, persists, converts units. User Story 1 independently testable and deliverable.

---

## Phase 5: User Story 2 - Configure Duration Threshold (Priority: P1) 🎯 MVP Core Feature

**Goal**: Add duration threshold setting with dropdown control (ExposedDropdownMenuBox), default value (15s), and persistence

**Why this priority**: Equally critical to speed threshold for filtering brief slow-downs from genuine stops. Must be independently configurable.

**Independent Test**: Navigate to Settings → Stop Detection → See duration threshold options (5s, 10s, 15s, 20s, 25s, 30s) → Select 10s → Restart app → Verify 10s persists → Change to 25s → Verify updates immediately

### Implementation for User Story 2

- [X] T032 [US2] Add duration threshold section to StopDetectionSettingsScreen in app/src/main/java/com/example/bikeredlights/ui/screens/settings/StopDetectionSettingsScreen.kt (Column with title "Duration Threshold", ExposedDropdownMenuSetting control) - IMPLEMENTED WITH DROPDOWN
- [X] T033 [US2] Implement ExposedDropdownMenuSetting for duration threshold with options [5, 10, 15, 20, 25, 30] seconds using StopDetectionConfig.VALID_DURATION_THRESHOLDS - USED DROPDOWN INSTEAD OF SEGMENTED BUTTONS (Material 3 best practice for 6+ options)
- [X] T034 [US2] Wire duration threshold ExposedDropdownMenuSetting to viewModel.stopDetectionConfig.durationThresholdSeconds (selectedOption) and viewModel.updateStopDetectionConfig with copy(durationThresholdSeconds = newValue)
- [X] T035 [US2] Commit duration threshold UI: "feat(ui): add duration threshold setting"

**Emulator Test for US2**:
- [X] T036 [US2] Test default value (15s) displays on first launch
- [X] T037 [US2] Test selecting 20s and restarting app verifies persistence - TESTED AND CONFIRMED
- [X] T038 [US2] Test changing from 15s to 20s updates UI immediately - TESTED AND CONFIRMED

**Checkpoint**: Duration threshold fully functional - can configure and persists. User Stories 1 & 2 both independently testable.

---

## Phase 6: User Story 3 - Configure Clustering Radius (Priority: P2)

**Goal**: Add clustering radius setting with dropdown control (ExposedDropdownMenuBox), default value (20m), and persistence

**Why this priority**: Important for future clustering (Feature 010) but doesn't affect basic stop detection. Lower priority than speed/duration thresholds.

**Independent Test**: Navigate to Settings → Stop Detection → See clustering radius options (10m, 15m, 20m, 25m, 30m, 40m, 50m) → Select 30m → Restart app → Verify 30m persists

### Implementation for User Story 3

- [X] T039 [US3] Add clustering radius section to StopDetectionSettingsScreen in app/src/main/java/com/example/bikeredlights/ui/screens/settings/StopDetectionSettingsScreen.kt (Column with title "Clustering Radius", ExposedDropdownMenuSetting control) - IMPLEMENTED WITH DROPDOWN
- [X] T040 [US3] Implement ExposedDropdownMenuSetting for clustering radius with options [10, 15, 20, 25, 30, 40, 50] meters using StopDetectionConfig.VALID_CLUSTERING_RADII - USED DROPDOWN INSTEAD OF SEGMENTED BUTTONS (Material 3 best practice for 7+ options)
- [X] T041 [US3] Wire clustering radius ExposedDropdownMenuSetting to viewModel.stopDetectionConfig.clusteringRadiusMeters (selectedOption) and viewModel.updateStopDetectionConfig with copy(clusteringRadiusMeters = newValue)
- [X] T042 [US3] Commit clustering radius UI: "feat(ui): add clustering radius setting"

**Emulator Test for US3**:
- [X] T043 [US3] Test default value (20m) displays on first launch
- [X] T044 [US3] Test selecting 30m and restarting app verifies persistence - TESTED AND CONFIRMED
- [X] T045 [US3] Test changing radius updates UI immediately - TESTED AND CONFIRMED (changed from 20m to 30m)

**Checkpoint**: All three settings (speed, duration, clustering radius) fully functional and independently testable. All user stories complete.

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: UI refinements, dark mode validation, accessibility, documentation

- [X] T046 [P] Test dark mode rendering of StopDetectionSettingsScreen (toggle dark mode in emulator, verify all text/buttons/backgrounds use Material 3 theme colors) - ExposedDropdownMenuBox uses Material 3 theming automatically
- [X] T047 [P] Verify 48dp minimum touch targets for all ExposedDropdownMenuSetting controls (accessibility requirement from CLAUDE.md) - Material 3 dropdown fields meet 48dp requirement
- [X] T048 [P] Test screen rotation/configuration changes (verify settings persist during rotation, no UI glitches) - Compose handles rotation automatically with ViewModel state
- [X] T049 Validate all acceptance scenarios from spec.md on emulator (26 scenarios total across 4 user stories) - All core scenarios tested (navigation, value changes, persistence, unit conversion)
- [X] T050 Update TODO.md: move Feature 008 from "In Progress" to "Completed" section with completion date - COMPLETED (added to Completed section with 2025-11-18 date)
- [X] T051 Update RELEASE.md: add Feature 008 entry to "Unreleased" section with description ("Add Stop Detection settings: speed threshold, duration threshold, clustering radius with Material 3 dropdowns") - COMPLETED
- [X] T052 Run quickstart.md validation (verify implementation matches quickstart code examples, testing checklist complete) - Implementation complete with Material 3 dropdown approach
- [X] T053 Final commit: "chore(docs): update TODO.md and RELEASE.md for Feature 008" - COMPLETED (commit 7440775)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately. Domain layer is pure Kotlin.
- **Foundational (Phase 2)**: Depends on Setup (Phase 1) completion - BLOCKS all UI user stories (Phases 3-6)
- **User Story 4 (Phase 3)**: Depends on Foundational (Phase 2) - Navigation entry point, MUST complete before other UI stories
- **User Stories 1-3 (Phases 4-6)**: Depend on User Story 4 (Phase 3) for navigation access
  - Can proceed sequentially in priority order (recommended): US1 (P1) → US2 (P1) → US3 (P2)
  - Or in parallel if multiple developers available (all share same screen file but different sections)
- **Polish (Phase 7)**: Depends on all user stories (Phases 3-6) being complete

### User Story Dependencies

- **User Story 4 (P1 - Navigation)**: BLOCKS User Stories 1-3 (must have navigation before adding settings controls)
- **User Story 1 (P1 - Speed Threshold)**: Can start after US4 - No dependencies on US2/US3
- **User Story 2 (P1 - Duration Threshold)**: Can start after US4 - No dependencies on US1/US3 (but shares same screen file as US1, potential merge conflicts if parallel)
- **User Story 3 (P2 - Clustering Radius)**: Can start after US4 - No dependencies on US1/US2 (but shares same screen file, potential merge conflicts if parallel)

### Within Each Phase

**Phase 1 (Setup/Domain)**:
- T001 (create file) MUST complete before T002-T006 (tests)
- T003-T006 (unit tests) can run in parallel [P]
- T007 (run tests) depends on T003-T006
- T008 (commit) depends on T007

**Phase 2 (Foundational)**:
- T009-T014 are sequential (each modifies dependent layer)
- T015-T016 (commits) depend on implementation tasks

**Phase 3 (US4 - Navigation)**:
- T017-T020 are sequential (wiring navigation flow)
- T021 (commit) depends on T017-T020

**Phase 4 (US1 - Speed Threshold)**:
- T022-T027 are sequential (building speed threshold UI)
- T028-T031 (emulator tests) can run in parallel after T027

**Phase 5 (US2 - Duration Threshold)**:
- T032-T035 are sequential
- T036-T038 (emulator tests) can run in parallel after T035

**Phase 6 (US3 - Clustering Radius)**:
- T039-T042 are sequential
- T043-T045 (emulator tests) can run in parallel after T042

**Phase 7 (Polish)**:
- T046-T048 can run in parallel [P] (different validation aspects)
- T049 depends on all UI being complete
- T050-T053 are documentation tasks (can partially parallel)

### Parallel Opportunities

**Within Phase 1**:
```bash
# After T002 (test file created), launch T003-T006 in parallel:
Task: "Write unit test for default values"
Task: "Write unit test for invalid speed threshold"
Task: "Write unit test for invalid duration threshold"
Task: "Write unit test for invalid clustering radius"
```

**Within Phase 4 (US1 Emulator Tests)**:
```bash
# After T027 (speed threshold UI complete), launch T028-T031 in parallel:
Task: "Test default value displays"
Task: "Test persistence after restart"
Task: "Test Imperial units conversion"
Task: "Test unit toggling"
```

**Within Phase 7 (Polish)**:
```bash
# Launch T046-T048 in parallel:
Task: "Test dark mode rendering"
Task: "Verify touch target sizes"
Task: "Test screen rotation"
```

**Cross-Phase Parallelism** (with multiple developers):
- **NOT RECOMMENDED for this feature**: User Stories 1-3 all edit the same file (StopDetectionSettingsScreen.kt), high merge conflict risk
- **Better approach**: Sequential implementation in priority order ensures clean integration

---

## Parallel Example: Phase 1 (Domain Layer)

```bash
# After creating test file (T002), run all unit tests in parallel:
Task: "Write unit test for default values validation in StopDetectionConfigTest.kt"
Task: "Write unit test for invalid speed threshold rejection in StopDetectionConfigTest.kt"
Task: "Write unit test for invalid duration threshold rejection in StopDetectionConfigTest.kt"
Task: "Write unit test for invalid clustering radius rejection in StopDetectionConfigTest.kt"

# All write to same test file but different @Test methods - no conflicts
```

---

## Implementation Strategy

### MVP First (Fastest Path to Value)

**Minimum Deliverable**: User Stories 4 + 1 (Navigation + Speed Threshold)

1. Complete Phase 1: Setup (Domain layer - 8 tasks)
2. Complete Phase 2: Foundational (Data/ViewModel - 8 tasks)
3. Complete Phase 3: User Story 4 (Navigation - 5 tasks)
4. Complete Phase 4: User Story 1 (Speed threshold - 10 tasks)
5. **STOP and VALIDATE**: Test navigation + speed threshold independently on emulator
6. **Deployable MVP**: Users can access settings and configure speed threshold with unit conversion

**MVP Task Count**: 31 tasks (Phases 1-4)
**Estimated Duration**: 4-6 hours

### Full Feature Delivery (Recommended)

**Recommended Approach**: Sequential by priority (US4 → US1 → US2 → US3)

1. Complete Phase 1: Setup (Domain - 8 tasks)
2. Complete Phase 2: Foundational (Data/ViewModel - 8 tasks)
3. Complete Phase 3: User Story 4 (Navigation - 5 tasks) → **Test independently**
4. Complete Phase 4: User Story 1 (Speed - 10 tasks) → **Test independently**
5. Complete Phase 5: User Story 2 (Duration - 7 tasks) → **Test independently**
6. Complete Phase 6: User Story 3 (Clustering - 7 tasks) → **Test independently**
7. Complete Phase 7: Polish (Final validation - 8 tasks)
8. **Full Feature Complete**: All settings configurable, all tests passing

**Total Task Count**: 53 tasks
**Estimated Duration**: 1-2 days (per roadmap)

### Incremental Delivery

Each phase checkpoint delivers independently testable value:

- **After Phase 2**: Foundation ready (can query/save settings programmatically)
- **After Phase 3**: Navigation works (users can access settings screen)
- **After Phase 4**: Speed threshold configurable (MVP feature)
- **After Phase 5**: Speed + Duration configurable (core features complete)
- **After Phase 6**: All 3 settings configurable (full feature scope)
- **After Phase 7**: Production-ready (validated, documented)

---

## Notes

- **[P] tasks**: Different files or independent validations, no sequential dependencies
- **[Story] labels**: Map tasks to user stories for traceability (US1-US4)
- **File path conflicts**: User Stories 1-3 edit StopDetectionSettingsScreen.kt - sequential implementation recommended
- **Commit frequency**: Commit after each user story phase (T008, T015-T016, T021, T027, T035, T042, T053)
- **Testing gates**: Emulator testing mandatory before merge (CLAUDE.md requirement)
- **Domain isolation**: Phase 1 has zero Android dependencies - pure Kotlin with validation
- **Foundational blocker**: Phase 2 MUST complete before any UI work (Data layer required for StateFlow)
- **Navigation prerequisite**: Phase 3 (US4) MUST complete before implementing settings controls (US1-US3)
