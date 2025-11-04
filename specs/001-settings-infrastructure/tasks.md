# Tasks: Basic Settings Infrastructure

**Feature**: 001-settings-infrastructure
**Input**: Design documents from `/specs/001-settings-infrastructure/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/datastore-schema.md, quickstart.md

**Tests**: Tests are included per CLAUDE.md requirements (unit tests for repository/utilities, UI tests for navigation/persistence)

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies on incomplete tasks)
- **[Story]**: Which user story this task belongs to (US1, US2, US3)
- Exact file paths included in all descriptions

## Path Conventions

BikeRedlights uses Android Clean Architecture:
- **Source**: `app/src/main/java/com/example/bikeredlights/`
- **Unit Tests**: `app/src/test/java/com/example/bikeredlights/`
- **UI/Instrumented Tests**: `app/src/androidTest/java/com/example/bikeredlights/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Create foundational infrastructure shared across all settings

- [X] T001 [P] Create PreferencesKeys object with 4 DataStore key definitions in `app/src/main/java/com/example/bikeredlights/data/preferences/PreferencesKeys.kt`
- [X] T002 [P] Create UnitsSystem enum in `app/src/main/java/com/example/bikeredlights/domain/model/settings/UnitsSystem.kt`
- [X] T003 [P] Create GpsAccuracy enum in `app/src/main/java/com/example/bikeredlights/domain/model/settings/GpsAccuracy.kt`
- [X] T004 [P] Create AutoPauseConfig data class with validation in `app/src/main/java/com/example/bikeredlights/domain/model/settings/AutoPauseConfig.kt`
- [X] T005 [P] Create UnitConversions utility object with km/h↔mph and km↔miles functions in `app/src/main/java/com/example/bikeredlights/domain/util/UnitConversions.kt`
- [X] T006 Create SettingsRepository interface (4 Flow reads, 3 suspend writes) in `app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepository.kt`
- [X] T007 Implement SettingsRepositoryImpl with DataStore and error handling in `app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepositoryImpl.kt`

**Tests for Setup Phase**:
- [X] T008 [P] Unit tests for AutoPauseConfig validation (valid/invalid thresholds) in `app/src/test/java/com/example/bikeredlights/domain/model/settings/AutoPauseConfigTest.kt`
- [X] T009 [P] Unit tests for UnitConversions (kmh→mph precision to 2 decimals, km→miles accuracy, edge cases: 0.0, 0.1, 100.0, 999.9 values, verify conversion factor 0.621371) in `app/src/test/java/com/example/bikeredlights/domain/util/UnitConversionsTest.kt`
- [X] T010 [P] Unit tests for SettingsRepository (read defaults, write/read persistence, error handling) in `app/src/test/java/com/example/bikeredlights/data/repository/SettingsRepositoryTest.kt`

**Checkpoint**: Domain models, repository, and utilities are fully implemented and tested

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Navigation infrastructure that ALL user stories depend on

**⚠️ CRITICAL**: No user story UI work can begin until bottom navigation is functional

- [ ] T011 Create BottomNavDestination enum (LIVE, RIDES, SETTINGS) in `app/src/main/java/com/example/bikeredlights/ui/navigation/BottomNavDestination.kt`
- [ ] T012 Create SettingsViewModel with SettingsUiState and StateFlow in `app/src/main/java/com/example/bikeredlights/ui/screens/settings/SettingsViewModel.kt`
- [ ] T013 Modify MainActivity to add Scaffold with NavigationBar (bottom nav) in `app/src/main/java/com/example/bikeredlights/ui/MainActivity.kt`
- [ ] T014 Create AppNavigation with NavHost and settings route in `app/src/main/java/com/example/bikeredlights/ui/navigation/AppNavigation.kt`

**Tests for Foundational Phase**:
- [ ] T015 [P] Unit tests for SettingsViewModel (StateFlow emissions, settings changes) in `app/src/test/java/com/example/bikeredlights/ui/screens/settings/SettingsViewModelTest.kt`
- [ ] T016 UI test for bottom navigation (tap Settings tab, verify navigation) in `app/src/androidTest/java/com/example/bikeredlights/ui/screens/settings/SettingsNavigationTest.kt`

**Checkpoint**: Foundation ready - bottom navigation works, Settings tab accessible, ViewModel reactive

---

## Phase 3: User Story 1 - Configure Units Preference (Priority: P1) 🎯 MVP

**Goal**: User can change between Metric and Imperial units, setting persists, and speed display updates immediately

**Independent Test**: Navigate to Settings → Ride & Tracking → Change to Imperial → Return to Live tab → Speed shows mph → Restart app → Imperial persists

### Reusable Components for US1

- [ ] T017 [P] [US1] Create SettingCard composable (Material 3 Card with icon, title, subtitle, click) in `app/src/main/java/com/example/bikeredlights/ui/components/settings/SettingCard.kt`
- [ ] T018 [P] [US1] Create SegmentedButtonSetting composable (label, 2 options, selected state, onSelect) in `app/src/main/java/com/example/bikeredlights/ui/components/settings/SegmentedButtonSetting.kt`

### Screens for US1

- [ ] T019 [US1] Create SettingsHomeScreen composable (displays "Ride & Tracking" SettingCard) in `app/src/main/java/com/example/bikeredlights/ui/screens/settings/SettingsHomeScreen.kt`
- [ ] T020 [US1] Create RideTrackingSettingsScreen composable with Units SegmentedButtonSetting (Metric/Imperial) in `app/src/main/java/com/example/bikeredlights/ui/screens/settings/RideTrackingSettingsScreen.kt`
- [ ] T021 [US1] Wire SettingsViewModel to SettingsHomeScreen and RideTrackingSettingsScreen (collect StateFlow, update on changes)

### Integration & Testing for US1

- [ ] T022 [US1] Update SpeedTrackingViewModel (UI layer, from v0.1.0) to inject SettingsRepository, collect unitsSystem Flow, and apply UnitConversions.toMph() when Imperial selected (affects live speed display only)
- [ ] T023 [US1] UI test: Change units to Imperial → Verify SegmentedButton selection changes in `app/src/androidTest/java/com/example/bikeredlights/ui/screens/settings/SettingsNavigationTest.kt`
- [ ] T024 [US1] Instrumented test: Change units to Imperial → Restart app → Verify Imperial persists in `app/src/androidTest/java/com/example/bikeredlights/ui/screens/settings/SettingsPersistenceTest.kt`
- [ ] T024b [US1] Integration test: Change units to Imperial during simulated location updates → Verify speed display updates within 1 second with correct mph value in `app/src/androidTest/java/com/example/bikeredlights/ui/screens/settings/SettingsPersistenceTest.kt`

**Checkpoint**: User Story 1 COMPLETE - Units setting fully functional, persists, and affects speed display

---

## Phase 4: User Story 2 - Adjust GPS Accuracy for Battery Life (Priority: P2)

**Goal**: User can toggle between High Accuracy (1s updates) and Battery Saver (3-5s updates) to optimize battery life

**Independent Test**: Settings → Ride & Tracking → Change to Battery Saver → Restart app → Battery Saver persists

### Implementation for US2

- [ ] T025 [US2] Add GPS Accuracy SegmentedButtonSetting to RideTrackingSettingsScreen (High Accuracy/Battery Saver) in `app/src/main/java/com/example/bikeredlights/ui/screens/settings/RideTrackingSettingsScreen.kt`
- [ ] T026 [US2] Wire GPS Accuracy setting to SettingsViewModel (add setGpsAccuracy method)
- [ ] T027 [US2] Update LocationRepository (data layer, from v0.1.0) to inject SettingsRepository, collect gpsAccuracy Flow, and configure FusedLocationProviderClient interval: 1000ms (HIGH_ACCURACY) or 4000ms (BATTERY_SAVER)

### Testing for US2

- [ ] T028 [US2] UI test: Change GPS Accuracy to Battery Saver → Verify selection changes in `app/src/androidTest/java/com/example/bikeredlights/ui/screens/settings/SettingsNavigationTest.kt`
- [ ] T029 [US2] Instrumented test: Change GPS Accuracy to Battery Saver → Restart app → Verify Battery Saver persists in `app/src/androidTest/java/com/example/bikeredlights/ui/screens/settings/SettingsPersistenceTest.kt`

**Checkpoint**: User Story 2 COMPLETE - GPS Accuracy setting functional, persists, affects location update interval

---

## Phase 5: User Story 3 - Enable Auto-Pause for Commutes (Priority: P3)

**Goal**: User can enable auto-pause with configurable threshold (1-15 minutes), ride pauses when stationary for threshold duration

**Independent Test**: Settings → Ride & Tracking → Enable Auto-Pause with 5 min → Restart app → Auto-Pause enabled with 5 min persists

### Reusable Components for US3

- [ ] T030 [P] [US3] Create ToggleWithPickerSetting composable (toggle switch, number picker, options list) in `app/src/main/java/com/example/bikeredlights/ui/components/settings/ToggleWithPickerSetting.kt`

### Implementation for US3

- [ ] T031 [US3] Add Auto-Pause ToggleWithPickerSetting to RideTrackingSettingsScreen (toggle, picker with 1,2,3,5,10,15 options) in `app/src/main/java/com/example/bikeredlights/ui/screens/settings/RideTrackingSettingsScreen.kt`
- [ ] T032 [US3] Wire Auto-Pause setting to SettingsViewModel (add setAutoPauseEnabled and setAutoPauseThreshold methods for persistence only; actual ride pause/resume logic deferred to Feature 1A TrackLocationUseCase)

### Testing for US3

- [ ] T033 [US3] UI test: Enable Auto-Pause → Verify toggle ON and picker visible in `app/src/androidTest/java/com/example/bikeredlights/ui/screens/settings/SettingsNavigationTest.kt`
- [ ] T034 [US3] UI test: Change threshold to 3 minutes → Verify picker shows 3 minutes in `app/src/androidTest/java/com/example/bikeredlights/ui/screens/settings/SettingsNavigationTest.kt`
- [ ] T035 [US3] Instrumented test: Enable Auto-Pause with 10 min → Restart app → Verify enabled with 10 min persists in `app/src/androidTest/java/com/example/bikeredlights/ui/screens/settings/SettingsPersistenceTest.kt`

**Checkpoint**: User Story 3 COMPLETE - Auto-Pause setting functional, persists (toggle + threshold)

**Note**: Auto-pause logic implementation (pause ride when stationary < 1 km/h for threshold) deferred to Feature 1A (Core Ride Recording)

---

## Phase 6: Polish & Cross-Cutting Concerns

**Purpose**: Final improvements, documentation, and emulator validation

- [ ] T036 [P] Add semantic labels for accessibility (TalkBack descriptions) to all settings components
- [ ] T037 [P] Verify 48dp minimum touch targets for all interactive elements (SegmentedButton, toggle, picker)
- [ ] T038 Test dark mode support in emulator (Settings → Display → Dark theme → Verify settings screens)
- [ ] T039 Test 200% font scaling (Settings → Display → Font size → Largest → Verify no text truncation)
- [ ] T040 Update TODO.md: Move "Feature 2A: Basic Settings Infrastructure" to "Completed" section with completion date
- [ ] T041 Update RELEASE.md: Add Feature 2A entries to "Unreleased" section (units, GPS accuracy, auto-pause settings)
- [ ] T042 Run quickstart.md emulator validation (all 6 test scenarios: navigation, units, GPS accuracy, auto-pause, dark mode, font scaling)

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
  - Tasks T001-T010 create domain models, repository, utilities, and their tests
  - All T001-T007 can run in parallel (different files)
  - Tests T008-T010 can run in parallel after T001-T007 complete

- **Foundational (Phase 2)**: Depends on Phase 1 completion - BLOCKS all user story UI work
  - Tasks T011-T016 create navigation infrastructure and ViewModel
  - CRITICAL: Bottom navigation MUST work before any settings screens can be accessed
  - T011-T014 run sequentially (navigation dependencies)
  - Tests T015-T016 run in parallel after T011-T014 complete

- **User Stories (Phase 3-5)**: All depend on Foundational (Phase 2) completion
  - User Story 1 (P1): Can start after Phase 2 - MVP functionality
  - User Story 2 (P2): Can start after Phase 2 - Reuses SegmentedButtonSetting from US1
  - User Story 3 (P3): Can start after Phase 2 - Adds new ToggleWithPickerSetting component
  - Stories can proceed sequentially (P1 → P2 → P3) or in parallel if multiple developers

- **Polish (Phase 6)**: Depends on all user stories being complete
  - Tasks T036-T042 finalize accessibility, documentation, and validation

### User Story Dependencies

**User Story 1 (P1) - Configure Units Preference**:
- Depends on: Phase 1 (domain models, repository) + Phase 2 (bottom nav, ViewModel)
- No dependencies on other user stories
- Creates: SettingCard, SegmentedButtonSetting (reused by US2)

**User Story 2 (P2) - Adjust GPS Accuracy**:
- Depends on: Phase 1 + Phase 2 + US1 (SegmentedButtonSetting component)
- Reuses: SegmentedButtonSetting from US1
- Can run in parallel with US3 if multiple developers

**User Story 3 (P3) - Enable Auto-Pause**:
- Depends on: Phase 1 + Phase 2
- No dependencies on US1 or US2 (creates new ToggleWithPickerSetting)
- Can run in parallel with US1/US2 if multiple developers

### Within Each User Story

**User Story 1**:
1. T017-T018 (components) can run in parallel
2. T019 (Settings home) depends on T017
3. T020 (Ride & Tracking screen) depends on T018
4. T021 (ViewModel wiring) depends on T019-T020
5. T022 (SpeedTrackingViewModel integration) depends on T021
6. T023-T024 (tests) run after implementation complete

**User Story 2**:
1. T025-T026 (add setting, wire ViewModel) run sequentially
2. T027 (LocationRepository integration) runs in parallel with T025-T026
3. T028-T029 (tests) run after implementation complete

**User Story 3**:
1. T030 (ToggleWithPickerSetting component) runs first
2. T031-T032 (add setting, wire ViewModel) depend on T030
3. T033-T035 (tests) run after implementation complete

### Parallel Opportunities

**Phase 1 (Setup)**:
- T001-T005 (all domain models and utils) in parallel
- T006-T007 (repository) sequential (interface then implementation)
- T008-T010 (all tests) in parallel after T001-T007 complete

**Phase 2 (Foundational)**:
- T011-T014 sequential (navigation dependencies)
- T015-T016 (tests) in parallel after T011-T014

**User Stories**:
- If multiple developers: US1, US2, US3 can all run in parallel after Phase 2
- Within each story: Components marked [P] can run in parallel
- Tests for each story can run in parallel after story implementation

**Phase 6 (Polish)**:
- T036-T037 (accessibility) in parallel
- T038-T039 (emulator tests) in parallel
- T040-T042 (documentation) in parallel

---

## Parallel Example: Phase 1 Setup

**Launch all domain models and utilities together**:
```bash
# Terminal 1
Task T001: "Create PreferencesKeys object..."

# Terminal 2
Task T002: "Create UnitsSystem enum..."

# Terminal 3
Task T003: "Create GpsAccuracy enum..."

# Terminal 4
Task T004: "Create AutoPauseConfig data class..."

# Terminal 5
Task T005: "Create UnitConversions utility object..."
```

**Then launch repository sequentially**:
```bash
Task T006: "Create SettingsRepository interface..."
Task T007: "Implement SettingsRepositoryImpl..."
```

**Finally launch all tests in parallel**:
```bash
# Terminal 1
Task T008: "Unit tests for AutoPauseConfig..."

# Terminal 2
Task T009: "Unit tests for UnitConversions..."

# Terminal 3
Task T010: "Unit tests for SettingsRepository..."
```

---

## Parallel Example: User Story 1

**Launch components in parallel**:
```bash
# Terminal 1
Task T017 [US1]: "Create SettingCard composable..."

# Terminal 2
Task T018 [US1]: "Create SegmentedButtonSetting composable..."
```

**Then screens sequentially** (depend on components):
```bash
Task T019 [US1]: "Create SettingsHomeScreen composable..."
Task T020 [US1]: "Create RideTrackingSettingsScreen composable..."
```

**Launch tests in parallel** (after implementation):
```bash
# Terminal 1
Task T023 [US1]: "UI test: Change units to Imperial..."

# Terminal 2
Task T024 [US1]: "Instrumented test: Change units to Imperial → Restart app..."
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

**Recommended path for initial release**:

1. Complete Phase 1: Setup (T001-T010)
   - Domain models, repository, utilities with tests
   - ~3-4 hours

2. Complete Phase 2: Foundational (T011-T016)
   - Bottom navigation, ViewModel, navigation tests
   - ~2-3 hours

3. Complete Phase 3: User Story 1 (T017-T024)
   - Units setting (Metric/Imperial) with persistence
   - ~3-4 hours

4. **STOP and VALIDATE**: Test User Story 1 independently
   - Run emulator tests from quickstart.md
   - Verify units change and persist
   - Verify speed display updates

5. Tag as v0.2.0-alpha or demo to stakeholders

**Total MVP Time**: 8-11 hours for fully functional Units setting

---

### Incremental Delivery (All User Stories)

**Full feature implementation**:

1. **Day 1**: Complete Setup + Foundational (T001-T016)
   - Foundation ready, bottom nav works
   - ~5-7 hours

2. **Day 2**: Complete User Story 1 (T017-T024)
   - Units setting fully functional
   - Test independently, tag v0.2.0-alpha
   - ~3-4 hours

3. **Day 2-3**: Complete User Story 2 (T025-T029)
   - GPS Accuracy setting fully functional
   - Test independently
   - ~2-3 hours

4. **Day 3**: Complete User Story 3 (T030-T035)
   - Auto-Pause setting fully functional
   - Test independently
   - ~3-4 hours

5. **Day 3**: Complete Polish (T036-T042)
   - Accessibility, documentation, validation
   - ~2-3 hours

6. Tag as v0.2.0, create GitHub release with APK

**Total Time**: 2-3 days (15-21 hours) for complete Feature 2A

---

### Parallel Team Strategy

**With 3 developers after Foundational phase complete**:

**Developer A (Senior)**: User Story 1 (P1 - MVP)
- T017-T024 (SettingCard, SegmentedButtonSetting, Units setting)
- Most critical path, requires integration with v0.1.0 code
- ~3-4 hours

**Developer B (Mid)**: User Story 2 (P2)
- T025-T029 (GPS Accuracy setting, LocationRepository integration)
- Reuses components from US1
- ~2-3 hours

**Developer C (Mid)**: User Story 3 (P3)
- T030-T035 (ToggleWithPickerSetting, Auto-Pause setting)
- Independent component, no integration yet
- ~3-4 hours

**Result**: All 3 user stories complete in ~4-5 hours (parallel work)

Then team converges for Polish phase (T036-T042) together (~2-3 hours).

**Total Parallel Time**: ~6-8 hours with 3 developers

---

## Notes

### Task Format Compliance
- ✅ Every task has checkbox: `- [ ]`
- ✅ Every task has Task ID: T001, T002, etc.
- ✅ Parallelizable tasks marked: [P]
- ✅ User story tasks labeled: [US1], [US2], [US3]
- ✅ Exact file paths included in all descriptions

### Independent Testing
- Each user story has clear "Independent Test" criteria
- User Story 1: Units change + persist + speed display updates
- User Story 2: GPS Accuracy change + persist
- User Story 3: Auto-Pause enable + threshold change + persist

### Test Coverage
- Unit tests for domain models (AutoPauseConfig validation)
- Unit tests for utilities (UnitConversions accuracy)
- Unit tests for repository (persistence, error handling)
- Unit tests for ViewModel (StateFlow emissions)
- UI tests for navigation (bottom nav, card tap, settings screens)
- Instrumented tests for persistence (app restart cycles)

### Commit Strategy
- Commit after each task or small group (2-3 tasks)
- Maximum ~200 lines per commit per Constitution
- Example commits:
  - "feat(domain): add settings domain models (UnitsSystem, GpsAccuracy, AutoPauseConfig)"
  - "feat(data): implement SettingsRepository with DataStore"
  - "feat(ui): add bottom navigation with Settings tab"
  - "feat(ui): implement Units setting (US1)"
  - "test(ui): add settings navigation and persistence tests"

### Emulator Testing Requirements
- Per CLAUDE.md Constitution, feature MUST be tested on emulator
- Quickstart.md has 6 test scenarios (navigation, units, GPS, auto-pause, dark mode, font scaling)
- Task T042 runs full emulator validation before marking feature complete

### Accessibility Requirements
- 48dp minimum touch targets (verified in T037)
- Semantic labels for TalkBack (T036)
- WCAG AA contrast ratios (Material 3 theme handles this)
- Font scaling support (tested in T039)

### Version Release
- Feature 2A → v0.2.0 (MINOR version bump per Constitution)
- Version code: 0 * 10000 + 2 * 100 + 0 = 200
- Update app/build.gradle.kts with new versionCode and versionName
- Pull request → merge to main → tag v0.2.0 → build APK → GitHub release

---

**Task Count Summary**:
- Phase 1 (Setup): 10 tasks (7 implementation + 3 tests)
- Phase 2 (Foundational): 6 tasks (4 implementation + 2 tests)
- Phase 3 (User Story 1): 8 tasks (5 implementation + 3 tests)
- Phase 4 (User Story 2): 5 tasks (3 implementation + 2 tests)
- Phase 5 (User Story 3): 6 tasks (3 implementation + 3 tests)
- Phase 6 (Polish): 7 tasks (documentation + validation)

**Total**: 42 tasks (29 implementation + 13 tests)

**Parallel Opportunities**: 15 tasks marked [P] can run in parallel within their phases

**Independent Test Criteria**: All 3 user stories have clear, measurable test criteria for standalone validation
