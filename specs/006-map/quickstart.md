# QuickStart Guide: Maps Integration Implementation

**Feature**: 006-map (Maps Integration)
**Date**: 2025-11-08
**Branch**: `006-map`
**Estimated Time**: 3-4 days

## Overview

This quickstart guide provides a step-by-step implementation plan for adding Google Maps SDK integration to BikeRedlights. Follow phases sequentially for best results.

---

## Phase 0: Google Cloud Setup (PREREQUISITE)

**Duration**: 1 hour
**Must complete before starting Phase 1**

### Step 1: Create Google Cloud Project

1. Go to [Google Cloud Console](https://console.cloud.google.com/)
2. Click **Project dropdown** → **NEW PROJECT**
3. Name: `BikeRedlights`
4. Click **CREATE**

### Step 2: Enable Billing

1. Navigate to **Billing** → **Link a Billing Account**
2. Create billing account or link existing
3. Accept free trial ($300 credit)

### Step 3: Enable Maps SDK for Android API

1. Navigate to **APIs & Services** → **Library**
2. Search: `Maps SDK for Android`
3. Click **ENABLE**

### Step 4: Generate & Restrict API Key

1. Go to **APIs & Services** → **Credentials**
2. Click **+ CREATE CREDENTIALS** → **API Key**
3. Copy API key (save securely)
4. Click on key → Edit
5. **Application Restrictions**: Select **Android apps**
6. Get SHA-1 fingerprint:
   ```bash
   keytool -list -v -alias androiddebugkey -keystore ~/.android/debug.keystore
   # Password: android
   ```
7. Add package name: `com.example.bikeredlights`
8. Add SHA-1 fingerprint
9. **API restrictions**: Select **Restrict key** → Check **Maps SDK for Android**
10. Click **SAVE**

### Step 5: Store API Key Securely

1. Add to `build.gradle.kts` (project-level):
   ```kotlin
   plugins {
       id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin") version "2.0.1" apply false
   }
   ```

2. Add to `app/build.gradle.kts`:
   ```kotlin
   plugins {
       id("com.google.android.libraries.mapsplatform.secrets-gradle-plugin")
   }
   ```

3. Create `secrets.properties` in project root:
   ```properties
   MAPS_API_KEY=AIzaSyDxxxx_YOUR_API_KEY_HERE_xxxx
   ```

4. Verify `.gitignore` includes:
   ```gitignore
   secrets.properties
   local.properties
   ```

5. Add to `AndroidManifest.xml` inside `<application>`:
   ```xml
   <meta-data
       android:name="com.google.android.geo.API_KEY"
       android:value="${MAPS_API_KEY}" />
   ```

### Step 6: Verify Setup

```bash
./gradlew assembleDebug
./gradlew installDebug
adb logcat | grep -E "Maps|ERROR"
```

Look for successful map initialization (no API key errors).

---

## Phase 1: Add Dependencies & Basic Map (1-2 days)

### Task 1.1: Add Dependencies

Edit `app/build.gradle.kts`:
```kotlin
dependencies {
    // Google Maps Compose
    implementation("com.google.maps.android:maps-compose:6.12.1")
    implementation("com.google.android.gms:play-services-maps:18.2.0")

    // Maps Utils (for polyline simplification)
    implementation("com.google.maps.android:android-maps-utils:3.8.2")

    // Existing dependencies...
}
```

Sync Gradle.

### Task 1.2: Create Domain Models

Create `domain/model/MapViewState.kt`:
```kotlin
package com.example.bikeredlights.domain.model

import com.google.android.gms.maps.model.CameraPosition

data class MapViewState(
    val cameraPosition: CameraPosition,
    val isFollowingUser: Boolean = true,
    val mapType: Int = 1,  // MapType.NORMAL
    val isDarkMode: Boolean = false
)
```

Create `domain/model/PolylineData.kt`:
```kotlin
package com.example.bikeredlights.domain.model

import androidx.compose.ui.graphics.Color
import com.google.android.gms.maps.model.LatLng

data class PolylineData(
    val points: List<LatLng>,
    val color: Color,
    val width: Float = 10f,
    val geodesic: Boolean = true
)
```

Create `domain/model/MarkerData.kt`:
```kotlin
package com.example.bikeredlights.domain.model

import com.google.android.gms.maps.model.LatLng

enum class MarkerType {
    START, END, CURRENT
}

data class MarkerData(
    val position: LatLng,
    val type: MarkerType,
    val title: String = "",
    val snippet: String = "",
    val visible: Boolean = true
)
```

Create `domain/model/MapBounds.kt`:
```kotlin
package com.example.bikeredlights.domain.model

import com.google.android.gms.maps.model.LatLngBounds

data class MapBounds(
    val bounds: LatLngBounds,
    val padding: Int = 100,
    val animationDurationMs: Int = 1000
)
```

### Task 1.3: Create Extension Functions

Create `domain/util/TrackPointExtensions.kt`:
```kotlin
package com.example.bikeredlights.domain.util

import com.example.bikeredlights.data.local.entity.TrackPoint
import com.google.android.gms.maps.model.LatLng

fun List<TrackPoint>.toLatLngList(): List<LatLng> {
    return map { LatLng(it.latitude, it.longitude) }
}

fun List<TrackPoint>.startLocation(): LatLng? {
    return firstOrNull()?.let { LatLng(it.latitude, it.longitude) }
}

fun List<TrackPoint>.endLocation(): LatLng? {
    return lastOrNull()?.let { LatLng(it.latitude, it.longitude) }
}
```

Create `domain/util/LatLngExtensions.kt`:
```kotlin
package com.example.bikeredlights.domain.util

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil

fun List<LatLng>.simplifyRoute(toleranceMeters: Double = 10.0): List<LatLng> {
    if (size <= 2) return this  // Can't simplify < 3 points

    // Convert meters to degrees (approximate)
    val toleranceDegrees = toleranceMeters / 111000.0

    return PolyUtil.simplify(this, toleranceDegrees)
}
```

### Task 1.4: Create Use Cases

Create `domain/usecase/GetRoutePolylineUseCase.kt`:
```kotlin
package com.example.bikeredlights.domain.usecase

import androidx.compose.ui.graphics.Color
import com.example.bikeredlights.data.local.entity.TrackPoint
import com.example.bikeredlights.domain.model.PolylineData
import com.example.bikeredlights.domain.util.simplifyRoute
import com.example.bikeredlights.domain.util.toLatLngList
import javax.inject.Inject

class GetRoutePolylineUseCase @Inject constructor() {
    suspend operator fun invoke(
        trackPoints: List<TrackPoint>,
        toleranceMeters: Double = 10.0,
        color: Color = Color.Red
    ): PolylineData? {
        if (trackPoints.isEmpty()) return null

        val latLngPoints = trackPoints.toLatLngList()
        val simplified = if (latLngPoints.size > 2) {
            latLngPoints.simplifyRoute(toleranceMeters)
        } else {
            latLngPoints
        }

        return PolylineData(
            points = simplified,
            color = color,
            width = 10f,
            geodesic = true
        )
    }
}
```

(Similar implementations for `CalculateMapBoundsUseCase`, `FormatMapMarkersUseCase`)

### Task 1.5: Create BikeMap Composable

Create `ui/components/map/BikeMap.kt`:
```kotlin
package com.example.bikeredlights.ui.components.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*

@Composable
fun BikeMap(
    modifier: Modifier = Modifier,
    cameraPositionState: CameraPositionState = rememberCameraPositionState(),
    onMapClick: ((LatLng) -> Unit)? = null,
    content: (@Composable @GoogleMapComposable () -> Unit)? = null
) {
    val isDarkTheme = isSystemInDarkTheme()

    GoogleMap(
        modifier = modifier,
        cameraPositionState = cameraPositionState,
        mapColorScheme = if (isDarkTheme) {
            MapColorScheme.DARK
        } else {
            MapColorScheme.LIGHT
        },
        properties = MapProperties(
            mapType = MapType.NORMAL,
            isBuildingEnabled = true,
            isTrafficEnabled = false
        ),
        uiSettings = MapUiSettings(
            compassEnabled = true,
            mapToolbarEnabled = true,
            zoomControlsEnabled = true
        ),
        onMapClick = onMapClick
    ) {
        content?.invoke()
    }
}
```

### Task 1.6: Test Basic Map

Add to `LiveRideScreen.kt` temporarily:
```kotlin
import com.example.bikeredlights.ui.components.map.BikeMap

BikeMap(
    modifier = Modifier.fillMaxSize()
)
```

Build and run:
```bash
./gradlew assembleDebug
./gradlew installDebug
```

Verify map displays without errors.

---

## Phase 2: Live Tab Route Polyline (1 day)

### Task 2.1: Create RoutePolyline Composable

Create `ui/components/map/RoutePolyline.kt`:
```kotlin
package com.example.bikeredlights.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bikeredlights.domain.model.PolylineData
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Polyline

@Composable
@GoogleMapComposable
fun RoutePolyline(
    polylineData: PolylineData?,
    modifier: Modifier = Modifier
) {
    polylineData?.let {
        Polyline(
            points = it.points,
            color = it.color,
            width = it.width,
            geodesic = it.geodesic
        )
    }
}
```

### Task 2.2: Update RideRecordingViewModel

Add to `RideRecordingViewModel.kt`:
```kotlin
import com.example.bikeredlights.domain.model.PolylineData
import com.example.bikeredlights.domain.usecase.GetRoutePolylineUseCase

@HiltViewModel
class RideRecordingViewModel @Inject constructor(
    // ... existing dependencies
    private val getRoutePolylineUseCase: GetRoutePolylineUseCase
) : ViewModel() {

    // Expose current ride track points
    val currentRideTrackPoints: StateFlow<List<TrackPoint>> =
        trackPointRepository.getCurrentRideTrackPoints()
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    // Expose polyline data
    val polylineData: StateFlow<PolylineData?> =
        currentRideTrackPoints
            .map { trackPoints ->
                getRoutePolylineUseCase(
                    trackPoints = trackPoints,
                    color = Color.Red
                )
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = null
            )
}
```

### Task 2.3: Update LiveRideScreen

Update `LiveRideScreen.kt`:
```kotlin
@Composable
fun LiveRideScreen(
    viewModel: RideRecordingViewModel = hiltViewModel()
) {
    val userLocation by viewModel.userLocation.collectAsStateWithLifecycle()
    val polylineData by viewModel.polylineData.collectAsStateWithLifecycle()

    val cameraPositionState = rememberCameraPositionState()

    // Follow user location
    LaunchedEffect(userLocation) {
        userLocation?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newCameraPosition(
                    CameraPosition.fromLatLngZoom(it, 17f)
                ),
                durationMs = 500
            )
        }
    }

    BikeMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        RoutePolyline(polylineData = polylineData)
        LocationMarker(location = userLocation)
    }
}
```

### Task 2.4: Test Real-Time Polyline

```bash
./gradlew assembleDebug
./gradlew installDebug
# Use GPS simulation on emulator
```

Verify polyline grows as ride progresses.

---

## Phase 3: Review Screen Complete Route (1 day)

### Task 3.1: Create StartEndMarkers Composable

Create `ui/components/map/StartEndMarkers.kt`:
```kotlin
package com.example.bikeredlights.ui.components.map

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bikeredlights.domain.model.MarkerData
import com.example.bikeredlights.domain.model.MarkerType
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.maps.android.compose.GoogleMapComposable
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState

@Composable
@GoogleMapComposable
fun StartEndMarkers(
    markers: List<MarkerData>,
    modifier: Modifier = Modifier
) {
    markers.forEach { markerData ->
        if (markerData.visible) {
            Marker(
                state = MarkerState(position = markerData.position),
                title = markerData.title,
                snippet = markerData.snippet,
                icon = markerData.type.toIcon()
            )
        }
    }
}

fun MarkerType.toIcon() = when (this) {
    MarkerType.START -> BitmapDescriptorFactory.defaultMarker(
        BitmapDescriptorFactory.HUE_GREEN
    )
    MarkerType.END -> BitmapDescriptorFactory.defaultMarker(
        BitmapDescriptorFactory.HUE_RED
    )
    MarkerType.CURRENT -> BitmapDescriptorFactory.defaultMarker(
        BitmapDescriptorFactory.HUE_BLUE
    )
}
```

### Task 3.2: Create RideDetailViewModel

Create `ui/viewmodel/RideDetailViewModel.kt`:
```kotlin
package com.example.bikeredlights.ui.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class RideDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rideRepository: RideRepository,
    private val getRoutePolylineUseCase: GetRoutePolylineUseCase,
    private val calculateMapBoundsUseCase: CalculateMapBoundsUseCase,
    private val formatMapMarkersUseCase: FormatMapMarkersUseCase
) : ViewModel() {

    private val rideId: Long = savedStateHandle["rideId"] ?: 0L

    val trackPoints: StateFlow<List<TrackPoint>> = flow {
        val points = rideRepository.getTrackPointsForRide(rideId)
        emit(points)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val polylineData: StateFlow<PolylineData?> =
        trackPoints.map { points ->
            getRoutePolylineUseCase(points)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val mapBounds: StateFlow<MapBounds?> =
        trackPoints.map { points ->
            calculateMapBoundsUseCase(points)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    val markers: StateFlow<List<MarkerData>> =
        trackPoints.map { points ->
            formatMapMarkersUseCase(points)
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}
```

### Task 3.3: Update RideDetailScreen

Update `RideDetailScreen.kt`:
```kotlin
@Composable
fun RideDetailScreen(
    rideId: Long,
    viewModel: RideDetailViewModel = hiltViewModel()
) {
    val polylineData by viewModel.polylineData.collectAsStateWithLifecycle()
    val mapBounds by viewModel.mapBounds.collectAsStateWithLifecycle()
    val markers by viewModel.markers.collectAsStateWithLifecycle()

    val cameraPositionState = rememberCameraPositionState()

    // Auto-zoom to fit route
    LaunchedEffect(mapBounds) {
        mapBounds?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(
                    it.bounds,
                    it.padding
                ),
                durationMs = it.animationDurationMs
            )
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BikeMap(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            cameraPositionState = cameraPositionState
        ) {
            RoutePolyline(polylineData = polylineData)
            StartEndMarkers(markers = markers)
        }

        // Statistics panel below map
        RideStatisticsPanel(ride = ride)
    }
}
```

### Task 3.4: Test Complete Route Display

```bash
./gradlew assembleDebug
./gradlew installDebug
```

Navigate to a saved ride and verify:
- Complete route displays
- Start (green) and end (red) markers visible
- Auto-zoom fits entire route

---

## Phase 4: Testing & Optimization (1 day)

### Task 4.1: Unit Tests

Create `domain/usecase/GetRoutePolylineUseCaseTest.kt`:
```kotlin
class GetRoutePolylineUseCaseTest {
    private lateinit var useCase: GetRoutePolylineUseCase

    @Before
    fun setup() {
        useCase = GetRoutePolylineUseCase()
    }

    @Test
    fun `returns null when track points is empty`() = runTest {
        val result = useCase(emptyList())
        assertThat(result).isNull()
    }

    @Test
    fun `converts track points to LatLng list`() = runTest {
        val trackPoints = listOf(
            TrackPoint(
                id = 1, rideId = 1,
                latitude = 37.4419, longitude = -122.1430,
                timestamp = 0L, speed = 0f, accuracy = 10f,
                isPaused = false, isManualPause = false, createdAt = 0L
            )
        )

        val result = useCase(trackPoints)

        assertThat(result).isNotNull()
        assertThat(result!!.points).hasSize(1)
        assertThat(result.points[0].latitude).isEqualTo(37.4419)
        assertThat(result.points[0].longitude).isEqualTo(-122.1430)
    }
}
```

(Similar tests for other use cases)

### Task 4.2: Instrumented Tests

Create `ui/components/map/BikeMapTest.kt`:
```kotlin
@RunWith(AndroidJUnit4::class)
class BikeMapTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun mapRendersSuccessfully() {
        composeTestRule.setContent {
            BikeMap(modifier = Modifier.fillMaxSize())
        }

        // Verify map container exists
        composeTestRule.onNodeWithTag("BikeMap").assertExists()
    }
}
```

### Task 4.3: Emulator Testing with GPS Simulation

1. Start emulator: `emulator -avd Pixel_9_API_34`
2. Open emulator Extended Controls (...)
3. Navigate to **Location** tab
4. Load GPX file with cycling route OR
5. Manually set lat/long and send:
   ```bash
   adb emu geo fix -122.084 37.422
   ```
6. Record test ride and verify:
   - Map follows location
   - Polyline grows
   - Route saves correctly

### Task 4.4: Physical Device Testing

1. Build release APK: `./gradlew assembleRelease`
2. Install on device
3. Take real bike ride
4. Verify:
   - Smooth camera following (60fps)
   - Polyline rendering without jank
   - Accurate route visualization
   - Dark mode works correctly

---

## Commit Strategy

Follow small, frequent commits per CLAUDE.md:

```bash
# After Phase 1 Task 1
git add app/build.gradle.kts
git commit -m "feat(deps): add Google Maps Compose and android-maps-utils dependencies"

# After Phase 1 Task 2
git add app/src/main/java/com/example/bikeredlights/domain/model/
git commit -m "feat(domain): add map-related domain models (MapViewState, PolylineData, MarkerData, MapBounds)"

# After Phase 1 Task 3
git add app/src/main/java/com/example/bikeredlights/domain/util/
git commit -m "feat(domain): add TrackPoint and LatLng extension functions for map integration"

# After Phase 1 Task 4
git add app/src/main/java/com/example/bikeredlights/domain/usecase/GetRoutePolylineUseCase.kt
git commit -m "feat(domain): add GetRoutePolylineUseCase for polyline simplification"

# After Phase 1 Task 5
git add app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt
git commit -m "feat(ui): create BikeMap composable with Material 3 theming"

# Continue with similar commits for each task...
```

---

## Verification Checklist

**Phase 0:**
- [ ] Google Cloud project created
- [ ] Maps SDK for Android API enabled
- [ ] API key generated and restricted
- [ ] API key stored in secrets.properties
- [ ] Map displays on emulator without errors

**Phase 1:**
- [ ] Dependencies added and synced
- [ ] Domain models created
- [ ] Extension functions implemented
- [ ] Use cases implemented
- [ ] BikeMap composable created
- [ ] Basic map displays correctly

**Phase 2:**
- [ ] RoutePolyline composable created
- [ ] RideRecordingViewModel updated
- [ ] LiveRideScreen integrated
- [ ] Real-time polyline grows during recording
- [ ] Camera follows user location smoothly

**Phase 3:**
- [ ] StartEndMarkers composable created
- [ ] RideDetailViewModel created
- [ ] RideDetailScreen updated
- [ ] Complete route displays with markers
- [ ] Auto-zoom fits entire route

**Phase 4:**
- [ ] Unit tests pass (all use cases)
- [ ] Instrumented tests pass
- [ ] Emulator testing with GPS simulation successful
- [ ] Physical device testing successful
- [ ] 60fps performance validated
- [ ] Dark mode works correctly

---

## Troubleshooting

**Map doesn't display (gray screen)**:
- Verify API key is correct in secrets.properties
- Check AndroidManifest.xml has meta-data tag
- Verify API key restrictions (package name + SHA-1)
- Check logcat for API key errors

**Polyline rendering slow**:
- Verify simplification is applied (check use case)
- Check tolerance value (should be 10m)
- Profile with Android Studio Profiler

**Camera doesn't follow location**:
- Verify StateFlow is emitting updates
- Check LaunchedEffect key includes userLocation
- Verify animate() is called (not move())

**Dark mode not working**:
- Check `isSystemInDarkTheme()` is called
- Verify `mapColorScheme` parameter is set
- Test on device with dark mode enabled

---

## Next Steps After Completion

1. Create pull request with detailed description
2. Request code review
3. Run all tests on CI/CD
4. Merge to main
5. Update version in `app/build.gradle.kts` to v0.5.0
6. Tag release: `git tag -a v0.5.0 -m "Release v0.5.0: Maps Integration"`
7. Push tag: `git push origin v0.5.0`
8. Build signed release APK
9. Create GitHub release with APK attached
10. Update TODO.md and RELEASE.md

---

**Total Estimated Time**: 3-4 days (including testing and refinement)
