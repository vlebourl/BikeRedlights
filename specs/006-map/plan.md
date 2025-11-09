# Implementation Plan: Maps Integration

**Branch**: `006-map` | **Date**: 2025-11-08 | **Spec**: [spec.md](./spec.md)
**Input**: Feature specification from `/specs/006-map/spec.md`

## Summary

Integrate Google Maps SDK to display real-time route tracking on the Live tab and complete route visualization on the Review Screen. Live tab shows current location with blue marker, growing route polyline during recording, and auto-centering map following rider position at city block zoom level (50-200m radius). Review Screen displays complete GPS track with green start marker and red end flag marker, auto-zoomed to fit entire route. Requires Google Cloud Console setup, Maps SDK for Android API enablement, and API key configuration in local.properties.

**Technical Approach**: Google Maps SDK via Compose GoogleMap component, StateFlow for live location/polyline updates, Room database track_points query for route rendering, CameraPositionState for smooth map following, PolylineOptions for route drawing, MarkerOptions for start/end indicators, LatLngBounds for auto-zoom calculation.

---

## Technical Context

**Language/Version**: Kotlin 2.0.21, Java 17 (OpenJDK)
**Primary Dependencies**: Google Maps SDK for Android (via maps-compose), Jetpack Compose BOM 2024.11.00, Room 2.6.1 (existing), Play Services Location 21.3.0 (existing)
**Storage**: Room database (existing rides + track_points tables from Feature 1A), Google Maps API key in local.properties
**Testing**: JUnit 4, MockK 1.13.13, Compose UI Test, Pixel 9 Pro Emulator with GPS simulation
**Target Platform**: Android 14+ (API 34+), requires Google Play Services
**Project Type**: Mobile (Android)
**Performance Goals**: <1s map initialization, <2s route polyline rendering, 60fps map panning, stay within 28,000 map loads/month (free tier)
**Constraints**: Requires Google Cloud account, billing enabled (free tier sufficient), API key must be kept secure (local.properties, not in source control), offline maps not required for v0.5.0
**Scale/Scope**: Single map per screen (Live tab + Review Screen), routes up to 100km, 3600 track points/hour, 3-4 day implementation

---

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

### Architecture Requirements ✅

- **Clean Architecture**: UI → ViewModel → Domain → Data separation maintained (map UI in composables, data from repositories)
- **MVVM Pattern**: StateFlow-based reactive updates for location, track points, and polyline data
- **Repository Pattern**: Reuse existing RideRepository and TrackPointRepository interfaces (no new repositories needed)
- **Separation of Concerns**: Map rendering logic in UI layer, business logic (route calculation, bounds) in domain/ViewModel

### Testing Requirements ✅

- **Emulator Testing**: Mandatory validation with GPS simulation (GPX route playback)
- **Unit Tests**: ViewModel map state logic, bounds calculation, polyline coordinate transformation
- **Instrumented Tests**: GoogleMap composable rendering, marker placement, polyline display
- **Physical Device**: Recommended for smooth panning validation and real GPS testing

### Accessibility Requirements ✅

- **Map Controls**: Zoom buttons (48dp touch targets), location button (FAB 56dp)
- **TalkBack Support**: Content descriptions for all map controls and markers
- **Color Contrast**: Route polyline uses high-contrast color (red or dynamic accent), markers use distinct shapes + colors
- **Dark Mode**: Maps SDK dark mode style applied automatically via Material 3 theme detection

### Technology Requirements ✅

- **Jetpack Compose**: GoogleMap composable from maps-compose library (no XML layouts)
- **Material 3 Expressive**: Floating action buttons for map controls, card overlays for map legends
- **StateFlow**: Live location updates, track points collection, camera position state
- **Google Maps SDK**: Official Android library, well-maintained, follows "androidesque" preference per roadmap

### Security Requirements ✅

- **API Key Protection**: Stored in local.properties (gitignored), loaded via BuildConfig
- **API Key Restriction**: Must be restricted to package name (com.example.bikeredlights) + SHA-1 fingerprint in Google Cloud Console
- **Quota Monitoring**: Stay within free tier (28,000 map loads/month), no sensitive data sent to Maps API

### Documentation Requirements ✅

- **Specification**: spec.md complete with 3 user stories, 17 functional requirements, 10 success criteria
- **Research**: research.md (to be created) - Google Maps SDK setup, Compose integration, API key security, polyline rendering
- **Data Model**: data-model.md (to be created) - reuse existing Ride/TrackPoint entities, add MapViewState model
- **Contracts**: contracts/ (to be created) - map composable contracts, ViewModel interfaces
- **Quickstart**: quickstart.md (to be created) - Google Cloud setup guide + implementation phases

**GATE STATUS**: ✅ **PASSED** - All requirements met, no violations. API key security handled via established Android best practices.

---

## Project Structure

### Documentation (this feature)

```text
specs/006-map/
├── spec.md              # Feature specification (3 user stories, 17 FRs, 10 SCs)
├── plan.md              # This file (/speckit.plan command output)
├── research.md          # Phase 0 output (Maps SDK setup, Compose integration, polyline rendering)
├── data-model.md        # Phase 1 output (MapViewState, reuse Ride/TrackPoint)
├── quickstart.md        # Phase 1 output (Google Cloud setup + 3-phase implementation)
├── contracts/           # Phase 1 output
│   └── map-component-contracts.md  # GoogleMap composable contracts, ViewModel interfaces
└── checklists/
    └── requirements.md  # Quality validation checklist (already created)
```

### Source Code (repository root)

```text
app/src/main/java/com/example/bikeredlights/
├── data/
│   └── repository/
│       # NO CHANGES - Reuse existing RideRepositoryImpl, TrackPointRepositoryImpl
│
├── domain/
│   ├── model/
│   │   └── MapViewState.kt          # NEW: Map camera position, zoom level, bounds
│   ├── repository/
│   │   # NO CHANGES - Reuse existing RideRepository, TrackPointRepository interfaces
│   └── usecase/
│       ├── GetRoutePolylineUseCase.kt   # NEW: Convert track points → LatLng list
│       ├── CalculateMapBoundsUseCase.kt # NEW: Calculate LatLngBounds for auto-zoom
│       └── FormatMapMarkersUseCase.kt   # NEW: Generate start/end marker data
│
├── ui/
│   ├── screens/
│   │   └── ride/
│   │       ├── LiveRideScreen.kt        # UPDATED: Add GoogleMap with live location + polyline
│   │       └── RideReviewScreen.kt      # UPDATED: Add GoogleMap with complete route + markers
│   ├── components/
│   │   └── map/
│   │       ├── BikeMap.kt               # NEW: Reusable GoogleMap wrapper with Material 3 styling
│   │       ├── RoutePolyline.kt         # NEW: Polyline composable with contrasting color
│   │       ├── LocationMarker.kt        # NEW: Current location blue marker
│   │       ├── StartEndMarkers.kt       # NEW: Green start pin + red end flag
│   │       └── MapControls.kt           # NEW: Zoom buttons + location FAB
│   ├── viewmodel/
│   │   └── RideRecordingViewModel.kt    # UPDATED: Add map state (camera, polyline, markers)
│   └── theme/
│       └── MapTheme.kt                  # NEW: Dark mode map style configuration
│
└── MainActivity.kt                      # NO CHANGES

app/src/test/java/  # Unit tests
├── domain/usecase/
│   ├── GetRoutePolylineUseCaseTest.kt       # NEW: Test LatLng conversion
│   ├── CalculateMapBoundsUseCaseTest.kt     # NEW: Test bounds calculation
│   └── FormatMapMarkersUseCaseTest.kt       # NEW: Test marker generation
└── ui/viewmodel/
    └── RideRecordingViewModelTest.kt        # UPDATED: Test map state updates

app/src/androidTest/java/  # Instrumented tests
└── ui/components/map/
    ├── BikeMapTest.kt                       # NEW: Test GoogleMap rendering
    ├── RoutePolylineTest.kt                 # NEW: Test polyline display
    └── StartEndMarkersTest.kt               # NEW: Test marker placement

app/build.gradle.kts                         # UPDATED: Add maps-compose dependency
local.properties                             # UPDATED: Add MAPS_API_KEY (gitignored)
```

**Structure Decision**: Android Clean Architecture with MVVM. Follows existing project structure. New directories: `ui/components/map/` for reusable map composables, `ui/theme/MapTheme.kt` for dark mode styling. Reuses existing `data/local/entity/TrackPoint` and `domain/repository/` contracts. Google Maps SDK added via `maps-compose` library dependency.

---

## Complexity Tracking

> **No violations** - All Constitution requirements satisfied without exceptions. Google Maps SDK is external dependency with no architectural impact (composable UI layer only).

---

## Phase Breakdown

### Phase 0: Research (IN PROGRESS)

**Duration**: 1-2 hours (parallel research agents)

**Research Tasks**:
1. **Google Cloud Console Setup & API Key Management**
   - Create Google Cloud project
   - Enable Maps SDK for Android API
   - Generate API key and restrict to package name + SHA-1
   - Configure billing (free tier: 28,000 map loads/month)
   - Store API key in local.properties securely

2. **Google Maps Compose Integration**
   - Add `com.google.maps.android:maps-compose` dependency
   - Understand GoogleMap composable API
   - Learn CameraPositionState for smooth following
   - Discover rememberCameraPositionState() lifecycle

3. **Polyline Rendering & Route Drawing**
   - Convert List<TrackPoint> → List<LatLng>
   - Use Polyline composable with PolylineOptions
   - Set polyline color (red or MaterialTheme.colorScheme.primary)
   - Handle large polylines (3600+ points) efficiently

4. **Map Markers & Auto-Zoom**
   - Use Marker composable with MarkerOptions
   - Set custom icons (green pin, red flag, blue dot)
   - Calculate LatLngBounds.Builder for route bounds
   - Use CameraPositionState.animate(CameraUpdateFactory.newLatLngBounds())
   - Handle edge cases (single point, very short routes)

**Deliverables**:
- [ ] Google Cloud setup documented with step-by-step guide
- [ ] Maps Compose integration patterns documented
- [ ] Polyline rendering best practices documented
- [ ] Marker and auto-zoom examples documented
- [ ] Consolidated findings in `research.md`

---

### Phase 1: Design & Contracts (PENDING)

**Duration**: 1-2 hours

**Deliverables**:
- [ ] Data model defined (`data-model.md`)
  - MapViewState (camera position, zoom, bounds)
  - Reuse Ride, TrackPoint entities (no changes)
  - PolylineData (list of LatLng, color, width)
  - MarkerData (position, icon, title)

- [ ] API contracts defined (`contracts/map-component-contracts.md`)
  - BikeMap composable signature (location, polyline, markers, controls)
  - RideRecordingViewModel map state interfaces
  - Use case contracts (GetRoutePolyline, CalculateMapBounds, FormatMapMarkers)

- [ ] Implementation guide (`quickstart.md`)
  - Phase 0: Google Cloud setup (prerequisite)
  - Phase 1: Add maps-compose dependency + API key configuration
  - Phase 2: Implement Live tab map (location + polyline)
  - Phase 3: Implement Review Screen map (complete route + markers)
  - Phase 4: Testing (emulator GPS simulation, physical device)

- [ ] Agent context updated (`.specify/memory/claude-context.md`)
  - Add Google Maps SDK technology
  - Add maps-compose library reference

---

### Phase 2: Task Generation (NOT PART OF /speckit.plan)

**Note**: This phase is handled by the `/speckit.tasks` command, not `/speckit.plan`.

The `/speckit.tasks` command will generate `tasks.md` with actionable implementation tasks based on the research, data model, and contracts created in Phases 0-1.

---

## Dependencies

### External Services
- **Google Cloud Console**: Must create project and enable Maps SDK for Android API
- **Google Play Services**: Required on device/emulator for Maps SDK to function
- **Billing Account**: Must be linked to Google Cloud project (free tier sufficient)

### Project Dependencies
- **Feature 1A (Core Ride Recording)**: CRITICAL - Must exist first
  - Provides Room database with rides + track_points tables
  - Provides RideRepository and TrackPointRepository interfaces
  - Provides RideRecordingViewModel with StateFlow for live location
  - Live tab and Review Screen already exist (maps will be added to them)

### Library Dependencies (New)
- `com.google.maps.android:maps-compose:4.3.0` (or latest stable)
- `com.google.android.gms:play-services-maps:18.2.0` (dependency of maps-compose)

### Library Dependencies (Existing)
- Room 2.6.1 (for track_points queries)
- Jetpack Compose BOM 2024.11.00
- Play Services Location 21.3.0 (for GPS coordinates)
- Kotlin Coroutines 1.9.0 (for StateFlow)

---

## Risk Assessment

### High Risk
- **Google Cloud Setup Complexity**: First-time setup can be confusing
  - **Mitigation**: Detailed step-by-step guide in research.md + quickstart.md

- **API Key Leakage**: If committed to source control, attackers can abuse quota
  - **Mitigation**: Store in local.properties (gitignored), verify .gitignore before first commit

### Medium Risk
- **Map Performance**: Large polylines (3600+ points) may lag on low-end devices
  - **Mitigation**: Test on emulator first, implement polyline simplification if needed (Douglas-Peucker algorithm)

- **Free Tier Quota**: 28,000 map loads/month may be insufficient for heavy users
  - **Mitigation**: Monitor usage in Google Cloud Console, implement caching if needed

### Low Risk
- **Dark Mode Styling**: Maps SDK dark mode may not match Material 3 theme perfectly
  - **Mitigation**: Use Maps SDK dark mode JSON style configuration, test in both modes

- **Offline Functionality**: Maps require network connectivity
  - **Mitigation**: Show offline indicator, degrade gracefully (no map tiles but markers still visible)

---

## Next Steps

1. **Run Phase 0 Research**: Generate `research.md` with all Google Maps SDK investigation results
2. **Run Phase 1 Design**: Generate `data-model.md`, `contracts/`, and `quickstart.md`
3. **Run `/speckit.tasks`**: Generate actionable task list based on research and design artifacts
4. **Implementation**: Follow quickstart.md phases to implement maps integration
5. **Testing**: Validate on emulator with GPS simulation, then physical device
6. **Release**: Merge PR and release v0.5.0 with maps enhancement

---

**Status**: Phase 0 (Research) ready to begin. Waiting for research.md generation.
