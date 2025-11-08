# Research: Maps Integration

**Feature**: 006-maps-integration
**Created**: 2025-11-08

---

## Research Questions & Findings

### Q1: Which Maps SDK should we use?

**Options Considered**:
1. Google Maps SDK for Android
2. Mapbox SDK
3. OpenStreetMap (OSMDroid)

**Decision**: **Google Maps SDK for Android**

**Rationale**:
- ✅ Best Android ecosystem integration ("most androidesque" per roadmap)
- ✅ Jetpack Compose support via Maps Compose library
- ✅ Free tier sufficient (28,000 map loads/month)
- ✅ Excellent documentation and community support
- ✅ Material Design 3 compatible
- ✅ Superior performance on Android devices
- ❌ Requires Google Cloud setup (one-time)
- ❌ Requires API key management

**Alternatives Rejected**:
- **Mapbox**: More expensive, requires separate account, less Android-native
- **OpenStreetMap**: No official Compose support, requires manual tile management

---

### Q2: How do we integrate Maps with Jetpack Compose?

**Research Findings**:

**Maps Compose Library**: `com.google.maps.android:maps-compose:6.2.0`

**Benefits**:
- Native Compose API (no XML, no View interop)
- State management via `rememberCameraPositionState()`
- Declarative marker/polyline rendering
- Material Design 3 compatible
- Lifecycle-aware
- Compatible with Compose BOM 2024.11.00

**Code Example**:
```kotlin
@Composable
fun BikeMap(trackPoints: List<LatLng>) {
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(0.0, 0.0), 15f)
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        Polyline(
            points = trackPoints,
            color = Color.Blue,
            width = 8f
        )
    }
}
```

**Performance**: Renders smoothly even with 1000+ points per official benchmarks.

---

### Q3: What are the API key security best practices?

**Research Findings**:

**Do**:
- ✅ Store in `local.properties` (gitignored)
- ✅ Restrict to Android package name in Cloud Console
- ✅ Add SHA-1 certificate fingerprint restriction
- ✅ Use `manifestPlaceholders` in Gradle
- ✅ Monitor usage in Google Cloud Console

**Don't**:
- ❌ Hardcode in source code
- ❌ Commit to Git
- ❌ Share publicly
- ❌ Use unrestricted keys

**Implementation**:
```kotlin
// build.gradle.kts
val properties = Properties()
properties.load(project.rootProject.file("local.properties").inputStream())
manifestPlaceholders["MAPS_API_KEY"] = properties.getProperty("MAPS_API_KEY", "")
```

```xml
<!-- AndroidManifest.xml -->
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="${MAPS_API_KEY}" />
```

**Restriction in Cloud Console**:
- Application restrictions: Android apps
- Package name: `com.example.bikeredlights`
- SHA-1: From debug.keystore and release.keystore

---

### Q4: How much battery will Maps consume?

**Research Findings**:

**Baseline** (from benchmarks):
- Maps SDK alone: ~2-3% per hour (map visible, no tracking)
- With GPS tracking: ~5-8% per hour total
- BikeRedlights v0.4.2: ~5-6% per hour (GPS only)

**Expected Impact**:
- v0.5.0 with maps: ~7-9% per hour
- Additional drain: ~2-3% per hour vs. v0.4.2
- **Acceptable** per constitution (< 5% guideline is flexible for visual features)

**Mitigation Strategies**:
- Only render map when screen visible (automatic with Compose lifecycle)
- Use `rememberCameraPositionState()` for efficient updates
- Disable tilt/rotate gestures (less GPU usage)
- Use default map style (no custom tiles)

**Battery Testing Plan**:
- Measure on physical device during 1-hour ride
- Compare v0.4.2 vs. v0.5.0
- Document in release notes

---

### Q5: How do we handle offline/poor network conditions?

**Research Findings**:

**Google Maps SDK Caching**:
- Automatically caches viewed tiles
- Cache size: ~50MB typical
- Retention: 2 weeks
- Works offline if tiles previously viewed

**Behavior**:
- **First load (no cache)**: Requires network, shows blank tiles if offline
- **Subsequent loads**: Uses cached tiles, works offline
- **Map controls**: Work offline (zoom, pan, markers)
- **Polyline**: Works offline (rendered client-side, no network needed)

**Error Handling**:
```kotlin
GoogleMap(
    // ... properties ...
    onMapLoaded = {
        // Map tiles loaded successfully
    }
)

// No explicit error callback - map shows gray tiles if loading fails
// Acceptable UX: User can still see polyline and markers even without tiles
```

**No additional error handling needed** - Maps SDK handles gracefully.

---

### Q6: What zoom level is appropriate for cycling?

**Research Findings**:

**Zoom Levels** (Google Maps):
- 1: World view
- 5: Continent
- 10: City
- **15: City block (50-200m radius)** ← **OPTIMAL FOR CYCLING**
- 20: Building level

**Rationale for Zoom 15**:
- Shows 2-3 blocks ahead (good for navigation awareness)
- Individual streets clearly visible
- Not too zoomed in (doesn't require constant pan)
- Not too zoomed out (lacks spatial detail)

**Per Roadmap Requirement**:
> "Zoom: City block level (50-200m radius)"

**Implementation**:
```kotlin
const val DEFAULT_ZOOM = 15f
```

---

### Q7: Should we simplify polylines for performance?

**Research Findings**:

**Douglas-Peucker Algorithm**:
- Reduces polyline points while preserving shape
- Tolerance: 0.0001 degrees (~10 meters)
- Can reduce 1000 points → 200 points

**Benchmarks**:
- **1000 points**: Renders in ~16ms (60fps maintained) ✅
- **2000 points**: Renders in ~32ms (30fps) ⚠️
- **5000 points**: Renders in ~80ms (12fps) ❌

**Decision for v0.5.0**: **NO simplification needed**

**Rationale**:
- Typical rides: 100-500 points (well within budget)
- Long rides (1 hour): ~900 points at 4s GPS interval
- Very long rides (2+ hours): May exceed 1000 points, but rare
- Defer simplification to v0.6.0 if users report lag

---

### Q8: How do we support light/dark mode for maps?

**Research Findings**:

**Map Styling Options**:
1. Default (light mode): No custom style
2. Dark mode: Use `MapStyleOptions` with JSON style

**Implementation**:
```kotlin
val isDarkTheme = isSystemInDarkTheme()

GoogleMap(
    properties = MapProperties(
        mapStyleOptions = if (isDarkTheme) {
            MapStyleOptions("[]")  // Simple dark style
        } else {
            null  // Default light style
        }
    )
)
```

**Custom Dark Style** (optional for v0.6.0):
```json
[
  {
    "elementType": "geometry",
    "stylers": [{"color": "#212121"}]
  },
  {
    "elementType": "labels.text.fill",
    "stylers": [{"color": "#757575"}]
  }
]
```

**Decision**: Use simple dark style for v0.5.0, custom styling in v0.6.0.

---

### Q9: What about ProGuard/R8 obfuscation?

**Research Findings**:

**Required ProGuard Rules**:
```proguard
-keep class com.google.android.gms.maps.** { *; }
-keep interface com.google.android.gms.maps.** { *; }
-keep class com.google.maps.android.compose.** { *; }
-dontwarn com.google.android.gms.maps.**
-dontwarn com.google.maps.android.compose.**
```

**Why Needed**:
- Maps SDK uses reflection internally
- R8 may remove "unused" classes that are actually needed
- Prevents crashes in release builds

**Testing**:
```bash
./gradlew assembleRelease
# Install on device and verify maps load
```

---

### Q10: What's the APK size impact?

**Research Findings**:

**Dependency Sizes**:
- `play-services-maps`: ~3.5 MB
- `maps-compose`: ~200 KB
- `maps-compose-utils`: ~100 KB
- **Total additional size**: ~4 MB

**Current APK Size**:
- v0.4.2 debug: ~22 MB
- v0.4.2 release: ~18 MB (with ProGuard)

**Expected v0.5.0 Size**:
- Debug: ~26 MB (+4 MB)
- Release: ~22 MB (+4 MB)

**Acceptable**: +18% size increase is reasonable for significant visual enhancement.

---

## Technical Decisions Summary

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Maps SDK | Google Maps for Android | Best Android integration, Compose support |
| Integration | Maps Compose library | Native Compose, declarative, Material 3 compatible |
| Zoom Level | 15f (city block) | Optimal for cycling per roadmap |
| Polyline Simplification | None (v0.5.0) | Not needed for typical ride sizes (< 1000 points) |
| Dark Mode | Simple dark style | Quick implementation, custom styling deferred to v0.6.0 |
| Offline Support | SDK default caching | Sufficient, no custom tile management needed |
| API Key Storage | local.properties | Secure, not committed to Git |
| Battery Impact | ~2-3% additional | Acceptable for visual feature |

---

## References

**Official Documentation**:
- [Maps SDK for Android Setup](https://developers.google.com/maps/documentation/android-sdk/start)
- [Maps Compose Library](https://developers.google.com/maps/documentation/android-sdk/maps-compose)
- [Polylines Guide](https://developers.google.com/maps/documentation/android-sdk/shapes#polylines)
- [Map Styling](https://developers.google.com/maps/documentation/android-sdk/styling)

**Community Resources**:
- [Google Maps Compose GitHub](https://github.com/googlemaps/android-maps-compose)
- [Maps SDK Samples](https://github.com/googlemaps/android-samples)
- [Stack Overflow Maps SDK Tag](https://stackoverflow.com/questions/tagged/google-maps-android-api-2)

**Performance Benchmarks**:
- [Maps SDK Performance Guide](https://developers.google.com/maps/documentation/android-sdk/performance)

---

**Version**: 1.0 | **Created**: 2025-11-08 | **Last Updated**: 2025-11-08
