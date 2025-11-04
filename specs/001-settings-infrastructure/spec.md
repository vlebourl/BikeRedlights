# Feature Specification: Basic Settings Infrastructure

**Feature Branch**: `001-settings-infrastructure`
**Created**: 2025-11-04
**Status**: Draft
**Input**: User description: "Feature 2A: Basic Settings Infrastructure"

## User Scenarios & Testing *(mandatory)*

### User Story 1 - Configure Units Preference (Priority: P1)

A cyclist wants to view speed and distance in their preferred measurement system (metric or imperial) based on their geographic location and personal preference.

**Why this priority**: Foundational preference that affects all speed/distance displays. Must be implemented first to support Feature 1A (Core Ride Recording). Without this, the app would be unusable for users who prefer imperial units.

**Independent Test**: Can be fully tested by navigating to Settings tab, changing Units setting from Metric to Imperial, returning to Live tab, and verifying speed display changes from km/h to mph. Delivers immediate value by personalizing the app for the user's location.

**Acceptance Scenarios**:

1. **Given** user opens app for first time, **When** they navigate to Live tab, **Then** speed displays in km/h (metric default)
2. **Given** user is on Settings tab, **When** they tap "Ride & Tracking" card, **Then** they see Units setting with Metric selected
3. **Given** user is on Units setting screen, **When** they tap "Imperial" option, **Then** selection changes to Imperial
4. **Given** user has selected Imperial units, **When** they return to Live tab, **Then** speed displays in mph
5. **Given** user has changed units to Imperial, **When** they close and reopen app, **Then** Imperial units persist
6. **Given** user is viewing a saved ride (future feature), **When** they have Imperial units selected, **Then** distance shows in miles

---

### User Story 2 - Adjust GPS Accuracy for Battery Life (Priority: P2)

A delivery rider working long shifts wants to extend battery life by reducing GPS update frequency while still tracking their rides accurately enough for business purposes.

**Why this priority**: Important for delivery riders (key user segment) but not critical for basic functionality. Users can operate with default High Accuracy setting. Enables battery optimization without sacrificing core tracking capability.

**Independent Test**: Can be fully tested by setting GPS Accuracy to "Battery Saver", starting a ride, and measuring battery consumption over time compared to "High Accuracy" mode. Delivers value by extending device battery life during long delivery shifts.

**Acceptance Scenarios**:

1. **Given** user opens GPS Accuracy setting, **When** they view options, **Then** "High Accuracy" is selected by default
2. **Given** user is on GPS Accuracy setting, **When** they tap "Battery Saver", **Then** selection changes to Battery Saver
3. **Given** user has Battery Saver enabled, **When** they start recording a ride, **Then** location updates occur every 3-5 seconds (vs 1 second)
4. **Given** user is recording with Battery Saver, **When** they check battery usage, **Then** GPS consumption is reduced compared to High Accuracy
5. **Given** user has Battery Saver enabled, **When** they close and reopen app, **Then** Battery Saver setting persists

---

### User Story 3 - Enable Auto-Pause for Commutes (Priority: P3)

A commuter cycling through urban areas with frequent stops at red lights and traffic doesn't want their ride statistics inflated by time spent stationary.

**Why this priority**: Nice-to-have quality-of-life feature. Core ride tracking works fine without it (user can manually stop/start rides). Most valuable for commuters in stop-heavy environments, less critical for continuous rural rides.

**Independent Test**: Can be fully tested by enabling Auto-Pause with 5-minute threshold, starting a ride, remaining stationary for 5+ minutes, and verifying ride duration/distance freeze with "⏸️ Paused" indicator. Delivers value by providing more accurate "moving time" statistics.

**Acceptance Scenarios**:

1. **Given** user opens Auto-Pause setting, **When** they view options, **Then** toggle is OFF by default
2. **Given** user is on Auto-Pause setting, **When** they tap toggle ON, **Then** number picker appears with "5 minutes" selected
3. **Given** user has Auto-Pause enabled, **When** they change threshold to 3 minutes, **Then** selection updates to 3 minutes
4. **Given** user is recording with Auto-Pause enabled, **When** they remain stationary (speed < 1 km/h) for configured duration, **Then** ride pauses automatically
5. **Given** ride is auto-paused, **When** user starts moving again (speed > 1 km/h), **Then** ride resumes automatically
6. **Given** ride is auto-paused, **When** user views Live tab, **Then** "⏸️ Paused" indicator displays and duration stops incrementing
7. **Given** user has Auto-Pause configured, **When** they close and reopen app, **Then** Auto-Pause setting persists (both enabled state and threshold value)

---

### Edge Cases

- What happens when user changes units setting during an active ride recording? (Setting change takes effect immediately; in-progress ride continues tracking in new units)
- What happens when user rapidly toggles between Metric and Imperial multiple times? (Each change persists immediately to DataStore; last selection wins)
- What happens when DataStore read fails on app startup? (App defaults to Metric, High Accuracy, Auto-Pause OFF; user can still navigate and update settings)
- What happens when user selects Auto-Pause threshold but toggle is OFF? (Threshold value is saved but not applied; becomes active when toggle is turned ON)
- What happens when user navigates away from settings screen before toggle animation completes? (Change persists immediately; animation is purely visual feedback)
- What happens when device locale uses imperial system by default? (App still defaults to Metric; user must explicitly change to Imperial if desired)
- What happens when user sets Auto-Pause to 1 minute but frequently stops for < 1 minute at red lights? (Ride never pauses; threshold intentionally designed for longer stops like lunch breaks)

## Requirements *(mandatory)*

### Functional Requirements

- **FR-001**: System MUST provide a Settings tab accessible via bottom navigation bar (3rd position)
- **FR-002**: System MUST display Settings home screen with card-based layout following Material 3 design guidelines
- **FR-003**: System MUST display "🚴 Ride & Tracking" card with subtitle "Units, GPS, Auto-pause" on Settings home screen
- **FR-004**: System MUST navigate to Ride & Tracking detail screen when user taps the card
- **FR-005**: System MUST provide Units setting with two mutually exclusive options: Metric and Imperial
- **FR-006**: System MUST render Units setting using Material 3 SegmentedButton component with two segments
- **FR-007**: System MUST default Units setting to Metric on first app launch
- **FR-008**: System MUST apply Units setting across entire app (current speed, ride distance, historical data)
- **FR-009**: System MUST display speed in km/h when Metric is selected
- **FR-010**: System MUST display speed in mph when Imperial is selected (conversion factor: 0.621371)
- **FR-011**: System MUST display distance in km when Metric is selected
- **FR-012**: System MUST display distance in miles when Imperial is selected (conversion factor: 0.621371)
- **FR-013**: System MUST provide GPS Accuracy setting with two mutually exclusive options: Battery Saver and High Accuracy
- **FR-014**: System MUST render GPS Accuracy setting using Material 3 SegmentedButton component with two segments
- **FR-015**: System MUST default GPS Accuracy to High Accuracy on first app launch
- **FR-016**: System MUST configure location updates at 1-second intervals when High Accuracy is selected
- **FR-017**: System MUST configure location updates at 3-5 second intervals when Battery Saver is selected
- **FR-018**: System MUST provide Auto-Pause Rides setting with toggle switch and duration picker
- **FR-019**: System MUST default Auto-Pause to OFF on first app launch
- **FR-020**: System MUST default Auto-Pause duration to 5 minutes when toggle is first enabled
- **FR-021**: System MUST provide duration picker with options: 1, 2, 3, 5, 10, 15 minutes
- **FR-022**: System MUST only apply Auto-Pause logic when toggle is ON during ride recording
- **FR-023**: System MUST pause ride (freeze duration/distance updates) when speed < 1 km/h for configured duration threshold
- **FR-024**: System MUST resume ride automatically when speed > 1 km/h after auto-pause
- **FR-025**: System MUST display "⏸️ Paused" indicator on Live tab when ride is auto-paused
- **FR-026**: System MUST persist all settings changes immediately to local storage
- **FR-027**: System MUST restore all settings from local storage on app startup
- **FR-028**: System MUST handle DataStore read failures gracefully by using default values
- **FR-029**: System MUST handle DataStore write failures gracefully without crashing the app
- **FR-030**: System MUST support Android back button navigation from Ride & Tracking detail screen to Settings home

### Key Entities

- **UnitsPreference**: User's measurement system choice
  - Possible values: METRIC, IMPERIAL
  - Affects: Speed display (km/h vs mph), Distance display (km vs miles)
  - Default: METRIC
  - Persisted: DataStore key `units_system` (string: "metric" or "imperial")

- **GpsAccuracyPreference**: User's location update frequency choice
  - Possible values: BATTERY_SAVER, HIGH_ACCURACY
  - Affects: LocationRepository update interval (3-5s vs 1s)
  - Default: HIGH_ACCURACY
  - Persisted: DataStore key `gps_accuracy` (string: "battery_saver" or "high_accuracy")

- **AutoPauseConfiguration**: User's ride auto-pause settings
  - Enabled: Boolean (ON/OFF)
  - Threshold: Integer (1-15 minutes)
  - Affects: TrackLocationUseCase stationary detection logic
  - Default: Enabled = false, Threshold = 5 minutes
  - Persisted: DataStore keys `auto_pause_enabled` (boolean) and `auto_pause_minutes` (integer)

## Success Criteria *(mandatory)*

### Measurable Outcomes

- **SC-001**: Users can navigate from any screen to Settings tab and back within 2 taps
- **SC-002**: Users can change Units setting and see speed display update within 1 second
- **SC-003**: Settings changes persist across app restarts 100% of the time (tested with 20 restart cycles)
- **SC-004**: Users with Battery Saver enabled observe 30-50% reduction in GPS-related battery consumption during 1-hour ride compared to High Accuracy
- **SC-005**: Users with Auto-Pause enabled at 5-minute threshold automatically pause after remaining stationary for 5 minutes (+/- 10 seconds)
- **SC-006**: Settings UI renders and responds to user input within 100ms on mid-range Android devices (API 26+)
- **SC-007**: 100% of settings changes save successfully to DataStore under normal conditions (tested with 100 consecutive saves)
- **SC-008**: App launches successfully and displays default settings within 2 seconds even when DataStore fails to load (fallback defaults applied)
- **SC-009**: Settings screens maintain readability at 200% system font scaling (accessibility requirement)
- **SC-010**: All interactive elements meet 48dp minimum touch target size (accessibility requirement)

## Assumptions *(optional)*

### Technical Assumptions

1. **DataStore Availability**: Jetpack DataStore Preferences library is already configured in the project (confirmed in v0.1.0)
2. **Material 3 Components**: Material Design 3 Compose components (Card, SegmentedButton, Switch) are available via Compose BOM 2024.11.00
3. **Bottom Navigation**: Bottom navigation structure will be shared with Feature 1A (Core Ride Recording) and Feature 3 (Ride History)
4. **Location Tracking**: LocationRepository from v0.1.0 can accept configurable update intervals
5. **Stationary Detection**: TrackLocationUseCase can detect speed < 1 km/h threshold from GPS data
6. **Android Version**: App targets API 26+ (Android 8.0+) per CLAUDE.md guidelines

### User Behavior Assumptions

1. **Units Preference**: Users prefer consistent units across app; changing mid-ride is rare edge case
2. **GPS Accuracy**: Most users will keep default High Accuracy; Battery Saver is opt-in for power users
3. **Auto-Pause**: Feature is primarily for urban commuters; rural/highway riders will keep it OFF
4. **Settings Discovery**: Users will explore Settings tab naturally via bottom navigation
5. **Persistence Expectation**: Users expect settings to persist indefinitely (no expiration)

### Business Assumptions

1. **No Cloud Sync**: Settings are device-local only (no account system or cloud backup in v0.2.0)
2. **No A/B Testing**: Single default values for all users (no personalization based on locale/demographics)
3. **No Analytics**: Settings changes are not tracked or reported (privacy-first approach)

## Out of Scope *(optional)*

### Explicitly Excluded from Feature 2A

1. **Stop Detection Settings**: Speed threshold, duration threshold, and clustering radius settings deferred to Feature 2B (separate card, implemented before Feature 4)
2. **App Behavior Settings**: Theme selection (light/dark/system), language, notifications - deferred to future version
3. **Data Management Settings**: Export, clear data, backup/restore - deferred to future version
4. **About Screen**: App version, privacy policy, licenses, open-source credits - deferred to future version
5. **Advanced GPS Settings**: Location provider selection, GPS accuracy filters, satellite count display - not needed for MVP
6. **Custom Auto-Pause Speeds**: Only < 1 km/h threshold supported; user cannot configure custom speed threshold
7. **Auto-Resume Delay**: Ride resumes immediately when speed > 1 km/h; no configurable delay before resuming
8. **Settings Import/Export**: Users cannot share settings between devices or backup configurations
9. **Settings Reset**: No "Reset to Defaults" button; user must manually change each setting

### Future Enhancements (Post-v0.2.0)

1. **Feature 2B (v0.6.0)**: Add "🚦 Stop Detection" card with stop-related settings
2. **Theme Settings**: Allow manual theme selection (light/dark/system) - Material 3 dynamic color already works
3. **Units by Context**: Automatically switch units based on ride location (e.g., Metric in Europe, Imperial in USA)
4. **Advanced Auto-Pause**: Configurable speed threshold (not just < 1 km/h), separate thresholds for different ride types
5. **Settings Search**: Search bar to quickly find specific settings as app grows
6. **Settings Backup**: Export/import settings to file for migration between devices
7. **Onboarding Flow**: Guide new users through essential settings on first launch

## Dependencies *(optional)*

### Internal Dependencies (BikeRedlights Codebase)

- **v0.1.0 Foundation**:
  - DataStore Preferences (already configured)
  - Material 3 Compose (already configured)
  - LocationRepository with TrackLocationUseCase (for GPS Accuracy and Auto-Pause integration)
  - Bottom navigation structure (will be created in this feature, shared with F1A/F3)

- **Feature Integration Points**:
  - **Feature 1A (Core Ride Recording)**: Will read Units, GPS Accuracy, and Auto-Pause settings from DataStore
  - **Feature 2B (Stop Detection Settings)**: Will extend Settings home screen with additional card
  - **Feature 3 (Ride History)**: Will read Units setting for displaying historical ride distances

### External Dependencies

- **Jetpack DataStore Preferences 1.1.1**: For settings persistence
- **Jetpack Compose BOM 2024.11.00**: For Material 3 UI components
- **Material 3 Components**:
  - `androidx.compose.material3.Card`: For settings card layout
  - `androidx.compose.material3.SegmentedButton`: For Units and GPS Accuracy settings
  - `androidx.compose.material3.Switch`: For Auto-Pause toggle
  - `androidx.compose.material3.NavigationBar`: For bottom navigation
- **Kotlin Coroutines 1.9.0**: For async DataStore operations
- **Kotlin Flow**: For reactive settings state management

### No External API Dependencies

- No Google Maps SDK required (that's Feature 1B)
- No cloud services or backend APIs
- No third-party analytics or crash reporting
- Purely local device storage

## Notes *(optional)*

### Design Decisions

**1. Why Card-Based Settings Home?**
- Scalable: Easy to add new category cards (Feature 2B will add "🚦 Stop Detection" card)
- Visual hierarchy: Each card groups related settings clearly
- Material 3 alignment: Cards are a core M3 component for content grouping
- User-friendly: Reduces cognitive load compared to flat list of 10+ settings

**2. Why SegmentedButton for Units and GPS Accuracy?**
- Material 3 guideline: Recommended for 2-3 mutually exclusive options
- Glanceable: Both options visible simultaneously (no dropdown hunting)
- Bike-friendly: Large touch targets (entire segment is tappable)
- Clear state: Selected option is visually obvious

**3. Why Auto-Pause Defaults to OFF?**
- Conservative: Users might not understand auto-pause initially
- Opt-in: Advanced feature for specific use cases (urban commuting)
- No surprises: Ride behavior matches user expectation (always recording)
- Explainable: User consciously enables and understands threshold

**4. Why 1-15 Minute Range for Auto-Pause?**
- < 1 minute: Too short - would trigger at every red light (annoying)
- 1-5 minutes: Useful for lunch breaks, quick errands during commute
- 10-15 minutes: Useful for longer stops (grocery shopping mid-ride)
- > 15 minutes: If stopped that long, user should manually stop ride

**5. Why Speed < 1 km/h for Stationary Detection?**
- GPS Noise: Even when stationary, GPS often reports 0.5-1 km/h due to accuracy drift
- Conservative: 1 km/h ensures true stationary state (not just slow crawling)
- Consistent: Matches v0.1.0 stationary detection for "0 km/h" display

### Implementation Guidance

**DataStore Schema:**
```kotlin
// Key definitions
object PreferencesKeys {
    val UNITS_SYSTEM = stringPreferencesKey("units_system")
    val GPS_ACCURACY = stringPreferencesKey("gps_accuracy")
    val AUTO_PAUSE_ENABLED = booleanPreferencesKey("auto_pause_enabled")
    val AUTO_PAUSE_MINUTES = intPreferencesKey("auto_pause_minutes")
}

// Default values
enum class UnitsSystem { METRIC, IMPERIAL }
enum class GpsAccuracy { BATTERY_SAVER, HIGH_ACCURACY }

// Conversion factors
const val KMH_TO_MPH = 0.621371
const val KM_TO_MILES = 0.621371
```

**Bottom Navigation Structure:**
```kotlin
// Tab order for v0.2.0-v0.5.0 (3 tabs)
enum class BottomNavDestination {
    LIVE,      // 🚴 Live - default
    RIDES,     // 📊 Rides (added in F3)
    SETTINGS   // ⚙️ Settings
}

// Tab order for v0.6.0+ (4 tabs, after Feature 6)
enum class BottomNavDestination {
    LIVE,      // 🚴 Live
    RIDES,     // 📊 Rides
    STOPS,     // 🚦 Stops (added in F6)
    SETTINGS   // ⚙️ Settings
}
```

**Settings Screen Navigation:**
```
Bottom Nav: Settings Tab
    ↓
Settings Home Screen (Card List)
    - "🚴 Ride & Tracking" card
    ↓ (tap card)
Ride & Tracking Detail Screen
    - Units: [Metric | Imperial]
    - GPS Accuracy: [Battery Saver | High Accuracy]
    - Auto-Pause: [Toggle] + [Duration Picker]
    ↓ (back button)
Settings Home Screen
```

### Testing Guidance

**Unit Tests Required:**
- SettingsRepository DataStore read/write operations
- Units conversion utility functions (km/h ↔ mph, km ↔ miles)
- GPS Accuracy value mapping (enum ↔ update interval)
- Auto-Pause threshold value validation (1-15 range)

**UI Tests Required:**
- Settings tab navigation from bottom nav
- Card tap navigation to detail screen
- SegmentedButton selection changes
- Toggle switch state changes
- Number picker value selection
- Settings persistence across app restart (instrumented test)

**Integration Tests Required:**
- Settings changes reflected in LocationRepository update interval
- Units changes reflected in speed/distance displays
- Auto-Pause logic triggered correctly during ride recording

**Emulator Testing Checklist:**
- Install debug APK on emulator
- Navigate to Settings tab
- Change each setting and verify persistence
- Test dark mode (toggle in emulator settings)
- Test 200% font scaling (emulator accessibility settings)
- Test back navigation from detail screen
- Restart app and verify all settings persist
- Test TalkBack (screen reader) navigation

### Accessibility Notes

**Semantic Labels Required:**
- Bottom nav Settings tab: "Settings"
- Ride & Tracking card: "Ride and Tracking settings. Tap to configure units, GPS accuracy, and auto-pause."
- Units SegmentedButton: "Units preference. Currently set to Metric. Tap Imperial to switch to miles and miles per hour."
- GPS Accuracy SegmentedButton: "GPS accuracy mode. Currently set to High Accuracy. Tap Battery Saver to reduce battery usage."
- Auto-Pause toggle: "Auto-pause rides. Currently off. Tap to enable automatic ride pausing when stationary."
- Duration picker: "Auto-pause threshold. Currently 5 minutes. Tap to select duration."

**Color Contrast:**
- All text meets WCAG AA standards (4.5:1 ratio for body text)
- Selected state clearly distinguishable without color alone (Material 3 handles this)

**Touch Targets:**
- Bottom nav tabs: 80dp height (Material 3 default)
- Settings cards: Minimum 64dp height
- SegmentedButton segments: Minimum 48dp height
- Toggle switch: 48dp × 48dp touch area
- Duration picker items: Minimum 48dp height

**Dynamic Type Support:**
- All text scales with system font size setting
- Test at 100%, 150%, 200% scaling
- Layout adapts without text truncation or overlap
