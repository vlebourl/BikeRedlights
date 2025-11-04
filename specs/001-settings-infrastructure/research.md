# Research: Basic Settings Infrastructure

**Feature**: 001-settings-infrastructure
**Phase**: 0 (Research & Technology Decisions)
**Date**: 2025-11-04

## Overview

This document captures technology decisions, patterns, and best practices for implementing the Basic Settings Infrastructure feature. All decisions are backed by official documentation, established patterns from v0.1.0, or Android best practices.

---

## 1. Material 3 SegmentedButton for Binary Choices

### Decision
Use `androidx.compose.material3.SingleChoiceSegmentedButtonRow` for Units and GPS Accuracy settings (2-option mutually exclusive selection).

### Rationale
- **Material 3 Guideline**: Official recommendation for 2-5 mutually exclusive options
- **Glanceable**: Both options visible simultaneously (no dropdown or dialog needed)
- **Bike-Friendly**: Large touch targets (minimum 48dp height per Material 3 spec)
- **Visual Clarity**: Selected state is obvious with filled background color
- **Native Component**: Jetpack Compose Material 3 library provides built-in support

### Alternatives Considered
- **RadioButton + Text**: More traditional but requires more vertical space, less modern
- **Switch**: Only suitable for boolean on/off, not for mutually exclusive labeled choices
- **Dropdown Menu**: Requires extra tap to see options, poor UX for binary choices
- **Custom Tabs**: Reinventing Material 3 component, unnecessary complexity

### Code Example

```kotlin
import androidx.compose.material3.*
import androidx.compose.runtime.*

@Composable
fun UnitsSetting(
    selectedUnits: UnitsSystem,
    onUnitsChange: (UnitsSystem) -> Unit
) {
    Column {
        Text("Units", style = MaterialTheme.typography.titleMedium)
        Spacer(modifier = Modifier.height(8.dp))

        SingleChoiceSegmentedButtonRow {
            SegmentedButton(
                selected = selectedUnits == UnitsSystem.METRIC,
                onClick = { onUnitsChange(UnitsSystem.METRIC) },
                shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2)
            ) {
                Text("Metric")
            }
            SegmentedButton(
                selected = selectedUnits == UnitsSystem.IMPERIAL,
                onClick = { onUnitsChange(UnitsSystem.IMPERIAL) },
                shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2)
            ) {
                Text("Imperial")
            }
        }
    }
}
```

### References
- [Material 3 Segmented Buttons](https://m3.material.io/components/segmented-buttons/overview)
- [Compose Material 3 SegmentedButton API](https://developer.android.com/reference/kotlin/androidx/compose/material3/package-summary#SingleChoiceSegmentedButtonRow(androidx.compose.ui.Modifier,androidx.compose.ui.graphics.Shape,androidx.compose.material3.SegmentedButtonColors,androidx.compose.foundation.BorderStroke,kotlin.Function1))

---

## 2. DataStore Preferences Error Handling

### Decision
Implement try-catch with graceful fallback to default values on read failures. Log errors but don't crash app. Write failures logged but not retried automatically.

### Rationale
- **Resilience**: App remains functional even if DataStore is corrupted or inaccessible
- **User Experience**: No crashes from storage issues; defaults always available
- **Android Guideline**: DataStore documentation recommends catch-and-default pattern
- **Logging**: Errors logged for debugging, but user experience not interrupted

### Alternatives Considered
- **Retry on Failure**: Adds complexity, may loop infinitely if device storage is full
- **User-Facing Error**: Confusing and unhelpful ("Settings failed to load" doesn't help user)
- **Crash on Error**: Violates Android best practice of graceful degradation

### Code Example

```kotlin
class SettingsRepository(
    private val dataStore: DataStore<Preferences>,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private val tag = "SettingsRepository"

    // Read with graceful fallback
    val unitsSystem: Flow<UnitsSystem> = dataStore.data
        .catch { exception ->
            Log.e(tag, "Error reading units_system preference", exception)
            emit(emptyPreferences()) // Fallback to empty prefs → default value
        }
        .map { preferences ->
            val value = preferences[PreferencesKeys.UNITS_SYSTEM] ?: "metric"
            when (value) {
                "imperial" -> UnitsSystem.IMPERIAL
                else -> UnitsSystem.METRIC // Default
            }
        }

    // Write with error logging (no retry)
    suspend fun setUnitsSystem(units: UnitsSystem) {
        try {
            dataStore.edit { preferences ->
                preferences[PreferencesKeys.UNITS_SYSTEM] = when (units) {
                    UnitsSystem.METRIC -> "metric"
                    UnitsSystem.IMPERIAL -> "imperial"
                }
            }
        } catch (exception: Exception) {
            Log.e(tag, "Error writing units_system preference", exception)
            // Don't rethrow - log and continue
        }
    }
}
```

### References
- [DataStore Error Handling](https://developer.android.com/topic/libraries/architecture/datastore#handle-exceptions)
- [Kotlin Flow catch operator](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/catch.html)

---

## 3. Compose UI Testing for Settings Persistence

### Decision
Use `@get:Rule val composeTestRule = createAndroidComposeRule<MainActivity>()` for UI tests. Use `ActivityScenario.launch` for instrumented persistence tests that simulate app restart.

### Rationale
- **Official Pattern**: Jetpack Compose testing guide recommends createAndroidComposeRule for activity-based tests
- **Real Device Context**: Instrumented tests on emulator verify actual DataStore behavior
- **App Restart Simulation**: ActivityScenario allows recreating activity to test persistence
- **Integration Testing**: Tests entire flow from UI → ViewModel → Repository → DataStore

### Alternatives Considered
- **Unit Tests Only**: Can't verify DataStore persistence without instrumented tests
- **Espresso**: Legacy UI testing framework, Compose has better test APIs
- **Manual Testing Only**: Not repeatable, doesn't scale, Constitution requires automated tests

### Code Example

```kotlin
@RunWith(AndroidJUnit4::class)
class SettingsPersistenceTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun unitsSettingPersistsAcrossAppRestart() {
        // Step 1: Navigate to settings and change to Imperial
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Ride & Tracking").performClick()
        composeTestRule.onNodeWithText("Imperial").performClick()

        // Step 2: Verify change applied
        composeTestRule.onNodeWithText("Imperial").assertIsSelected()

        // Step 3: Simulate app restart by recreating activity
        composeTestRule.activityRule.scenario.recreate()

        // Step 4: Navigate back to settings
        composeTestRule.onNodeWithText("Settings").performClick()
        composeTestRule.onNodeWithText("Ride & Tracking").performClick()

        // Step 5: Verify Imperial is still selected after restart
        composeTestRule.onNodeWithText("Imperial").assertIsSelected()
    }
}
```

### References
- [Compose Testing Guide](https://developer.android.com/jetpack/compose/testing)
- [Testing State Restoration](https://developer.android.com/topic/libraries/architecture/saving-states#test-state-restoration)

---

## 4. Bottom Navigation Integration Pattern

### Decision
Create `BottomNavDestination` enum and use `Scaffold` with `NavigationBar` in MainActivity. Use `NavHost` for screen navigation. Settings tab is 3rd position (after Live and Rides).

### Rationale
- **Material 3 Pattern**: Scaffold + NavigationBar is the official Material 3 pattern
- **Type Safety**: Enum-based destinations prevent route typos
- **Scalability**: Easy to add 4th tab (Stops) in Feature 6
- **Existing Pattern**: Matches v0.1.0 architecture (single activity, multiple composable screens)

### Alternatives Considered
- **Multiple Activities**: Outdated Android pattern, adds complexity, slow transitions
- **Drawer Navigation**: Not suitable for top-level destinations, requires extra tap
- **Tab Row**: Material 2 pattern, NavigationBar is Material 3 replacement

### Code Example

```kotlin
enum class BottomNavDestination(
    val route: String,
    val icon: ImageVector,
    val label: String
) {
    LIVE("live", Icons.Default.DirectionsBike, "Live"),
    RIDES("rides", Icons.Default.Analytics, "Rides"),
    SETTINGS("settings", Icons.Default.Settings, "Settings")
}

@Composable
fun MainScreen() {
    var selectedDestination by remember { mutableStateOf(BottomNavDestination.LIVE) }
    val navController = rememberNavController()

    Scaffold(
        bottomBar = {
            NavigationBar {
                BottomNavDestination.values().forEach { destination ->
                    NavigationBarItem(
                        selected = selectedDestination == destination,
                        onClick = {
                            selectedDestination = destination
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.startDestinationId)
                                launchSingleTop = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = null) },
                        label = { Text(destination.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = BottomNavDestination.LIVE.route,
            modifier = Modifier.padding(paddingValues)
        ) {
            composable(BottomNavDestination.LIVE.route) { LiveScreen() }
            composable(BottomNavDestination.RIDES.route) { RidesScreen() }
            composable(BottomNavDestination.SETTINGS.route) { SettingsHomeScreen() }
        }
    }
}
```

### References
- [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- [Material 3 Navigation Bar](https://m3.material.io/components/navigation-bar/overview)

---

## 5. Auto-Pause Logic Implementation Pattern

### Decision
Implement timer-based state tracking in TrackLocationUseCase. Track consecutive seconds below 1 km/h threshold. Emit "paused" state to ViewModel when threshold reached. Auto-resume when speed exceeds 1 km/h.

### Rationale
- **Domain Layer Logic**: Auto-pause is business logic, belongs in use case not UI
- **State Machine**: Clear states (recording, paused) prevent bugs
- **Configurable**: Threshold read from SettingsRepository, easy to change
- **Testable**: Use case can be unit tested with fake location data

### Alternatives Considered
- **ViewModel Timer**: Mixes UI state with business logic, harder to test
- **Foreground Service Timer**: Over-engineering, use case is simpler
- **Manual Start/Stop Only**: Defers to user, but loses auto-pause feature value

### Code Example

```kotlin
class TrackLocationUseCase(
    private val locationRepository: LocationRepository,
    private val settingsRepository: SettingsRepository
) {
    data class RideState(
        val isRecording: Boolean,
        val isPaused: Boolean,
        val location: LocationData?,
        val speedKmh: Float
    )

    fun trackRide(): Flow<RideState> = combine(
        locationRepository.getLocationUpdates(),
        settingsRepository.autoPauseConfig
    ) { location, autoPauseConfig ->
        // Auto-pause detection logic
        val speedKmh = location.speed * 3.6f // m/s to km/h
        val isStationary = speedKmh < 1.0f

        // Track consecutive stationary seconds
        var stationarySeconds = 0
        if (isStationary) {
            stationarySeconds++
        } else {
            stationarySeconds = 0 // Reset on movement
        }

        // Check if should pause
        val shouldPause = autoPauseConfig.enabled &&
                          isStationary &&
                          stationarySeconds >= (autoPauseConfig.thresholdMinutes * 60)

        RideState(
            isRecording = true,
            isPaused = shouldPause,
            location = location,
            speedKmh = speedKmh
        )
    }
}
```

**Note**: Full implementation will use `StateFlow` to track stationary duration across emissions. Example above is simplified for illustration.

### References
- [Clean Architecture Use Cases](https://developer.android.com/topic/architecture/domain-layer#use-cases)
- [Kotlin Flow combine](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/combine.html)

---

## 6. Unit Conversion Utilities

### Decision
Create standalone utility object with pure functions for km/h ↔ mph and km ↔ miles conversions. Use standard conversion factors: 0.621371 (km to miles), 1.60934 (miles to km).

### Rationale
- **Stateless**: Pure functions, easy to test, no side effects
- **Reusable**: Used by Settings, Live screen, Ride History, Review screen
- **Domain Layer**: No Android dependencies, can be tested with plain JUnit
- **Accuracy**: Standard conversion factors, well-documented

### Alternatives Considered
- **Extension Functions**: Less discoverable, harder to find all conversions
- **ViewModel Logic**: Duplication across multiple ViewModels, not DRY
- **Third-Party Library**: Overkill for two simple conversions

### Code Example

```kotlin
object UnitConversions {
    // Conversion factors
    private const val KM_TO_MILES = 0.621371
    private const val MILES_TO_KM = 1.60934
    private const val KMH_TO_MPH = 0.621371
    private const val MPH_TO_KMH = 1.60934

    // Speed conversions
    fun kmhToMph(kmh: Float): Float = kmh * KMH_TO_MPH.toFloat()
    fun mphToKmh(mph: Float): Float = mph * MPH_TO_KMH.toFloat()

    // Distance conversions
    fun kmToMiles(km: Float): Float = km * KM_TO_MILES.toFloat()
    fun milesToKm(miles: Float): Float = miles * MILES_TO_KM.toFloat()

    // Helper: Convert speed based on units preference
    fun convertSpeed(speedKmh: Float, toUnits: UnitsSystem): Float {
        return when (toUnits) {
            UnitsSystem.METRIC -> speedKmh
            UnitsSystem.IMPERIAL -> kmhToMph(speedKmh)
        }
    }

    // Helper: Convert distance based on units preference
    fun convertDistance(distanceKm: Float, toUnits: UnitsSystem): Float {
        return when (toUnits) {
            UnitsSystem.METRIC -> distanceKm
            UnitsSystem.IMPERIAL -> kmToMiles(distanceKm)
        }
    }

    // Helper: Get speed unit label
    fun getSpeedUnit(units: UnitsSystem): String {
        return when (units) {
            UnitsSystem.METRIC -> "km/h"
            UnitsSystem.IMPERIAL -> "mph"
        }
    }

    // Helper: Get distance unit label
    fun getDistanceUnit(units: UnitsSystem): String {
        return when (units) {
            UnitsSystem.METRIC -> "km"
            UnitsSystem.IMPERIAL -> "mi"
        }
    }
}
```

### Unit Tests

```kotlin
class UnitConversionsTest {
    @Test
    fun `kmhToMph converts correctly`() {
        val result = UnitConversions.kmhToMph(100f)
        assertThat(result).isWithin(0.01f).of(62.14f)
    }

    @Test
    fun `kmToMiles converts correctly`() {
        val result = UnitConversions.kmToMiles(10f)
        assertThat(result).isWithin(0.01f).of(6.21f)
    }

    @Test
    fun `convertSpeed returns unchanged for metric`() {
        val result = UnitConversions.convertSpeed(50f, UnitsSystem.METRIC)
        assertThat(result).isEqualTo(50f)
    }

    @Test
    fun `convertSpeed converts for imperial`() {
        val result = UnitConversions.convertSpeed(50f, UnitsSystem.IMPERIAL)
        assertThat(result).isWithin(0.01f).of(31.07f)
    }
}
```

### References
- [Unit Conversion Standards](https://en.wikipedia.org/wiki/Conversion_of_units)
- [Android Testing Best Practices](https://developer.android.com/training/testing/fundamentals)

---

## 7. ViewModel State Management Pattern

### Decision
Use single `SettingsUiState` data class with `StateFlow` for reactive UI updates. ViewModel exposes read-only StateFlow, UI collects with `collectAsStateWithLifecycle()`. Settings changes trigger repository writes immediately.

### Rationale
- **Single Source of Truth**: One state object prevents inconsistencies
- **Reactive**: UI automatically updates when settings change
- **Lifecycle-Aware**: `collectAsStateWithLifecycle()` prevents memory leaks
- **Established Pattern**: Matches v0.1.0 SpeedTrackingViewModel pattern

### Alternatives Considered
- **LiveData**: Older pattern, Flow/StateFlow is modern Kotlin approach
- **Multiple StateFlows**: Harder to manage, not atomic updates
- **Channels**: For one-time events only, settings are state not events

### Code Example

```kotlin
data class SettingsUiState(
    val unitsSystem: UnitsSystem = UnitsSystem.METRIC,
    val gpsAccuracy: GpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
    val autoPauseConfig: AutoPauseConfig = AutoPauseConfig.default(),
    val isLoading: Boolean = false
)

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        // Load settings on init
        viewModelScope.launch {
            combine(
                settingsRepository.unitsSystem,
                settingsRepository.gpsAccuracy,
                settingsRepository.autoPauseConfig
            ) { units, gps, autoPause ->
                SettingsUiState(
                    unitsSystem = units,
                    gpsAccuracy = gps,
                    autoPauseConfig = autoPause
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setUnitsSystem(units: UnitsSystem) {
        viewModelScope.launch {
            settingsRepository.setUnitsSystem(units)
        }
    }

    fun setGpsAccuracy(accuracy: GpsAccuracy) {
        viewModelScope.launch {
            settingsRepository.setGpsAccuracy(accuracy)
        }
    }

    fun setAutoPauseEnabled(enabled: Boolean) {
        viewModelScope.launch {
            val current = _uiState.value.autoPauseConfig
            settingsRepository.setAutoPauseConfig(current.copy(enabled = enabled))
        }
    }

    fun setAutoPauseThreshold(minutes: Int) {
        viewModelScope.launch {
            val current = _uiState.value.autoPauseConfig
            settingsRepository.setAutoPauseConfig(current.copy(thresholdMinutes = minutes))
        }
    }
}
```

### UI Collection

```kotlin
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SettingsContent(
        unitsSystem = uiState.unitsSystem,
        gpsAccuracy = uiState.gpsAccuracy,
        autoPauseConfig = uiState.autoPauseConfig,
        onUnitsChange = viewModel::setUnitsSystem,
        onGpsAccuracyChange = viewModel::setGpsAccuracy,
        onAutoPauseEnabledChange = viewModel::setAutoPauseEnabled,
        onAutoPauseThresholdChange = viewModel::setAutoPauseThreshold
    )
}
```

### References
- [StateFlow and SharedFlow](https://developer.android.com/kotlin/flow/stateflow-and-sharedflow)
- [Jetpack Compose State Management](https://developer.android.com/jetpack/compose/state)

---

## Summary

All research tasks completed. Key decisions:

1. ✅ **SegmentedButton**: Material 3 component for binary choices
2. ✅ **DataStore Error Handling**: Catch-and-default pattern with logging
3. ✅ **Persistence Testing**: Instrumented tests with activity recreation
4. ✅ **Bottom Navigation**: Scaffold + NavigationBar + NavHost pattern
5. ✅ **Auto-Pause Logic**: Timer in TrackLocationUseCase with state machine
6. ✅ **Unit Conversions**: Pure utility functions with standard factors
7. ✅ **ViewModel State**: Single StateFlow with lifecycle-aware collection

**No unresolved clarifications remain. Ready for Phase 1: Design & Contracts.**
