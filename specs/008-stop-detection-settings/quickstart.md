# Quick Start: Stop Detection Settings Implementation

**Feature**: 008-stop-detection-settings
**Date**: 2025-11-18
**Purpose**: Get developers started with implementing this feature quickly

## 5-Minute Overview

**What**: Add settings UI for configuring stop detection parameters (speed threshold, duration threshold, clustering radius)

**Why**: Prepare infrastructure for Feature 009 (Stop Detection) and Feature 010 (Clustering)

**How**: Extend existing Settings infrastructure with new domain model, repository methods, and UI screen

**Effort**: 16-19 tasks across 7 phases (estimated 1-2 days per roadmap)

---

## Prerequisites

1. **Environment Setup**:
   - Java 17 (OpenJDK) installed and configured
   - Android Studio with Kotlin 2.0.21 plugin
   - Android emulator or physical device (API 26+)

2. **Knowledge Required**:
   - Kotlin data classes and validation
   - Jetpack Compose UI development
   - DataStore Preferences
   - MVVM pattern
   - Kotlin Coroutines and Flow/StateFlow

3. **Existing Code to Familiarize With**:
   - `domain/model/settings/AutoPauseConfig.kt` - Domain model pattern
   - `data/repository/SettingsRepository.kt` - Repository interface
   - `ui/screens/settings/RideTrackingSettingsScreen.kt` - Settings screen pattern
   - `ui/components/settings/SegmentedButtonSetting.kt` - UI control component

---

## Implementation Order

### Phase 1: Domain Layer (2 tasks)

**File**: `app/src/main/java/com/example/bikeredlights/domain/model/settings/StopDetectionConfig.kt`

```kotlin
data class StopDetectionConfig(
    val speedThresholdKmh: Float = 3f,
    val durationThresholdSeconds: Int = 15,
    val clusteringRadiusMeters: Int = 20
) {
    companion object {
        const val DEFAULT_SPEED_THRESHOLD_KMH = 3f
        const val DEFAULT_DURATION_THRESHOLD_SECONDS = 15
        const val DEFAULT_CLUSTERING_RADIUS_METERS = 20

        val VALID_SPEED_THRESHOLDS = listOf(1f, 2f, 3f, 4f, 5f)
        val VALID_DURATION_THRESHOLDS = listOf(5, 10, 15, 20, 25, 30)
        val VALID_CLUSTERING_RADII = listOf(10, 15, 20, 25, 30, 40, 50)
    }

    init {
        require(speedThresholdKmh in VALID_SPEED_THRESHOLDS) { "Invalid speed threshold" }
        require(durationThresholdSeconds in VALID_DURATION_THRESHOLDS) { "Invalid duration threshold" }
        require(clusteringRadiusMeters in VALID_CLUSTERING_RADII) { "Invalid clustering radius" }
    }
}
```

**Test**: `app/src/test/java/com/example/bikeredlights/domain/model/settings/StopDetectionConfigTest.kt`

---

### Phase 2: Data Layer (3 tasks)

**File 1**: `app/src/main/java/com/example/bikeredlights/data/local/datastore/PreferencesKeys.kt`

```kotlin
object PreferencesKeys {
    // ... existing keys ...

    val STOP_DETECTION_SPEED_THRESHOLD_KMH = floatPreferencesKey("stop_detection_speed_threshold_kmh")
    val STOP_DETECTION_DURATION_THRESHOLD_SECONDS = intPreferencesKey("stop_detection_duration_threshold_seconds")
    val STOP_DETECTION_CLUSTERING_RADIUS_METERS = intPreferencesKey("stop_detection_clustering_radius_meters")
}
```

**File 2**: `app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepository.kt`

```kotlin
interface SettingsRepository {
    // ... existing methods ...

    val stopDetectionConfig: Flow<StopDetectionConfig>
    suspend fun setStopDetectionConfig(config: StopDetectionConfig)
}
```

**File 3**: `app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepositoryImpl.kt`

```kotlin
override val stopDetectionConfig: Flow<StopDetectionConfig> = dataStore.data
    .map { prefs ->
        StopDetectionConfig(
            speedThresholdKmh = prefs[STOP_DETECTION_SPEED_THRESHOLD_KMH]
                ?: StopDetectionConfig.DEFAULT_SPEED_THRESHOLD_KMH,
            durationThresholdSeconds = prefs[STOP_DETECTION_DURATION_THRESHOLD_SECONDS]
                ?: StopDetectionConfig.DEFAULT_DURATION_THRESHOLD_SECONDS,
            clusteringRadiusMeters = prefs[STOP_DETECTION_CLUSTERING_RADIUS_METERS]
                ?: StopDetectionConfig.DEFAULT_CLUSTERING_RADIUS_METERS
        )
    }

override suspend fun setStopDetectionConfig(config: StopDetectionConfig) {
    dataStore.edit { prefs ->
        prefs[STOP_DETECTION_SPEED_THRESHOLD_KMH] = config.speedThresholdKmh
        prefs[STOP_DETECTION_DURATION_THRESHOLD_SECONDS] = config.durationThresholdSeconds
        prefs[STOP_DETECTION_CLUSTERING_RADIUS_METERS] = config.clusteringRadiusMeters
    }
}
```

---

### Phase 3: ViewModel Layer (2 tasks)

**File**: `app/src/main/java/com/example/bikeredlights/ui/viewmodel/SettingsViewModel.kt`

```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    // ... existing StateFlows ...

    val stopDetectionConfig = settingsRepository.stopDetectionConfig
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = StopDetectionConfig()
        )

    fun updateStopDetectionConfig(config: StopDetectionConfig) {
        viewModelScope.launch {
            settingsRepository.setStopDetectionConfig(config)
        }
    }
}
```

---

### Phase 4: UI Layer - Settings Home (1 task)

**File**: `app/src/main/java/com/example/bikeredlights/ui/screens/settings/SettingsHomeScreen.kt`

```kotlin
Column(spacing = 16.dp) {
    // Existing Ride & Tracking card
    SettingCard(
        title = "Ride & Tracking",
        subtitle = "Units, GPS, Auto-pause",
        icon = Icons.Default.DirectionsBike,
        onClick = onRideTrackingClick
    )

    // NEW: Stop Detection card
    SettingCard(
        title = "Stop Detection",
        subtitle = "Thresholds, Clustering",
        icon = Icons.Default.Traffic,  // or appropriate icon
        onClick = onStopDetectionClick
    )
}
```

---

### Phase 5: UI Layer - Detail Screen (3-4 tasks)

**File**: `app/src/main/java/com/example/bikeredlights/ui/screens/settings/StopDetectionSettingsScreen.kt`

```kotlin
@Composable
fun StopDetectionSettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel(),
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val stopDetectionConfig by viewModel.stopDetectionConfig.collectAsStateWithLifecycle()
    val unitsSystem by viewModel.unitsSystem.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stop Detection") },
                navigationIcon = { BackButton(onClick = onBackClick) }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding).padding(16.dp), spacing = 24.dp) {
            // Speed Threshold
            SettingSection(
                title = "Speed Threshold",
                description = "Consider stopped when speed drops below:",
                control = {
                    SegmentedButtonSetting(
                        options = if (unitsSystem == UnitsSystem.METRIC)
                            StopDetectionConfig.VALID_SPEED_THRESHOLDS
                        else
                            StopDetectionConfig.VALID_SPEED_THRESHOLDS.map { it * 0.621371f },
                        selectedOption = stopDetectionConfig.speedThresholdKmh,
                        unit = if (unitsSystem == UnitsSystem.METRIC) "km/h" else "mph",
                        onOptionSelected = { newSpeed ->
                            viewModel.updateStopDetectionConfig(
                                stopDetectionConfig.copy(speedThresholdKmh = newSpeed)
                            )
                        }
                    )
                }
            )

            // Duration Threshold
            SettingSection(
                title = "Duration Threshold",
                description = "Minimum time stationary to count as stop:",
                control = {
                    SegmentedButtonSetting(
                        options = StopDetectionConfig.VALID_DURATION_THRESHOLDS,
                        selectedOption = stopDetectionConfig.durationThresholdSeconds,
                        unit = "s",
                        rows = 2,
                        onOptionSelected = { newDuration ->
                            viewModel.updateStopDetectionConfig(
                                stopDetectionConfig.copy(durationThresholdSeconds = newDuration)
                            )
                        }
                    )
                }
            )

            // Clustering Radius
            SettingSection(
                title = "Clustering Radius",
                description = "Group stops within this distance:",
                control = {
                    SegmentedButtonSetting(
                        options = StopDetectionConfig.VALID_CLUSTERING_RADII,
                        selectedOption = stopDetectionConfig.clusteringRadiusMeters,
                        unit = "m",
                        rows = 2,
                        onOptionSelected = { newRadius ->
                            viewModel.updateStopDetectionConfig(
                                stopDetectionConfig.copy(clusteringRadiusMeters = newRadius)
                            )
                        }
                    )
                }
            )
        }
    }
}
```

---

### Phase 6: Navigation (1 task)

**File**: `app/src/main/java/com/example/bikeredlights/ui/navigation/SettingsNavGraph.kt`

```kotlin
composable("stopDetection") {
    StopDetectionSettingsScreen(
        viewModel = hiltViewModel(),
        onBackClick = { navController.popBackStack() }
    )
}
```

---

## Testing Checklist

### Unit Tests
- ✅ StopDetectionConfig default values correct
- ✅ StopDetectionConfig validation rejects invalid values
- ✅ SettingsRepository emits defaults when no keys exist
- ✅ SettingsRepository persists all 3 keys atomically

### Emulator Testing (MANDATORY before merge)
- ✅ Navigate to Settings → Stop Detection
- ✅ Change each setting value
- ✅ Restart app, verify persistence
- ✅ Toggle Imperial units, verify mph conversion
- ✅ Test dark mode rendering
- ✅ Verify back navigation works

---

## Common Pitfalls

1. **Forgetting to read file before editing**: Always use `Read` tool before `Edit`/`Write`
2. **Missing DataStore keys**: All 3 keys must be added to PreferencesKeys.kt
3. **Validation in wrong layer**: Validation belongs in domain model, not UI or repository
4. **Not converting units**: Speed threshold must convert km/h ↔ mph based on unitsSystem
5. **Forgetting to commit frequently**: Commit after each logical unit (domain model, repository, UI)

---

## Quick Commands

```bash
# Build project
./gradlew assembleDebug

# Run unit tests
./gradlew test

# Install on emulator
./gradlew installDebug

# Check for linting errors
./gradlew lint
```

---

## Next Steps After Implementation

1. **Update TODO.md**: Move feature from "In Progress" to "Completed"
2. **Update RELEASE.md**: Add feature entry to "Unreleased" section
3. **Create PR**: Follow PR workflow from CLAUDE.md
4. **After merge**: Version bump to v0.8.0, create release APK, GitHub Release

---

## Questions?

Refer to:
- **spec.md**: Full requirements and acceptance criteria
- **research.md**: Design decisions and rationale
- **data-model.md**: Detailed entity definitions
- **contracts/**: Interface definitions with usage examples
- **CLAUDE.md**: Android development standards
