# Research & Design Decisions: Stop Detection Settings

**Feature**: Stop Detection Settings (008-stop-detection-settings)
**Date**: 2025-11-18
**Phase**: 0 - Research & Analysis

## Overview

This document captures research findings and design decisions for implementing Stop Detection Settings. The feature extends existing settings infrastructure to add three configurable parameters that will be consumed by future stop detection (Feature 009) and clustering (Feature 010) features.

## Technical Context Research

### 1. Existing Settings Infrastructure (Feature 002/v0.2.0)

**Research**: Analyzed existing settings implementation to ensure consistency and reusability.

**Findings**:
- **SettingsRepository** pattern established with Flow-based reads and suspend-based writes
- **SettingsViewModel** manages UI state with StateFlow emissions
- **SettingCard** composable provides consistent card UI
- **SegmentedButtonSetting** and **SwitchSetting** composables available for controls
- **DataStore Preferences** used for persistence (not SharedPreferences)
- Navigation handled via **SettingsNavGraph** with typed routes

**Decision**: Extend existing infrastructure rather than creating parallel patterns.

**Rationale**: Maintains consistency, reduces code duplication, leverages tested components.

**Alternatives Considered**:
- ❌ **Create separate StopDetectionRepository**: Rejected - settings should be centralized in one repository
- ❌ **Use Room database for settings**: Rejected - DataStore is appropriate for simple key-value pairs
- ❌ **Create new UI components**: Rejected - existing components handle all required patterns

**Reference Files**:
- `app/src/main/java/com/example/bikeredlights/data/repository/SettingsRepository.kt` (lines 18-79)
- `app/src/main/java/com/example/bikeredlights/ui/screens/settings/SettingsHomeScreen.kt` (lines 38-82)

---

### 2. Domain Model Design Pattern

**Research**: Reviewed existing domain models to determine validation approach.

**Findings**:
- **AutoPauseConfig** (lines 10-35 in domain/model/settings/AutoPauseConfig.kt) uses:
  - Data class with validation in `init` block
  - Companion object with constants for valid ranges
  - `require()` statements for validation with descriptive error messages
  - Default values in constructor parameters

**Decision**: Follow AutoPauseConfig pattern for StopDetectionConfig.

**Rationale**: Proven pattern, validation at domain layer (Pure Kotlin), type-safe defaults.

**StopDetectionConfig Structure**:
```kotlin
data class StopDetectionConfig(
    val speedThresholdKmh: Float = DEFAULT_SPEED_THRESHOLD_KMH,
    val durationThresholdSeconds: Int = DEFAULT_DURATION_THRESHOLD_SECONDS,
    val clusteringRadiusMeters: Int = DEFAULT_CLUSTERING_RADIUS_METERS
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
        require(speedThresholdKmh in VALID_SPEED_THRESHOLDS) {
            "Speed threshold must be one of $VALID_SPEED_THRESHOLDS km/h"
        }
        require(durationThresholdSeconds in VALID_DURATION_THRESHOLDS) {
            "Duration threshold must be one of $VALID_DURATION_THRESHOLDS seconds"
        }
        require(clusteringRadiusMeters in VALID_CLUSTERING_RADII) {
            "Clustering radius must be one of $VALID_CLUSTERING_RADII meters"
        }
    }
}
```

**Alternatives Considered**:
- ❌ **Validation in ViewModel**: Rejected - violates Clean Architecture (domain logic should be in domain layer)
- ❌ **Validation in Repository**: Rejected - repository is for persistence, not business rules
- ❌ **Range validation only (1-5, 5-30, 10-50)**: Rejected - spec requires discrete values, not ranges

---

### 3. DataStore Keys Naming Convention

**Research**: Analyzed existing PreferencesKeys.kt to determine naming pattern.

**Findings**:
- Snake_case naming: `units_system`, `gps_accuracy`, `auto_pause_enabled`
- Type-specific key functions: `stringPreferencesKey`, `booleanPreferencesKey`, `intPreferencesKey`
- Keys match domain model field names (with underscores)

**Decision**: Use consistent naming pattern for new keys.

**DataStore Keys**:
```kotlin
val STOP_DETECTION_SPEED_THRESHOLD_KMH = floatPreferencesKey("stop_detection_speed_threshold_kmh")
val STOP_DETECTION_DURATION_THRESHOLD_SECONDS = intPreferencesKey("stop_detection_duration_threshold_seconds")
val STOP_DETECTION_CLUSTERING_RADIUS_METERS = intPreferencesKey("stop_detection_clustering_radius_meters")
```

**Rationale**: Maintains consistency with existing keys, descriptive names prevent collisions.

**Alternatives Considered**:
- ❌ **Shorter keys (e.g., `stop_speed`)**: Rejected - clarity preferred over brevic brevity
- ❌ **Grouped key prefix (e.g., `settings.stop_detection.speed`)**: Rejected - DataStore doesn't support hierarchical keys
- ❌ **Camel case**: Rejected - inconsistent with existing convention

---

### 4. UI Control Selection

**Research**: Evaluated control types for three settings based on existing patterns and Material 3 guidelines.

**Findings**:
- **SegmentedButtonSetting** exists for discrete choices (auto-pause timing uses this)
- **Spinner/Dropdown** not used in current UI (not Material 3 Expressive pattern)
- **Slider** not appropriate for discrete values
- **NumberPicker** (Android built-in) exists but requires XML integration

**Decision**: Use **SegmentedButtonSetting** for all three settings.

**Rationale**:
- Existing composable, consistent UI
- Works well for 5-7 discrete options
- Visible choices (no hidden dropdown)
- Material 3 compliant
- Accessibility-friendly (large touch targets)

**Layout Strategy**:
- **Speed Threshold**: Single row, 5 buttons (1, 2, 3, 4, 5 km/h)
- **Duration Threshold**: Two rows, 3 buttons each (5s, 10s, 15s | 20s, 25s, 30s)
- **Clustering Radius**: Two rows (10m, 15m, 20m, 25m | 30m, 40m, 50m)

**Alternatives Considered**:
- ❌ **Dropdown/Spinner**: Rejected - not used elsewhere, requires extra tap to view options
- ❌ **Slider with snapping**: Rejected - discrete values better shown as buttons
- ❌ **Number input field**: Rejected - prone to validation errors, poor UX for predefined choices

**Reference**: See RideTrackingSettingsScreen.kt (auto-pause timing) for segmented button example.

---

### 5. Imperial/Metric Unit Conversion

**Research**: Analyzed existing unit conversion patterns.

**Findings**:
- **UnitsSystem** enum has METRIC and IMPERIAL values
- **RideRecordingViewModel** has conversion utilities:
  - `convertSpeed(m/s, unitsSystem)` → km/h or mph
  - `convertDistance(meters, unitsSystem)` → km or miles
  - `getSpeedUnit(unitsSystem)` → "km/h" or "mph"
- Storage is always in metric (canonical format)
- Conversion happens in UI layer only

**Decision**: Store speed threshold in km/h, convert to mph for display when Imperial units selected.

**Rationale**: Consistent with existing patterns, avoids dual storage, single source of truth.

**Conversion Formula**: `mph = kmh * 0.621371`

**Display Values (Imperial)**:
- 1 km/h → 0.6 mph
- 2 km/h → 1.2 mph
- 3 km/h → 1.9 mph (default)
- 4 km/h → 2.5 mph
- 5 km/h → 3.1 mph

**Precision**: Display to 1 decimal place for clarity (Success Criterion SC-003: ±0.1 mph precision).

**Alternatives Considered**:
- ❌ **Store in both km/h and mph**: Rejected - dual storage invites sync bugs
- ❌ **Store in mph when Imperial selected**: Rejected - complicates logic, inconsistent
- ❌ **Round mph values**: Rejected - loses precision, spec requires accurate conversion

---

### 6. Navigation Integration

**Research**: Reviewed existing navigation setup.

**Findings**:
- **SettingsNavGraph** manages navigation between SettingsHomeScreen and detail screens
- Routes are string-based (e.g., "rideTracking")
- Navigation uses `navController.navigate(route)` pattern
- Back navigation handled automatically by Scaffold

**Decision**: Add "stopDetection" route to SettingsNavGraph.

**Navigation Flow**:
1. User taps "Stop Detection" card on SettingsHomeScreen
2. `onStopDetectionClick()` callback invoked
3. NavController navigates to "stopDetection" route
4. StopDetectionSettingsScreen composable displayed
5. Back button returns to SettingsHomeScreen

**Route Definition**:
```kotlin
// In SettingsNavGraph.kt
composable("stopDetection") {
    StopDetectionSettingsScreen(
        viewModel = hiltViewModel(),
        onBackClick = { navController.popBackStack() }
    )
}
```

**Alternatives Considered**:
- ❌ **Type-safe navigation (Navigation Compose 2.8+)**: Rejected - would require refactoring existing navigation
- ❌ **Deep linking**: Rejected - settings are not deep-linkable in this app
- ❌ **Bottom sheet instead of new screen**: Rejected - too much content for bottom sheet

---

### 7. Testing Strategy

**Research**: Reviewed project testing patterns and requirements from CLAUDE.md.

**Findings**:
- **Unit tests** required for domain models (JUnit 5)
- **Emulator testing** mandatory before merge (CLAUDE.md line 137-196)
- **Compose UI tests** used for screen validation
- No integration tests for settings (DataStore is local-only)

**Decision**: Three-tier testing approach.

**Testing Plan**:

**Tier 1 - Unit Tests** (StopDetectionConfigTest.kt):
```kotlin
@Test
fun `default values are correct`() {
    val config = StopDetectionConfig()
    assertEquals(3f, config.speedThresholdKmh)
    assertEquals(15, config.durationThresholdSeconds)
    assertEquals(20, config.clusteringRadiusMeters)
}

@Test
fun `validation rejects invalid speed threshold`() {
    assertThrows<IllegalArgumentException> {
        StopDetectionConfig(speedThresholdKmh = 0f)
    }
}

@Test
fun `validation rejects invalid duration threshold`() {
    assertThrows<IllegalArgumentException> {
        StopDetectionConfig(durationThresholdSeconds = 7)
    }
}

@Test
fun `validation rejects invalid clustering radius`() {
    assertThrows<IllegalArgumentException> {
        StopDetectionConfig(clusteringRadiusMeters = 5)
    }
}
```

**Tier 2 - Compose UI Tests**:
- Verify Stop Detection card appears on Settings home
- Verify tapping card navigates to detail screen
- Verify all three settings display correctly
- Verify default values shown on first launch

**Tier 3 - Emulator Testing** (Manual QA):
- Install debug APK on emulator
- Navigate to Settings → Stop Detection
- Change each setting value
- Restart app, verify persistence
- Toggle Imperial units, verify mph conversion
- Test dark mode rendering

**Alternatives Considered**:
- ❌ **Skip unit tests**: Rejected - CLAUDE.md requires 80%+ coverage for domain models
- ❌ **Skip emulator testing**: Rejected - mandatory per CLAUDE.md line 155
- ❌ **Add integration tests**: Rejected - not needed for local-only settings

---

## Design Decisions Summary

| Decision Area | Choice | Rationale |
|--------------|--------|-----------|
| Repository Pattern | Extend SettingsRepository | Consistency with Feature 002 |
| Domain Model | StopDetectionConfig data class with validation | Follows AutoPauseConfig pattern |
| Storage | DataStore Preferences | Existing infrastructure, appropriate for key-value pairs |
| UI Controls | SegmentedButtonSetting (all 3 settings) | Consistent, Material 3 compliant, visible options |
| Unit Conversion | Store km/h, convert to mph for display | Single source of truth, matches existing patterns |
| Navigation | String route "stopDetection" | Consistent with existing SettingsNavGraph |
| Default Values | 3 km/h, 15s, 20m | Per roadmap spec (urban cycling optimized) |
| Testing | Unit tests + Compose UI tests + Emulator | Meets CLAUDE.md requirements |

---

## Open Questions (Resolved)

All technical questions resolved during research phase. No clarifications needed.

---

## References

**Existing Code Analyzed**:
- `SettingsRepository.kt` - Repository interface pattern
- `SettingsRepositoryImpl.kt` - DataStore implementation
- `AutoPauseConfig.kt` - Domain model validation pattern
- `SettingsHomeScreen.kt` - Card UI pattern
- `RideTrackingSettingsScreen.kt` - Detail screen pattern
- `SegmentedButtonSetting.kt` - UI control component
- `PreferencesKeys.kt` - DataStore key naming
- `SettingsViewModel.kt` - StateFlow emission pattern
- `SettingsNavGraph.kt` - Navigation routing

**Documentation**:
- `docs/roadmap.md` (lines 1019-1030, 1406-1409) - Feature requirements
- `CLAUDE.md` - Android development standards
- `NEXT_FEATURE_PROMPT.md` - Detailed feature specification

**External Resources**:
- Material Design 3 Expressive - Segmented buttons best practices
- Jetpack DataStore - Preferences API documentation
- Kotlin data classes - Validation in init blocks

---

**Research Complete**: All design decisions made. Ready to proceed to Phase 1 (data-model.md and contracts/).
