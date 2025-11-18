# Next Feature Prompt for /speckit.specify

> **Purpose**: Prompt for Feature 008 - Stop Detection Settings (v0.8.0)
> **Created**: 2025-11-18
> **Target Version**: v0.8.0 (MINOR - new settings category)
> **Roadmap Reference**: Feature 2B: Stop Detection Settings

## 🚦 Feature Description

**Feature 008: Stop Detection Settings Infrastructure**

Add a new "Stop Detection" settings card to the Settings tab with configuration for red light detection parameters. This feature prepares the settings infrastructure needed for Feature 009 (actual stop detection implementation).

According to `docs/roadmap.md` lines 1019-1030 and 1406-1409, this feature adds settings for:
1. **Speed Threshold**: Consider stopped when speed drops below this value (1-5 km/h)
2. **Duration Threshold**: Minimum time stationary to count as a stop (5-30s)
3. **Clustering Radius**: Group stops within this distance as same location (10-50m)

## 📋 User Story

**As a cyclist**, I want to configure how the app detects stops at red lights, so I can customize the detection sensitivity to match my riding style and urban environment.

**Specific needs**:
- **Urban commuters**: Short duration threshold (10s) for quick traffic light cycles
- **Suburban riders**: Longer duration threshold (20-30s) to avoid counting brief slow-downs
- **Dense city riders**: Small clustering radius (10-15m) for closely-spaced intersections
- **Suburban riders**: Larger clustering radius (30-50m) for spread-out intersections

## 🎯 Key Requirements

### Functional Requirements

1. **New Settings Card**
   - Add "🚦 Stop Detection" card to Settings home screen
   - Card subtitle: "Thresholds, Clustering" (shows what's inside)
   - Tappable → navigates to Stop Detection detail screen
   - Positioned below "🚴 Ride & Tracking" card

2. **Stop Detection Detail Screen**
   - Three settings with clear labels and descriptions
   - Validation: ensure values are within acceptable ranges
   - Defaults chosen for typical urban cycling

3. **Setting A: Speed Threshold**
   - **Control**: Number picker or segmented buttons
   - **Range**: 1-5 km/h (or mph if Imperial units selected)
   - **Options**: 1, 2, 3, 4, 5 km/h
   - **Default**: 3 km/h (~1.9 mph)
   - **Description**: "Consider stopped when speed drops below this value"
   - **Why 3 km/h**: Typical walking pace, clearly indicates stopped at intersection

4. **Setting B: Duration Threshold**
   - **Control**: Number picker or segmented buttons
   - **Range**: 5s, 10s, 15s, 20s, 25s, 30s
   - **Default**: 15s
   - **Description**: "Minimum time stationary to count as a stop"
   - **Why 15s**: Filters out brief slow-downs, captures typical red light waits

5. **Setting C: Clustering Radius**
   - **Control**: Number picker or segmented buttons
   - **Range**: 10m, 15m, 20m, 25m, 30m, 40m, 50m
   - **Default**: 20m (~65 feet)
   - **Description**: "Group stops within this distance as same location"
   - **Why 20m**: Typical intersection size, accounts for GPS accuracy variations

6. **Data Persistence (DataStore)**
   - Store all three settings in DataStore Preferences
   - Key-value pairs:
     - `stop_detection_speed_threshold_kmh`: 1.0-5.0 (Float)
     - `stop_detection_duration_threshold_seconds`: 5-30 (Int)
     - `stop_detection_clustering_radius_meters`: 10-50 (Int)
   - Persist across app restarts
   - Apply defaults on first launch

7. **Units Integration**
   - Speed threshold displays in km/h or mph based on existing units setting
   - Convert internally: `mph = kmh * 0.621371`
   - Store in DataStore as km/h (canonical format)
   - Display conversion in UI when Imperial selected

### Non-Functional Requirements

1. **Material Design 3 Compliance**
   - Card component with elevation and proper spacing
   - Number pickers or segmented buttons (consistent with existing settings)
   - 48dp minimum touch targets for accessibility
   - Dark mode support
   - Dynamic color scheme

2. **Architecture**
   - Follow existing settings pattern (from Feature 2A v0.2.0)
   - SettingsRepository extension with new methods
   - SettingsViewModel update to expose new StateFlows
   - Reuse SettingCard composable from Feature 2A

3. **Validation**
   - Ensure speed threshold ≥ 1 km/h and ≤ 5 km/h
   - Ensure duration threshold in [5, 10, 15, 20, 25, 30]s
   - Ensure clustering radius in [10, 15, 20, 25, 30, 40, 50]m
   - Prevent invalid values from being stored

## 🔍 Edge Cases to Consider

1. **Imperial Units Conversion**
   - 1 km/h = 0.62 mph, 5 km/h = 3.1 mph
   - Should mph options be rounded (1, 2, 3 mph) or precise (0.6, 1.2, 1.9, 2.5, 3.1)?
   - **Recommendation**: Show precise mph values (e.g., "1.9 mph") to maintain accuracy

2. **Settings Migration**
   - No existing stop detection settings in DataStore
   - First launch: apply defaults silently
   - No migration needed (new feature)

3. **Future Feature Dependency**
   - These settings will be READ by Feature 009 (Stop Detection implementation)
   - Must ensure repository interface is ready for consumption
   - Document expected behavior for future integration

4. **Validation Feedback**
   - If user somehow enters invalid value (edge case), show error message
   - Reset to default on validation failure
   - Log warning for debugging

## 🏗️ Suggested Architecture

### Domain Layer Changes

**New Models**:
```kotlin
// domain/model/settings/StopDetectionConfig.kt
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
        require(speedThresholdKmh in 1f..5f) { "Speed threshold must be 1-5 km/h" }
        require(durationThresholdSeconds in VALID_DURATION_THRESHOLDS) { "Invalid duration threshold" }
        require(clusteringRadiusMeters in VALID_CLUSTERING_RADII) { "Invalid clustering radius" }
    }
}
```

### Data Layer Changes

**SettingsRepository Extension**:
```kotlin
interface SettingsRepository {
    // Existing methods...

    // NEW: Stop detection settings
    suspend fun saveStopDetectionConfig(config: StopDetectionConfig)
    fun getStopDetectionConfig(): Flow<StopDetectionConfig>
}
```

**DataStore Keys** (in `PreferencesKeys.kt`):
```kotlin
val STOP_DETECTION_SPEED_THRESHOLD_KMH = floatPreferencesKey("stop_detection_speed_threshold_kmh")
val STOP_DETECTION_DURATION_THRESHOLD_SECONDS = intPreferencesKey("stop_detection_duration_threshold_seconds")
val STOP_DETECTION_CLUSTERING_RADIUS_METERS = intPreferencesKey("stop_detection_clustering_radius_meters")
```

### UI Layer Changes

**SettingsHomeScreen**:
- Add new "Stop Detection" card after "Ride & Tracking" card
- Use existing SettingCard composable (reuse from Feature 2A)

**New Screen: StopDetectionSettingsScreen**:
- Three settings with number pickers or segmented buttons
- Validation and error handling
- Units conversion for speed threshold

**ViewModel Update**:
```kotlin
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    // Existing StateFlows...

    // NEW: Stop detection config
    val stopDetectionConfig = settingsRepository.getStopDetectionConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), StopDetectionConfig())

    fun updateStopDetectionConfig(config: StopDetectionConfig) {
        viewModelScope.launch {
            settingsRepository.saveStopDetectionConfig(config)
        }
    }
}
```

## 🎨 UI/UX Mockup

**Settings Home Screen (Updated)**:
```
⚙️ Settings

┌────────────────────────────────┐
│ 🚴 Ride & Tracking             │
│ Units, GPS, Auto-pause         │
│                             >  │
└────────────────────────────────┘

┌────────────────────────────────┐  ← NEW
│ 🚦 Stop Detection              │  ← NEW
│ Thresholds, Clustering         │  ← NEW
│                             >  │  ← NEW
└────────────────────────────────┘  ← NEW

(Future: About, Privacy, etc.)
```

**Stop Detection Settings Screen (NEW)**:
```
← Back    Stop Detection

Speed Threshold
Consider stopped when speed drops below:
┌────────────────────────────────┐
│ [1] [2] [3] [4] [5] km/h       │  ← Segmented buttons (3 selected)
└────────────────────────────────┘
OR
┌────────────────────────────────┐
│ Speed Threshold     [3 km/h ▼] │  ← Number picker
└────────────────────────────────┘

Duration Threshold
Minimum time stationary to count as stop:
┌────────────────────────────────┐
│ [5s] [10s] [15s] [20s] [25s] [30s] │  ← Segmented buttons (15s selected)
│ (Selected: 15s)                 │
└────────────────────────────────┘

Clustering Radius
Group stops within this distance:
┌────────────────────────────────┐
│ [10m] [15m] [20m] [25m]        │  ← Segmented buttons (20m selected)
│ [30m] [40m] [50m]              │  ← Second row
└────────────────────────────────┘
```

**With Imperial Units (mph)**:
```
Speed Threshold
Consider stopped when speed drops below:
┌────────────────────────────────┐
│ [0.6] [1.2] [1.9] [2.5] [3.1] mph │  ← Converted from km/h
└────────────────────────────────┘
```

## 🚀 Implementation Phases

### Phase 1: Domain Layer (2 tasks)
- Create StopDetectionConfig domain model with validation
- Add validation constants and init block

### Phase 2: Data Layer (3 tasks)
- Add DataStore keys to PreferencesKeys.kt
- Extend SettingsRepository interface with new methods
- Implement new methods in SettingsRepositoryImpl (save/load)

### Phase 3: ViewModel Layer (2 tasks)
- Add stopDetectionConfig StateFlow to SettingsViewModel
- Add updateStopDetectionConfig() method
- Wire to repository with Flow transformation

### Phase 4: UI Layer - Settings Home (1 task)
- Add "Stop Detection" card to SettingsHomeScreen
- Reuse SettingCard composable from Feature 2A
- Wire navigation to detail screen

### Phase 5: UI Layer - Detail Screen (3-4 tasks)
- Create StopDetectionSettingsScreen composable
- Implement speed threshold picker/segmented buttons with units conversion
- Implement duration threshold picker/segmented buttons
- Implement clustering radius picker/segmented buttons
- Wire to ViewModel StateFlow and update methods

### Phase 6: Testing & Validation (3-4 tasks)
- Unit tests for StopDetectionConfig validation
- Unit tests for SettingsRepository save/load
- ViewModel tests for StateFlow emissions
- Emulator testing (all three settings, units conversion, persistence)

### Phase 7: Documentation & Release (2 tasks)
- Update TODO.md and RELEASE.md
- Create PR, code review, merge
- Version bump to v0.8.0
- Build release APK and create GitHub Release

**Estimated Total Tasks**: 16-19 tasks

## 📚 References

### Existing Code to Review
- `SettingsHomeScreen.kt` - Add new card here
- `SettingsViewModel.kt` - Extend with new StateFlow
- `SettingsRepository.kt` + `SettingsRepositoryImpl.kt` - Add new methods
- `PreferencesKeys.kt` - Add new DataStore keys
- `RideTrackingSettingsScreen.kt` - Template for detail screen layout
- `SegmentedButtonSetting.kt` - Reusable component for segmented buttons

### Related Features
- **Feature 2A (v0.2.0)**: Basic Settings Infrastructure - template for this feature
- **Feature 009 (Future)**: Stop Detection implementation - will READ these settings
- **Feature 010 (Future)**: Stop Clustering - will READ clustering radius

## 🎯 Success Criteria

1. ✅ "Stop Detection" card appears on Settings home screen
2. ✅ Tapping card navigates to Stop Detection detail screen
3. ✅ All three settings are configurable with proper controls
4. ✅ Default values applied on first launch (3 km/h, 15s, 20m)
5. ✅ Settings persist across app restarts
6. ✅ Speed threshold converts correctly to mph when Imperial units selected
7. ✅ Validation prevents invalid values from being stored
8. ✅ Material 3 design with dark mode support
9. ✅ Emulator testing validates all settings and persistence
10. ✅ No crashes, no errors in logcat
11. ✅ Repository interface ready for Feature 009 consumption

## 🔒 Safety Considerations

**This feature prepares infrastructure for safety-critical stop detection** - settings must be:
1. **Reliable**: Always persist correctly, no data loss
2. **Validated**: Prevent out-of-range values that could cause detection failures
3. **Accessible**: Clear labels and descriptions for user understanding
4. **Reversible**: Users can reset to defaults if needed

**No actual stop detection occurs in this feature** - it only prepares the settings. Feature 009 will implement the actual detection logic.

## 🎓 Learning Opportunities

This feature provides experience with:
- Extending existing settings infrastructure (building on Feature 2A)
- DataStore Preferences for new key-value pairs
- Settings validation and error handling
- Units conversion (km/h ↔ mph)
- Reusable UI components (SettingCard, SegmentedButtonSetting)
- Material 3 settings patterns

## 📝 Prompt for /speckit.specify

**Paste this into Claude Code**:

```
/speckit.specify

Feature 008: Stop Detection Settings (Roadmap Feature 2B)

Add a new "Stop Detection" settings card and detail screen to configure red light detection parameters. This prepares the settings infrastructure for Feature 009 (actual stop detection implementation).

**User Story**: As a cyclist, I want to configure how the app detects stops at red lights (speed threshold, duration threshold, clustering radius), so I can customize detection sensitivity to match my riding style and urban environment.

**Three Settings**:
1. **Speed Threshold**: 1-5 km/h (default 3 km/h) - "Consider stopped when speed drops below this value"
2. **Duration Threshold**: 5s, 10s, 15s, 20s, 25s, 30s (default 15s) - "Minimum time stationary to count as a stop"
3. **Clustering Radius**: 10m, 15m, 20m, 25m, 30m, 40m, 50m (default 20m) - "Group stops within this distance as same location"

**Key Requirements**:
1. Add "🚦 Stop Detection" card to Settings home screen
2. Create Stop Detection detail screen with three configurable settings
3. Use number pickers or segmented buttons (consistent with Feature 2A patterns)
4. Persist settings in DataStore Preferences
5. Validate all values are within acceptable ranges
6. Convert speed threshold to mph when Imperial units selected
7. Apply defaults on first launch (3 km/h, 15s, 20m)

**Architecture**:
- Domain: Create StopDetectionConfig model with validation
- Data: Extend SettingsRepository with save/load methods, add DataStore keys
- ViewModel: Add stopDetectionConfig StateFlow to SettingsViewModel
- UI: Add card to SettingsHomeScreen, create StopDetectionSettingsScreen

**Build On**:
- Reuse SettingCard composable from Feature 2A (v0.2.0)
- Reuse SegmentedButtonSetting if applicable
- Follow existing settings patterns (RideTrackingSettingsScreen as template)
- Extend existing SettingsRepository and SettingsViewModel

**Testing**:
- MANDATORY emulator testing (all settings, persistence, units conversion)
- Validate settings survive app restart
- Test Imperial units conversion for speed threshold
- Verify defaults applied correctly on first launch

**Prepares For**: Feature 009 (Stop Detection implementation) which will READ these settings to detect stops at red lights.

**Target Release**: v0.8.0 (MINOR - new settings category)
**Estimated Tasks**: 16-19 tasks across 7 phases
**Roadmap Reference**: Feature 2B (docs/roadmap.md line 1406-1409)
```

---

## 🎉 Why This is the Right Next Feature (Per Roadmap)

1. **Roadmap Alignment**: Explicitly listed as Feature 2B in `docs/roadmap.md`
2. **Logical Progression**: Prepares settings for upcoming stop detection (Feature 009/F4)
3. **Low Complexity**: 1-2 days according to roadmap (16-19 tasks estimated)
4. **Builds on Foundation**: Extends Feature 2A settings infrastructure (already implemented)
5. **Enables Future Features**: Required dependency for Features 009 (Stop Detection) and 010 (Clustering)
6. **No External Dependencies**: Uses existing DataStore, no Google Maps needed

---

**Last Updated**: 2025-11-18
**Status**: Ready for /speckit.specify
**Recommended Priority**: P1 (Roadmap Feature)
**Roadmap Phase**: Phase 3 (Red Light Detection System preparation)
