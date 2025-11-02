# Quick Start Guide: Real-Time Speed and Location Tracking

**Feature**: 001-speed-tracking
**Branch**: `001-speed-tracking`
**Phase**: Phase 1 - Implementation Guide
**Date**: 2025-11-02

## Overview

This guide provides step-by-step instructions for implementing the real-time speed tracking feature. Follow this guide after reading:
- `spec.md` (requirements)
- `plan.md` (technical approach)
- `research.md` (technology decisions)
- `data-model.md` (domain models)
- `contracts/` (interfaces)

## Prerequisites

- [x] Android Studio installed (Arctic Fox or later)
- [x] Project cloned and branch `001-speed-tracking` checked out
- [x] Dependencies already configured in `app/build.gradle.kts`:
  - Play Services Location 21.3.0
  - Jetpack Compose BOM 2024.11.00
  - Lifecycle Runtime Compose 2.8.7
- [x] Manifest permissions already declared:
  - `ACCESS_FINE_LOCATION`
  - `ACCESS_COARSE_LOCATION`

## Implementation Steps

### Step 1: Create Domain Models (30 minutes)

**Goal**: Define immutable data classes for location and speed.

**Files to Create**:
1. `/app/src/main/java/com/example/bikeredlights/domain/model/LocationData.kt`
2. `/app/src/main/java/com/example/bikeredlights/domain/model/SpeedMeasurement.kt`
3. `/app/src/main/java/com/example/bikeredlights/domain/model/GpsStatus.kt`

**Reference**: `data-model.md` for complete definitions

**Key Points**:
- Use `@Immutable` annotation for Compose optimization
- All fields non-nullable except `speedMps` and `bearing`
- Add validation in init blocks if needed

**Verification**:
```bash
# Ensure files compile without errors
./gradlew :app:compileDebugKotlin
```

---

### Step 2: Create Repository Interface (15 minutes)

**Goal**: Define contract for location tracking.

**File to Create**:
- `/app/src/main/java/com/example/bikeredlights/domain/repository/LocationRepository.kt`

**Reference**: `contracts/LocationRepository.kt`

**Code Template**:
```kotlin
package com.example.bikeredlights.domain.repository

import com.example.bikeredlights.domain.model.LocationData
import kotlinx.coroutines.flow.Flow

interface LocationRepository {
    fun getLocationUpdates(): Flow<LocationData>
}
```

**Key Points**:
- Interface lives in `domain/repository` (not `data/repository`)
- Returns cold Flow (starts on collect, stops on cancel)
- Throws SecurityException if permission not granted

---

### Step 3: Implement Repository (60 minutes)

**Goal**: Wrap FusedLocationProviderClient in repository implementation.

**File to Create**:
- `/app/src/main/java/com/example/bikeredlights/data/repository/LocationRepositoryImpl.kt`

**Reference**: `research.md` Section "Research Area 4: Lifecycle-Aware Location Tracking"

**Code Template**:
```kotlin
package com.example.bikeredlights.data.repository

import android.annotation.SuppressLint
import android.content.Context
import com.example.bikeredlights.domain.model.LocationData
import com.example.bikeredlights.domain.repository.LocationRepository
import com.google.android.gms.location.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

class LocationRepositoryImpl(
    private val context: Context
) : LocationRepository {

    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(): Flow<LocationData> = callbackFlow {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000L
        ).apply {
            setMinUpdateIntervalMillis(500L)
            setWaitForAccurateLocation(false)
        }.build()

        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    trySend(LocationData(
                        latitude = location.latitude,
                        longitude = location.longitude,
                        accuracy = location.accuracy,
                        timestamp = location.time,
                        speedMps = if (location.hasSpeed()) location.speed else null,
                        bearing = if (location.hasBearing()) location.bearing else null
                    ))
                }
            }
        }

        // Get last known location immediately
        try {
            fusedLocationClient.lastLocation.await()?.let { lastLocation ->
                send(LocationData(
                    latitude = lastLocation.latitude,
                    longitude = lastLocation.longitude,
                    accuracy = lastLocation.accuracy,
                    timestamp = lastLocation.time,
                    speedMps = if (lastLocation.hasSpeed()) lastLocation.speed else null,
                    bearing = if (lastLocation.hasBearing()) lastLocation.bearing else null
                ))
            }
        } catch (e: Exception) {
            // Last location unavailable, continue with real-time updates
        }

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

**Key Points**:
- `@SuppressLint("MissingPermission")` because permission checked before calling
- `callbackFlow` with `awaitClose` for automatic cleanup
- `trySend()` for non-blocking emission (returns failure if channel closed)
- `Priority.PRIORITY_HIGH_ACCURACY` for GPS-based speed

**Testing**:
```bash
# Run repository tests (create test first)
./gradlew :app:testDebugUnitTest --tests LocationRepositoryImplTest
```

---

### Step 4: Create Use Case (45 minutes)

**Goal**: Implement business logic for speed calculation.

**File to Create**:
- `/app/src/main/java/com/example/bikeredlights/domain/usecase/TrackLocationUseCase.kt`

**Reference**: `data-model.md` Section "Conversion Extensions"

**Code Template**:
```kotlin
package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.*
import com.example.bikeredlights.domain.repository.LocationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.scan

class TrackLocationUseCase(
    private val locationRepository: LocationRepository
) {
    operator fun invoke(): Flow<SpeedMeasurement> {
        return locationRepository.getLocationUpdates()
            .scan(Pair<LocationData?, SpeedMeasurement?>(null, null)) { acc, location ->
                val previousLocation = acc.first
                val speedMeasurement = calculateSpeed(location, previousLocation)
                Pair(location, speedMeasurement)
            }
            .map { it.second!! }
    }

    private fun calculateSpeed(
        current: LocationData,
        previous: LocationData?
    ): SpeedMeasurement {
        val (speedMs, source) = when {
            current.speedMps != null && current.speedMps > 0 ->
                current.speedMps to SpeedSource.GPS

            previous != null -> {
                val elapsedSeconds = (current.timestamp - previous.timestamp) / 1000.0
                val distanceMeters = distanceBetween(current, previous)
                if (elapsedSeconds > 0) {
                    (distanceMeters / elapsedSeconds).toFloat() to SpeedSource.CALCULATED
                } else 0f to SpeedSource.UNKNOWN
            }

            else -> 0f to SpeedSource.UNKNOWN
        }

        val sanitizedSpeedMs = speedMs.coerceIn(0f, 100f / 3.6f)
        val stationaryThresholdMs = 1f / 3.6f
        val isStationary = sanitizedSpeedMs < stationaryThresholdMs

        return SpeedMeasurement(
            speedKmh = if (isStationary) 0f else sanitizedSpeedMs * 3.6f,
            timestamp = current.timestamp,
            accuracyKmh = null,
            isStationary = isStationary,
            source = source
        )
    }

    private fun distanceBetween(loc1: LocationData, loc2: LocationData): Float {
        // Haversine formula for great-circle distance
        val earthRadius = 6371000f // meters
        val dLat = Math.toRadians(loc2.latitude - loc1.latitude)
        val dLon = Math.toRadians(loc2.longitude - loc1.longitude)

        val a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(loc1.latitude)) *
                Math.cos(Math.toRadians(loc2.latitude)) *
                Math.sin(dLon / 2) * Math.sin(dLon / 2)

        val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return (earthRadius * c).toFloat()
    }
}
```

**Key Points**:
- `operator fun invoke()` allows calling use case as function: `useCase()`
- `.scan()` maintains previous location for fallback calculation
- Haversine formula for accurate distance on curved Earth surface

---

### Step 5: Create ViewModel (45 minutes)

**Goal**: Manage UI state with StateFlow.

**File to Create**:
- `/app/src/main/java/com/example/bikeredlights/ui/viewmodel/SpeedTrackingViewModel.kt`
- `/app/src/main/java/com/example/bikeredlights/ui/viewmodel/SpeedTrackingUiState.kt`

**Reference**: `research.md` Section "Repository Implementation"

**Code Template**:
```kotlin
// SpeedTrackingUiState.kt
package com.example.bikeredlights.ui.viewmodel

import com.example.bikeredlights.domain.model.*

data class SpeedTrackingUiState(
    val speedMeasurement: SpeedMeasurement? = null,
    val locationData: LocationData? = null,
    val gpsStatus: GpsStatus = GpsStatus.Acquiring,
    val hasLocationPermission: Boolean = false,
    val errorMessage: String? = null
)

// SpeedTrackingViewModel.kt
package com.example.bikeredlights.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.bikeredlights.domain.model.GpsStatus
import com.example.bikeredlights.domain.usecase.TrackLocationUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class SpeedTrackingViewModel(
    private val trackLocationUseCase: TrackLocationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(SpeedTrackingUiState())
    val uiState: StateFlow<SpeedTrackingUiState> = _uiState.asStateFlow()

    fun startLocationTracking() {
        viewModelScope.launch {
            trackLocationUseCase()
                .catch { exception ->
                    _uiState.update {
                        it.copy(
                            gpsStatus = GpsStatus.Unavailable,
                            errorMessage = "GPS unavailable: ${exception.message}"
                        )
                    }
                }
                .collect { speedMeasurement ->
                    _uiState.update { currentState ->
                        val gpsStatus = determineGpsStatus(speedMeasurement)
                        currentState.copy(
                            speedMeasurement = speedMeasurement,
                            gpsStatus = gpsStatus,
                            errorMessage = null
                        )
                    }
                }
        }
    }

    fun onPermissionGranted() {
        _uiState.update { it.copy(hasLocationPermission = true) }
        startLocationTracking()
    }

    fun onPermissionDenied() {
        _uiState.update {
            it.copy(
                hasLocationPermission = false,
                errorMessage = "Location permission required"
            )
        }
    }

    private fun determineGpsStatus(measurement: SpeedMeasurement): GpsStatus {
        // Derive GPS status from speed source and accuracy
        return when {
            measurement.source == com.example.bikeredlights.domain.model.SpeedSource.UNKNOWN ->
                GpsStatus.Unavailable
            measurement.accuracyKmh != null && measurement.accuracyKmh > 10f ->
                GpsStatus.Acquiring
            else -> GpsStatus.Active(measurement.accuracyKmh ?: 5f)
        }
    }
}
```

**Key Points**:
- `StateFlow` for state management (Compose-friendly)
- `viewModelScope.launch` for coroutine lifecycle
- `.catch()` for error handling
- `.update()` for atomic state modifications

---

### Step 6: Create Permission Handler (45 minutes)

**Goal**: Handle runtime location permissions in Compose.

**File to Create**:
- `/app/src/main/java/com/example/bikeredlights/ui/permissions/LocationPermissionHandler.kt`

**Reference**: `research.md` Section "Research Area 3: Location Permission Handling"

**Code Template**: See `research.md` for complete implementation (too long for quickstart)

**Key Points**:
- Use `rememberLauncherForActivityResult` with `RequestMultiplePermissions`
- Request both `ACCESS_FINE_LOCATION` and `ACCESS_COARSE_LOCATION`
- Show rationale dialog after first denial
- Show settings dialog for "Don't ask again" scenario
- Check permissions on `Lifecycle.Event.ON_START`

---

### Step 7: Create UI Components (90 minutes)

**Goal**: Build Compose UI for speed display.

**Files to Create**:
1. `/app/src/main/java/com/example/bikeredlights/ui/screens/SpeedTrackingScreen.kt`
2. `/app/src/main/java/com/example/bikeredlights/ui/components/SpeedDisplay.kt`
3. `/app/src/main/java/com/example/bikeredlights/ui/components/LocationDisplay.kt`
4. `/app/src/main/java/com/example/bikeredlights/ui/components/GpsStatusIndicator.kt`

**Reference**: `research.md` Section "UI Implementation"

**Code Template (SpeedTrackingScreen.kt)**:
```kotlin
package com.example.bikeredlights.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModel

@Composable
fun SpeedTrackingScreen(
    viewModel: SpeedTrackingViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle(
        minActiveState = Lifecycle.State.STARTED
    )

    Surface(modifier = modifier.fillMaxSize()) {
        if (uiState.hasLocationPermission) {
            SpeedTrackingContent(uiState = uiState)
        } else {
            PermissionRequiredContent(
                onRequestPermission = { /* Handle permission request */ }
            )
        }
    }
}

@Composable
private fun SpeedTrackingContent(uiState: SpeedTrackingUiState) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Speed Display (large, prominent)
        SpeedDisplay(measurement = uiState.speedMeasurement)

        Spacer(modifier = Modifier.height(32.dp))

        // Location Display
        LocationDisplay(locationData = uiState.locationData)

        Spacer(modifier = Modifier.height(16.dp))

        // GPS Status Indicator
        GpsStatusIndicator(gpsStatus = uiState.gpsStatus)
    }
}
```

**Key Points**:
- `collectAsStateWithLifecycle(minActiveState = STARTED)` for lifecycle-aware collection
- Split into composable functions for reusability
- Use Material 3 components for consistency

---

### Step 8: Wire Up MainActivity (30 minutes)

**Goal**: Integrate screen into app entry point.

**File to Modify**:
- `/app/src/main/java/com/example/bikeredlights/MainActivity.kt`

**Code Template**:
```kotlin
package com.example.bikeredlights

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.bikeredlights.data.repository.LocationRepositoryImpl
import com.example.bikeredlights.domain.usecase.TrackLocationUseCase
import com.example.bikeredlights.ui.screens.SpeedTrackingScreen
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModel
import com.example.bikeredlights.ui.viewmodel.SpeedTrackingViewModelFactory

class MainActivity : ComponentActivity() {

    // Manual DI (Hilt disabled for MVP)
    private val viewModel: SpeedTrackingViewModel by viewModels {
        val repository = LocationRepositoryImpl(applicationContext)
        val useCase = TrackLocationUseCase(repository)
        SpeedTrackingViewModelFactory(useCase)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            BikeRedlightsTheme {
                SpeedTrackingScreen(viewModel = viewModel)
            }
        }
    }
}

// ViewModelFactory (required without Hilt)
class SpeedTrackingViewModelFactory(
    private val trackLocationUseCase: TrackLocationUseCase
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(SpeedTrackingViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return SpeedTrackingViewModel(trackLocationUseCase) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
```

**Key Points**:
- Manual DI since Hilt disabled for MVP
- `by viewModels()` ensures ViewModel survives configuration changes
- ViewModelFactory required to inject dependencies

---

### Step 9: Write Tests (120 minutes)

**Goal**: Achieve 90%+ test coverage (constitution requirement for safety-critical features).

**Files to Create**:
1. `/app/src/test/java/com/example/bikeredlights/domain/usecase/TrackLocationUseCaseTest.kt`
2. `/app/src/test/java/com/example/bikeredlights/ui/viewmodel/SpeedTrackingViewModelTest.kt`
3. `/app/src/test/java/com/example/bikeredlights/data/repository/LocationRepositoryImplTest.kt`
4. `/app/src/androidTest/java/com/example/bikeredlights/ui/SpeedTrackingScreenTest.kt`

**Test Priorities**:
1. **Use Case Tests** (highest priority):
   - m/s to km/h conversion
   - Stationary threshold (<1 km/h)
   - Negative/unrealistic speed handling
   - Speed source determination
   - Distance calculation accuracy

2. **ViewModel Tests**:
   - State flow emissions
   - Permission handling
   - Error handling
   - GPS status determination

3. **Repository Tests**:
   - Flow emissions
   - Cleanup on cancellation
   - SecurityException when permission denied

4. **UI Tests**:
   - Speed display updates
   - Permission request flow
   - GPS status indicators

**Testing Tools**:
- JUnit 4, MockK, Turbine, Truth, Compose UI Test (already configured)

**Example Test**:
```kotlin
class TrackLocationUseCaseTest {

    @Test
    fun `converts 10 m_s GPS speed to 36 km_h`() = runTest {
        val fakeRepo = FakeLocationRepository()
        val useCase = TrackLocationUseCase(fakeRepo)

        fakeRepo.emitLocation(
            LocationData(
                latitude = 37.7749,
                longitude = -122.4194,
                accuracy = 5f,
                timestamp = System.currentTimeMillis(),
                speedMps = 10f,
                bearing = 90f
            )
        )

        useCase().test {
            val measurement = awaitItem()
            assertEquals(36f, measurement.speedKmh, 0.1f)
            assertEquals(SpeedSource.GPS, measurement.source)
            assertFalse(measurement.isStationary)
        }
    }
}
```

---

### Step 10: Test on Emulator (60 minutes)

**Goal**: Validate feature in Android runtime environment (constitution requirement).

**Setup**:
1. Create Android emulator (Pixel 6, API 34)
2. Enable location services in emulator
3. Set mock location route (cycling path)

**Commands**:
```bash
# Build and install debug APK
./gradlew :app:installDebug

# Launch emulator
emulator -avd Pixel_6_API_34

# Set mock location (requires adb)
adb emu geo fix -122.4194 37.7749
```

**Test Checklist**:
- [ ] App launches without crashes
- [ ] Permission request dialog appears
- [ ] Granting permission starts location tracking
- [ ] Speed display shows 0 km/h when stationary
- [ ] Speed updates when emulator location changes
- [ ] GPS status indicator shows correct state
- [ ] Dark mode renders correctly
- [ ] Screen rotation preserves state (no tracking restart)
- [ ] App backgrounding stops tracking (verify with battery profiler)
- [ ] App foregrounding resumes tracking

**Battery Profiling**:
```bash
# Record battery usage
adb shell dumpsys batterystats --reset
# Use app for 30 minutes
adb shell dumpsys batterystats > battery_stats.txt
# Analyze: Look for "Uid <your_app_uid>" section
```

Target: ≤5% battery drain per hour (per spec SC-007)

---

## Common Issues & Solutions

### Issue: SecurityException when requesting location updates

**Solution**: Ensure permission checked before calling repository:
```kotlin
if (ContextCompat.checkSelfPermission(context, ACCESS_FINE_LOCATION)
    == PackageManager.PERMISSION_GRANTED) {
    viewModel.startLocationTracking()
}
```

### Issue: Flow never emits location data

**Possible Causes**:
1. Location permission not granted
2. GPS disabled in emulator/device
3. Flow not being collected (check lifecycle state)
4. FusedLocationProviderClient not initialized

**Debug**:
```kotlin
locationRepository.getLocationUpdates()
    .onStart { Log.d("Location", "Flow started") }
    .onCompletion { Log.d("Location", "Flow completed") }
    .collect { location ->
        Log.d("Location", "Received: $location")
    }
```

### Issue: Speed always shows 0 km/h

**Possible Causes**:
1. `Location.hasSpeed()` returns false → GPS not providing speed
2. Stationary threshold applied when shouldn't be
3. Speed conversion formula incorrect

**Debug**:
```kotlin
Log.d("Speed", "speedMps: ${location.speedMps}, hasSpeed: ${location.speedMps != null}")
```

### Issue: App crashes on rotation

**Possible Causes**:
1. ViewModel not surviving configuration change
2. Context reference held in ViewModel
3. Location callback not cleaned up

**Solution**: Ensure using `by viewModels()` delegate and repository `awaitClose`

---

## Performance Optimization Tips

### Battery Optimization
- Use `Lifecycle.State.STARTED` (not RESUMED) for tracking when visible
- Stop updates immediately when app backgrounds
- Consider fallback to `PRIORITY_BALANCED_POWER_ACCURACY` if 5%/hour exceeded

### Memory Optimization
- Use `StateFlow` (replay = 1) not `SharedFlow`
- No location history storage in MVP
- Clean up resources in `awaitClose`

### UI Responsiveness
- Don't block main thread in location callback
- Use `trySend()` for non-blocking emission
- Apply Compose performance best practices (stable types, remember)

---

## Next Steps After Implementation

1. **Run Tests**:
   ```bash
   ./gradlew :app:test :app:connectedAndroidTest
   ```

2. **Verify Coverage**:
   ```bash
   ./gradlew :app:jacocoTestReport
   # Check build/reports/jacoco/jacocoTestReport/html/index.html
   # Target: 90%+ for safety-critical code
   ```

3. **Profile Battery**:
   - Use Android Studio Battery Profiler
   - 30+ minute test session
   - Verify ≤5%/hour consumption

4. **Manual Testing**:
   - Permission flows
   - GPS signal loss scenarios
   - Speed accuracy with external GPS device

5. **Code Review**:
   - Check constitution compliance
   - Verify Clean Architecture separation
   - Ensure accessibility implemented

6. **Commit Changes**:
   - Follow small, frequent commit pattern (constitution requirement)
   - Use conventional commit messages
   - Update TODO.md and RELEASE.md

7. **Create Pull Request**:
   - See constitution Section "Release Pattern & Workflow"
   - Link to spec.md in PR description
   - Confirm emulator testing completion

---

## Helpful Commands

```bash
# Build project
./gradlew :app:assembleDebug

# Run unit tests
./gradlew :app:testDebugUnitTest

# Run instrumentation tests
./gradlew :app:connectedAndroidTest

# Install on device/emulator
./gradlew :app:installDebug

# Clean build
./gradlew clean :app:assembleDebug

# Check lint
./gradlew :app:lintDebug

# Format code (if ktlint configured)
./gradlew ktlintFormat

# Profile battery
adb shell dumpsys batterystats --reset
# Use app for 30 minutes
adb shell dumpsys batterystats > battery_stats.txt
```

---

## Estimated Time: 8-10 hours

- Step 1: Domain Models - 30 min
- Step 2: Repository Interface - 15 min
- Step 3: Repository Implementation - 60 min
- Step 4: Use Case - 45 min
- Step 5: ViewModel - 45 min
- Step 6: Permission Handler - 45 min
- Step 7: UI Components - 90 min
- Step 8: MainActivity - 30 min
- Step 9: Tests - 120 min
- Step 10: Emulator Testing - 60 min
- **Total**: 8.5 hours

---

## Questions?

- **Spec unclear**: Refer to `spec.md` acceptance scenarios
- **Technical approach**: Check `research.md` for decisions and rationale
- **Architecture**: Review `plan.md` constitution check
- **Data models**: See `data-model.md` for detailed definitions
- **Interfaces**: Check `contracts/` directory

**Ready to implement? Start with Step 1: Create Domain Models**
