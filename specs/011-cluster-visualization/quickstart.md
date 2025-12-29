# Developer Quickstart: Stop Cluster Visualization

**Feature**: 011-cluster-visualization
**Date**: 2025-12-29
**Target Release**: v0.11.0

This guide helps developers quickly understand and implement Feature 011: Stop Cluster Visualization.

---

## Table of Contents

1. [Feature Overview](#feature-overview)
2. [Prerequisites](#prerequisites)
3. [Architecture Overview](#architecture-overview)
4. [Implementation Checklist](#implementation-checklist)
5. [Key Files](#key-files)
6. [Development Workflow](#development-workflow)
7. [Testing Strategy](#testing-strategy)
8. [Common Pitfalls](#common-pitfalls)

---

## Feature Overview

### What We're Building

An interactive map screen showing clustered bike stops with:
- **Color-coded markers**: Green (2-5 stops), Yellow (6-10), Red (11+)
- **Tappable cluster details**: Bottom sheet with stop count, avg duration, analytics, and scrollable list
- **Filters**: Date range (Last 7/30 days, Custom) and minimum cluster size (2+, 3+, 5+, 10+)
- **New navigation tab**: "Stops" tab with stop_circle icon

### User Flow

```
User taps "Stops" tab
    ↓
Map loads with color-coded cluster markers
    ↓
User taps a cluster marker
    ↓
Bottom sheet opens showing:
  - "You stopped here 15 times this month"
  - Total stops: 15 | Avg duration: 45s
  - Scrollable list of individual stops (dates/times/durations)
    ↓
User swipes down or taps outside → bottom sheet dismisses
    ↓
User applies filters (e.g., "Last 7 days" + "5+ stops")
    ↓
Map updates to show only matching clusters
```

---

## Prerequisites

### Dependencies (Already Satisfied)

- **Jetpack Compose BOM 2024.11.00**: Material 3 ModalBottomSheet
- **Maps Compose 6.2.0**: Google Maps integration (Feature 006)
- **Room 2.6.1**: Database queries (Feature 009)
- **Hilt 2.51.1**: Dependency injection
- **Kotlin Coroutines 1.9.0**: Asynchronous data flow

### Required Knowledge

- **Feature 006**: BikeMap composable, Google Maps integration
- **Feature 009**: Stop entity, StopRepository, StopDao
- **Feature 010**: cluster_id field, DBSCAN clustering logic
- **MVVM + Clean Architecture**: Domain → Data → UI layers
- **Jetpack Compose**: State management, LazyColumn, ModalBottomSheet

### Database Schema (Existing)

```sql
CREATE TABLE stops (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    ride_id INTEGER NOT NULL,
    start_time INTEGER NOT NULL,
    end_time INTEGER,
    latitude REAL NOT NULL,
    longitude REAL NOT NULL,
    cluster_id INTEGER,  -- Populated by Feature 010, NULL = noise
    FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE
);

CREATE INDEX idx_stops_cluster_id ON stops(cluster_id);
CREATE INDEX idx_stops_start_time ON stops(start_time);  -- NEW: for date filtering
CREATE INDEX idx_stops_cluster_start ON stops(cluster_id, start_time);  -- NEW: composite
```

---

## Architecture Overview

### Layer Diagram

```
┌──────────────────────────────────────────────────────────────┐
│ UI Layer                                                     │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ StopsMapScreen (composable)                              │ │
│ │   ├── BikeMap with ClusterMarkers                        │ │
│ │   └── if (selectedCluster) ModalBottomSheet {            │ │
│ │         ClusterDetailBottomSheet                         │ │
│ │       }                                                   │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ ClusterMapViewModel                                      │ │
│ │   State: StateFlow<ClusterMapUiState>                    │ │
│ │   Events: applyFilter(), selectCluster(), clearFilters() │ │
│ └──────────────────────────────────────────────────────────┘ │
└────────────────────────┬─────────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────────┐
│ Domain Layer (Use Cases)                                     │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ GetClusteredStopsUseCase(filter)                         │ │
│ │   → Flow<List<Stop>>                                     │ │
│ │                                                           │ │
│ │ CalculateClusterStatsUseCase(stops)                      │ │
│ │   → List<ClusterSummary>                                 │ │
│ │                                                           │ │
│ │ FormatClusterAnalyticsUseCase(count, timestamps)         │ │
│ │   → "You stopped here 15 times this month"               │ │
│ │                                                           │ │
│ │ CalculateClusterCenterUseCase(stops)                     │ │
│ │   → LatLng (arithmetic mean)                              │ │
│ └──────────────────────────────────────────────────────────┘ │
└────────────────────────┬─────────────────────────────────────┘
                         │
┌────────────────────────▼─────────────────────────────────────┐
│ Data Layer (Repository)                                      │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ StopRepository (extended)                                │ │
│ │   - getClusteredStops(): Flow<List<Stop>>                │ │
│ │   - getClusteredStopsByDateRange(...): Flow<List<Stop>>  │ │
│ │   - getStopsGroupedByCluster(): Flow<Map<Long, ...>>     │ │
│ └──────────────────────────────────────────────────────────┘ │
│                                                              │
│ ┌──────────────────────────────────────────────────────────┐ │
│ │ StopDao (extended)                                       │ │
│ │   @Query("SELECT * FROM stops WHERE cluster_id IS NOT    │ │
│ │           NULL ORDER BY start_time DESC")                │ │
│ └──────────────────────────────────────────────────────────┘ │
└──────────────────────────────────────────────────────────────┘
```

### Data Flow

```
User Action → ViewModel Event → Use Case → Repository → DAO → Room
                                                                 ↓
User sees UI ← Compose Recomposition ← StateFlow ← Use Case ← Flow
```

---

## Implementation Checklist

### Phase 1: Domain Layer (No Android Dependencies)

- [ ] Create `domain/model/ClusterSummary.kt`
- [ ] Create `domain/model/ClusterMarkerData.kt` with MarkerColor enum
- [ ] Create `domain/model/StopClusterFilter.kt` with DateRange + presets
- [ ] Create `domain/usecase/GetClusteredStopsUseCase.kt`
- [ ] Create `domain/usecase/CalculateClusterStatsUseCase.kt`
- [ ] Create `domain/usecase/FormatClusterAnalyticsUseCase.kt`
- [ ] Create `domain/usecase/CalculateClusterCenterUseCase.kt`
- [ ] **Test**: Write unit tests for all use cases (pure Kotlin logic)

### Phase 2: Data Layer

- [ ] Extend `data/local/dao/StopDao.kt` with cluster query methods
- [ ] Extend `data/repository/StopRepositoryImpl.kt` with cluster methods
- [ ] Add indexes to StopEntity: `start_time`, `(cluster_id, start_time)`
- [ ] **Test**: Integration tests with in-memory Room database

### Phase 3: UI Layer - Components

- [ ] Create `ui/components/clusters/ClusterMarker.kt` (color-coded marker)
- [ ] Create `ui/components/clusters/ClusterFilterControls.kt` (filter UI)
- [ ] Create `ui/components/clusters/StopListItem.kt` (individual stop card)
- [ ] **Test**: Compose preview functions for each component

### Phase 4: UI Layer - Screen & ViewModel

- [ ] Create `ui/viewmodel/ClusterMapViewModel.kt` with StateFlow
- [ ] Create `ui/screens/clusters/StopsMapScreen.kt` (main screen)
- [ ] Create `ui/screens/clusters/ClusterDetailBottomSheet.kt` (popup)
- [ ] **Test**: ViewModel unit tests, Compose UI tests

### Phase 5: Navigation

- [ ] Add `STOPS` to `ui/navigation/BottomNavDestination.kt`
- [ ] Update `MainActivity.kt`:
  - Import `Icons.Outlined.StopCircle`
  - Add STOPS case in icon when block
  - Change `alwaysShowLabel = false`
- [ ] Add Stops route in `ui/navigation/AppNavigation.kt`
- [ ] **Test**: Manual navigation testing on emulator

### Phase 6: Dependency Injection

- [ ] Create `di/ClusterModule.kt` (provide use cases)
- [ ] **Test**: Verify Hilt graph builds without errors

### Phase 7: End-to-End Testing

- [ ] **Emulator Test**: Install debug build on emulator
- [ ] Navigate to Stops tab → verify map displays
- [ ] Tap cluster marker → verify bottom sheet opens
- [ ] Scroll stop list → verify smooth scrolling
- [ ] Apply filters → verify map updates
- [ ] Test dark mode → verify theming
- [ ] Test TalkBack → verify accessibility

---

## Key Files

### New Files to Create (22 files)

```
Domain Layer (7 files):
├── domain/model/ClusterSummary.kt
├── domain/model/ClusterMarkerData.kt
├── domain/model/StopClusterFilter.kt
├── domain/usecase/GetClusteredStopsUseCase.kt
├── domain/usecase/CalculateClusterStatsUseCase.kt
├── domain/usecase/FormatClusterAnalyticsUseCase.kt
└── domain/usecase/CalculateClusterCenterUseCase.kt

Data Layer (no new files, extend existing):
├── data/local/dao/StopDao.kt                    [MODIFY]
└── data/repository/StopRepositoryImpl.kt        [MODIFY]

UI Layer (8 files):
├── ui/components/clusters/ClusterMarker.kt
├── ui/components/clusters/ClusterFilterControls.kt
├── ui/components/clusters/StopListItem.kt
├── ui/screens/clusters/StopsMapScreen.kt
├── ui/screens/clusters/ClusterDetailBottomSheet.kt
├── ui/viewmodel/ClusterMapViewModel.kt
├── ui/navigation/BottomNavDestination.kt        [MODIFY]
└── ui/navigation/AppNavigation.kt               [MODIFY]

DI (1 file):
└── di/ClusterModule.kt

MainActivity (1 file):
└── MainActivity.kt                               [MODIFY]

Tests (5 files):
├── test/domain/usecase/GetClusteredStopsUseCaseTest.kt
├── test/domain/usecase/CalculateClusterStatsUseCaseTest.kt
├── test/domain/usecase/FormatClusterAnalyticsUseCaseTest.kt
├── test/ui/viewmodel/ClusterMapViewModelTest.kt
└── androidTest/ui/screens/StopsMapScreenTest.kt
```

### Files to Modify (4 files)

```
1. app/src/main/java/com/example/bikeredlights/data/local/dao/StopDao.kt
   Add 4 new @Query methods (see contracts/repository.md)

2. app/src/main/java/com/example/bikeredlights/data/repository/StopRepositoryImpl.kt
   Add 3 new repository methods (see contracts/repository.md)

3. app/src/main/java/com/example/bikeredlights/ui/navigation/BottomNavDestination.kt
   Add STOPS enum value:
   STOPS(route = "stops", label = "Stops", icon = "stop_circle")

4. app/src/main/java/com/example/bikeredlights/MainActivity.kt
   - Import Icons.Outlined.StopCircle
   - Add STOPS case: Icon(imageVector = Icons.Outlined.StopCircle, ...)
   - Change alwaysShowLabel = false (line 92)
```

---

## Development Workflow

### Step-by-Step Implementation

#### Step 1: Domain Models (20 minutes)

```kotlin
// domain/model/ClusterSummary.kt
@Immutable
data class ClusterSummary(
    val clusterId: Long,
    val centerPosition: LatLng,
    val stopCount: Int,
    val averageDuration: Long,  // seconds
    val frequencyText: String,
    val stops: List<Stop>
)

// domain/model/StopClusterFilter.kt
@Immutable
data class StopClusterFilter(
    val dateRange: DateRange? = null,
    val minClusterSize: Int = 2
)

@Immutable
data class DateRange(
    val startMillis: Long,
    val endMillis: Long,
    val label: String
)
```

#### Step 2: Use Cases (30 minutes)

**Order matters**: Build from bottom-up (no dependencies first).

1. `CalculateClusterCenterUseCase` (no dependencies)
2. `FormatClusterAnalyticsUseCase` (no dependencies)
3. `CalculateClusterStatsUseCase` (depends on 1 & 2)
4. `GetClusteredStopsUseCase` (depends on repository)

**Example** (`CalculateClusterCenterUseCase`):
```kotlin
class CalculateClusterCenterUseCase {
    operator fun invoke(stops: List<Stop>): LatLng {
        require(stops.isNotEmpty()) { "Cannot calculate center of empty cluster" }

        val avgLat = stops.map { it.latitude }.average()
        val avgLng = stops.map { it.longitude }.average()

        return LatLng(latitude = avgLat, longitude = avgLng)
    }
}
```

**Test immediately**:
```kotlin
@Test
fun `single stop returns exact coordinates`() {
    val stops = listOf(Stop(latitude = 37.422, longitude = -122.084))
    val center = useCase(stops)
    assertThat(center.latitude).isEqualTo(37.422)
    assertThat(center.longitude).isEqualTo(-122.084)
}
```

#### Step 3: Data Layer (30 minutes)

**StopDao extension** (add to existing file):
```kotlin
@Query("""
    SELECT * FROM stops
    WHERE cluster_id IS NOT NULL
    ORDER BY start_time DESC
""")
fun getClusteredStops(): Flow<List<StopEntity>>
```

**StopRepositoryImpl extension**:
```kotlin
override fun getClusteredStops(): Flow<List<Stop>> {
    return stopDao.getClusteredStops()
        .map { entities -> entities.map { it.toDomainModel() } }
}
```

**Test with in-memory database**:
```kotlin
@Test
fun `getClusteredStops excludes noise points`() = runTest {
    stopDao.insert(createStopEntity(clusterId = 5))
    stopDao.insert(createStopEntity(clusterId = null))  // noise

    val result = repository.getClusteredStops().first()
    assertThat(result).hasSize(1)
}
```

#### Step 4: ViewModel (45 minutes)

```kotlin
@HiltViewModel
class ClusterMapViewModel @Inject constructor(
    private val getClusteredStopsUseCase: GetClusteredStopsUseCase,
    private val calculateClusterStatsUseCase: CalculateClusterStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ClusterMapUiState())
    val uiState: StateFlow<ClusterMapUiState> = _uiState.asStateFlow()

    init {
        loadClusters()
    }

    private fun loadClusters(filter: StopClusterFilter = StopClusterFilter()) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            getClusteredStopsUseCase(filter)
                .map { stops -> calculateClusterStatsUseCase(stops) }
                .catch { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.message
                        )
                    }
                }
                .collect { clusterSummaries ->
                    _uiState.update {
                        it.copy(
                            clusters = clusterSummaries,
                            activeFilter = filter,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun applyFilter(filter: StopClusterFilter) {
        loadClusters(filter)
    }

    fun selectCluster(cluster: ClusterSummary) {
        _uiState.update { it.copy(selectedCluster = cluster) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedCluster = null) }
    }
}
```

#### Step 5: UI Screen (60 minutes)

**StopsMapScreen** (simplified):
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopsMapScreen(
    viewModel: ClusterMapViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var showBottomSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState()

    // Show bottom sheet when cluster is selected
    LaunchedEffect(uiState.selectedCluster) {
        showBottomSheet = uiState.selectedCluster != null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map with cluster markers
        BikeMap(
            cameraPositionState = rememberCameraPositionState(),
            modifier = Modifier.fillMaxSize()
        ) {
            uiState.clusters.forEach { cluster ->
                Marker(
                    state = MarkerState(position = cluster.centerPosition),
                    title = "${cluster.stopCount} stops",
                    icon = BitmapDescriptorFactory.defaultMarker(
                        when (cluster.stopCount) {
                            in 2..5 -> BitmapDescriptorFactory.HUE_GREEN
                            in 6..10 -> BitmapDescriptorFactory.HUE_YELLOW
                            else -> BitmapDescriptorFactory.HUE_RED
                        }
                    ),
                    onClick = {
                        viewModel.selectCluster(cluster)
                        true
                    }
                )
            }
        }

        // Loading indicator
        if (uiState.isLoading) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        }

        // Bottom sheet
        if (showBottomSheet && uiState.selectedCluster != null) {
            ModalBottomSheet(
                onDismissRequest = {
                    showBottomSheet = false
                    viewModel.clearSelection()
                },
                sheetState = sheetState
            ) {
                ClusterDetailBottomSheet(
                    cluster = uiState.selectedCluster!!,
                    onDismiss = {
                        showBottomSheet = false
                        viewModel.clearSelection()
                    }
                )
            }
        }
    }
}
```

#### Step 6: Navigation Integration (15 minutes)

**BottomNavDestination.kt**:
```kotlin
enum class BottomNavDestination(val route: String, val label: String, val icon: String) {
    LIVE("live", "Live", "navigation"),
    RIDES("rides", "Rides", "list"),
    STOPS("stops", "Stops", "stop_circle"),  // NEW
    SETTINGS("settings", "Settings", "settings")
}
```

**MainActivity.kt** (add import + case):
```kotlin
import androidx.compose.material.icons.outlined.StopCircle

// In icon when block:
BottomNavDestination.STOPS -> Icon(
    imageVector = Icons.Outlined.StopCircle,
    contentDescription = destination.label
)

// Change label visibility (line 92):
alwaysShowLabel = false  // For 4+ tabs
```

---

## Testing Strategy

### Unit Tests (Domain Layer)

**Coverage Target**: 80%+

**Test Pyramid**:
```
Unit Tests (Fast, Many)
  └── Use Cases: Business logic validation
  └── ViewModels: State management
Integration Tests (Medium Speed, Some)
  └── Repository + DAO: Database queries
UI Tests (Slow, Few)
  └── Critical user flows only
```

**Example Test** (`CalculateClusterStatsUseCaseTest`):
```kotlin
@Test
fun `groups stops by cluster_id and calculates stats`() = runTest {
    // Given: 5 stops in 2 clusters
    val stops = listOf(
        Stop(clusterId = 5, latitude = 37.42, longitude = -122.08, startTime = 1000, endTime = 1030),
        Stop(clusterId = 5, latitude = 37.43, longitude = -122.09, startTime = 2000, endTime = 2045),
        Stop(clusterId = 7, latitude = 37.50, longitude = -122.10, startTime = 3000, endTime = 3060),
        Stop(clusterId = 7, latitude = 37.51, longitude = -122.11, startTime = 4000, endTime = 4090),
        Stop(clusterId = 7, latitude = 37.52, longitude = -122.12, startTime = 5000, endTime = 5120)
    )

    // When: Calculate cluster stats
    val result = calculateClusterStatsUseCase(stops)

    // Then: 2 clusters with correct stats
    assertThat(result).hasSize(2)

    val cluster5 = result.find { it.clusterId == 5L }!!
    assertThat(cluster5.stopCount).isEqualTo(2)
    assertThat(cluster5.averageDuration).isEqualTo(37)  // (30 + 45) / 2 = 37.5 → 37

    val cluster7 = result.find { it.clusterId == 7L }!!
    assertThat(cluster7.stopCount).isEqualTo(3)
    assertThat(cluster7.averageDuration).isEqualTo(90)  // (60 + 90 + 120) / 3 = 90
}
```

### Integration Tests (Data Layer)

**Use In-Memory Room Database**:
```kotlin
@Before
fun setup() {
    database = Room.inMemoryDatabaseBuilder(
        ApplicationProvider.getApplicationContext(),
        BikeDatabase::class.java
    ).allowMainThreadQueries().build()

    stopDao = database.stopDao()
    repository = StopRepositoryImpl(stopDao)
}
```

### UI Tests (Compose)

**Test Critical Flows Only** (slow tests):
```kotlin
@Test
fun tappingClusterMarkerOpensBottomSheet() {
    composeTestRule.setContent {
        StopsMapScreen()
    }

    // Wait for map to load
    composeTestRule.waitUntil(5000) {
        composeTestRule.onAllNodesWithTag("cluster_marker").fetchSemanticsNodes().isNotEmpty()
    }

    // Tap first cluster marker
    composeTestRule.onNodeWithTag("cluster_marker_0").performClick()

    // Verify bottom sheet appears
    composeTestRule.onNodeWithText("Stop Cluster").assertIsDisplayed()
}
```

### Emulator Testing (Manual)

**Required Before Merge**:
1. Install debug build: `./gradlew installDebug`
2. Navigate to Stops tab
3. Verify map displays with markers
4. Tap cluster → verify popup opens
5. Scroll stop list → verify smooth scrolling
6. Apply filters → verify map updates
7. Test dark mode → verify theming
8. Enable TalkBack → navigate with accessibility

---

## Common Pitfalls

### ❌ Pitfall 1: Forgetting to Filter cluster_id IS NOT NULL

**Problem**: Querying all stops including noise points (cluster_id == null).

**Solution**: Always filter in SQL:
```kotlin
@Query("SELECT * FROM stops WHERE cluster_id IS NOT NULL")
```

### ❌ Pitfall 2: Calculating Center Without Validation

**Problem**: Empty stop list crashes arithmetic mean calculation.

**Solution**: Validate before calculation:
```kotlin
require(stops.isNotEmpty()) { "Cannot calculate center of empty cluster" }
```

### ❌ Pitfall 3: Not Using LazyColumn for Scrollable Lists

**Problem**: Using regular `Column` for 20+ items causes performance issues.

**Solution**: Use `LazyColumn` with unique keys:
```kotlin
LazyColumn {
    items(items = stops, key = { it.id }) { stop ->
        StopListItem(stop)
    }
}
```

### ❌ Pitfall 4: Forgetting Conditional Composition for ModalBottomSheet

**Problem**: Bottom sheet stays in composition tree even when hidden, wasting memory.

**Solution**: Wrap in `if` block:
```kotlin
if (showBottomSheet) {
    ModalBottomSheet(...) { ... }
}
```

### ❌ Pitfall 5: Hardcoding Marker Colors in UI

**Problem**: Color logic in composable makes it untestable.

**Solution**: Calculate in domain layer (MarkerColor enum), use in UI:
```kotlin
// Domain
enum class MarkerColor(val hue: Float) {
    GREEN(BitmapDescriptorFactory.HUE_GREEN),
    YELLOW(BitmapDescriptorFactory.HUE_YELLOW),
    RED(BitmapDescriptorFactory.HUE_RED)
}

// UI
icon = BitmapDescriptorFactory.defaultMarker(markerData.markerColor.hue)
```

### ❌ Pitfall 6: Not Testing Edge Cases

**Missing Test Cases**:
- Empty cluster list → empty state message
- Single-stop clusters → shouldn't exist (DBSCAN minimum is 2)
- Very large clusters (100+ stops) → scrolling performance
- Filters with no matches → empty state

---

## Debugging Tips

### Issue: Map doesn't display clusters

**Check**:
1. Database has stops with `cluster_id NOT NULL` (Feature 010 must run first)
2. ViewModel `isLoading` transitions to `false`
3. `uiState.clusters` is not empty
4. BikeMap composable renders (check Logcat for map initialization errors)
5. Camera position includes cluster markers (try auto-zoom)

### Issue: Bottom sheet doesn't open on marker tap

**Check**:
1. `onClick` returns `true` (consumes event)
2. `selectedCluster` state updates in ViewModel
3. `showBottomSheet` triggers to `true`
4. Conditional `if (showBottomSheet)` evaluates correctly

### Issue: Stop list scrolling is laggy

**Check**:
1. Using `LazyColumn` (not regular `Column`)
2. Providing unique `key` for each item
3. Caching formatted strings with `remember`
4. Avoiding state reads inside `items { }` lambda

### Issue: Filters don't update map

**Check**:
1. ViewModel `applyFilter()` method is called
2. Repository receives correct filter parameters
3. SQL query includes filter WHERE clauses
4. StateFlow emits new value (triggers recomposition)

---

## Next Steps After Implementation

### Code Review Checklist

Before submitting PR:
- [ ] All unit tests passing (domain + ViewModel)
- [ ] Integration tests passing (repository + DAO)
- [ ] Emulator testing completed and validated
- [ ] Dark mode works correctly
- [ ] TalkBack accessibility tested
- [ ] No memory leaks (Profiler check)
- [ ] TODO.md updated with completion status
- [ ] RELEASE.md updated with feature entry

### Release Process

See `RELEASE.md` for full process:
1. Merge PR to main
2. Bump version to v0.11.0 in build.gradle.kts
3. Update RELEASE.md (move from Unreleased to v0.11.0 section)
4. Create git tag: `git tag -a v0.11.0 -m "Release v0.11.0: Stop Cluster Visualization"`
5. Build signed APK: `./gradlew assembleRelease`
6. Create GitHub Release with APK attachment

---

## Resources

- **Feature Spec**: `/specs/011-cluster-visualization/spec.md`
- **Data Model**: `/specs/011-cluster-visualization/data-model.md`
- **Use Case Contracts**: `/specs/011-cluster-visualization/contracts/use-cases.md`
- **Repository Contracts**: `/specs/011-cluster-visualization/contracts/repository.md`
- **Research Findings**: `/specs/011-cluster-visualization/research.md`
- **Material 3 Bottom Sheets**: https://m3.material.io/components/bottom-sheets
- **Google Maps Compose**: https://github.com/googlemaps/android-maps-compose
- **BikeRedlights CLAUDE.md**: Architecture standards and code review checklist

---

**Estimated Total Implementation Time**: 6-8 hours (including tests)

**Complexity**: Medium (builds on existing patterns from Features 006, 009, 010)
