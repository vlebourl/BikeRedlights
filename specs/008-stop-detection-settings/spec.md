# Feature Specification: Stop Detection Settings

**Feature Branch**: `008-stop-detection-settings`
**Created**: 2025-11-18
**Status**: Draft
**Release Type**: Minor (v0.7.0 → v0.8.0)
**Input**: User description: "Feature 008 - Stop Detection Settings (Roadmap Feature 2B). Use the neo4j mcp to get a full view and the project and make correct assumptions. use the roadmap file for a reference of the feature to develop."

## User Scenarios & Testing

### User Story 1 - Configure Speed Detection Threshold (Priority: P1)

As an urban cyclist who frequently encounters traffic lights, I want to configure the speed threshold that determines when I'm considered "stopped" so the app accurately detects when I'm waiting at red lights versus just slowing down for turns or obstacles.

**Why this priority**: This is the foundational setting that defines what constitutes a "stop" in the detection algorithm. Without this, the app cannot distinguish between being stationary at a traffic light versus momentarily slowing down. Urban cyclists need different sensitivity than suburban riders due to different traffic patterns.

**Independent Test**: Can be fully tested by navigating to Settings → Stop Detection, changing the speed threshold value, saving it, restarting the app, and verifying the value persists. The setting will be consumed by the stop detection feature (Feature 009) but can be independently configured and validated.

**Acceptance Scenarios**:

1. **Given** I am on the Stop Detection settings screen, **When** I view the speed threshold options, **Then** I see choices ranging from 1 to 5 km/h (or equivalent in mph if using Imperial units)
2. **Given** I have not configured stop detection before, **When** I first view the settings, **Then** the speed threshold defaults to 3 km/h (approximately 1.9 mph)
3. **Given** I am on the Stop Detection settings screen, **When** I select a speed threshold of 2 km/h and save, **Then** the setting persists across app restarts
4. **Given** I have Imperial units enabled in app settings, **When** I view the speed threshold options, **Then** values display in mph with proper conversion (1 km/h = 0.62 mph, etc.)
5. **Given** I select a speed threshold value, **When** I navigate away and return, **Then** my selected value remains active

---

### User Story 2 - Configure Duration Threshold (Priority: P1)

As a cyclist who rides in different environments (urban vs suburban), I want to configure how long I must remain stationary before counting as a "stop" so the app doesn't record brief slow-downs as red light stops.

**Why this priority**: This setting is equally critical to speed threshold - it filters out momentary pauses from genuine traffic light stops. Urban riders with short light cycles need lower thresholds (5-10s), while suburban riders need higher thresholds (20-30s) to avoid false positives from rolling stops or brief yielding.

**Independent Test**: Can be fully tested by navigating to Stop Detection settings, selecting different duration thresholds (5s, 10s, 15s, 20s, 25s, 30s), saving, and verifying persistence. Delivers immediate configuration value even before stop detection implementation.

**Acceptance Scenarios**:

1. **Given** I am on the Stop Detection settings screen, **When** I view duration threshold options, **Then** I see choices: 5 seconds, 10 seconds, 15 seconds, 20 seconds, 25 seconds, 30 seconds
2. **Given** I have not configured stop detection before, **When** I first view the settings, **Then** the duration threshold defaults to 15 seconds
3. **Given** I am on the Stop Detection settings screen, **When** I select a duration threshold of 10 seconds and save, **Then** the setting persists across app restarts
4. **Given** I select a duration threshold, **When** I change it multiple times, **Then** the most recent selection is always saved and displayed
5. **Given** I have configured a custom duration threshold, **When** future stop detection runs, **Then** only stops exceeding this duration will be recorded

---

### User Story 3 - Configure Clustering Radius (Priority: P2)

As a cyclist who rides through the same neighborhoods regularly, I want to configure the clustering radius so the app groups nearby stops at the same intersection as a single "red light location" rather than creating duplicate entries for slight GPS variations.

**Why this priority**: While important for data quality in future clustering (Feature 010), this setting is less critical for initial stop detection. It prepares the infrastructure for stop clustering but doesn't affect basic stop recording. Riders in dense urban areas need smaller radii (10-15m) while suburban riders benefit from larger radii (30-50m).

**Independent Test**: Can be fully tested by accessing Stop Detection settings, selecting different clustering radius values (10m, 15m, 20m, 25m, 30m, 40m, 50m), and verifying persistence. The value will be used by clustering algorithms in Feature 010 but can be independently configured now.

**Acceptance Scenarios**:

1. **Given** I am on the Stop Detection settings screen, **When** I view clustering radius options, **Then** I see choices: 10m, 15m, 20m, 25m, 30m, 40m, 50m
2. **Given** I have not configured stop detection before, **When** I first view the settings, **Then** the clustering radius defaults to 20 meters
3. **Given** I am on the Stop Detection settings screen, **When** I select a clustering radius of 30 meters and save, **Then** the setting persists across app restarts
4. **Given** I select a clustering radius value, **When** I navigate away and return, **Then** my selected value remains active
5. **Given** I have configured a clustering radius, **When** future clustering algorithms run, **Then** stops within this distance are grouped as the same location

---

### User Story 4 - Access Stop Detection Settings (Priority: P1)

As a user of the BikeRedlights app, I want to easily find and access stop detection configuration options from the main Settings screen so I can customize detection behavior without searching through multiple menus.

**Why this priority**: This is the entry point for all stop detection configuration. Without clear navigation to these settings, users cannot configure any of the detection parameters. Must be immediately discoverable.

**Independent Test**: Can be fully tested by opening the app, navigating to Settings tab, verifying the "Stop Detection" card appears, tapping it, and confirming navigation to the detail screen with all three settings visible.

**Acceptance Scenarios**:

1. **Given** I am on the Settings home screen, **When** I view available setting categories, **Then** I see a "Stop Detection" card with title and subtitle "Thresholds, Clustering"
2. **Given** I am on the Settings home screen, **When** I tap the "Stop Detection" card, **Then** I navigate to a detail screen showing three settings: Speed Threshold, Duration Threshold, and Clustering Radius
3. **Given** I am on the Stop Detection detail screen, **When** I view the screen, **Then** each setting displays its current value, description, and available options
4. **Given** I am on the Stop Detection detail screen, **When** I tap the back button, **Then** I return to the Settings home screen
5. **Given** I have made changes to settings, **When** I navigate back, **Then** my changes are saved automatically

---

### Edge Cases

- What happens when a user tries to set an invalid speed threshold (e.g., 0 km/h or 10 km/h)? The UI controls should only present valid options (1-5 km/h), preventing invalid input.
- What happens when a user switches between Metric and Imperial units after configuring speed threshold? The displayed value should convert correctly (e.g., 3 km/h becomes 1.9 mph), but the underlying stored value remains in km/h.
- What happens if the DataStore file becomes corrupted or is deleted? The app should fall back to default values (3 km/h, 15s, 20m) and recreate the settings.
- What happens when a user sets clustering radius very small (10m) in an area with poor GPS accuracy? The setting should still persist, but future clustering may create more separate clusters - this is expected behavior based on user preference.
- What happens when settings are accessed while a ride is actively recording? Changes should save normally, but the current ride continues using the settings active when it started (changes apply to future rides only).
- What happens on first app launch with no prior settings? Default values (3 km/h, 15s, 20m) are applied automatically without user intervention.

## Requirements

### Functional Requirements

- **FR-001**: System MUST display a "Stop Detection" settings card on the Settings home screen with title and subtitle "Thresholds, Clustering"
- **FR-002**: System MUST navigate to a Stop Detection detail screen when the settings card is tapped
- **FR-003**: System MUST provide a Speed Threshold setting with selectable values: 1, 2, 3, 4, 5 km/h
- **FR-004**: System MUST provide a Duration Threshold setting with selectable values: 5, 10, 15, 20, 25, 30 seconds
- **FR-005**: System MUST provide a Clustering Radius setting with selectable values: 10, 15, 20, 25, 30, 40, 50 meters
- **FR-006**: System MUST set default values on first launch: 3 km/h (speed), 15 seconds (duration), 20 meters (radius)
- **FR-007**: System MUST persist all three settings across app restarts using local storage
- **FR-008**: System MUST convert speed threshold display to mph when Imperial units are selected, using conversion factor 0.621371
- **FR-009**: System MUST store speed threshold values internally in km/h regardless of display units
- **FR-010**: System MUST display each setting with a clear descriptive label and explanation of what it controls
- **FR-011**: System MUST prevent selection of values outside the defined ranges for each setting
- **FR-012**: System MUST display current selected values for all three settings when the detail screen loads
- **FR-013**: System MUST save setting changes immediately when user selects a new value
- **FR-014**: System MUST maintain consistent UI styling with existing settings screens (Material Design 3)

### Key Entities

- **Stop Detection Configuration**: Settings bundle containing three values:
  - Speed threshold (float, 1.0-5.0 km/h): Minimum speed below which rider is considered stopped
  - Duration threshold (integer, 5-30 seconds): Minimum time stationary required to count as a stop
  - Clustering radius (integer, 10-50 meters): Distance within which stops are considered same location

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can access Stop Detection settings within 2 taps from the app home screen (Settings tab → Stop Detection card)
- **SC-002**: 100% of setting changes persist correctly across app restarts and device reboots
- **SC-003**: Speed threshold values display accurately in both Metric and Imperial units with correct conversion (±0.1 mph precision)
- **SC-004**: Default values (3 km/h, 15s, 20m) apply automatically on first app launch without requiring user action
- **SC-005**: All three settings can be independently configured and saved without requiring changes to other settings
- **SC-006**: Users can identify the purpose of each setting from its label and description without external documentation
- **SC-007**: Setting changes save within 500ms of user selection with no perceivable UI lag
- **SC-008**: Settings UI maintains visual consistency with existing settings screens (same component style, spacing, colors)

### Assumptions

- Users understand the concept of "stopped" versus "moving" in the context of cycling
- Default values (3 km/h, 15s, 20m) are appropriate for typical urban cycling scenarios based on roadmap specifications
- GPS accuracy of mobile devices is sufficient for clustering radius values down to 10 meters
- Future stop detection feature (Feature 009) will consume these settings via the Settings Repository interface
- Future clustering feature (Feature 010) will use the clustering radius setting
- The existing Settings infrastructure from Feature 002 (v0.2.0) provides reusable UI components and data persistence patterns
- Users do not need to configure these settings before using basic ride recording features
- Imperial unit conversion is only required for speed threshold (duration and distance are already in universal units)
