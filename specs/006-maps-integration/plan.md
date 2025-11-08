# Implementation Plan: Maps Integration

**Feature**: 006-maps-integration
**Target Release**: v0.5.0
**Estimated Effort**: 3-4 days
**Created**: 2025-11-08

---

## 🎯 Implementation Strategy

**Approach**: Incremental integration following Clean Architecture principles

**Phases**:
1. **Setup & Configuration** (Day 1): Google Maps SDK setup, dependencies, API key
2. **Core Map Components** (Day 1-2): Reusable BikeMap composable, polyline rendering
3. **Live Tab Integration** (Day 2-3): Add map to LiveRideScreen with real-time updates
4. **Review Screen Integration** (Day 3): Replace placeholder with complete route map
5. **Testing & Polish** (Day 3-4): Emulator + physical device testing, refinements

**Key Principles**:
- Reusable components (BikeMap used in both screens)
- Minimal ViewM

odel changes (expose track points Flow only)
- No domain/data layer changes (uses existing TrackPoint repository)
- Material 3 compliance (theme integration, dynamic colors)

---

## 📋 Phase 1: Setup & Configuration (Day 1, ~3-4 hours)

### Goals
- Set up Google Maps SDK in project
- Configure API key securely
- Verify map loads in preview

### Tasks

**T001: Interactive Google Cloud Console Setup**
- Guide user through Google Cloud Console
- Create or select project for BikeRedlights
- Enable "Maps SDK for Android" API
- Create API key with Android app restriction
- Set up billing account (free tier: 28,000 map loads/month)
- Copy API key for project configuration

**Interactive Steps** (will guide user):
1. Navigate to [Google Cloud Console](https://console.cloud.google.com/)
2. Create/select project
3. Navigate to "APIs & Services" > "Library"
4. Search for "Maps SDK for Android" and enable
5. Navigate to "APIs & Services" > "Credentials"
6. Click "Create Credentials" > "API Key"
7. Restrict key to Android apps + package name: `com.example.bikeredlights`
8. Copy API key

**T002: Project Configuration**
- Add Maps dependencies to `gradle/libs.versions.toml`:
  ```toml
  [versions]
  playServicesMaps = "19.0.0"
  mapsCompose = "6.2.0"

  [libraries]
  play-services-maps = { group = "com.google.android.gms", module = "play-services-maps", version.ref = "playServicesMaps" }
  maps-compose = { group = "com.google.maps.android", module = "maps-compose", version.ref = "mapsCompose" }
  maps-compose-utils = { group = "com.google.maps.android", module = "maps-compose-utils", version.ref = "mapsCompose" }
  ```
- Add to `app/build.gradle.kts`:
  ```kotlin
  implementation(libs.play.services.maps)
  implementation(libs.maps.compose)
  implementation(libs.maps.compose.utils)
  ```
- Add API key to `local.properties`:
  ```properties
  MAPS_API_KEY=AIzaSy...
  ```
- Update `AndroidManifest.xml`:
  ```xml
  <meta-data
      android:name="com.google.android.geo.API_KEY"
      android:value="${MAPS_API_KEY}" />
  ```
- Gradle sync and verify build

**T003: ProGuard Configuration**
- Add Maps SDK ProGuard rules to `proguard-rules.pro`:
  ```proguard
  # Google Maps SDK
  -keep class com.google.android.gms.maps.** { *; }
  -keep interface com.google.android.gms.maps.** { *; }
  -dontwarn com.google.android.gms.**

  # Maps Compose
  -keep class com.google.maps.android.compose.** { *; }
  -dontwarn com.google.maps.android.compose.**
  ```
- Test release build compiles: `./gradlew assembleRelease`

---

## 📋 Phase 2: Core Map Components (Day 1-2, ~6-8 hours)

### Goals
- Create reusable BikeMap composable
- Implement polyline rendering from track points
- Create marker utilities (start, end, current location)

### Architecture

**Component Structure**:
```
ui/components/map/
├── BikeMap.kt              # Main reusable map component
├── MapPolyline.kt          # Polyline rendering logic
├── MapMarkers.kt           # Marker utilities
└── MapUtils.kt             # Camera bounds, zoom calculations
```

### Tasks

**T004: Create BikeMap Composable** (`ui/components/map/BikeMap.kt`)

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

**Features**:
- GoogleMap composable from maps-compose
- Camera position state management
- Light/dark mode map style
- Loading state indicator
- Error handling (no API key, network issues)
- Material 3 theme integration

**Implementation**:
```kotlin
val cameraPositionState = rememberCameraPositionState {
    position = CameraPosition.fromLatLngZoom(
        currentLocation ?: LatLng(0.0, 0.0),
        15f  // City block zoom
    )
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

GoogleMap(
    modifier = modifier,
    cameraPositionState = cameraPositionState,
    uiSettings = MapUiSettings(
        zoomControlsEnabled = false,
        zoomGesturesEnabled = gesturesEnabled,
        scrollGesturesEnabled = gesturesEnabled
    ),
    properties = MapProperties(
        mapType = MapType.NORMAL,
        mapStyleOptions = if (isSystemInDarkTheme()) {
            MapStyleOptions.loadRawResourceStyle(context, R.raw.map_style_dark)
        } else {
            null
        }
    )
) {
    // Polyline, markers added here
}
```

**T005: Create MapPolyline Component** (`ui/components/map/MapPolyline.kt`)

```kotlin
fun DrawPolyline(
    trackPoints: List<LatLng>,
    color: Color,
    width: Float = 8f
)
```

**Features**:
- Converts List<TrackPoint> to List<LatLng>
- Renders polyline with Material 3 primary color
- Handles empty list gracefully
- Optimized for large point lists (1000+)

**Implementation**:
```kotlin
@Composable
fun MapPolyline(
    trackPoints: List<TrackPoint>,
    modifier: Modifier = Modifier
) {
    val latLngPoints = remember(trackPoints) {
        trackPoints.map { LatLng(it.latitude, it.longitude) }
    }

    val polylineColor = MaterialTheme.colorScheme.primary

    if (latLngPoints.isNotEmpty()) {
        Polyline(
            points = latLngPoints,
            color = polylineColor,
            width = 8f,
            clickable = false
        )
    }
}
```

**T006: Create MapMarkers Component** (`ui/components/map/MapMarkers.kt`)

```kotlin
@Composable
fun CurrentLocationMarker(position: LatLng)

@Composable
fun StartMarker(position: LatLng)

@Composable
fun EndMarker(position: LatLng)
```

**Features**:
- Current location: Blue circle marker
- Start: Green pin with label "Start"
- End: Red flag with label "End"
- Uses Material 3 semantic colors

**T007: Create Map Utilities** (`ui/components/map/MapUtils.kt`)

```kotlin
object MapUtils {
    fun calculateBounds(trackPoints: List<LatLng>): LatLngBounds

    fun calculateZoomLevel(bounds: LatLngBounds, mapWidthPx: Int, mapHeightPx: Int): Float

    fun calculateCenter(trackPoints: List<LatLng>): LatLng
}
```

**Features**:
- Calculate LatLngBounds from track points list
- Auto-zoom to fit entire route
- Calculate center point for camera positioning

---

## 📋 Phase 3: Live Tab Integration (Day 2-3, ~6-8 hours)

### Goals
- Add map to LiveRideScreen (50-60% of screen height)
- Wire up real-time location and polyline updates
- Implement map following behavior
- Lock gestures during recording

### Tasks

**T008: Modify RideRecordingViewModel**

**Add track points Flow**:
```kotlin
// In RideRecordingViewModel.kt
private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

init {
    viewModelScope.launch {
        rideRecordingStateRepository.getCurrentRideId().collectLatest { rideId ->
            if (rideId != null) {
                trackPointRepository.getTrackPointsForRide(rideId).collectLatest { points ->
                    _trackPoints.value = points
                }
            } else {
                _trackPoints.value = emptyList()
            }
        }
    }
}
```

**Add current location Flow**:
```kotlin
private val _currentLocation = MutableStateFlow<LatLng?>(null)
val currentLocation: StateFlow<LatLng?> = _currentLocation.asStateFlow()

// Update from service broadcasts
private fun updateCurrentLocation(latitude: Double, longitude: Double) {
    _currentLocation.value = LatLng(latitude, longitude)
}
```

**T009: Modify LiveRideScreen Layout**

**New layout structure**:
```kotlin
Column(modifier = Modifier.fillMaxSize()) {
    // GPS Status Indicator (existing, moved to Column)
    GpsStatusIndicator(
        uiState = uiState,
        modifier = Modifier
            .align(Alignment.End)
            .padding(top = 16.dp, end = 16.dp)
    )

    // Map Section (NEW - 55% height)
    BikeMap(
        currentLocation = currentLocation,
        trackPoints = trackPoints.map { LatLng(it.latitude, it.longitude) },
        showPolyline = isRecording,
        showCurrentLocationMarker = true,
        cameraFollowsLocation = isRecording,
        gesturesEnabled = !isRecording,  // Lock during recording
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.55f)  // 55% of screen
    )

    // Statistics Section (EXISTING - 45% height)
    when (uiState) {
        is RideRecordingUiState.Recording -> RecordingContent(...)
        is RideRecordingUiState.Paused -> PausedContent(...)
        // ... other states
    }
}
```

**T010: Implement Re-center FAB**

```kotlin
// Inside BikeMap composable
Box(modifier = modifier) {
    GoogleMap(...) { ... }

    // Re-center FAB (bottom-right corner)
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
```

**T011: Test Live Map on Emulator**
- Use GPS simulation with route playback (GPX file)
- Verify map loads and shows current location
- Verify polyline grows as track points added
- Test pause/resume behavior
- Test re-center FAB functionality
- Test dark mode map styling
- Test screen rotation

---

## 📋 Phase 4: Review Screen Integration (Day 3, ~3-4 hours)

### Goals
- Replace MapPlaceholder with actual route map
- Show complete route with start/end markers
- Auto-zoom to fit entire route

### Tasks

**T012: Modify RideReviewViewModel**

**Add track points Flow for selected ride**:
```kotlin
// In RideReviewViewModel.kt
private val _trackPoints = MutableStateFlow<List<TrackPoint>>(emptyList())
val trackPoints: StateFlow<List<TrackPoint>> = _trackPoints.asStateFlow()

fun loadRide(rideId: Long) {
    viewModelScope.launch {
        try {
            _uiState.value = RideReviewUiState.Loading

            val ride = rideRepository.getRideById(rideId)
            if (ride != null) {
                // Load track points for this ride
                trackPointRepository.getTrackPointsForRide(rideId).collectLatest { points ->
                    _trackPoints.value = points
                }

                _uiState.value = RideReviewUiState.Success(ride)
            } else {
                _uiState.value = RideReviewUiState.Error("Ride not found")
            }
        } catch (e: Exception) {
            _uiState.value = RideReviewUiState.Error(e.message ?: "Unknown error")
        }
    }
}
```

**T013: Replace MapPlaceholder in RideReviewScreen**

**Before** (v0.4.2):
```kotlin
MapPlaceholder(
    modifier = Modifier
        .fillMaxWidth()
        .height(200.dp)
)
```

**After** (v0.5.0):
```kotlin
val trackPoints by viewModel.trackPoints.collectAsStateWithLifecycle()
val latLngPoints = remember(trackPoints) {
    trackPoints.map { LatLng(it.latitude, it.longitude) }
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
        gesturesEnabled = true,  // Allow zoom/pan on review
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
} else {
    // Fallback for rides with poor GPS
    NoGpsDataPlaceholder(
        modifier = Modifier
            .fillMaxWidth()
            .height(300.dp)
    )
}
```

**Auto-zoom to fit route**:
```kotlin
// Inside BikeMap when startMarker and endMarker provided
LaunchedEffect(trackPoints) {
    if (trackPoints.size >= 2 && !cameraFollowsLocation) {
        val bounds = MapUtils.calculateBounds(trackPoints)
        cameraPositionState.animate(
            CameraUpdateFactory.newLatLngBounds(bounds, 100),  // 100px padding
            durationMs = 1000
        )
    }
}
```

**T014: Test Review Map on Emulator**
- Open rides from history (use Feature 003 saved rides)
- Verify complete route displays with polyline
- Verify start marker (green) at first point
- Verify end marker (red) at last point
- Verify map auto-zooms to show full route
- Test rides with < 2 points show fallback

---

## 📋 Phase 5: Testing & Polish (Day 3-4, ~4-6 hours)

### Goals
- Comprehensive emulator testing
- Physical device testing (required per CLAUDE.md)
- Performance validation
- Bug fixes and refinements

### Tasks

**T015: Emulator Testing Checklist**
- [ ] Map loads correctly with valid API key
- [ ] Map shows error message with invalid/missing API key
- [ ] Live tab: Map displays current location in Idle state
- [ ] Live tab: Polyline grows during GPS simulation
- [ ] Live tab: Map camera follows user location smoothly
- [ ] Live tab: Gestures locked during recording
- [ ] Live tab: Re-center FAB works correctly
- [ ] Live tab: Dark mode map style loads
- [ ] Review screen: Complete route displays
- [ ] Review screen: Start/end markers positioned correctly
- [ ] Review screen: Map auto-zooms to fit route
- [ ] Review screen: Rides with < 2 points show fallback
- [ ] Screen rotation preserves map state
- [ ] No memory leaks (check Android Profiler)

**T016: Physical Device Testing** (MANDATORY per CLAUDE.md)
- [ ] Real bike ride with GPS tracking
- [ ] Map loads and tracks location accurately
- [ ] Polyline renders smoothly without lag
- [ ] Battery impact acceptable (< 5% additional drain per hour)
- [ ] Map tiles load on cellular data
- [ ] Offline behavior (cached tiles work)
- [ ] No crashes or ANR events

**T017: Performance Validation**
- [ ] Map loads in < 2 seconds on Wi-Fi
- [ ] Polyline renders 1000+ points without lag
- [ ] Memory usage < 100MB additional vs. v0.4.2
- [ ] Battery drain < 5% additional per hour
- [ ] 60fps maintained during map interactions

**T018: Accessibility Testing**
- [ ] BikeMap has semantic contentDescription
- [ ] Re-center FAB has 48dp touch target
- [ ] Map works with TalkBack enabled
- [ ] Zoom controls accessible

**T019: Documentation Updates**
- [ ] Update CLAUDE.md with Maps SDK setup instructions
- [ ] Update TODO.md: Move Feature 006 to completed
- [ ] Update RELEASE.md: Add v0.5.0 entry with Maps integration
- [ ] Create API key setup guide for future developers

---

## 🔧 Implementation Notes

### Key Design Decisions

**1. Reusable BikeMap Component**
- Single composable used in both LiveRideScreen and RideReviewScreen
- Parameters control behavior (follow location, gestures enabled, markers)
- Easier to maintain and test

**2. Minimal ViewModel Changes**
- Only expose `trackPoints: StateFlow` from existing repository
- No new use cases needed (reuse existing TrackPointRepository)
- Follows single responsibility principle

**3. No Domain Layer Changes**
- Maps are purely UI concern
- Domain models (TrackPoint) already contain lat/long
- Keeps architecture clean

**4. Gesture Locking During Recording**
- Prevents accidental zoom/pan while riding
- Improves safety (hands-free operation)
- Re-enabled during pause for user control

**5. Material 3 Integration**
- Polyline uses `MaterialTheme.colorScheme.primary`
- Map style adapts to light/dark theme
- Semantic colors for markers (green/red/blue)

### Potential Issues & Mitigations

**Issue 1: API Key Security**
- **Risk**: API key leaked in repository
- **Mitigation**: Store in `local.properties` (gitignored), restrict key to package name

**Issue 2: Battery Drain**
- **Risk**: Maps SDK increases battery usage
- **Mitigation**: Monitor battery impact, ensure map only active when screen visible

**Issue 3: Large Polylines**
- **Risk**: 1000+ track points cause lag
- **Mitigation**: Use Polyline simplification if needed (reduce points for display)

**Issue 4: Network Dependency**
- **Risk**: Map tiles fail to load without network
- **Mitigation**: Google Maps SDK caches tiles automatically, graceful degradation

**Issue 5: Missing GPS Data**
- **Risk**: Some rides have < 2 track points (poor GPS)
- **Mitigation**: Show fallback message instead of empty map

---

## 🎯 Definition of Done

**Code Complete**:
- [ ] All 19 tasks (T001-T019) completed
- [ ] BikeMap composable fully functional
- [ ] Live tab shows map with real-time updates
- [ ] Review screen shows complete route with markers
- [ ] No compilation errors or warnings
- [ ] ProGuard rules added for Maps SDK

**Testing Complete**:
- [ ] All emulator tests passing
- [ ] Physical device testing completed
- [ ] Performance metrics validated
- [ ] Accessibility tests passing

**Documentation Complete**:
- [ ] spec.md finalized
- [ ] plan.md (this file) finalized
- [ ] tasks.md created with 19 tasks
- [ ] TODO.md updated
- [ ] RELEASE.md updated
- [ ] CLAUDE.md updated with Maps setup

**Release Ready**:
- [ ] Release APK built successfully
- [ ] Version bumped to v0.5.0 (versionCode = 500)
- [ ] Git commits following conventional commits
- [ ] Feature branch merged to main
- [ ] GitHub release created with APK

---

**Created**: 2025-11-08 | **Last Updated**: 2025-11-08 | **Status**: Draft
