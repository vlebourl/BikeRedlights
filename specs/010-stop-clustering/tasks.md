# Tasks: Stop Clustering

**Input**: Design documents from `/specs/010-stop-clustering/`
**Prerequisites**: plan.md, spec.md, research.md, data-model.md, contracts/, quickstart.md

**Tests**: Unit tests are REQUIRED for this feature (critical business logic: DBSCAN algorithm, Haversine distance). Following TDD workflow from quickstart.md.

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Path Conventions

Android mobile app (single module):
- Domain layer: `app/src/main/java/com/example/bikeredlights/domain/`
- Data layer: `app/src/main/java/com/example/bikeredlights/data/`
- DI layer: `app/src/main/java/com/example/bikeredlights/di/`
- Unit tests: `app/src/test/java/com/example/bikeredlights/`

---

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization - verify database schema and settings infrastructure

- [ ] T001 Verify database at version 2 with stops table containing cluster_id column in app/src/main/java/com/example/bikeredlights/data/local/BikeRedlightsDatabase.kt
- [ ] T002 Verify clustering radius setting exists in Feature 008 (Stop Detection Settings) via SettingsRepository
- [ ] T003 [P] Create domain/util/ directory for pure Kotlin functions (HaversineDistance, DBSCANAlgorithm)

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Core clustering utilities that MUST be complete before ANY user story can be implemented

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

### HaversineDistance Implementation (Pure Function - No Android Dependencies)

- [ ] T004 [P] Write HaversineDistance unit tests in app/src/test/java/com/example/bikeredlights/domain/util/HaversineDistanceTest.kt
  - Test: Same point returns 0m distance
  - Test: Known distance (Google campus to Moscone Center ≈ 49km)
  - Test: Two stops within 20m are neighbors
  - Test: Cross-meridian (longitude wrap at ±180°)
  - Test: Polar regions (high latitude)
- [ ] T005 Implement haversineDistance function in app/src/main/java/com/example/bikeredlights/domain/util/HaversineDistance.kt
  - Use Haversine formula: a = sin²(Δlat/2) + cos(lat1)*cos(lat2)*sin²(Δlon/2), c = 2*atan2(√a, √(1−a)), distance = R*c
  - Earth radius R = 6371000.0 meters
  - Return Float (meters)
- [ ] T006 Verify all HaversineDistance tests pass: `./gradlew test --tests HaversineDistanceTest`

### DBSCANAlgorithm Implementation (Pure Function - No Android Dependencies)

- [ ] T007 [P] Write DBSCANAlgorithm unit tests in app/src/test/java/com/example/bikeredlights/domain/util/DBSCANAlgorithmTest.kt
  - Test: Empty dataset (0 points) returns empty clusters
  - Test: Single point returns 1 singleton cluster
  - Test: Three points within epsilon form 1 cluster (minPts=3)
  - Test: Two points far apart (>epsilon) get 2 singleton clusters
  - Test: 10 points forming 2 distinct clusters
  - Test: Epsilon boundary (exactly 20.0m distance, inclusive comparison)
  - Test: Noise points get unique singleton cluster_ids
- [ ] T008 Create DBSCANAlgorithm interface in app/src/main/java/com/example/bikeredlights/domain/util/DBSCANAlgorithm.kt
  - Define cluster(pointCount, epsilon, minPts, distanceFunction) method
  - Define ClusteringResult data class (clusters Map, clusterCount, noiseCount)
- [ ] T009 Implement DBSCANAlgorithmImpl in app/src/main/java/com/example/bikeredlights/domain/util/DBSCANAlgorithm.kt
  - Algorithm: For each unvisited point, find neighbors within epsilon
  - If neighbors.size < minPts: assign singleton cluster (noise)
  - Else: expand cluster via density-reachable points
  - Return Map<clusterId, List<pointIndices>>
- [ ] T010 Verify all DBSCANAlgorithm tests pass: `./gradlew test --tests DBSCANAlgorithmTest`

**Checkpoint**: Foundation ready - pure functions tested and working. User story implementation can now begin.

---

## Phase 3: User Story 1 - Automatic Stop Clustering (Priority: P1) 🎯 MVP

**Goal**: Automatically cluster nearby stops (within configured radius) across all rides using DBSCAN algorithm. Updates cluster_id in database. Enables "you stopped here 3 times" insights.

**Independent Test**: Record 3 rides with stops at same intersection (within 20m), query database to verify all 3 stops have same cluster_id.

**Acceptance Criteria**:
- 3 stops at same intersection (≤20m) → same cluster_id
- 2 stops at different intersections (>20m) → unique cluster_ids
- Single isolated stop → unique cluster_id (singleton)
- Radius change (20m→30m) triggers re-clustering
- Delete ride → remaining stops retain cluster_id

### Domain Layer Tests & Implementation

- [ ] T011 [P] [US1] Write ClusterStopsUseCase unit tests in app/src/test/java/com/example/bikeredlights/domain/usecase/ClusterStopsUseCaseTest.kt
  - Test: Empty database (0 stops) returns 0
  - Test: 3 stops at same location create 1 cluster
  - Test: 3 stops at different locations create 3 singleton clusters
  - Test: Mock StopRepository.getAllStops() and verify updateClusterAssignments() called correctly
  - Test: Verify epsilon from SettingsRepository.getClusteringRadius()
- [ ] T012 [P] [US1] Create StopCluster domain model in app/src/main/java/com/example/bikeredlights/domain/model/StopCluster.kt
  - Properties: clusterId, stopCount, centroidLatitude, centroidLongitude, averageDuration, totalDuration, earliestStop, latestStop
  - Validation: stopCount >= 1, centroid in valid GPS range, averageDuration > 0
  - Computed property: isFrequent (stopCount >= 5)
- [ ] T013 [US1] Implement ClusterStopsUseCase in app/src/main/java/com/example/bikeredlights/domain/usecase/ClusterStopsUseCase.kt
  - Inject: StopRepository, SettingsRepository, DBSCANAlgorithm
  - clusterAllStops(): Fetch stops, run DBSCAN with Haversine distance, persist assignments
  - getClusteringConfig(): Return (epsilon, minPts=3)
  - Use Hilt @Inject constructor
- [ ] T014 Verify ClusterStopsUseCase tests pass: `./gradlew test --tests ClusterStopsUseCaseTest`

### Data Layer: DAO Extensions

- [ ] T015 [P] [US1] Extend StopDao with clustering queries in app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt
  - Add: getAllStops(): List<StopEntity> (ORDER BY start_timestamp ASC)
  - Add: updateClusterIds(clusterId: Long, stopIds: List<Long>) - batch UPDATE query
  - Add: getStopsByClusterId(clusterId: Long): List<StopEntity>
  - Add: clearAllClusterAssignments() - SET cluster_id = NULL for all
- [ ] T016 [P] [US1] Create ClusterStatsDto in app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt
  - @ColumnInfo annotations for: cluster_id, stop_count, centroid_lat, centroid_lon, avg_duration, total_duration, earliest_stop, latest_stop
  - Extension function: toDomain() → StopCluster
- [ ] T017 [US1] Add getClusterStats query to StopDao in app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt
  - SQL: SELECT cluster_id, COUNT(*) as stop_count, AVG(latitude), AVG(longitude), AVG(duration_seconds), SUM(duration_seconds), MIN(start_timestamp), MAX(start_timestamp) FROM stops WHERE cluster_id = :clusterId GROUP BY cluster_id
  - Return: ClusterStatsDto?
- [ ] T018 [US1] Add getAllClusterStatsFlow query to StopDao in app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt
  - SQL: Same as getClusterStats but WHERE cluster_id IS NOT NULL, ORDER BY stop_count DESC
  - Return: Flow<List<ClusterStatsDto>>
- [ ] T019 Build project to verify DAO compiles: `./gradlew assembleDebug`

### Data Layer: Repository Implementation

- [ ] T020 [US1] Extend StopRepository interface in app/src/main/java/com/example/bikeredlights/domain/repository/StopRepository.kt
  - Add: getAllStops(): List<Stop>
  - Add: updateClusterAssignments(clusterAssignments: Map<Long, List<Long>>)
  - Add: getClusterStats(clusterId: Long): StopCluster?
  - Add: getAllClustersFlow(): Flow<List<StopCluster>>
  - Add: clearAllClusterAssignments()
- [ ] T021 [US1] Implement clustering methods in StopRepositoryImpl in app/src/main/java/com/example/bikeredlights/data/repository/StopRepositoryImpl.kt
  - getAllStops(): Map stopDao.getAllStops() to domain models
  - updateClusterAssignments(): @Transaction loop over map, call stopDao.updateClusterIds()
  - getClusterStats(): Map stopDao.getClusterStats()?.toDomain()
  - getAllClustersFlow(): Map Flow of DTOs to domain models
  - clearAllClusterAssignments(): Call stopDao.clearAllClusterAssignments()

### Dependency Injection

- [ ] T022 [US1] Provide ClusterStopsUseCase in Hilt module app/src/main/java/com/example/bikeredlights/di/AppModule.kt
  - @Provides @Singleton fun provideClusterStopsUseCase(stopRepository: StopRepository, settingsRepository: SettingsRepository, dbscanAlgorithm: DBSCANAlgorithm): ClusterStopsUseCase
- [ ] T023 [US1] Provide DBSCANAlgorithm in Hilt module app/src/main/java/com/example/bikeredlights/di/AppModule.kt
  - @Provides @Singleton fun provideDBSCANAlgorithm(): DBSCANAlgorithm = DBSCANAlgorithmImpl()

### Integration Tests

- [ ] T024 [US1] Write StopDao integration tests in app/src/test/java/com/example/bikeredlights/data/local/dao/StopDaoTest.kt
  - Use in-memory Room database
  - Test: Insert 50 stops, getAllStops() returns 50 ordered by timestamp
  - Test: updateClusterIds with 10 stop IDs, verify cluster_id = 5 in database
  - Test: getClusterStats returns correct COUNT, AVG, SUM calculations
  - Test: getAllClusterStatsFlow emits 3 clusters sorted by stop_count DESC
  - Test: DELETE ride CASCADE deletes stops + cluster_id
- [ ] T025 [US1] Write ClusterStopsUseCase integration tests in app/src/test/java/com/example/bikeredlights/domain/usecase/ClusterStopsUseCaseIntegrationTest.kt
  - Use in-memory Room database + real repository
  - Test: Insert 3 stops at same location, clusterAllStops(), verify 1 cluster_id
  - Test: Insert 5 stops (3 at location A, 2 at location B >20m away), verify 2 clusters
  - Test: Insert 10 stops all >20m apart, verify 10 singleton clusters

### Manual Testing Preparation

- [ ] T026 [US1] Document manual testing SQL queries in specs/010-stop-clustering/manual-test.md
  - SQL: INSERT test rides with stops at known coordinates
  - SQL: SELECT cluster_id, COUNT(*) FROM stops GROUP BY cluster_id
  - SQL: Verify cluster stats query results
  - Expected results documented

**Checkpoint**: At this point, User Story 1 (Automatic Clustering) should be fully functional and testable independently via database queries.

---

## Phase 4: User Story 1 Extension - WorkManager Re-Clustering (Priority: P1 continuation)

**Goal**: Trigger full re-clustering in background when clustering radius setting changes (via WorkManager).

**Independent Test**: Change radius setting in UI, verify WorkManager job enqueued, verify stops re-clustered with new radius.

### Background Worker Implementation

- [ ] T027 [US1] Create ClusteringWorker in app/src/main/java/com/example/bikeredlights/background/ClusteringWorker.kt
  - Extend CoroutineWorker
  - Inject ClusterStopsUseCase via Hilt WorkerFactory
  - doWork(): Call clusterStopsUseCase.clusterAllStops(), setProgress with status
  - Return Result.success() or Result.retry() on exception
- [ ] T028 [US1] Provide HiltWorkerFactory in app/src/main/java/com/example/bikeredlights/BikeRedlightsApplication.kt
  - Inject WorkerFactory, configure WorkManager with it in onCreate()
- [ ] T029 [US1] Add triggerReClustering to SettingsRepository in app/src/main/java/com/example/bikeredlights/domain/repository/SettingsRepository.kt
  - Enqueue OneTimeWorkRequest for ClusteringWorker
  - Set constraints: requiresBatteryNotLow = true
- [ ] T030 [US1] Call triggerReClustering() when radius setting changes in SettingsRepositoryImpl
  - Detect radius change in updateClusteringRadius()
  - Enqueue WorkManager job

**Checkpoint**: WorkManager integration complete. Changing radius triggers background re-clustering.

---

## Phase 5: User Story 2 - Cluster Analytics (Priority: P2)

**Goal**: Display aggregated statistics for each cluster (stop count, average duration, total time, frequency ranking). UI shows "You stopped at this intersection 15 times this month, avg delay 30s".

**Independent Test**: Create 3 rides with 5 stops at same cluster, verify analytics screen shows "5 stops, avg duration 30s, total 2.5 min".

**Acceptance Criteria**:
- Cluster with 10 stops averaging 25s → shows "10 stops, avg 25s, total 4m 10s"
- 5 clusters sorted by frequency → ranked most stops to fewest
- Filter by "this month" → shows only current calendar month stops
- Cluster with durations 10s-120s → shows min/max/avg statistics

**Note**: This phase is OPTIONAL for MVP. Can defer UI to post-MVP and validate via database queries only.

### ViewModel Layer (if implementing UI)

- [ ] T031 [P] [US2] Create ClusterAnalyticsUiState in app/src/main/java/com/example/bikeredlights/ui/viewmodel/ClusterAnalyticsViewModel.kt
  - Data class: clusters: List<StopCluster>, dateFilter: DateFilter (ALL/THIS_WEEK/THIS_MONTH), isLoading: Boolean
- [ ] T032 [US2] Create ClusterAnalyticsViewModel in app/src/main/java/com/example/bikeredlights/ui/viewmodel/ClusterAnalyticsViewModel.kt
  - Inject: StopRepository
  - Collect getAllClustersFlow() → filter by dateFilter → emit as uiState StateFlow
  - Function: setDateFilter(filter: DateFilter)
  - Use @HiltViewModel annotation
- [ ] T033 [P] [US2] Write ClusterAnalyticsViewModel unit tests in app/src/test/java/com/example/bikeredlights/ui/viewmodel/ClusterAnalyticsViewModelTest.kt
  - Test: uiState emits clusters sorted by frequency
  - Test: setDateFilter(THIS_MONTH) filters to current month only
  - Test: Mock repository.getAllClustersFlow()

### UI Layer (if implementing UI)

- [ ] T034 [P] [US2] Create ClusterAnalyticsScreen composable in app/src/main/java/com/example/bikeredlights/ui/screens/cluster/ClusterAnalyticsScreen.kt
  - Display list of clusters with: centroid coordinates, stop count, avg duration, total duration
  - Sorted by frequency (most stops first)
  - Date filter chips: All Time, This Week, This Month
  - Use Material 3 components (LazyColumn, FilterChip, Card)
- [ ] T035 [P] [US2] Create ClusterAnalyticsItem composable in app/src/main/java/com/example/bikeredlights/ui/screens/cluster/ClusterAnalyticsScreen.kt
  - Display: cluster location (lat/lon formatted), "Stopped here X times", "Avg duration: Xs", "Total time: Xm"
  - Use Material 3 Card with onClick for details
- [ ] T036 [US2] Add navigation route to ClusterAnalyticsScreen in app/src/main/java/com/example/bikeredlights/ui/navigation/AppNavGraph.kt
  - Route: "cluster_analytics"
  - Add navigation from Settings or Ride History screen

**Checkpoint**: At this point, User Stories 1 AND 2 should both work independently (clustering + analytics).

---

## Phase 6: User Story 3 - Manual Cluster Management (Priority: P3) ⏸️ DEFER TO POST-MVP

**Goal**: Allow power users to manually split clusters into 2 or merge 2 clusters together. Preserves manual edits during auto-reclustering.

**Independent Test**: Create cluster with 6 stops, split into two 3-stop clusters, verify both persist and are not overwritten by re-clustering.

**Status**: DEFERRED - Low priority, most users satisfied with automatic clustering. Implement only if user feedback requests it.

**If Implemented Later**:

### Domain Layer

- [ ] T037 [P] [US3] Add splitCluster method to StopRepository interface
  - splitCluster(originalClusterId: Long, stopIdsForNewCluster: List<Long>)
  - Generate new cluster_id, update selected stops
  - Mark cluster as manually edited (prevent auto-overwrite)
- [ ] T038 [P] [US3] Add mergeClusters method to StopRepository interface
  - mergeClusters(cluster1Id: Long, cluster2Id: Long)
  - Move all stops from cluster2 to cluster1
  - Mark cluster as manually edited

### UI Layer

- [ ] T039 [US3] Create ClusterDetailsScreen with split/merge UI
  - List all stops in cluster with checkboxes
  - "Split" button → create new cluster from selected stops
  - "Merge" button → select target cluster, merge
- [ ] T040 [US3] Update ClusterStopsUseCase to skip manually-edited clusters during auto-reclustering
  - Use negative cluster_id (-1, -2, -3) for manual clusters
  - Filter out negative cluster_ids during DBSCAN

**Checkpoint**: All 3 user stories independently functional (if P3 implemented).

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Improvements that affect multiple user stories

- [ ] T041 [P] Update TODO.md marking Feature 010 as complete in /Users/vlb/AndroidStudioProjects/BikeRedlights/TODO.md
  - Move from "In Progress" to "Completed" section
  - Add completion date and implementation summary
- [ ] T042 [P] Update RELEASE.md with Feature 010 entry in /Users/vlb/AndroidStudioProjects/BikeRedlights/RELEASE.md
  - Add to "Unreleased" section: "Feature 010: Stop Clustering - Automatic geospatial clustering with DBSCAN"
  - Document: FR-001 to FR-016 implemented
  - Note database version remains at 2 (no migration needed)
- [ ] T043 [P] Add performance benchmarks in app/src/test/java/com/example/bikeredlights/domain/util/DBSCANPerformanceTest.kt
  - Benchmark: 100 stops cluster in <20ms
  - Benchmark: 500 stops cluster in <75ms
  - Benchmark: 1000 stops cluster in <200ms
- [ ] T044 Run quickstart.md manual validation steps
  - Insert test rides with stops at known coordinates
  - Run clustering via debug breakpoint or temporary button
  - Verify cluster_id assignments via Database Inspector
  - Validate cluster stats SQL queries
- [ ] T045 [P] Code cleanup and refactoring
  - Remove debug logging from production code
  - Verify Kotlin lint warnings addressed
  - Format code per project style guide
- [ ] T046 Test on emulator with GPS simulation
  - Record 3 real rides using GPX playback (same intersection stops)
  - Verify clustering works with realistic GPS drift (±5-10m)
  - Change radius setting 20m→30m, verify re-clustering
- [ ] T047 Build signed release APK: `./gradlew assembleRelease`

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies - can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion - BLOCKS all user stories
- **User Story 1 (Phase 3+4)**: Depends on Foundational phase completion - No dependencies on other stories
- **User Story 2 (Phase 5)**: Depends on Foundational + User Story 1 completion (uses cluster_id from US1)
- **User Story 3 (Phase 6)**: DEFERRED - Depends on User Story 1 completion if implemented
- **Polish (Phase 7)**: Depends on desired user stories being complete (minimum: US1)

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) - No dependencies on other stories - **MVP COMPLETE AT THIS POINT**
- **User Story 2 (P2)**: Depends on User Story 1 (needs cluster_id assignments) - Analytics requires clustering to run first
- **User Story 3 (P3)**: DEFERRED - Would depend on User Story 1 if implemented

### Within Each User Story

- Tests MUST be written and FAIL before implementation (TDD workflow)
- HaversineDistance before DBSCANAlgorithm (distance function used by clustering)
- DBSCANAlgorithm before ClusterStopsUseCase (algorithm used by use case)
- Domain models before DAO extensions (Stop already exists, StopCluster new)
- DAO extensions before Repository implementation (DAO methods called by repository)
- Repository before UseCase (use case calls repository)
- UseCase before WorkManager (worker calls use case)
- Core clustering (US1) before Analytics UI (US2)

### Parallel Opportunities

**Phase 2 - Foundational**:
- T004 HaversineDistance tests [P] || T007 DBSCAN tests [P]
- T005 Haversine impl || (wait for T004 to pass)
- T008 DBSCAN interface [P] || T009 DBSCAN impl [P] (after T007 written)

**Phase 3 - User Story 1**:
- T011 UseCase tests [P] || T012 StopCluster model [P]
- T015 DAO getAllStops [P] || T016 ClusterStatsDto [P] || T017 getClusterStats [P] || T018 getAllClusterStatsFlow [P]
- T022 Hilt UseCase provider [P] || T023 Hilt DBSCAN provider [P]
- T024 StopDao tests [P] || T025 UseCase integration tests [P]

**Phase 5 - User Story 2** (if implemented):
- T031 UiState [P] || T033 ViewModel tests [P]
- T034 Analytics screen [P] || T035 Analytics item [P]

**Phase 7 - Polish**:
- T041 TODO.md [P] || T042 RELEASE.md [P] || T043 Performance tests [P] || T045 Code cleanup [P]

---

## Parallel Example: User Story 1 Foundational

```bash
# Launch HaversineDistance and DBSCAN tests together:
Task: "Write HaversineDistance unit tests in .../HaversineDistanceTest.kt"
Task: "Write DBSCANAlgorithm unit tests in .../DBSCANAlgorithmTest.kt"

# Launch DAO extension methods together (different queries):
Task: "Add getAllStops() to StopDao"
Task: "Add getClusterStats() to StopDao"
Task: "Add getAllClusterStatsFlow() to StopDao"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only) - RECOMMENDED

1. ✅ Complete Phase 1: Setup (verify database, settings) - **5 min**
2. ✅ Complete Phase 2: Foundational (HaversineDistance, DBSCAN) - **90 min**
3. ✅ Complete Phase 3: User Story 1 (clustering algorithm + database integration) - **90 min**
4. ✅ Complete Phase 4: WorkManager integration - **20 min**
5. **STOP and VALIDATE**: Test clustering via database queries
6. ✅ Complete Phase 7: Polish (docs, benchmarks, emulator test) - **60 min**
7. Deploy/demo **MVP READY** - Clustering works, validates via SQL

**Total MVP Time**: ~4 hours (matches quickstart.md estimate)

### Incremental Delivery

1. Complete Setup + Foundational → Pure functions tested and working
2. Add User Story 1 + WorkManager → Test clustering via database → Deploy/Demo (MVP!)
3. **OPTIONAL**: Add User Story 2 (Analytics UI) → Test independently → Deploy/Demo
4. **DEFERRED**: User Story 3 (Manual Management) → Implement only if requested by users

### Validation Points

- After T010: DBSCAN algorithm proven correct with unit tests
- After T014: ClusterStopsUseCase logic validated with mocked repository
- After T025: End-to-end clustering works with real database
- After T044: Manual testing confirms clustering works on emulator
- After T046: GPS simulation validates clustering with realistic drift

---

## Notes

- [P] tasks = different files, no dependencies, can run in parallel
- [US1] label = task belongs to User Story 1 (Automatic Clustering)
- [US2] label = task belongs to User Story 2 (Analytics)
- [US3] label = task belongs to User Story 3 (Manual Management) - DEFERRED
- Tests written FIRST (TDD workflow), must FAIL before implementation
- Commit after each task or logical group (conventional commit format)
- Stop at any checkpoint to validate story independently
- MVP = User Story 1 only (Phases 1-4 + 7) = ~4 hours
- User Story 2 (Analytics) is optional UI enhancement
- User Story 3 (Manual Management) is deferred to post-MVP

---

## Task Count Summary

- **Phase 1 (Setup)**: 3 tasks
- **Phase 2 (Foundational)**: 7 tasks (HaversineDistance: 3, DBSCAN: 4)
- **Phase 3 (User Story 1 - Core Clustering)**: 16 tasks
- **Phase 4 (User Story 1 - WorkManager)**: 4 tasks
- **Phase 5 (User Story 2 - Analytics)**: 6 tasks (OPTIONAL)
- **Phase 6 (User Story 3 - Manual Management)**: 4 tasks (DEFERRED)
- **Phase 7 (Polish)**: 7 tasks

**Total Tasks**: 47 tasks
**MVP Tasks (US1 only)**: 30 tasks (Phases 1-4 + 7)
**Parallel Opportunities**: 15 tasks marked [P]
