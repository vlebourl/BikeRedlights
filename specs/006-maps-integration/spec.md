# Feature Specification: Maps Integration

**Feature Branch**: `006-maps-integration`
**Created**: 2025-11-08
**Status**: Draft
**Input**: User description: "Feature 1B: Maps Integration - Add Google Maps visualization to Live tab and Review screens. Display real-time location and route polyline during recording. Show complete route with start/end markers on Review screen."

## User Scenarios & Testing *(mandatory)*

### User Story 1 - View Map on Live Tab During Recording (Priority: P1)

As a cyclist recording a ride, I want to see a map showing my current location and route traveled so that I can visualize where I am and where I've been.

**Why this priority**: Visual context is essential for cycling navigation. Seeing the route on a map provides immediate spatial awareness and confirms GPS tracking is working correctly. This is the core value of maps integration.

**Independent Test**: Can be fully tested by starting a ride on emulator with GPS simulation, verifying map appears showing current location (blue marker), and route polyline grows as simulated movement occurs.

**Acceptance Scenarios**:

1. **Given** user is on Live tab in Idle state, **When** viewing the screen, **Then** a Google Maps view occupies 50-60% of screen height showing current location with blue marker
2. **Given** user starts a ride, **When** GPS acquires first fix, **Then** map centers on user's current location at city block zoom level (50-200m radius)
3. **Given** ride is actively recording, **When** user moves to new locations, **Then** map camera smoothly follows user location and centers on current position
4. **Given** ride is recording and user has moved, **When** viewing the map, **Then** a contrasting polyline shows the complete route traveled from start to current position
5. **Given** ride is recording, **When** user attempts to zoom or pan the map, **Then** map gestures are locked to prevent accidental interactions while riding
6. **Given** ride is paused or auto-paused, **When** viewing the map, **Then** polyline remains visible and frozen at pause point, current location marker still updates
7. **Given** user has moved map manually during pause, **When** tapping re-center FAB, **Then** map smoothly animates back to center on current location
8. **Given** user stops ride and returns to Idle state, **When** viewing the map, **Then** polyline clears and only current location marker remains visible

---

### User Story 2 - View Complete Route Map on Review Screen (Priority: P2)

As a cyclist reviewing a completed ride, I want to see the complete GPS track on a map so that I can visualize my entire route.

**Why this priority**: After completing a ride, users want to see where they went. The route map provides a complete overview that complements the statistics. Essential for understanding ride context.

**Independent Test**: Can be fully tested by opening any completed ride from history, verifying map loads showing complete route polyline, and start/end markers are correctly positioned at first/last track points.

**Acceptance Scenarios**:

1. **Given** user opens a completed ride from history, **When** Review screen loads, **Then** map placeholder is replaced with Google Maps showing complete route polyline
2. **Given** Review screen map is displayed, **When** viewing the route, **Then** a green pin marker appears at the start location (first track point)
3. **Given** Review screen map is displayed, **When** viewing the route, **Then** a red flag marker appears at the end location (last track point)
4. **Given** route polyline is drawn, **When** viewing the map, **Then** map is automatically zoomed and centered to show entire route from start to end
5. **Given** ride has fewer than 2 track points (poor GPS), **When** Review screen loads, **Then** map shows a message "No GPS data available for this ride" instead of polyline
6. **Given** Review screen map is loaded, **When** user zooms or pans, **Then** map gestures are enabled and user can freely explore the route

---

### User Story 3 - Understand Map Status and Errors (Priority: P3)

As a cyclist using maps, I want clear feedback when maps aren't available so that I understand why the map isn't loading.

**Why this priority**: Maps can fail due to missing API key, network issues, or service errors. Users need to understand what's wrong rather than seeing a blank screen. Lower priority but essential for production quality.

**Independent Test**: Can be fully tested by temporarily removing API key from manifest, launching app, and verifying error message displays instead of map.

**Acceptance Scenarios**:

1. **Given** Google Maps API key is missing or invalid, **When** screen with map loads, **Then** map area shows error message "Map unavailable - check API key configuration"
2. **Given** device has no network connection, **When** map loads for first time (no cached tiles), **Then** map shows loading spinner and eventually displays available cached tiles or error
3. **Given** map is loading tiles, **When** waiting for network response, **Then** loading indicator is displayed until tiles load or timeout occurs
4. **Given** device is in airplane mode after map has cached tiles, **When** viewing map, **Then** map displays using cached tiles without error

---

## 🎯 Success Metrics *(mandatory)*

**Mandatory Success Criteria**:
1. Live tab map displays current location within 5 seconds of GPS lock ✅ (testable on emulator)
2. Route polyline grows in real-time as track points are added ✅ (testable on emulator)
3. Review screen map shows complete route with start/end markers ✅ (testable with saved rides)
4. Map gestures locked during active recording (no accidental zoom/pan) ✅ (testable manually)
5. Map automatically zooms to fit entire route on Review screen ✅ (testable with various ride lengths)

**Performance Requirements**:
- Map initial load time: < 2 seconds on Wi-Fi
- Polyline rendering: handles 1000+ track points without lag
- Battery impact: < 5% additional drain vs. v0.4.2 (1 hour recording)

**Accessibility**:
- Map area has semantic contentDescription for screen readers
- Re-center FAB has 48dp minimum touch target
- Map works with TalkBack enabled (descriptive labels)

---

## 📋 Out of Scope *(mandatory)*

Explicitly **NOT** included in this feature (defer to future releases):

1. **Heading-up map rotation** - Map always north-up orientation (defer to v0.6.0)
2. **Route color-coding by speed** - Polyline is single color (defer to v0.6.0)
3. **Offline map downloads** - Uses standard Google Maps caching only
4. **Custom map styling beyond light/dark** - Material 3 Dynamic Color only
5. **Map layers** (satellite, terrain) - Standard map view only
6. **Turn-by-turn navigation** - Read-only map display, no routing
7. **Waypoints or POIs** - Just start/end markers and current location
8. **Heatmaps or clustering** - Simple polyline rendering only

---

## 🔗 Dependencies *(mandatory)*

**Critical Dependencies**:
- ✅ Feature 1A (Core Ride Recording) - Complete (provides track points database)
- ⚠️ Google Maps SDK API Key - **REQUIRED USER ACTION** (must be obtained before implementation)
- ⚠️ Google Cloud Console Setup - **REQUIRED USER ACTION** (Maps SDK for Android enabled, billing configured)

**Technical Dependencies**:
- Google Maps SDK for Android v19.0.0+
- Maps Compose library v6.2.0+ (Jetpack Compose integration)
- Play Services Location v21.3.0 (already integrated in v0.1.0)

**Data Dependencies**:
- Room database `track_points` table (populated by Feature 1A)
- Settings repository `unitsSystem` (for distance formatting if needed)

---

## 🏗️ Architecture Overview

**MVVM + Clean Architecture** maintained:

**Domain Layer**:
- No new entities (uses existing `TrackPoint` from Feature 1A)
- New utility: `MapBoundsCalculator` (calculate zoom bounds from track points)

**Data Layer**:
- No changes (uses existing `TrackPointRepository`)

**UI Layer**:
- New: `ui/components/map/BikeMap.kt` - Reusable GoogleMap composable
- New: `ui/components/map/MapPolyline.kt` - Polyline rendering logic
- New: `ui/components/map/MapMarkers.kt` - Marker utilities
- Modified: `ui/screens/ride/LiveRideScreen.kt` - Add map section (50-60% height)
- Modified: `ui/screens/ride/RideReviewScreen.kt` - Replace MapPlaceholder with BikeMap
- Modified: `ui/viewmodel/RideRecordingViewModel.kt` - Expose `trackPoints: StateFlow`
- Modified: `ui/viewmodel/RideReviewViewModel.kt` - Expose `trackPoints: StateFlow`

**Component Hierarchy**:
```
LiveRideScreen / RideReviewScreen
    └── BikeMap (reusable component)
            ├── GoogleMap (from maps-compose)
            ├── Polyline (route visualization)
            ├── Marker (current location / start / end)
            └── FloatingActionButton (re-center)
```

---

## 🎨 UI/UX Design

**Live Tab Layout** (Modified from v0.4.2):
```
┌─────────────────────────────────────┐
│ GPS Ready                    ●      │ ← GPS indicator (existing)
├─────────────────────────────────────┤
│                                     │
│         [GOOGLE MAPS - 55%]         │ ← NEW: GoogleMap component
│    Blue marker (current location)   │   Polyline (route trail)
│    Polyline showing route           │   Zoom: 50-200m radius
│                                     │   Locked during recording
│         [Re-center FAB]  ⊕          │   Re-center button (bottom-right)
│                                     │
├─────────────────────────────────────┤
│                                     │
│      [STATISTICS AREA - 45%]        │ ← EXISTING: RideStatistics
│                                     │
│      25 km/h  (displayLarge)        │   Speed hero metric
│      🚴 5.2 km  ⏱ 12:34            │   Duration + distance
│      Avg: 18 km/h   Max: 32 km/h   │
│                                     │
│      [  STOP RIDE  ]                │   Control button
└─────────────────────────────────────┘
```

**Review Screen Layout** (Modified from v0.4.2):
```
┌─────────────────────────────────────┐
│ ← Morning Ride                      │ ← Top app bar
├─────────────────────────────────────┤
│ Nov 8, 2025 at 8:30 AM              │ ← Ride date
├─────────────────────────────────────┤
│                                     │
│         [GOOGLE MAPS]               │ ← MODIFIED: Replace placeholder
│    🟢 Start marker (green pin)      │   Complete route polyline
│    Route polyline (full track)      │   Zoomed to fit route
│    🔴 End marker (red flag)         │   Gestures enabled
│                                     │
├─────────────────────────────────────┤
│                                     │
│      [STATISTICS CARD]              │ ← EXISTING: RideStatistics
│      Duration: 45:23                │
│      Distance: 12.5 km              │
│      Avg Speed: 16.8 km/h           │
│      Max Speed: 28.3 km/h           │
└─────────────────────────────────────┘
```

**Material 3 Design Compliance**:
- Map integrates with dynamic color scheme
- Light/dark mode map styles (map adapts to theme)
- Polyline color: `MaterialTheme.colorScheme.primary`
- Markers use semantic colors (green for start, red for end, blue for current)
- Re-center FAB: `FloatingActionButton` with `primaryContainer` color
- Map loading uses `CircularProgressIndicator` from Material 3

---

## 🔐 Security & Privacy

**API Key Security**:
- API key stored in `local.properties` (not committed to Git)
- API key restricted to Android package name in Google Cloud Console
- No hardcoded keys in source code

**Location Privacy**:
- No location data sent to Google beyond standard Maps SDK tile requests
- No tracking or analytics added (uses existing Google Maps SDK behavior)
- Map tiles may be cached locally by Google Maps SDK

**Data Storage**:
- Track points remain local only (no cloud sync)
- Map tiles cached by system (managed by Google Maps SDK)

---

## 📚 Reference Materials

**Google Maps Documentation**:
- [Maps SDK for Android Setup](https://developers.google.com/maps/documentation/android-sdk/start)
- [Maps Compose Library](https://developers.google.com/maps/documentation/android-sdk/maps-compose)
- [Polylines Guide](https://developers.google.com/maps/documentation/android-sdk/shapes#polylines)

**Material Design 3**:
- [Maps Integration Guidelines](https://m3.material.io/components/maps/overview)

**Project Standards**:
- CLAUDE.md: Android development standards
- Roadmap: docs/roadmap.md (Feature 1B specification)
- Constitution: .specify/memory/constitution.md

---

**Version**: 1.0 | **Created**: 2025-11-08 | **Last Updated**: 2025-11-08
