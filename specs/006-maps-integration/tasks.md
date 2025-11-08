# Implementation Tasks: Maps Integration

**Feature**: 006-maps-integration
**Target Release**: v0.5.0
**Total Tasks**: 19
**Estimated Effort**: 3-4 days

---

## Task Breakdown by Phase

### 📦 Phase 1: Setup & Configuration (3 tasks, ~3-4 hours)

#### T001: Interactive Google Cloud Console Setup
**Type**: Setup | **Priority**: P0-Critical | **Estimate**: 1.5 hours

**Description**: Guide user through Google Cloud Console to enable Maps SDK and create API key

**Steps**:
1. Navigate to [Google Cloud Console](https://console.cloud.google.com/)
2. Create new project or select existing "BikeRedlights" project
3. Enable "Maps SDK for Android" API:
   - Go to "APIs & Services" > "Library"
   - Search "Maps SDK for Android"
   - Click "Enable"
4. Create API key:
   - Go to "APIs & Services" > "Credentials"
   - Click "Create Credentials" > "API Key"
   - Edit API key → Add restriction:
     - Application restrictions: Android apps
     - Package name: `com.example.bikeredlights`
     - SHA-1 certificate fingerprint: (from keystore)
5. Set up billing:
   - Go to "Billing"
   - Link billing account (free tier: 28,000 map loads/month)
   - Verify billing is active
6. Copy API key for next task

**Interactive Guidance** (will provide to user):
- Screenshots of each step
- Explanation of why each restriction is important
- How to find SHA-1 fingerprint: `keytool -list -v -keystore ~/.android/debug.keystore`

**Acceptance Criteria**:
- [ ] Maps SDK for Android API enabled in project
- [ ] API key created with Android restriction
- [ ] API key restricted to package name
- [ ] Billing account linked
- [ ] API key copied to clipboard

---

#### T002: Project Configuration
**Type**: Setup | **Priority**: P0-Critical | **Estimate**: 1 hour

**Description**: Add Google Maps dependencies and configure API key in project

**Files to Modify**:
1. `gradle/libs.versions.toml`
2. `app/build.gradle.kts`
3. `local.properties` (create if doesn't exist)
4. `app/src/main/AndroidManifest.xml`

**Implementation**:

**Step 1**: Add to `gradle/libs.versions.toml`:
```toml
[versions]
# ... existing versions
playServicesMaps = "19.0.0"
mapsCompose = "6.2.0"

[libraries]
# ... existing libraries
play-services-maps = { group = "com.google.android.gms", module = "play-services-maps", version.ref = "playServicesMaps" }
maps-compose = { group = "com.google.maps.android", module = "maps-compose", version.ref = "mapsCompose" }
maps-compose-utils = { group = "com.google.maps.android", module = "maps-compose-utils", version.ref = "mapsCompose" }
```

**Step 2**: Add to `app/build.gradle.kts` dependencies:
```kotlin
dependencies {
    // ... existing dependencies

    // Google Maps
    implementation(libs.play.services.maps)
    implementation(libs.maps.compose)
    implementation(libs.maps.compose.utils)
}
```

**Step 3**: Add API key to `local.properties`:
```properties
# Google Maps API Key (do not commit to Git)
MAPS_API_KEY=AIzaSy...  # Paste key from T001
```

**Step 4**: Add to `app/build.gradle.kts` defaultConfig:
```kotlin
android {
    defaultConfig {
        // ... existing config

        // Load Maps API key from local.properties
        val properties = Properties()
        properties.load(project.rootProject.file("local.properties").inputStream())
        manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY", "")
    }
}
```

**Step 5**: Add to `app/src/main/AndroidManifest.xml` inside `<application>`:
```xml
<application>
    <!-- ... existing content ... -->

    <!-- Google Maps API Key -->
    <meta-data
        android:name="com.google.android.geo.API_KEY"
        android:value="${MAPS_API_KEY}" />
</application>
```

**Step 6**: Gradle sync
```bash
./gradlew --refresh-dependencies
```

**Acceptance Criteria**:
- [ ] Dependencies added to libs.versions.toml
- [ ] Dependencies added to build.gradle.kts
- [ ] API key added to local.properties
- [ ] Manifest meta-data added
- [ ] Gradle sync successful
- [ ] Build succeeds: `./gradlew assembleDebug`

---

#### T003: ProGuard Configuration
**Type**: Setup | **Priority**: P1-High | **Estimate**: 30 minutes

**Description**: Add ProGuard rules for Maps SDK to prevent obfuscation errors in release builds

**Files to Modify**:
- `app/proguard-rules.pro`

**Implementation**:

Add to `app/proguard-rules.pro`:
```proguard
##---------------Begin: proguard configuration for Google Maps SDK  ----------
# Keep Google Maps classes
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.** { *; }

# Prevent warnings
-dontwarn com.google.android.gms.maps.**
-dontwarn com.google.maps.android.**

# Keep Maps Compose library
-keep class com.google.maps.android.compose.** { *; }
-dontwarn com.google.maps.android.compose.**

# Keep LatLng and related classes
-keep class com.google.android.gms.maps.model.** { *; }

##---------------End: proguard configuration for Google Maps SDK  ------------
```

**Testing**:
```bash
# Build release APK
./gradlew assembleRelease

# Check for ProGuard warnings in build output
# Should see no warnings related to Maps SDK
```

**Acceptance Criteria**:
- [ ] ProGuard rules added
- [ ] Release build compiles without errors
- [ ] No Maps SDK warnings in build output
- [ ] APK size increase reasonable (< 5MB for Maps SDK)

---

### 🗺️ Phase 2: Core Map Components (4 tasks, ~6-8 hours)

#### T004: Create BikeMap Composable
**Type**: UI Component | **Priority**: P0-Critical | **Estimate**: 2.5 hours

**Description**: Create reusable GoogleMap composable used in both Live and Review screens

**Files to Create**:
- `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt`

**Component Signature**:
```kotlin
@Composable
fun BikeMap(
    currentLocation: LatLng?,
    trackPoints: List<LatLng>,
    showPolyline: Boolean = true,
    showCurrentLocationMarker: Boolean = true,
    startMarker: LatLng? = null,
    endMarker: LatLng? = null,
    cameraFollowsLocation: Boolean = false,
    gesturesEnabled: Boolean = true,
    onRecenterClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
)
```

**Implementation**:
```kotlin
package com.example.bikeredlights.ui.components.map

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.maps.android.compose.*

/**
 * Reusable map component for BikeRedlights.
 *
 * Displays Google Maps with optional polyline, markers, and location tracking.
 * Used in both Live tab (real-time tracking) and Review screen (historical routes).
 *
 * @param currentLocation Current GPS location (blue marker)
 * @param trackPoints List of GPS coordinates for route polyline
 * @param showPolyline Whether to draw route polyline
 * @param showCurrentLocationMarker Whether to show blue current location marker
 * @param startMarker Green pin at ride start (Review screen only)
 * @param endMarker Red flag at ride end (Review screen only)
 * @param cameraFollowsLocation True to auto-center on currentLocation
 * @param gesturesEnabled Allow zoom/pan (false during recording for safety)
 * @param onRecenterClick Callback for re-center FAB (null to hide FAB)
 */
@Composable
fun BikeMap(
    currentLocation: LatLng?,
    trackPoints: List<LatLng>,
    showPolyline: Boolean = true,
    showCurrentLocationMarker: Boolean = true,
    startMarker: LatLng? = null,
    endMarker: LatLng? = null,
    cameraFollowsLocation: Boolean = false,
    gesturesEnabled: Boolean = true,
    onRecenterClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    // Initial camera position
    val defaultLocation = currentLocation ?: trackPoints.firstOrNull() ?: LatLng(0.0, 0.0)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(defaultLocation, 15f)
    }

    // Update camera when location changes and following is enabled
    LaunchedEffect(currentLocation, cameraFollowsLocation) {
        if (cameraFollowsLocation && currentLocation != null) {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(currentLocation, 15f),
                durationMs = 1000
            )
        }
    }

    // Auto-zoom to fit route when not following location (Review screen)
    LaunchedEffect(trackPoints) {
        if (!cameraFollowsLocation && trackPoints.size >= 2) {
            val bounds = calculateBounds(trackPoints)
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngBounds(bounds, 100),
                durationMs = 1000
            )
        }
    }

    Box(modifier = modifier) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                zoomGesturesEnabled = gesturesEnabled,
                scrollGesturesEnabled = gesturesEnabled,
                tiltGesturesEnabled = false,
                rotateGesturesEnabled = false,
                myLocationButtonEnabled = false
            ),
            properties = MapProperties(
                mapType = MapType.NORMAL,
                isMyLocationEnabled = false,
                mapStyleOptions = if (isDarkTheme) {
                    MapStyleOptions("[]")  // Dark mode style
                } else {
                    null
                }
            )
        ) {
            // Polyline showing route
            if (showPolyline && trackPoints.isNotEmpty()) {
                Polyline(
                    points = trackPoints,
                    color = MaterialTheme.colorScheme.primary,
                    width = 8f,
                    clickable = false
                )
            }

            // Current location marker (blue dot)
            if (showCurrentLocationMarker && currentLocation != null) {
                Marker(
                    state = MarkerState(position = currentLocation),
                    title = "Current Location",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_AZURE)
                )
            }

            // Start marker (green pin)
            if (startMarker != null) {
                Marker(
                    state = MarkerState(position = startMarker),
                    title = "Start",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_GREEN)
                )
            }

            // End marker (red flag)
            if (endMarker != null) {
                Marker(
                    state = MarkerState(position = endMarker),
                    title = "End",
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                )
            }
        }

        // Re-center FAB (bottom-right)
        if (onRecenterClick != null) {
            FloatingActionButton(
                onClick = onRecenterClick,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = MaterialTheme.colorScheme.primaryContainer
            ) {
                Icon(
                    imageVector = Icons.Default.MyLocation,
                    contentDescription = "Re-center map on current location"
                )
            }
        }
    }
}

/**
 * Calculate LatLngBounds from list of track points
 */
private fun calculateBounds(points: List<LatLng>): LatLngBounds {
    val builder = LatLngBounds.Builder()
    points.forEach { builder.include(it) }
    return builder.build()
}
```

**Acceptance Criteria**:
- [ ] BikeMap composable created
- [ ] Compiles without errors
- [ ] Can preview in Android Studio with sample data
- [ ] Light mode displays correctly
- [ ] Dark mode displays correctly
- [ ] All parameters work as expected

---

#### T005: Create MapUtils Helper
**Type**: Utility | **Priority**: P2-Medium | **Estimate**: 1 hour

**Description**: Create utility functions for map bounds and zoom calculations

**Files to Create**:
- `app/src/main/java/com/example/bikeredlights/ui/components/map/MapUtils.kt`

**Implementation**:
```kotlin
package com.example.bikeredlights.ui.components.map

import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.example.bikeredlights.data.local.entity.TrackPoint

object MapUtils {
    /**
     * Convert TrackPoint entities to LatLng coordinates
     */
    fun trackPointsToLatLng(trackPoints: List<TrackPoint>): List<LatLng> {
        return trackPoints.map { LatLng(it.latitude, it.longitude) }
    }

    /**
     * Calculate bounding box from list of coordinates
     */
    fun calculateBounds(points: List<LatLng>): LatLngBounds? {
        if (points.isEmpty()) return null

        val builder = LatLngBounds.Builder()
        points.forEach { builder.include(it) }
        return builder.build()
    }

    /**
     * Calculate center point from list of coordinates
     */
    fun calculateCenter(points: List<LatLng>): LatLng? {
        if (points.isEmpty()) return null

        var lat = 0.0
        var lng = 0.0

        points.forEach {
            lat += it.latitude
            lng += it.longitude
        }

        return LatLng(lat / points.size, lng / points.size)
    }

    /**
     * Default zoom level for city block view (cycling)
     */
    const val DEFAULT_ZOOM = 15f

    /**
     * Padding for bounds (in pixels)
     */
    const val BOUNDS_PADDING = 100
}
```

**Acceptance Criteria**:
- [ ] MapUtils object created
- [ ] All utility functions implemented
- [ ] Functions handle empty lists gracefully
- [ ] Unit tests written (optional for v0.5.0)

---

#### T006-T007: Reserved for Future Map Enhancements
(Combined into T004 - no separate tasks needed for now)

---

### 🚴 Phase 3: Live Tab Integration (4 tasks, ~6-8 hours)

#### T008: Modify RideRecordingViewModel
**Type**: ViewModel | **Priority**: P0-Critical | **Estimate**: 2 hours

**Description**: Expose track points and current location as StateFlows for Live tab

**Files to Modify**:
- `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideRecordingViewModel.kt`

**Implementation**:

**Add track points StateFlow**:
```kotlin
// Add to RideRecordingViewModel class

private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

private val _currentLocation = MutableStateFlow<LatLng?>(null)
val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

init {
    // ... existing init code ...

    // Subscribe to track points for current ride
    viewModelScope.launch {
        rideRecordingStateRepository.getCurrentRideId().collectLatest { rideId ->
            if (rideId != null) {
                trackPointRepository.getTrackPointsForRide(rideId).collectLatest { points ->
                    _trackPoints.value = points

                    // Update current location from latest point
                    points.lastOrNull()?.let {
                        _currentLocation.value = LatLng(it.latitude, it.longitude)
                    }
                }
            } else {
                _trackPoints.value = emptyList()
                _currentLocation.value = null
            }
        }
    }
}
```

**Acceptance Criteria**:
- [ ] trackPoints StateFlow added
- [ ] currentLocation StateFlow added
- [ ] StateFlows update when ride recording
- [ ] StateFlows reset when ride stops
- [ ] No memory leaks (verify with Android Profiler)
- [ ] Compiles without errors

---

#### T009: Modify LiveRideScreen Layout
**Type**: UI Screen | **Priority**: P0-Critical | **Estimate**: 3 hours

**Description**: Add map to Live tab occupying 50-60% of screen height

**Files to Modify**:
- `app/src/main/java/com/example/bikeredlights/ui/screens/ride/LiveRideScreen.kt`

**Current Layout** (v0.4.2):
```
Box (fills screen)
    └── Content based on state (centered)
```

**New Layout** (v0.5.0):
```
Column (fills screen)
    ├── GPS Status Indicator (top-end)
    ├── BikeMap (55% height)
    └── Statistics + Controls (45% height)
```

**Implementation**:

Replace main content in `LiveRideScreen`:
```kotlin
@Composable
fun LiveRideScreen(
    viewModel: RideRecordingViewModel = hiltViewModel(),
    onNavigateToReview: (Long) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val unitsSystem by viewModel.unitsSystem.collectAsStateWithLifecycle()
    val currentSpeed by viewModel.currentSpeed.collectAsStateWithLifecycle()
    val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()  // NEW
    val currentLocation by viewModel.currentLocation.collectAsStateWithLifecycle()  // NEW

    // ... existing code (navigation, GPS pre-warming, keep screen on, save dialog) ...

    // Main content with map
    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // GPS Status Indicator (moved to column)
        Box(
            modifier = Modifier.fillMaxWidth()
        ) {
            GpsStatusIndicator(
                uiState = uiState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 16.dp, end = 16.dp)
            )
        }

        // Map Section (NEW - 55% height)
        val isRecording = uiState is RideRecordingUiState.Recording ||
                         uiState is RideRecordingUiState.Paused ||
                         uiState is RideRecordingUiState.AutoPaused

        val latLngPoints = remember(trackPoints) {
            MapUtils.trackPointsToLatLng(trackPoints)
        }

        BikeMap(
            currentLocation = currentLocation,
            trackPoints = latLngPoints,
            showPolyline = isRecording,
            showCurrentLocationMarker = true,
            cameraFollowsLocation = isRecording,
            gesturesEnabled = !isRecording,  // Lock during recording
            onRecenterClick = if (isRecording) {
                { /* Re-center logic */ }
            } else null,
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.55f)  // 55% of screen
        )

        // Statistics + Controls Section (45% height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()  // Remaining 45%
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            when (uiState) {
                is RideRecordingUiState.Idle -> {
                    IdleContent(onStartRide = { viewModel.startRide() })
                }
                is RideRecordingUiState.WaitingForGps -> {
                    WaitingForGpsContent()
                }
                is RideRecordingUiState.Recording -> {
                    val ride = (uiState as RideRecordingUiState.Recording).ride
                    RecordingContent(
                        ride = ride,
                        currentSpeed = currentSpeed,
                        unitsSystem = unitsSystem,
                        onPauseRide = { viewModel.pauseRide() },
                        onStopRide = { viewModel.stopRide() }
                    )
                }
                // ... other states ...
            }
        }
    }
}
```

**Modify RecordingContent, PausedContent, AutoPausedContent**:
- Remove Column wrapper (now inside Box)
- Remove Spacer(weight = 1f) elements
- Adjust vertical arrangement

**Acceptance Criteria**:
- [ ] Map displays in all states (Idle, Recording, Paused, AutoPaused)
- [ ] Map occupies 50-60% of screen height
- [ ] Statistics occupy 40-50% of screen height
- [ ] Layout responsive to different screen sizes
- [ ] GPS indicator remains visible
- [ ] No layout clipping or overflow

---

#### T010: Implement Map Following and Gestures
**Type**: Feature | **Priority**: P1-High | **Estimate**: 1.5 hours

**Description**: Implement camera following behavior and gesture locking

**Files to Modify**:
- Already handled in BikeMap component (T004)

**Additional Implementation**:

Add re-center logic in LiveRideScreen:
```kotlin
var mapManuallyMoved by remember { mutableStateOf(false) }

BikeMap(
    // ... other params ...
    cameraFollowsLocation = isRecording && !mapManuallyMoved,
    onRecenterClick = if (isRecording) {
        {
            mapManuallyMoved = false  // Reset flag, resume following
        }
    } else null,
    // ... rest of params ...
)
```

**Gesture Locking Logic**:
- When `isRecording = true` → `gesturesEnabled = false`
- When paused → `gesturesEnabled = true`
- When idle → `gesturesEnabled = true`

**Acceptance Criteria**:
- [ ] Camera follows user location during recording
- [ ] Camera animations smooth (not jarring)
- [ ] Zoom/pan locked during active recording
- [ ] Re-center FAB appears during recording
- [ ] Re-center FAB hidden when idle
- [ ] Tapping re-center resumes camera following

---

#### T011: Test Live Map on Emulator
**Type**: Testing | **Priority**: P0-Critical | **Estimate**: 2 hours

**Description**: Comprehensive emulator testing of Live tab maps

**Testing Checklist**:

**Basic Functionality**:
- [ ] Map loads in Idle state showing current location
- [ ] Map loads in Recording state
- [ ] Polyline appears when ride starts
- [ ] Polyline grows as GPS simulation progresses
- [ ] Current location marker (blue) visible
- [ ] No start/end markers on Live tab

**Camera Behavior**:
- [ ] Camera centers on location when recording starts
- [ ] Camera follows smoothly as location changes
- [ ] Camera animations smooth (1 second duration)
- [ ] No jittering or jumping

**Gesture Locking**:
- [ ] Cannot zoom during active recording
- [ ] Cannot pan during active recording
- [ ] Can zoom when paused
- [ ] Can pan when paused
- [ ] Can zoom when idle

**Re-center FAB**:
- [ ] FAB appears during recording
- [ ] FAB hidden when idle
- [ ] FAB click re-centers map
- [ ] FAB has 48dp touch target

**State Transitions**:
- [ ] Idle → Recording: Map starts following
- [ ] Recording → Paused: Gestures enabled
- [ ] Paused → Recording: Gestures locked again
- [ ] Recording → Idle: Polyline clears

**Edge Cases**:
- [ ] Screen rotation preserves map state
- [ ] App backgrounding doesn't break map
- [ ] Dark mode map style loads correctly
- [ ] No crashes or ANR events

**GPS Simulation Setup**:
```bash
# Create GPX file with route
# Load in emulator Extended Controls → Location
# Set playback speed to 10 km/h
# Start simulation
```

**Acceptance Criteria**:
- [ ] All checklist items passing
- [ ] No crashes during 10-minute ride simulation
- [ ] Memory usage stable (< 150MB total)
- [ ] Frame rate 60fps maintained

---

### 📊 Phase 4: Review Screen Integration (3 tasks, ~3-4 hours)

#### T012: Modify RideReviewViewModel
**Type**: ViewModel | **Priority**: P0-Critical | **Estimate**: 1 hour

**Description**: Expose track points StateFlow for selected ride

**Files to Modify**:
- `app/src/main/java/com/example/bikeredlights/ui/viewmodel/RideReviewViewModel.kt`

**Implementation**:

```kotlin
// Add to RideReviewViewModel class

private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

fun loadRide(rideId: Long) {
    viewModelScope.launch {
        try {
            _uiState.value = RideReviewUiState.Loading

            val ride = getRideByIdUseCase(rideId)
            if (ride != null) {
                _uiState.value = RideReviewUiState.Success(ride)

                // Load track points for this ride
                trackPointRepository.getTrackPointsForRide(rideId).collectLatest { points ->
                    _trackPoints.value = points
                }
            } else {
                _uiState.value = RideReviewUiState.Error("Ride not found")
            }
        } catch (e: Exception) {
            _uiState.value = RideReviewUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

**Acceptance Criteria**:
- [ ] trackPoints StateFlow added
- [ ] StateFlow updates when ride loads
- [ ] StateFlow resets when different ride loaded
- [ ] Compiles without errors

---

#### T013: Replace MapPlaceholder in RideReviewScreen
**Type**: UI Screen | **Priority**: P0-Critical | **Estimate**: 2 hours

**Description**: Replace placeholder with actual map showing complete route

**Files to Modify**:
- `app/src/main/java/com/example/bikeredlights/ui/screens/ride/RideReviewScreen.kt`

**Current Code** (v0.4.2):
```kotlin
// Map placeholder
MapPlaceholder(
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
)
```

**New Code** (v0.5.0):
```kotlin
// Map with complete route
val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
val latLngPoints = remember(trackPoints) {
    MapUtils.trackPointsToLatLng(trackPoints)
}

if (latLngPoints.size >= 2) {
    BikeMap(
        currentLocation = null,  // Not needed for review
        trackPoints = latLngPoints,
        showPolyline = true,
        showCurrentLocationMarker = false,
        startMarker = latLngPoints.firstOrNull(),
        endMarker = latLngPoints.lastOrNull(),
        cameraFollowsLocation = false,
        gesturesEnabled = true,  // Allow exploration
        onRecenterClick = null,  // No FAB on review screen
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
} else {
    // Fallback for rides with poor GPS
    NoGpsDataCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}

@Composable
private fun NoGpsDataCard(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "No GPS data available",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "This ride has insufficient GPS coordinates to display a route",
                    style = MaterialTheme.typography.bodySmall,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

**Delete MapPlaceholder composable** (no longer needed)

**Acceptance Criteria**:
- [ ] MapPlaceholder replaced with BikeMap
- [ ] Map height increased to 300dp
- [ ] Fallback card shows for rides with < 2 points
- [ ] Compiles without errors

---

#### T014: Test Review Map on Emulator
**Type**: Testing | **Priority**: P0-Critical | **Estimate**: 1 hour

**Description**: Test Review screen map with completed rides

**Testing Checklist**:

**Basic Functionality**:
- [ ] Map loads when opening ride from history
- [ ] Complete route polyline displays
- [ ] Start marker (green pin) at first point
- [ ] End marker (red flag) at last point
- [ ] No current location marker
- [ ] No re-center FAB

**Auto-Zoom**:
- [ ] Map zooms to fit entire route
- [ ] 100px padding around bounds
- [ ] Works for short rides (< 1 km)
- [ ] Works for long rides (> 20 km)
- [ ] Works for narrow routes (straight line)
- [ ] Works for wide routes (circular)

**Gestures**:
- [ ] Can zoom in/out freely
- [ ] Can pan around map
- [ ] Pinch-to-zoom works
- [ ] Double-tap to zoom works

**Edge Cases**:
- [ ] Rides with 0 points show fallback
- [ ] Rides with 1 point show fallback
- [ ] Rides with 1000+ points render smoothly
- [ ] Polyline color matches theme
- [ ] Dark mode styling correct

**Data Scenarios**:
```bash
# Use existing rides from Feature 003
# Test with:
# - Short ride (< 1 km, < 100 points)
# - Medium ride (5-10 km, 500 points)
# - Long ride (> 20 km, 1000+ points)
# - Poor GPS ride (< 2 valid points)
```

**Acceptance Criteria**:
- [ ] All checklist items passing
- [ ] No crashes when loading any ride
- [ ] Map renders within 2 seconds
- [ ] No lag when zooming/panning

---

### ✅ Phase 5: Testing & Documentation (1 task, ~4-6 hours)

#### T015: Comprehensive Testing & Documentation
**Type**: Testing + Documentation | **Priority**: P0-Critical | **Estimate**: 4-6 hours

**Description**: Final validation, physical device testing, documentation updates

**Sub-tasks**:

**1. Emulator Regression Testing** (1 hour):
- [ ] Run through all T011 Live tab tests again
- [ ] Run through all T014 Review screen tests again
- [ ] Test all Feature 1A scenarios still work (recording, pause, save)
- [ ] Test Feature 003 navigation (History → Review with maps)
- [ ] Test screen rotations
- [ ] Test dark mode toggle
- [ ] Test with different GPS accuracy settings

**2. Physical Device Testing** (2-3 hours) - MANDATORY:
- [ ] Install debug APK on physical device
- [ ] Real bike ride (15-30 minutes minimum)
- [ ] Verify map loads and tracks accurately
- [ ] Verify polyline draws smoothly
- [ ] Measure battery drain (compare to v0.4.2 baseline)
- [ ] Test offline behavior (airplane mode after tiles cached)
- [ ] Test cellular data usage (< 5MB per hour)
- [ ] Verify no crashes or ANR events
- [ ] Check memory usage with Android Profiler

**3. Performance Validation** (1 hour):
- [ ] Map initial load time: < 2 seconds on Wi-Fi
- [ ] Polyline with 1000 points renders without lag
- [ ] Memory usage < 100MB additional vs. v0.4.2
- [ ] Battery drain < 5% additional per hour recording
- [ ] 60fps maintained during map interactions
- [ ] No memory leaks (Android Profiler)

**4. Accessibility Testing** (30 minutes):
- [ ] BikeMap has semantic contentDescription
- [ ] Re-center FAB has 48dp touch target
- [ ] TalkBack reads map elements correctly
- [ ] High contrast mode supported
- [ ] Font scaling works (up to 200%)

**5. Documentation Updates** (1 hour):

**Update TODO.md**:
```markdown
### Feature 006: Maps Integration
- **Completed**: 2025-11-XX
- **Type**: P2 UX Enhancement
- **Description**: Google Maps visualization on Live tab and Review screens
- **Status**: ✅ COMPLETE - Released v0.5.0
- **Implementation Summary**:
  - Live tab: Real-time map with polyline, camera following, gesture locking
  - Review screen: Complete route with start/end markers, auto-zoom
  - Reusable BikeMap component
  - Material 3 theme integration (light/dark mode)
- **Testing**: Physical device testing completed
- **Pull Request**: #X
- **Release**: v0.5.0
- **Specification**: specs/006-maps-integration/spec.md
```

**Update RELEASE.md**:
```markdown
## v0.5.0 - Maps Integration (2025-11-XX)

### 🗺️ Maps Enhancement

**Status**: ✅ COMPLETE - Live location and route visualization
**Focus**: Add Google Maps to Live tab and Review screens
**APK Size**: TBD (release build)
**Tested On**: Physical device + Pixel 9 Pro emulator

### ✨ Features Added

- **Live Tab Map** (User Story 1):
  - Google Maps occupies 50-60% of screen height
  - Blue marker shows current location
  - Route polyline grows in real-time during recording
  - Camera auto-follows user location
  - Map gestures locked during active recording (safety)
  - Re-center FAB to reset camera position
  - City block zoom level (50-200m radius)

- **Review Screen Map** (User Story 2):
  - Complete route visualization with polyline
  - Green pin marker at ride start
  - Red flag marker at ride end
  - Auto-zoom to fit entire route
  - Gestures enabled for route exploration
  - Fallback message for rides with < 2 GPS points

- **Material 3 Integration**:
  - Polyline uses theme primary color
  - Light/dark mode map styles
  - Semantic marker colors (green/red/blue)
  - Dynamic color scheme support

### 🏗️ Architecture

**New Components**:
- `ui/components/map/BikeMap.kt` - Reusable map composable
- `ui/components/map/MapUtils.kt` - Map utility functions

**Modified Components**:
- `ui/screens/ride/LiveRideScreen.kt` - Added map section (55% height)
- `ui/screens/ride/RideReviewScreen.kt` - Replaced placeholder with map
- `ui/viewmodel/RideRecordingViewModel.kt` - Exposed track points StateFlow
- `ui/viewmodel/RideReviewViewModel.kt` - Exposed track points StateFlow

**Dependencies Added**:
- Google Maps SDK for Android v19.0.0
- Maps Compose v6.2.0

### 🔧 Technical Details
- **API**: Google Maps SDK for Android
- **Integration**: Maps Compose library for Jetpack Compose
- **Camera**: Auto-follow with smooth animations (1s duration)
- **Polyline**: Renders 1000+ track points without lag
- **Battery**: < 5% additional drain vs. v0.4.2

### 🔬 Testing
✅ **Emulator Testing**: GPS simulation, route playback, state transitions
✅ **Physical Device Testing**: Real bike ride, battery measurement, network usage
✅ **Performance**: Map load < 2s, 60fps maintained, no memory leaks

### 📦 Dependencies
- play-services-maps: 19.0.0
- maps-compose: 6.2.0
- maps-compose-utils: 6.2.0

### 💥 Breaking Changes
None. Backward compatible with v0.4.2.

### ⚙️ Setup Requirements
**User Action Required**: Google Maps API key must be configured
- Enable Maps SDK for Android in Google Cloud Console
- Create API key restricted to Android package name
- Add key to `local.properties`: `MAPS_API_KEY=...`
- See `specs/006-maps-integration/` for detailed setup guide
```

**Update CLAUDE.md** (add Maps setup section):
```markdown
### Google Maps SDK Setup (Feature 1B - v0.5.0+)

**Required for**: v0.5.0 and later releases

**One-Time Setup**:
1. Create Google Cloud project
2. Enable "Maps SDK for Android" API
3. Create API key with Android restriction
4. Add key to `local.properties`: `MAPS_API_KEY=your_key_here`
5. Restrict key to package name: `com.example.bikeredlights`

**Billing**: Free tier includes 28,000 map loads/month
**Documentation**: See `specs/006-maps-integration/spec.md`
```

**Acceptance Criteria**:
- [ ] All tests passing
- [ ] Physical device testing completed
- [ ] Documentation updated
- [ ] No open issues or blockers
- [ ] Ready for release

---

## Task Dependencies

```
T001 (Google Cloud Setup)
  ↓
T002 (Project Config)
  ↓
T003 (ProGuard)
  ↓
T004 (BikeMap Component)
  ├→ T005 (MapUtils)
  ├→ T008 (ViewModel - Live)
  └→ T012 (ViewModel - Review)
      ↓
T009 (LiveRideScreen) → T010 (Following) → T011 (Test Live)
      ↓
T013 (RideReviewScreen) → T014 (Test Review)
      ↓
T015 (Final Testing & Docs)
```

**Critical Path**: T001 → T002 → T003 → T004 → T008 → T009 → T011 → T015
**Estimated Duration**: 3-4 days

---

## Definition of Done

**Code Complete**:
- [ ] All 15 tasks (T001-T015) completed
- [ ] BikeMap composable fully functional
- [ ] Live tab shows real-time map
- [ ] Review screen shows complete route
- [ ] No compilation errors or warnings

**Testing Complete**:
- [ ] Emulator tests passing (T011, T014)
- [ ] Physical device testing completed (T015)
- [ ] Performance metrics validated
- [ ] Accessibility tests passing

**Documentation Complete**:
- [ ] spec.md finalized
- [ ] plan.md finalized
- [ ] tasks.md (this file) finalized
- [ ] TODO.md updated
- [ ] RELEASE.md updated
- [ ] CLAUDE.md updated

**Release Ready**:
- [ ] Version bumped to v0.5.0 (versionCode = 500)
- [ ] Release APK built and signed
- [ ] All commits follow conventional commits
- [ ] Feature branch merged to main
- [ ] Pull request created and approved
- [ ] GitHub release published with APK

---

**Created**: 2025-11-08 | **Last Updated**: 2025-11-08 | **Status**: Draft
