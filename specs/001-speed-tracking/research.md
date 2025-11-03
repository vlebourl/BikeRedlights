# Research: Real-Time Speed and Location Tracking

**Feature**: 001-speed-tracking
**Date**: 2025-11-02
**Research Phase**: Phase 0

## Overview

This document consolidates research findings for implementing foreground GPS location tracking with real-time speed display for cycling. All decisions are based on Android 14+ (API 34+) best practices and the project's Clean Architecture requirements.

---

## Research Area 1: FusedLocationProviderClient Configuration

### Decision

Use `LocationRequest.Builder` with `Priority.PRIORITY_HIGH_ACCURACY`, 1000ms interval, 500ms min update interval, and 2000ms max delay.

### Configuration Code

```kotlin
val locationRequest = LocationRequest.Builder(
    Priority.PRIORITY_HIGH_ACCURACY,
    1000L // Desired interval: 1 second
)
    .setMinUpdateIntervalMillis(500L) // Fastest interval: 0.5 seconds
    .setMaxUpdateDelayMillis(2000L) // Max delay: 2 seconds (allows batching)
    .setWaitForAccurateLocation(false) // Don't wait indefinitely for high accuracy
    .build()
```

### Rationale

- **API 34+ Requirement**: `LocationRequest.Builder` is the modern approach (old setter methods deprecated in Play Services 21.0.0+)
- **Priority Choice**: `PRIORITY_HIGH_ACCURACY` necessary for GPS-based speed tracking to meet ±2 km/h accuracy requirement
- **Interval Strategy**: 1000ms provides real-time feedback while meeting battery constraints (≤5%/hour)
- **Batching**: 2000ms max delay allows some batching to reduce wake-ups
- **Cycling Context**: Outdoor GPS availability, speeds 0-40 km/h

### Alternatives Considered

| Alternative | Why Rejected |
|-------------|--------------|
| `PRIORITY_BALANCED_POWER_ACCURACY` | Rarely uses GPS, relies on WiFi/cell towers - insufficient accuracy for cycling speed (may be off by >10 km/h) |
| 2000ms interval | Would reduce battery drain by ~20-30% but feels less responsive for real-time speed display |
| 500ms interval | Increases battery drain significantly (10-15%/hour), only consider for opt-in "high precision mode" |

### Battery Impact

- **Expected**: 5-10% per hour with `PRIORITY_HIGH_ACCURACY` and 1-second updates in foreground
- **Mitigation**: Stop updates immediately when app backgrounded (lifecycle-aware)
- **Testing Strategy**: Profile with Android Studio Battery Profiler over 30+ minute sessions

---

## Research Area 2: Speed Calculation & Conversion

### Decision

Use `Location.getSpeed()` as primary source with proper validation and m/s to km/h conversion.

### Rationale

- **GPS Speed Accuracy**: GPS calculates speed using Doppler shift of satellite signals, more accurate than position-based calculations
- **Cycling Speed Range**: Reliable for 0-40 km/h when GPS signal is good
- **FusedLocationProvider**: Integrates multiple sensors (GPS, accelerometer, gyroscope) for improved accuracy

### Implementation Code

```kotlin
data class SpeedMeasurement(
    val speedKmh: Float,
    val timestamp: Long,
    val accuracy: Float?,
    val isStationary: Boolean
)

fun convertToKmh(location: Location, previousLocation: Location?): SpeedMeasurement {
    val speedMs = when {
        location.hasSpeed() && location.speed > 0 -> location.speed
        previousLocation != null -> {
            val elapsedTimeInSeconds = (location.time - previousLocation.time) / 1000.0
            val distanceInMeters = previousLocation.distanceTo(location)
            if (elapsedTimeInSeconds > 0) {
                (distanceInMeters / elapsedTimeInSeconds).toFloat()
            } else 0f
        }
        else -> 0f
    }

    // Edge case handling
    val sanitizedSpeedMs = speedMs.coerceAtLeast(0f) // Handle negative
    val maxSpeedMs = 100f / 3.6f // ~27.8 m/s max for cycling
    val clampedSpeedMs = sanitizedSpeedMs.coerceAtMost(maxSpeedMs)

    // Stationary threshold (GPS jitter below 1 km/h)
    val stationaryThresholdMs = 1f / 3.6f // ~0.28 m/s
    val isStationary = clampedSpeedMs < stationaryThresholdMs

    // Conversion: m/s to km/h
    val speedKmh = if (isStationary) 0f else clampedSpeedMs * 3.6f

    val accuracyKmh = if (location.hasSpeedAccuracy()) {
        location.speedAccuracyMetersPerSecond * 3.6f
    } else null

    return SpeedMeasurement(
        speedKmh = speedKmh,
        timestamp = location.time,
        accuracy = accuracyKmh,
        isStationary = isStationary
    )
}
```

### Edge Cases Handled

1. **Null/Missing Speed**: Check `hasSpeed()`, use fallback calculation or return 0
2. **Negative Values**: Coerce to 0 (GPS errors can produce negative speeds)
3. **Unrealistic Values**: Clamp to reasonable max (100 km/h for cycling)
4. **GPS Jitter When Stationary**: Apply threshold (<1 km/h = 0 km/h)
5. **Speed Accuracy**: Display confidence indicator if available (API 26+)
6. **First Location Fix**: No previous location for fallback - display "Acquiring GPS" state

---

## Research Area 3: Location Permission Handling

### Decision

Use native `rememberLauncherForActivityResult` with `ActivityResultContracts.RequestMultiplePermissions` rather than Accompanist Permissions library.

### Rationale

- **Library Status**: Accompanist Permissions remains experimental and Google announced no new features
- **Native API Maturity**: `rememberLauncherForActivityResult` is stable and officially supported
- **Project Architecture**: No DI framework (Hilt disabled), lightweight solution preferred
- **Future-Proof**: Native APIs maintained by Google indefinitely

### Android 14+ Requirements

- **No Breaking Changes**: Android 14 doesn't introduce breaking changes vs Android 13
- **Critical Android 12+ Requirement**: Must request BOTH `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION` together
- **User Choice**: System presents "Precise" vs "Approximate" options
- **Background Location**: Not needed for foreground-only MVP

### Implementation Pattern

```kotlin
@Composable
fun LocationPermissionHandler(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val locationPermissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION
    )

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val fineLocationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] ?: false

        when {
            fineLocationGranted -> {
                onPermissionGranted() // Precise location granted
            }
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                // Approximate only - show warning for cycling app
                onPermissionGranted() // Allow but inform user
            }
            else -> {
                onPermissionDenied() // All permissions denied
            }
        }
    }

    // Check permissions on lifecycle start
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_START) {
                if (!hasLocationPermissions(context)) {
                    permissionLauncher.launch(locationPermissions)
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    content()
}
```

### UX Recommendations

**Permission Request Timing**: In-context request when user taps "Start Tracking" button

**Rationale Strategy**:
- **Before first request**: Brief in-line explanation, no dialog
- **After first denial**: Show detailed rationale dialog
- **After "Don't ask again"**: Show settings navigation dialog

**Handling "Don't Ask Again"**:
```kotlin
val shouldShowRationale = ActivityCompat.shouldShowRequestPermissionRationale(
    activity,
    Manifest.permission.ACCESS_FINE_LOCATION
)

// State machine:
// First request: shouldShowRationale = false, permission = not granted
// User denied once: shouldShowRationale = true
// "Don't ask again": shouldShowRationale = false, permission = not granted
```

### Alternatives Considered

| Alternative | Why Rejected |
|-------------|--------------|
| Accompanist Permissions | Still experimental, no new features planned, uncertain long-term support |
| Third-Party Libraries | Unnecessary dependency, overkill for straightforward permission handling |
| Traditional Activity-Based | Not idiomatic for Compose, more boilerplate, complex lifecycle management |

---

## Research Area 4: Lifecycle-Aware Location Tracking

### Decision

Use Repository-emitted Flow with ViewModel StateFlow + `collectAsStateWithLifecycle` in Compose.

### Architecture Pattern

```
Compose UI (collectAsStateWithLifecycle)
    ↓ observes StateFlow<UiState>
ViewModel (viewModelScope.launch)
    ↓ collects Flow<LocationData>
Repository (callbackFlow with awaitClose)
    ↓ wraps
FusedLocationProviderClient
```

### Rationale

- **Automatic Lifecycle Management**: `collectAsStateWithLifecycle()` starts/stops collection based on lifecycle state
- **Battery Conservation**: Stops when app backgrounds (lifecycle < STARTED)
- **Configuration Change Resilience**: ViewModel survives rotation, no tracking restart
- **Clean Architecture**: Repository → ViewModel → UI separation per constitution
- **Modern Best Practice**: Google-recommended pattern for 2024-2025

### Repository Implementation

```kotlin
interface LocationRepository {
    fun getLocationUpdates(): Flow<LocationData>
}

class LocationRepositoryImpl(
    private val context: Context
) : LocationRepository {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(location.toLocationData())
                }
            }
        }

        // Get last known location immediately
        fusedLocationClient.lastLocation.await()?.let { send(it.toLocationData()) }

        // Start location updates
        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            context.mainLooper
        ).await()

        // Cleanup when Flow cancelled
        awaitClose {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
}
```

### ViewModel Implementation

```kotlin
class SpeedTrackingViewModel(
    private val locationRepository: LocationRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedTrackingUiState())
    val uiState: StateFlow<SpeedTrackingUiState> = _uiState.asStateFlow()

    fun startLocationTracking() {
        viewModelScope.launch {
            locationRepository.getLocationUpdates()
                .catch { exception ->
                    _uiState.update { it.copy(gpsStatus = GpsStatus.Unavailable) }
                }
                .collect { locationData ->
                    _uiState.update { currentState ->
                        currentState.copy(
                            currentSpeedKmh = locationData.speedKmh,
                            locationData = locationData,
                            gpsStatus = GpsStatus.Active(locationData.accuracy)
                        )
                    }
                }
        }
    }
}
```

### UI Implementation

```kotlin
@Composable
fun SpeedTrackingScreen(viewModel: SpeedTrackingViewModel) {
    // Lifecycle-aware collection - stops when app backgrounded
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(
        minActiveState = Lifecycle.State.STARTED
    )

    // UI renders based on uiState
}
```

### Lifecycle Behavior

1. **App starts** → Activity onCreate → Compose rendered
2. **Permission granted** → ViewModel starts collecting repository Flow
3. **Location updates** → Repository emits → ViewModel transforms → UI renders
4. **App backgrounded** → `collectAsStateWithLifecycle` stops collecting
5. **Repository Flow cancelled** → `awaitClose` executes → removes location updates (battery saved)
6. **App foregrounded** → Collection resumes → Flow restarts → updates resume
7. **Configuration change** → Activity destroyed → ViewModel survives → Flow continues
8. **Process death** → Everything destroyed → Restart when user reopens

### Alternatives Considered

| Alternative | Why Rejected |
|-------------|--------------|
| DisposableEffect with manual lifecycle observation | More boilerplate, prone to bugs, `collectAsStateWithLifecycle` achieves same with less code |
| LaunchedEffect with repeatOnLifecycle | More verbose, extra nesting, `collectAsStateWithLifecycle` is syntactic sugar for this |
| ViewModel owns Location Client | Violates Clean Architecture, tight coupling to Android framework, difficult to test |
| Foreground Service | Out of scope for MVP (foreground-only per spec FR-009) |

### Edge Cases Handled

| Edge Case | Solution |
|-----------|----------|
| **Screen Rotation** | ViewModel survives, Flow continues, no restart needed |
| **Process Death** | No persistence required for MVP per spec |
| **GPS Signal Loss** | Add timeout to Flow, display "GPS Lost" indicator |
| **Permission Revoked** | SecurityException caught, update UI to permission required state |
| **Multi-Window/PiP** | Use `Lifecycle.State.STARTED` (not RESUMED) to track when visible |
| **Zero Speed Detection** | Apply threshold: <1 km/h = 0 km/h (filter GPS jitter) |
| **First Launch** | Attempt `lastLocation` but don't fail if null, show "Acquiring GPS" |

---

## Summary of Key Decisions

| Research Area | Decision | Rationale |
|---------------|----------|-----------|
| **LocationRequest** | Builder with PRIORITY_HIGH_ACCURACY, 1000ms interval | Modern API, balances accuracy (±2 km/h) and battery (≤5%/hour) |
| **Speed Source** | `Location.getSpeed()` with validation | GPS Doppler shift accurate for cycling speeds |
| **Conversion** | `speed_ms * 3.6f` with edge case handling | Handle null, negative, unrealistic values, stationary threshold |
| **Permissions** | Native `rememberLauncherForActivityResult` | Stable, no external dependency, future-proof |
| **Lifecycle** | Repository Flow + ViewModel StateFlow + `collectAsStateWithLifecycle` | Automatic lifecycle management, battery efficient, Clean Architecture |

---

## Technology Stack Confirmed

- **Language**: Kotlin 2.0.21
- **Location API**: Play Services Location 21.3.0 (FusedLocationProviderClient)
- **UI Framework**: Jetpack Compose (BOM 2024.11.00)
- **Architecture**: Clean Architecture (UI → ViewModel → Domain → Data)
- **Async**: Coroutines + Flow/StateFlow
- **Testing**: JUnit 4, MockK, Turbine, Truth, Compose UI Test

---

## Implementation Priorities

### Phase 1: Foundation (Must Have)
1. Domain models (LocationData, SpeedMeasurement, GpsStatus)
2. Repository interface and implementation
3. Location permission handling
4. ViewModel with StateFlow
5. Basic UI with speed/location display

### Phase 2: Refinement (Should Have)
1. GPS status indicator
2. Speed accuracy validation
3. Edge case handling (signal loss, zero speed)
4. Accessibility (content descriptions, contrast)

### Phase 3: Polish (Nice to Have)
1. Smooth animations for speed changes
2. GPS signal strength indicator
3. Battery optimization profiling
4. User feedback for approximate vs precise location

---

## Testing Strategy

### Unit Tests (90%+ coverage required per constitution)
- ViewModel state management
- Speed calculation and conversion logic
- Edge case handling (null, negative, unrealistic values)
- Permission state management

### Integration Tests
- End-to-end location tracking flow
- Repository with mocked FusedLocationProviderClient
- Permission request flow

### UI Tests
- Speed display updates
- GPS status indicators
- Permission required state
- Configuration changes (rotation)

### Battery Profiling
- Android Studio Battery Profiler
- 30+ minute test sessions
- Multiple devices (GPS hardware varies)
- Different conditions (clear sky, urban canyon)

---

## References

- **Android Location Best Practices**: https://developer.android.com/develop/sensors-and-location/location/permissions
- **FusedLocationProviderClient**: https://developers.google.com/android/reference/com/google/android/gms/location/FusedLocationProviderClient
- **Lifecycle-aware Components**: https://developer.android.com/jetpack/androidx/releases/lifecycle
- **Jetpack Compose State**: https://developer.android.com/jetpack/compose/state
- **Clean Architecture**: https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html

---

## Next Steps

Proceed to Phase 1: Design & Contracts
- Generate data-model.md (domain entities)
- Create API contracts (internal interfaces)
- Generate quickstart.md (implementation guide)
- Update agent context
