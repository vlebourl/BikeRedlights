# Implementation Tasks: Stop Cluster Visualization

**Feature**: 011-cluster-visualization
**Branch**: `011-cluster-visualization`
**Created**: 2025-12-29
**Target Release**: v0.11.0

This document provides a dependency-ordered task breakdown for implementing Feature 011: Stop Cluster Visualization.

---

## Task Summary

| Phase | User Story | Task Count | Parallel Opportunities |
|-------|-----------|------------|----------------------|
| Phase 1 | Setup | 2 | 0 |
| Phase 2 | Foundational | 9 | 7 |
| Phase 3 | US1 - View Clusters on Map (P1) | 7 | 3 |
| Phase 4 | US2 - View Cluster Details (P1) | 5 | 2 |
| Phase 5 | US3 - Filter Clusters (P2) | 4 | 2 |
| Phase 6 | US4 - Stops Tab Navigation (P2) | 5 | 3 |
| Phase 7 | Polish & Integration | 4 | 2 |
| **Total** | **4 user stories** | **36 tasks** | **19 parallel** |

**Estimated Implementation Time**: 6-8 hours

---

## Implementation Strategy

### MVP Scope (Minimum Viable Product)

**MVP = User Story 1 + User Story 2** (both P1)

This delivers the core value proposition:
- ✅ Users can view stop clusters on an interactive map
- ✅ Users can tap clusters to see detailed information
- ❌ Filters (US3) deferred to post-MVP
- ❌ Dedicated tab (US4) deferred to post-MVP (can access via settings or rides screen)

**Why**: US1+US2 answer the key user questions ("Where do I stop most often?" and "When/how long do I stop here?") and validate the clustering feature's value before investing in advanced filtering and navigation.

### Incremental Delivery

**Milestone 1**: Complete Phase 1-2 (Setup + Foundation)
→ All domain logic and data layer ready, independently testable

**Milestone 2**: Complete Phase 3-4 (US1 + US2)
→ **MVP COMPLETE** - functional cluster map with details, ready for user testing

**Milestone 3**: Complete Phase 5 (US3)
→ Advanced filtering capability

**Milestone 4**: Complete Phase 6-7 (US4 + Polish)
→ **FULL FEATURE COMPLETE** - dedicated navigation + production ready

---

## Dependency Graph

### User Story Completion Order

```
Setup (Phase 1)
    ↓
Foundational (Phase 2) ← BLOCKING for all user stories
    ↓
    ├─→ US1: View Clusters (P1) ← MVP Core
    │       ↓
    │   US2: Cluster Details (P1) ← MVP Complete (depends on US1 for map screen)
    │       ↓
    ├─→ US3: Filter Clusters (P2) ← Independent of US2, extends US1
    │       ↓
    └─→ US4: Navigation Tab (P2) ← Independent, can run parallel with US3
            ↓
        Polish & Integration (Phase 7)
```

**Critical Path**: Setup → Foundation → US1 → US2 → US3 → US4 → Polish (sequential MVP delivery)

**Parallel Opportunities**:
- US3 and US4 can be developed in parallel (both independent)
- Within each phase, tasks marked `[P]` can run in parallel

---

## Phase 1: Setup

**Goal**: Initialize feature branch and validate prerequisites

### Tasks

- [ ] T001 Verify feature branch `011-cluster-visualization` is checked out and up to date with main
- [ ] T002 Verify Feature 010 (Stop Clustering) is complete and cluster_id column exists in stops table (run test query: `SELECT cluster_id FROM stops WHERE cluster_id IS NOT NULL LIMIT 1`)

**Completion Criteria**: Branch ready, database schema validated with existing cluster data

---

## Phase 2: Foundational (Blocking for All User Stories)

**Goal**: Build reusable domain layer, data layer, and UI state infrastructure that all user stories depend on

**Independent Test**: Domain use cases can be unit tested without UI, repository methods return expected data from database

### Tasks

#### Domain Layer - Models (Parallelizable)

- [ ] T003 [P] Create ClusterSummary domain model in `app/src/main/java/com/example/bikeredlights/domain/model/ClusterSummary.kt` with properties: clusterId, centerPosition, stopCount, averageDuration, frequencyText, stops list
- [ ] T004 [P] Create ClusterMarkerData domain model with MarkerColor enum in `app/src/main/java/com/example/bikeredlights/domain/model/ClusterMarkerData.kt` with color logic: GREEN (2-5), YELLOW (6-10), RED (11+)
- [ ] T005 [P] Create StopClusterFilter domain model in `app/src/main/java/com/example/bikeredlights/domain/model/StopClusterFilter.kt` with DateRange data class and DateRangePresets/ClusterSizePresets objects

#### Domain Layer - Use Cases (Sequential within group, parallelizable between groups)

- [ ] T006 [P] Create CalculateClusterCenterUseCase in `app/src/main/java/com/example/bikeredlights/domain/usecase/CalculateClusterCenterUseCase.kt` with arithmetic mean logic for GPS coordinates (no dependencies)
- [ ] T007 [P] Create FormatClusterAnalyticsUseCase in `app/src/main/java/com/example/bikeredlights/domain/usecase/FormatClusterAnalyticsUseCase.kt` with frequency text generation ("X times this week/month") (no dependencies)
- [ ] T008 Create CalculateClusterStatsUseCase in `app/src/main/java/com/example/bikeredlights/domain/usecase/CalculateClusterStatsUseCase.kt` injecting CalculateClusterCenterUseCase and FormatClusterAnalyticsUseCase (depends on T006, T007)
- [ ] T009 Create GetClusteredStopsUseCase in `app/src/main/java/com/example/bikeredlights/domain/usecase/GetClusteredStopsUseCase.kt` with StopRepository dependency and filter logic (depends on T010)

#### Data Layer (Parallelizable after use cases)

- [ ] T010 [P] Extend StopDao in `app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt` with 4 new query methods: getClusteredStops(), getClusteredStopsByDateRange(), getClusterIdsWithMinSize(), getStopsByClusterIds()
- [ ] T011 [P] Extend StopRepositoryImpl in `app/src/main/java/com/example/bikeredlights/data/repository/StopRepositoryImpl.kt` implementing 3 new interface methods: getClusteredStops(), getClusteredStopsByDateRange(), getStopsGroupedByCluster()

**Completion Criteria**:
- All domain models compile with @Immutable annotations
- All use cases have operator fun invoke() methods
- Repository methods return Flow<List<Stop>> or Flow<Map<Long, List<Stop>>>
- Unit tests pass for CalculateClusterCenterUseCase, FormatClusterAnalyticsUseCase, CalculateClusterStatsUseCase

**Validation Commands**:
```bash
# Compile domain layer
./gradlew :app:compileDebugKotlin

# Run use case unit tests
./gradlew :app:testDebugUnitTest --tests="*UseCase*"
```

---

## Phase 3: User Story 1 - View Stop Clusters on Interactive Map (P1)

**User Story**: Users can view all their stop clusters on an interactive Google Maps interface with color-coded markers indicating cluster density or frequency.

**Goal**: Render clustered stops as color-coded markers on Google Maps

**Independent Test**: Navigate to Stops screen (temporary route), verify map displays with color-coded cluster markers. Can tap markers (though detail popup not yet implemented).

**Acceptance Criteria** (from spec.md AS1-AS5):
- AS1: Map displays with color-coded markers at cluster locations
- AS2: Cluster markers remain correctly positioned during pan/zoom
- AS3: Empty state message shown if no clustered stops exist
- AS4: Marker color indicates cluster size (green/yellow/red per FR-003)
- AS5: Nearby clusters remain distinguishable without overlapping

### Tasks

#### UI State & ViewModel

- [ ] T012 [US1] Create ClusterMapUiState data class in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/ClusterMapUiState.kt` with properties: clusters, activeFilter, isLoading, errorMessage, selectedCluster
- [ ] T013 [US1] Create ClusterMapViewModel in `app/src/main/java/com/example/bikeredlights/ui/viewmodel/ClusterMapViewModel.kt` with @HiltViewModel annotation, inject GetClusteredStopsUseCase and CalculateClusterStatsUseCase, emit StateFlow<ClusterMapUiState>, implement loadClusters() method

#### UI Components

- [ ] T014 [P] [US1] Create ClusterMarker composable in `app/src/main/java/com/example/bikeredlights/ui/components/clusters/ClusterMarker.kt` rendering Google Maps Marker with color from MarkerColor enum and onClick handler
- [ ] T015 [US1] Create StopsMapScreen composable in `app/src/main/java/com/example/bikeredlights/ui/screens/clusters/StopsMapScreen.kt` with BikeMap integration, ClusterMarker rendering for each cluster, loading state, empty state, and error handling

#### Integration & DI

- [ ] T016 [P] [US1] Create ClusterModule Hilt module in `app/src/main/java/com/example/bikeredlights/di/ClusterModule.kt` providing all 4 use cases with @InstallIn(ViewModelComponent::class)
- [ ] T017 [US1] Add temporary navigation route "stops_temp" in AppNavigation.kt for testing (will be replaced in US4 with bottom nav integration)
- [ ] T018 [US1] Test US1 on emulator: Navigate to stops_temp route, verify map loads, verify color-coded markers render correctly, verify pan/zoom interactions, verify empty state if no clusters

**Completion Criteria**:
- Map displays with clustered stops as color-coded markers (green/yellow/red)
- Markers correctly positioned at cluster center coordinates
- Empty state shows "No clusters found" message if no data
- All acceptance scenarios AS1-AS5 validated on emulator

**Validation Commands**:
```bash
# Install debug build
./gradlew installDebug

# Emulator steps:
# 1. Navigate to temporary route (adb shell input tap coordinates for nav button)
# 2. Verify map displays
# 3. Verify cluster markers are color-coded correctly
# 4. Test pan/zoom interactions
```

---

## Phase 4: User Story 2 - View Detailed Cluster Information (P1)

**User Story**: Users can tap on any cluster marker to view detailed information including total stops, average duration, frequency analytics, and scrollable list of individual stops.

**Goal**: Implement Material 3 ModalBottomSheet popup with cluster details

**Dependencies**: Requires US1 complete (StopsMapScreen must exist to add bottom sheet)

**Independent Test**: Tap cluster marker, verify bottom sheet opens with correct data, scroll stop list, dismiss via swipe/scrim/back

**Acceptance Criteria** (from spec.md AS1-AS6):
- AS1: Tapping cluster marker opens detailed popup with summary stats
- AS2: Each stop in list shows date, time, duration formatted correctly
- AS3: Frequency analytics displays "X times this week/month"
- AS4: Popup dismisses on swipe/back/scrim tap
- AS5: Stop list is scrollable for 20+ items
- AS6: Durations formatted consistently (seconds, MM:SS, HH:MM)

### Tasks

#### UI Components

- [ ] T019 [P] [US2] Create StopListItem composable in `app/src/main/java/com/example/bikeredlights/ui/components/clusters/StopListItem.kt` displaying stop date (MMM DD, YYYY), time (HH:MM AM/PM), and formatted duration
- [ ] T020 [US2] Create ClusterDetailBottomSheet composable in `app/src/main/java/com/example/bikeredlights/ui/screens/clusters/ClusterDetailBottomSheet.kt` with summary cards (total stops, avg duration), frequency analytics text, LazyColumn with StopListItem, and close button

#### ViewModel Integration

- [ ] T021 [US2] Add selectCluster() and clearSelection() methods to ClusterMapViewModel updating selectedCluster in UI state

#### Screen Integration

- [ ] T022 [US2] Integrate ModalBottomSheet into StopsMapScreen with conditional composition (if showBottomSheet), sheetState management, onDismissRequest handler, and ClusterDetailBottomSheet content
- [ ] T023 [US2] Test US2 on emulator: Tap cluster marker, verify bottom sheet opens, verify summary stats are correct, scroll stop list, verify duration formatting, test dismiss via swipe/back/scrim tap, verify all AS1-AS6 acceptance scenarios

**Completion Criteria**:
- Tapping cluster marker opens bottom sheet with correct data
- Summary shows total stops, average duration, frequency text
- Stop list scrollable with formatted dates/times/durations
- Bottom sheet dismisses correctly via all methods
- All acceptance scenarios AS1-AS6 validated on emulator

**Validation Commands**:
```bash
# Install debug build with US2 changes
./gradlew installDebug

# Emulator steps:
# 1. Navigate to stops screen
# 2. Tap cluster marker (use adb shell uiautomator dump /dev/tty to get coordinates)
# 3. Verify bottom sheet opens with correct data
# 4. Scroll stop list (verify smooth scrolling)
# 5. Test dismiss gestures (swipe down, tap outside, back button)
```

**MVP Checkpoint**: After completing Phase 4, the MVP is COMPLETE (US1 + US2). Feature can be released with basic cluster visualization and details.

---

## Phase 5: User Story 3 - Filter Clusters by Criteria (P2)

**User Story**: Users can apply filters to narrow down which clusters are displayed on the map (date range, minimum cluster size).

**Goal**: Add filter controls UI and wire to ViewModel filter logic

**Dependencies**: Requires US1 complete (map screen must exist), independent of US2

**Independent Test**: Apply date range filter, verify only matching clusters display. Apply min size filter, verify only large clusters display. Combine filters, verify correct results.

**Acceptance Criteria** (from spec.md AS1-AS6):
- AS1: Date range filter updates map to show only matching clusters
- AS2: Min cluster size filter displays only clusters >= threshold
- AS3: Empty state displays "No clusters match filters" when no results
- AS4: Clear filters button resets to all clusters
- AS5: Custom date range selection works correctly (inclusive boundaries)
- AS6: Filter indicator shows active filters on screen

### Tasks

#### UI Components

- [ ] T024 [P] [US3] Create ClusterFilterControls composable in `app/src/main/java/com/example/bikeredlights/ui/components/clusters/ClusterFilterControls.kt` with date range dropdown (All Time, Last 7 Days, Last 30 Days, Custom Range), min cluster size dropdown (2+, 3+, 5+, 10+), filter indicator chip, and clear filters button

#### ViewModel Integration

- [ ] T025 [US3] Add applyFilter(StopClusterFilter) and clearFilters() methods to ClusterMapViewModel calling loadClusters() with filter parameter

#### Screen Integration

- [ ] T026 [P] [US3] Integrate ClusterFilterControls into StopsMapScreen above map with filter state binding and callback handlers
- [ ] T027 [US3] Test US3 on emulator: Apply date range filter (Last 7 Days), verify map updates, apply min size filter (5+ stops), verify correct clusters shown, combine filters, verify empty state, tap Clear Filters, verify all clusters return, test custom date range, verify filter indicator displays, verify all AS1-AS6 acceptance scenarios

**Completion Criteria**:
- Filter controls render correctly on map screen
- Date range filtering works (all presets + custom)
- Min cluster size filtering works (2+, 3+, 5+, 10+)
- Filter indicator displays active filters
- Clear filters resets to default state
- All acceptance scenarios AS1-AS6 validated on emulator

**Validation Commands**:
```bash
# Install debug build with US3 changes
./gradlew installDebug

# Emulator steps:
# 1. Navigate to stops screen
# 2. Tap filter dropdown (get coordinates with uiautomator dump)
# 3. Select "Last 7 Days"
# 4. Verify only recent clusters display
# 5. Change to "5+ stops" min size
# 6. Verify only large clusters display
# 7. Tap "Clear Filters"
# 8. Verify all clusters return
```

---

## Phase 6: User Story 4 - Access via Dedicated Stops Tab (P2)

**User Story**: Users can access the cluster map view through a new "Stops" tab in the bottom navigation bar.

**Goal**: Add Stops tab to bottom navigation with StopCircle icon

**Dependencies**: Requires US1 complete (StopsMapScreen must exist), independent of US2 and US3

**Independent Test**: Tap Stops tab, verify map screen opens. Switch to other tabs and return, verify map state persists (zoom/position).

**Acceptance Criteria** (from spec.md AS1-AS5):
- AS1: Tapping Stops tab opens cluster map view
- AS2: Map retains zoom/position when switching tabs
- AS3: 4 tabs labeled Live, Rides, Stops, Settings (left to right)
- AS4: Active tab highlighted correctly
- AS5: First-time onboarding tooltip explains feature

### Tasks

#### Navigation

- [ ] T028 [P] [US4] Add STOPS enum value to BottomNavDestination in `app/src/main/java/com/example/bikeredlights/ui/navigation/BottomNavDestination.kt` with route="stops", label="Stops", icon="stop_circle"
- [ ] T029 [P] [US4] Add Stops route to AppNavigation.kt composable block routing to StopsMapScreen (remove temporary "stops_temp" route from T017)
- [ ] T030 [P] [US4] Update MainActivity.kt: import Icons.Outlined.StopCircle, add STOPS case in icon when block, change alwaysShowLabel = false for 4-tab mode

#### Map State Persistence

- [ ] T031 [US4] Add rememberSaveable for CameraPositionState in StopsMapScreen to persist zoom/position across tab switches
- [ ] T032 [US4] Test US4 on emulator: Tap Stops tab from any screen, verify map opens, zoom/pan to custom position, switch to Rides tab, return to Stops tab, verify map position persisted, verify 4 tabs render correctly with icon-only mode for inactive tabs, verify active tab highlighted, verify all AS1-AS5 acceptance scenarios

**Completion Criteria**:
- Stops tab appears as 3rd tab in bottom navigation
- Tab icon is StopCircle (outlined)
- Tapping tab navigates to cluster map
- Map state persists across tab switches
- All 4 tabs render correctly with proper spacing
- All acceptance scenarios AS1-AS5 validated on emulator

**Validation Commands**:
```bash
# Install debug build with US4 changes
./gradlew installDebug

# Emulator steps:
# 1. Start on Live tab
# 2. Tap Stops tab (3rd position)
# 3. Verify map opens
# 4. Zoom/pan to custom position
# 5. Switch to Rides tab
# 6. Return to Stops tab
# 7. Verify map position unchanged
# 8. Verify tab highlighting works correctly
```

---

## Phase 7: Polish & Cross-Cutting Concerns

**Goal**: Final integration testing, dark mode validation, accessibility checks, performance optimization

### Tasks

#### Testing & Validation

- [ ] T033 [P] Test dark mode on emulator: Enable dark mode in system settings, navigate through all user stories (US1-US4), verify Material 3 theming applies correctly to map markers/bottom sheet/filters/navigation
- [ ] T034 [P] Test accessibility with TalkBack: Enable TalkBack, navigate to Stops tab via screen reader, verify content descriptions for all interactive elements (cluster markers, bottom sheet close button, filter controls, navigation tabs)

#### Performance & Edge Cases

- [ ] T035 Test with large dataset: Insert 100+ cluster markers in database via SQL, verify map renders within 2 seconds (SC-002), verify smooth 60fps pan/zoom (SC-006), verify filter application within 1 second (SC-004)
- [ ] T036 Test edge cases: Verify empty state message when no clustered stops exist, verify cluster markers near map boundaries don't clip, verify single-stop clusters (shouldn't exist, only 2+ per DBSCAN), verify custom date range picker works correctly

**Completion Criteria**:
- Dark mode renders correctly (all components themed)
- TalkBack navigation works for all interactive elements
- Performance targets met (SC-001 through SC-010 from spec.md)
- All edge cases handled gracefully
- Feature ready for code review and merge

**Validation Commands**:
```bash
# Run all tests
./gradlew test
./gradlew connectedAndroidTest

# Performance profiling (Android Studio)
# 1. Profile → CPU Profiler
# 2. Record while loading map with 100 clusters
# 3. Verify <2s load time
# 4. Profile → Memory Profiler
# 5. Verify no memory leaks during tab switching

# Final emulator validation
./gradlew installDebug
# Run through all user stories (US1-US4) end-to-end
# Verify all acceptance criteria pass
```

---

## Parallel Execution Opportunities

### Within Foundation (Phase 2)

**Parallel Group A** (Domain Models - 3 tasks):
```bash
# Can be implemented simultaneously (different files)
T003 - ClusterSummary.kt
T004 - ClusterMarkerData.kt
T005 - StopClusterFilter.kt
```

**Parallel Group B** (Use Cases - 2 tasks):
```bash
# No dependencies on each other
T006 - CalculateClusterCenterUseCase.kt
T007 - FormatClusterAnalyticsUseCase.kt
```

**Parallel Group C** (Data Layer - 2 tasks):
```bash
# Can be done after T009 completes
T010 - StopDao.kt (DAO queries)
T011 - StopRepositoryImpl.kt (repository implementation)
```

### Within User Story 1 (Phase 3)

**Parallel Group**:
```bash
# Independent files
T014 - ClusterMarker.kt composable
T016 - ClusterModule.kt Hilt module
```

### Within User Story 2 (Phase 4)

**Parallel Group**:
```bash
# Independent UI components
T019 - StopListItem.kt
T020 - ClusterDetailBottomSheet.kt (depends on T019 completing first within same file)
```

### Within User Story 3 (Phase 5)

**Parallel Group**:
```bash
# Independent tasks
T024 - ClusterFilterControls.kt composable
T026 - StopsMapScreen integration (after T024 for binding)
```

### Within User Story 4 (Phase 6)

**Parallel Group**:
```bash
# Independent navigation changes
T028 - BottomNavDestination.kt
T029 - AppNavigation.kt
T030 - MainActivity.kt
```

### Within Polish (Phase 7)

**Parallel Group**:
```bash
# Independent test scenarios
T033 - Dark mode testing
T034 - Accessibility testing
```

---

## Testing Strategy

### Unit Tests (Domain Layer)

**Priority**: High (test business logic before UI)

**Test Files to Create**:
- `app/src/test/.../GetClusteredStopsUseCaseTest.kt` (test filter logic)
- `app/src/test/.../CalculateClusterStatsUseCaseTest.kt` (test aggregation)
- `app/src/test/.../FormatClusterAnalyticsUseCaseTest.kt` (test frequency text)
- `app/src/test/.../CalculateClusterCenterUseCaseTest.kt` (test coordinate averaging)
- `app/src/test/.../ClusterMapViewModelTest.kt` (test state management)

**Run After**: Phase 2 (Foundation) complete

```bash
./gradlew :app:testDebugUnitTest --tests="*ClusterStatsUseCase*"
./gradlew :app:testDebugUnitTest --tests="*ClusterMapViewModel*"
```

### Integration Tests (Data Layer)

**Priority**: Medium (validate database queries)

**Test Files to Create**:
- `app/src/androidTest/.../StopDaoTest.kt` (test cluster queries)
- `app/src/androidTest/.../StopRepositoryImplTest.kt` (test repository methods)

**Run After**: Phase 2 (Foundation) complete

```bash
./gradlew :app:connectedAndroidTest --tests="*StopDao*"
```

### UI Tests (Compose)

**Priority**: Low (manual emulator testing sufficient for MVP)

**Test Files to Create** (Optional):
- `app/src/androidTest/.../StopsMapScreenTest.kt` (test map rendering, marker taps, bottom sheet)

**Run After**: Phase 4 (MVP complete)

```bash
./gradlew :app:connectedAndroidTest --tests="*StopsMapScreen*"
```

### Emulator Testing (Manual)

**Priority**: Critical (required before merge)

**Required Tests**:
- Phase 3: US1 emulator validation (T018)
- Phase 4: US2 emulator validation (T023)
- Phase 5: US3 emulator validation (T027)
- Phase 6: US4 emulator validation (T032)
- Phase 7: Dark mode, TalkBack, performance, edge cases (T033-T036)

**Validation Checklist**:
- [ ] Map displays with clustered stops as color-coded markers
- [ ] Tapping marker opens bottom sheet with correct data
- [ ] Scrolling stop list is smooth (60fps)
- [ ] Filters update map display correctly
- [ ] Stops tab navigation works
- [ ] Map state persists across tab switches
- [ ] Dark mode renders correctly
- [ ] TalkBack navigation works for all elements
- [ ] Performance targets met (<2s load, 60fps gestures)
- [ ] All edge cases handled gracefully

---

## Code Review Checklist

Before submitting PR, verify:
- [ ] All 36 tasks completed (T001-T036)
- [ ] Unit tests passing for all use cases and ViewModel
- [ ] Emulator testing completed for all user stories
- [ ] Dark mode works correctly
- [ ] TalkBack accessibility tested
- [ ] No memory leaks (Profiler check)
- [ ] TODO.md updated with Feature 011 completion
- [ ] RELEASE.md updated with v0.11.0 entry
- [ ] No lint warnings in new code
- [ ] All files follow BikeRedlights MVVM + Clean Architecture patterns
- [ ] Kotlin coding conventions followed
- [ ] Material 3 theming used consistently
- [ ] No hardcoded strings (use string resources)
- [ ] All composables have @Composable annotation
- [ ] StateFlow used for reactive UI updates
- [ ] Hilt dependency injection configured correctly

---

## File Checklist

### New Files (22)

**Domain Layer (7)**:
- [ ] `app/src/main/java/com/example/bikeredlights/domain/model/ClusterSummary.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/domain/model/ClusterMarkerData.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/domain/model/StopClusterFilter.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/domain/usecase/GetClusteredStopsUseCase.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/domain/usecase/CalculateClusterStatsUseCase.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/domain/usecase/FormatClusterAnalyticsUseCase.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/domain/usecase/CalculateClusterCenterUseCase.kt`

**UI Layer (8)**:
- [ ] `app/src/main/java/com/example/bikeredlights/ui/viewmodel/ClusterMapUiState.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/ui/viewmodel/ClusterMapViewModel.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/ui/components/clusters/ClusterMarker.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/ui/components/clusters/ClusterFilterControls.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/ui/components/clusters/StopListItem.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/ui/screens/clusters/StopsMapScreen.kt`
- [ ] `app/src/main/java/com/example/bikeredlights/ui/screens/clusters/ClusterDetailBottomSheet.kt`

**DI (1)**:
- [ ] `app/src/main/java/com/example/bikeredlights/di/ClusterModule.kt`

**Tests (6 - Optional)**:
- [ ] `app/src/test/java/com/example/bikeredlights/domain/usecase/GetClusteredStopsUseCaseTest.kt`
- [ ] `app/src/test/java/com/example/bikeredlights/domain/usecase/CalculateClusterStatsUseCaseTest.kt`
- [ ] `app/src/test/java/com/example/bikeredlights/domain/usecase/FormatClusterAnalyticsUseCaseTest.kt`
- [ ] `app/src/test/java/com/example/bikeredlights/domain/usecase/CalculateClusterCenterUseCaseTest.kt`
- [ ] `app/src/test/java/com/example/bikeredlights/ui/viewmodel/ClusterMapViewModelTest.kt`
- [ ] `app/src/androidTest/java/com/example/bikeredlights/ui/screens/StopsMapScreenTest.kt`

### Modified Files (4)

**Data Layer (2)**:
- [ ] `app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt` (add 4 query methods)
- [ ] `app/src/main/java/com/example/bikeredlights/data/repository/StopRepositoryImpl.kt` (add 3 repository methods)

**Navigation (2)**:
- [ ] `app/src/main/java/com/example/bikeredlights/ui/navigation/BottomNavDestination.kt` (add STOPS enum)
- [ ] `app/src/main/java/com/example/bikeredlights/MainActivity.kt` (add StopCircle icon case, update alwaysShowLabel)

---

## Success Metrics

**Feature is COMPLETE when**:
- ✅ All 36 tasks (T001-T036) marked as complete
- ✅ All 4 user stories (US1-US4) validated on emulator
- ✅ Code review checklist 100% complete
- ✅ Performance targets met (SC-001 through SC-010 from spec.md)
- ✅ Dark mode and accessibility validated
- ✅ No regressions in existing features (Live, Rides, Settings tabs still work)

**Merge Ready when**:
- ✅ All tests passing (unit, integration, UI)
- ✅ TODO.md and RELEASE.md updated
- ✅ No lint warnings
- ✅ Emulator testing completed successfully
- ✅ Code review completed

**Post-Merge**:
- Version bump to v0.11.0 in build.gradle.kts
- Git tag creation: `git tag -a v0.11.0 -m "Release v0.11.0: Stop Cluster Visualization"`
- Signed APK build: `./gradlew assembleRelease`
- GitHub Release with APK attachment

---

**Total Estimated Time**: 6-8 hours (including testing)
**MVP Time**: 3-4 hours (Phase 1-4 only)
**Complexity**: Medium (builds on established patterns)
