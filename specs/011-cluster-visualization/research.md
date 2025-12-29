# Research Findings: Stop Cluster Visualization

**Feature**: 011-cluster-visualization
**Date**: 2025-12-29
**Phase**: Phase 0 - Technical Research

This document captures key technical decisions made during implementation planning based on research into unknowns identified in the specification.

---

## 1. Marker Clustering Implementation

### Decision: Use maps-compose-utils library with manual cluster aggregation

**Context**: Feature 011 needs to display cluster markers on Google Maps with color-coded visual representation (green/yellow/red based on cluster size). The spec requires handling up to 100 cluster markers with smooth performance.

**Rationale**:
- **Existing dependency**: Project already uses `maps-compose-utils` (via `libs.maps.utils` in build.gradle.kts) which provides clustering utilities
- **Custom rendering required**: Spec demands specific color-coding (2-5 = green, 6-10 = yellow, 11+ = red) which requires custom cluster rendering
- **Simpler approach for static clusters**: Since clusters are pre-computed by Feature 010 (DBSCAN algorithm), we don't need dynamic marker clustering at different zoom levels
- **Performance**: 100 pre-computed clusters can be rendered as individual markers without clustering library overhead

**Implementation Approach**:
1. Query stops with `cluster_id NOT NULL` from Room database
2. Group stops by `cluster_id` in domain layer (CalculateClusterStatsUseCase)
3. Calculate cluster center as mean of GPS coordinates (CalculateClusterCenterUseCase)
4. Render each cluster as a single `Marker` with custom color based on size
5. No runtime clustering needed - all clusters pre-identified by Feature 010

**Alternatives Considered**:
- **maps-compose-utils Clustering composable**: Designed for dynamic clustering of thousands of markers at different zoom levels. Overkill for our use case with pre-computed clusters.
- **DefaultClusterRenderer customization**: Would require learning complex renderer API for features we don't need.
- **Manual marker aggregation at zoom changes**: Unnecessary complexity since clusters are already computed.

**Code Pattern**:
```kotlin
@Composable
fun ClusterMarkers(
    clusters: List<ClusterSummary>,
    onClusterClick: (ClusterSummary) -> Unit
) {
    clusters.forEach { cluster ->
        Marker(
            state = MarkerState(position = cluster.centerPosition),
            title = "Cluster: ${cluster.stopCount} stops",
            icon = BitmapDescriptorFactory.defaultMarker(
                when (cluster.stopCount) {
                    in 2..5 -> BitmapDescriptorFactory.HUE_GREEN
                    in 6..10 -> BitmapDescriptorFactory.HUE_YELLOW
                    else -> BitmapDescriptorFactory.HUE_RED
                }
            ),
            onClick = {
                onClusterClick(cluster)
                true
            }
        )
    }
}
```

**References**:
- Project uses Maps Compose 6.2.0 (verified in gradle/libs.versions.toml)
- Existing BikeMap composable from Feature 006 already set up for marker rendering
- DBSCAN clustering from Feature 010 provides cluster_id field

---

## 2. Bottom Sheet for Cluster Details

### Decision: Use Material 3 ModalBottomSheet with conditional composition

**Context**: When users tap a cluster marker, they need to see detailed information: total stop count, average duration, frequency analytics ("15 times this month"), and scrollable list of individual stops (up to 50+ items with dates/times/durations).

**Rationale**:
- **ModalBottomSheet vs BottomSheetScaffold**: ModalBottomSheet is designed for temporary, dialog-like content triggered by user actions (marker taps). BottomSheetScaffold is for persistent UI elements. Modal pattern is perfect for our use case.
- **Built-in dismiss patterns**: ModalBottomSheet provides swipe-down, scrim tap, and back button dismissal out of the box
- **Material 3 alignment**: Project uses Compose BOM 2024.11.00 which includes latest Material 3 optimizations (tween animations, better performance)
- **Accessibility**: Material 3 bottom sheets include built-in accessibility support (screen reader announcements, focus management)

**Implementation Approach**:
1. **State Management**: Use boolean flag + `rememberModalBottomSheetState()`
   ```kotlin
   var showBottomSheet by remember { mutableStateOf(false) }
   var selectedCluster by remember { mutableStateOf<ClusterSummary?>(null) }
   val sheetState = rememberModalBottomSheetState()
   ```

2. **Conditional Composition**: Wrap ModalBottomSheet in `if (showBottomSheet)` block
   - Removes sheet from composition tree when hidden
   - Critical for performance with 20+ scrollable items
   - Zero memory overhead when dismissed

3. **Trigger on Marker Click**: Set `showBottomSheet = true` and store cluster data
   ```kotlin
   Marker(
       onClick = {
           selectedCluster = cluster
           showBottomSheet = true
           true
       }
   )
   ```

4. **Scrollable Content**: Use LazyColumn for stop list (not Column)
   - Only composes visible items
   - Efficient for 50+ stops
   - Always provide unique keys: `items(items = stops, key = { it.id })`

5. **Layout Structure**:
   - Header with title + close button (fixed)
   - Summary cards: total stops, avg duration (fixed)
   - Frequency analytics card (fixed)
   - LazyColumn with `heightIn(max = 400.dp)` for scrollable stops

**Alternatives Considered**:
- **BottomSheetScaffold**: Too persistent for temporary detail view, clutters screen when not needed
- **Dialog**: Too modal and doesn't feel native for mobile maps pattern
- **Separate detail screen**: Over-engineering for simple data display, breaks user flow

**Performance Optimizations**:
- Conditional composition removes sheet from tree when hidden
- LazyColumn with unique keys enables efficient item reuse
- `remember` for cached date/time formatting
- Immutable/Stable data classes for optimal recomposition

**Code Example** (simplified):
```kotlin
if (showBottomSheet) {
    ModalBottomSheet(
        onDismissRequest = {
            showBottomSheet = false
            selectedCluster = null
        },
        sheetState = sheetState
    ) {
        Column(Modifier.padding(16.dp)) {
            // Header + close button
            Text("Stop Cluster", style = MaterialTheme.typography.headlineSmall)

            // Summary cards
            Row {
                StatCard("Total Stops", cluster.stopCount.toString())
                StatCard("Avg Duration", cluster.avgDuration)
            }

            // Analytics
            Text(cluster.frequencyText)

            // Scrollable list
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp)
            ) {
                items(items = cluster.stops, key = { it.id }) { stop ->
                    StopListItem(stop)
                }
            }
        }
    }
}
```

**References**:
- Material Design 3 Bottom Sheets: https://m3.material.io/components/bottom-sheets
- Project already uses Compose BOM 2024.11.00 (includes ModalBottomSheet)
- Pattern matches existing BikeRedlights composable structure

---

## 3. Four-Tab Bottom Navigation

### Decision: Add STOPS as 3rd tab with `stop_circle` icon, use icon-only mode for inactive tabs

**Context**: Feature 011 adds "Stops" tab to existing 3-tab navigation (Live, Rides, Settings). Need to determine if 4 tabs fits Material 3 guidelines, select appropriate icon, and choose label display mode.

**Rationale**:
- **Material 3 Guidelines**: Navigation bars should contain 3-5 destinations. 4 tabs is fully supported and recommended.
- **Screen Fit**: On 360dp-420dp width screens, 4 tabs = 90-105dp per tab (well above 48dp minimum tap target). No scrolling needed.
- **Icon Selection**: `stop_circle` (StopCircle) chosen for:
  - Clear semantic meaning (represents "stops")
  - Visual distinction from existing icons (compass, list, settings)
  - Available in Material Icons Outlined
  - Perfect context fit for red light stop detection
- **Label Display**: Material 3 recommends icon-only for inactive tabs with 4+ destinations to conserve space

**Implementation Changes**:

1. **BottomNavDestination.kt**: Add STOPS enum value
   ```kotlin
   STOPS(
       route = "stops",
       label = "Stops",
       icon = "stop_circle"
   ),
   ```

2. **MainActivity.kt**:
   - Import `Icons.Outlined.StopCircle`
   - Change `alwaysShowLabel = false` (line 92)
   - Add STOPS case in icon when block:
     ```kotlin
     BottomNavDestination.STOPS -> Icon(
         imageVector = Icons.Outlined.StopCircle,
         contentDescription = destination.label
     )
     ```

3. **AppNavigation.kt**: Add Stops destination route
   ```kotlin
   composable(BottomNavDestination.STOPS.route) {
       StopsMapScreen()
   }
   ```

**Tab Order Recommendation**: Live → Rides → Stops → Settings
- Keeps Settings at conventional end position
- Places Stops next to Rides (both historical data review)
- Live remains at primary position (most frequent use)

**Alternatives Considered**:
- **place/location_on icons**: Too generic, might confuse with live tracking
- **flag icon**: Less clear semantic meaning for "stops"
- **pause_circle icon**: Could confuse with ride pause feature
- **5 tabs**: Would exceed recommendation, cramped on 360dp screens
- **Scrollable navigation**: Material 3 explicitly advises against scrollable bottom navigation

**Accessibility Considerations**:
- Content descriptions already implemented (✅)
- Labels are short enough to avoid truncation (✅)
- TalkBack testing required on emulator (pending)
- Icon-only mode still accessible via content descriptions

**Testing Checklist**:
- [ ] Verify tap targets comfortable on 360dp screen
- [ ] Test TalkBack navigation between tabs
- [ ] Verify dark mode theming
- [ ] Ensure active/inactive visual distinction is clear
- [ ] Test rapid tab switching (no lag/jank)

**References**:
- Material 3 Navigation Bar: https://m3.material.io/components/navigation-bar
- Current implementation: MainActivity.kt lines 67-120
- Material Icons: https://fonts.google.com/icons (stop_circle verified available)

---

## 4. GPS Coordinate Averaging for Cluster Centers

### Decision: Calculate cluster center as arithmetic mean of latitude/longitude

**Context**: Each cluster contains 2-50 stops with GPS coordinates. Need to determine single center point for marker placement on map.

**Rationale**:
- **Simplicity**: For small geographic areas (intersection clusters within 30m radius), arithmetic mean is sufficient
- **Performance**: Simple calculation, no complex spherical geometry needed
- **Accuracy**: Clusters are small enough that curvature of Earth doesn't significantly affect center calculation
- **BikeRedlights Context**: Stop clustering radius is 30m (from Feature 008 settings). Within this radius, flat-earth approximation is acceptable.

**Implementation**:
```kotlin
data class LatLng(val latitude: Double, val longitude: Double)

fun calculateClusterCenter(stops: List<Stop>): LatLng {
    require(stops.isNotEmpty()) { "Cannot calculate center of empty cluster" }

    val avgLat = stops.map { it.latitude }.average()
    val avgLng = stops.map { it.longitude }.average()

    return LatLng(latitude = avgLat, longitude = avgLng)
}
```

**Alternatives Considered**:
- **Geographic centroid (spherical)**: Overkill for 30m radius clusters. Would add complexity for negligible accuracy improvement.
- **Weighted average by duration**: Could weight by stop duration, but spec doesn't require this. Keep simple for MVP.
- **First stop coordinates**: Arbitrary and could misrepresent cluster location if first stop is outlier.

**Mathematical Justification**:
- At 30m scale, Earth's curvature introduces <0.001% error in distance calculation
- For intersection clusters (typical use case), arithmetic mean places marker near geographic center
- Google Maps API expects LatLng coordinates which work fine with averaged values

**Edge Cases**:
- Single-stop clusters: Center = stop's exact coordinates (averaging one value = that value)
- Clusters near poles: Not applicable (BikeRedlights targets cycling use cases in temperate latitudes)
- Clusters crossing 180° meridian: Unlikely for 30m radius clusters

**References**:
- Stop clustering radius: 30m (Feature 008 - stop detection settings)
- GPS precision: TrackPoint entities store latitude/longitude as Double (sufficient precision)

---

## Summary of Key Decisions

| Technical Area | Decision | Confidence |
|----------------|----------|------------|
| Marker Clustering | Manual cluster aggregation (no runtime clustering library) | ✅ High |
| Cluster Center Calculation | Arithmetic mean of lat/lng | ✅ High |
| Cluster Detail Popup | Material 3 ModalBottomSheet with conditional composition | ✅ High |
| Scrollable Stops List | LazyColumn with unique keys, max height 400.dp | ✅ High |
| Bottom Navigation (4 tabs) | Add STOPS as 3rd tab, icon-only for inactive | ✅ High |
| Stops Tab Icon | `stop_circle` (Icons.Outlined.StopCircle) | ✅ High |
| Label Display Mode | `alwaysShowLabel = false` (auto mode for 4+ tabs) | ✅ High |

All decisions align with Material Design 3 guidelines and BikeRedlights architecture standards.

---

## Next Steps

Phase 1 (Design & Contracts):
1. Generate data-model.md with domain entities
2. Create API contracts for use cases and repository methods
3. Generate quickstart.md developer guide
4. Update agent context with new feature details
5. Re-evaluate Constitution Check post-design
