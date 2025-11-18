# Feature Specification: Stop Detection & Recording

**Feature Branch**: `009-stop-detection`
**Created**: 2025-11-18
**Status**: Draft
**Release Type**: Minor (v0.8.0 → v0.9.0)
**Input**: User description: "feature 009. Now that we added the settings part from feature 8, read the roadmap and prepare the next feature."

## User Scenarios & Testing

### User Story 1 - Real-Time Stop Detection During Active Ride (Priority: P1)

As a daily bike commuter who frequently encounters traffic lights, I want the app to automatically detect when I've stopped during a ride so I can later analyze where I spend the most time waiting at intersections.

**Why this priority**: This is the core detection engine that makes all other stop-related features possible. Without accurate stop detection during rides, there is no raw data for clustering (Feature 010) or visualization (future features). This is the foundational capability that enables the red light inference mission.

**Independent Test**: Can be fully tested by starting a ride, riding at normal speed, then stopping for various durations (5s, 15s, 30s), observing the live UI feedback during stops, and verifying that stop records appear in the database after the ride. Delivers immediate value by showing riders when they're stopped, even before clustering is implemented.

**Acceptance Scenarios**:

1. **Given** I am recording a ride and moving at 10 km/h, **When** I come to a complete stop (speed < 3 km/h default threshold) for 15 seconds, **Then** a stop is detected and recorded with start timestamp, location, and sequential stop number (#1)
2. **Given** I am recording a ride and have been stopped for 12 seconds, **When** my stop duration reaches the configured threshold (15s default), **Then** the system assigns a stop ID, marks the stop as "confirmed", and begins tracking duration
3. **Given** I am recording a ride and moving at 8 km/h, **When** I slow down to 2 km/h for only 5 seconds then speed up again, **Then** no stop is recorded because the duration threshold (15s default) was not met
4. **Given** I have configured a custom speed threshold of 2 km/h in settings, **When** I ride at 2.5 km/h for 20 seconds, **Then** no stop is detected because my speed is above the configured threshold
5. **Given** I am recording a ride and have stopped for 45 seconds, **When** I start moving again (speed > threshold), **Then** the stop end timestamp is recorded, final duration is calculated (45s), and stop data is persisted to the database
6. **Given** I am recording a ride and have already recorded 2 stops, **When** I come to a third stop meeting detection criteria, **Then** the new stop is assigned sequential number #3 and recorded with all required data

---

### User Story 2 - Live Stop Status UI During Ride (Priority: P1)

As a cyclist focused on safety, I want to see visual feedback when the app detects I'm stopped so I know the detection system is working correctly and can trust the data being collected.

**Why this priority**: Live UI feedback is critical for user trust and validation. Without visual confirmation, users won't know if stops are being detected, leading to uncertainty about data quality. This is especially important for a safety-critical app where users need confidence in the system. Equal priority to detection logic because they must ship together for a complete experience.

**Independent Test**: Can be fully tested by starting a ride, stopping for various durations, and observing the Live tab UI for the stop indicator popup. Tests both the appearance (when stop confirmed) and disappearance (when moving again) of the UI element. Delivers standalone value by providing real-time rider feedback.

**Acceptance Scenarios**:

1. **Given** I am on the Live tab with an active ride recording, **When** a stop is detected and confirmed (threshold met), **Then** a semi-transparent popup appears showing "🛑 Stop #2" and a live duration counter starting at 00:00
2. **Given** the stop popup is visible and I've been stopped for 35 seconds, **When** the duration updates, **Then** the popup displays "🛑 Stop #2" with duration "00:35" and updates every second
3. **Given** the stop popup is visible showing Stop #3, **When** I start moving again (speed > threshold for 3 consecutive seconds), **Then** the popup auto-dismisses with a smooth fade-out animation (200ms)
4. **Given** I am on the Live tab with no active ride, **When** I come to a complete stop, **Then** no stop detection occurs and no popup appears (detection only active during recording)
5. **Given** the stop popup is visible, **When** I navigate to the Rides tab or Settings tab, **Then** the popup remains visible on the Live tab (does not follow navigation) but stops updating
6. **Given** the stop popup is visible and I've been stopped for 2 minutes, **When** I view the popup, **Then** it displays duration in MM:SS format (e.g., "02:15") for readability

---

### User Story 3 - Stop Count Display on Live Tab (Priority: P2)

As a delivery rider tracking my efficiency, I want to see how many stops I've made during the current ride so I can gauge how much time I'm losing to traffic lights and optimize my routes accordingly.

**Why this priority**: Provides at-a-glance awareness of stop frequency during the ride, which is valuable for route optimization and time management. Lower priority than core detection (P1) because the system works without this display, but higher value than post-ride statistics because it enables real-time decision-making during the ride.

**Independent Test**: Can be fully tested by starting a ride, making multiple stops (e.g., 5 stops), and verifying the "Stops: 5" counter appears on the Live tab statistics row. Tests increment behavior and persistence throughout the ride session. Delivers standalone value as a live metric.

**Acceptance Scenarios**:

1. **Given** I am recording a ride with no stops yet, **When** I view the Live tab, **Then** I see "Stops: 0" displayed in the statistics row below the map
2. **Given** I am recording a ride and have completed 3 confirmed stops, **When** I view the Live tab statistics, **Then** I see "Stops: 3" displayed alongside distance and duration
3. **Given** I am recording a ride with 5 stops, **When** I pause the ride manually, **Then** the stop count remains "Stops: 5" (does not reset) and no new stops are detected while paused
4. **Given** I am recording a ride with 8 stops, **When** I stop the ride and save it, **Then** the stop count resets to "Stops: 0" for the next ride
5. **Given** I have not started a ride yet, **When** I view the Live tab, **Then** "Stops: 0" is displayed in a frozen/inactive state (gray color)
6. **Given** I am recording a ride and complete my first stop, **When** the stop is confirmed (threshold met), **Then** the counter updates from "Stops: 0" to "Stops: 1" with a subtle animation or color change

---

### User Story 4 - Stop Data Persistence in Database (Priority: P1)

As a data-conscious cyclist, I want all my stop data to be saved permanently in the app's database so I can analyze my stop patterns over time and identify problematic intersections.

**Why this priority**: Database persistence is critical for the long-term mission of red light inference. Without persistent stop data, clustering (Feature 010) cannot happen, and the app's core value proposition (identifying red light locations from patterns) fails. This is foundational infrastructure that must be rock-solid.

**Independent Test**: Can be fully tested by recording a ride with multiple stops, saving the ride, then querying the database directly (via Room DAO or database inspector) to verify stop records exist with correct data (ride_id, timestamps, lat/long, duration, stop_number). Delivers value by ensuring data integrity for future features.

**Acceptance Scenarios**:

1. **Given** I complete a ride with 4 confirmed stops, **When** I save the ride, **Then** 4 stop records are inserted into the `stops` table with unique IDs and correct ride_id foreign key
2. **Given** a stop is detected at latitude 40.7580° N, longitude -73.9855° W, **When** the stop is persisted, **Then** the database record contains accurate lat/long coordinates (within GPS accuracy margin)
3. **Given** a stop starts at 14:23:10 and ends at 14:23:55, **When** the stop is saved, **Then** the database record stores start_timestamp (1699885390), end_timestamp (1699885435), and calculated duration (45 seconds)
4. **Given** I complete a ride with 3 stops, **When** I delete the ride from history, **Then** all associated stop records are automatically deleted via CASCADE foreign key constraint (no orphaned stops)
5. **Given** a stop record is created, **When** initially persisted, **Then** the cluster_id field is NULL (will be populated later by clustering algorithm in Feature 010)
6. **Given** I record a ride with 6 stops over 2 days (app backgrounded overnight), **When** all stops are saved, **Then** sequential stop_number values (1, 2, 3, 4, 5, 6) are correctly maintained in database order

---

### User Story 5 - Integration with Settings Thresholds (Priority: P1)

As a cyclist who rides in different environments (urban vs suburban), I want the stop detection to respect my configured speed and duration thresholds so detection sensitivity matches my riding conditions.

**Why this priority**: Without settings integration, the detection system is rigid and won't adapt to different use cases. Urban riders need different sensitivity than suburban riders. This must ship with detection logic (P1) because hardcoded thresholds would make the feature unusable for many riders. Settings were already implemented in Feature 008, so this is just consumption.

**Independent Test**: Can be fully tested by configuring different threshold values in Settings (e.g., speed=2 km/h, duration=10s), then recording a ride and verifying detection behavior matches the configured values. Tests both initial load from DataStore and dynamic updates if settings change between rides.

**Acceptance Scenarios**:

1. **Given** I have configured speed threshold to 2 km/h in settings, **When** I start a new ride and slow down to 2.5 km/h, **Then** no stop is detected because my speed is above the custom threshold
2. **Given** I have configured duration threshold to 10 seconds in settings, **When** I stop for exactly 10 seconds, **Then** a stop is confirmed and recorded (threshold met)
3. **Given** I have configured speed=4 km/h and duration=20s, **When** I ride at 3 km/h for 15 seconds, **Then** a stop is detected (speed below threshold) but NOT confirmed yet (duration not met), and if I speed up before 20s, no stop is recorded
4. **Given** I have not configured any stop detection settings (first app launch), **When** I start my first ride, **Then** default thresholds are used (3 km/h speed, 15s duration per Feature 008 spec)
5. **Given** I change speed threshold from 3 km/h to 5 km/h in settings, **When** I start a new ride, **Then** the new threshold (5 km/h) is active for that ride (reads latest settings from DataStore)
6. **Given** I am mid-ride with a stop in progress (duration=12s), **When** I navigate to Settings and change duration threshold, **Then** the current ride continues using the original threshold (changes apply to future rides only, not in-progress rides)

---

### Edge Cases

- **What happens when GPS signal is lost during a detected stop?** The stop remains active and duration continues counting. When GPS returns, if the rider is still stationary (speed < threshold), the stop continues. If GPS shows movement, the stop ends normally. No crash or data corruption occurs.

- **What happens when the app is backgrounded or killed mid-stop?** The stop data (start timestamp, location, stop number) is saved to the database immediately when the stop is confirmed (threshold met), not when it ends. If the app is killed, the stop end timestamp may be missing, but partial stop data is preserved. When the app restarts and the ride resumes, a new stop sequence begins.

- **What happens when a rider "yo-yos" (repeatedly crosses the speed threshold)?** If speed oscillates above/below threshold (e.g., 2.8 km/h → 3.2 km/h → 2.9 km/h), a consecutive seconds counter is used. Speed must remain below threshold for 3 consecutive seconds before stop detection starts. This filters out GPS noise and brief slowdowns.

- **What happens when a stop lasts an extremely long time (e.g., 30+ minutes)?** The stop duration continues counting indefinitely. The database duration field (INTEGER seconds) can handle values up to 2.1 billion seconds (~68 years). The UI displays durations > 60 minutes in HH:MM:SS format (e.g., "01:23:45"). No timeout or auto-end logic is applied.

- **What happens when clustering radius setting changes after stops are recorded?** Clustering radius is consumed by Feature 010 (clustering algorithm), not Feature 009 (detection). Stops recorded in Feature 009 store raw lat/long coordinates and have cluster_id=NULL. Changing clustering radius does not affect existing stop records, only future clustering runs.

- **What happens when a rider pauses the ride manually while stopped?** If a stop is in progress (confirmed but not ended), manual pause ends the stop immediately with current timestamp and duration. The stop is saved to the database. While paused, no new stops are detected (detection only active during "recording" state, not "paused" state).

- **What happens on first app launch with stop detection settings at defaults?** On first ride after Feature 009 deployment, default thresholds (3 km/h, 15s from Feature 008) are loaded from DataStore. Detection works immediately with no user configuration required.

- **What happens when location accuracy is very poor (e.g., 50+ meters)?** Stop detection continues using the speed value from the GPS location object, regardless of accuracy. Poor accuracy may cause false positives (stationary with jittery GPS appears as slow movement) or false negatives (movement appears as stationary). No accuracy-based filtering is applied in v0.9.0. Future enhancement: ignore locations with accuracy > 50m.

## Requirements

### Functional Requirements

- **FR-001**: System MUST continuously monitor rider speed during active ride recording (not during paused or stopped states)
- **FR-002**: System MUST detect when rider speed falls below configured speed threshold (default 3 km/h, range 1-5 km/h from settings)
- **FR-003**: System MUST use a consecutive seconds counter (3 seconds) to filter GPS noise before confirming speed is below threshold
- **FR-004**: System MUST start a stop timer when speed remains below threshold for 3 consecutive seconds
- **FR-005**: System MUST confirm a stop when duration timer reaches configured duration threshold (default 15s, range 5-30s from settings)
- **FR-006**: System MUST assign sequential stop numbers (1, 2, 3...) within each ride, resetting to 1 for each new ride
- **FR-007**: System MUST capture stop start timestamp (Unix epoch milliseconds) when stop is confirmed (not when speed first drops)
- **FR-008**: System MUST capture GPS coordinates (latitude, longitude) at the moment stop is confirmed
- **FR-009**: System MUST detect when rider starts moving again (speed > threshold for 3 consecutive seconds)
- **FR-010**: System MUST capture stop end timestamp when movement is detected and calculate total duration (end - start)
- **FR-011**: System MUST persist stop data to local database (`stops` table) immediately when stop is confirmed, not waiting for ride end
- **FR-012**: System MUST update stop record with end timestamp and final duration when stop ends
- **FR-013**: System MUST display live stop popup on Live tab showing stop number and live duration counter during confirmed stops
- **FR-014**: System MUST update stop duration counter in popup every second while stop is active
- **FR-015**: System MUST auto-dismiss stop popup with fade-out animation (200ms) when rider starts moving again
- **FR-016**: System MUST display cumulative stop count on Live tab statistics row (e.g., "Stops: 5")
- **FR-017**: System MUST increment stop count display immediately when a stop is confirmed
- **FR-018**: System MUST reset stop count to 0 when a new ride starts
- **FR-019**: System MUST read speed threshold and duration threshold from SettingsRepository (DataStore) when ride starts
- **FR-020**: System MUST use thresholds active at ride start for entire ride duration (no mid-ride threshold changes)
- **FR-021**: System MUST apply default thresholds (3 km/h, 15s) if settings are not configured (first launch)
- **FR-022**: System MUST create foreign key relationship between stops and rides tables (ride_id column with CASCADE delete)
- **FR-023**: System MUST initialize cluster_id field as NULL for all new stop records (populated later by Feature 010)
- **FR-024**: System MUST delete all associated stop records when parent ride is deleted (CASCADE constraint)
- **FR-025**: System MUST NOT detect stops when ride is paused manually (detection only active during "recording" state)
- **FR-026**: System MUST end any in-progress stop when ride is paused manually, saving current timestamp and duration
- **FR-027**: System MUST format stop duration as MM:SS for durations < 60 minutes, HH:MM:SS for durations ≥ 60 minutes
- **FR-028**: System MUST handle app backgrounding gracefully by persisting partial stop data (start time, location) immediately on confirmation

### Key Entities

- **Stop**: Individual stationary period detected during a ride
  - Unique identifier (auto-incremented integer)
  - Ride association (foreign key to rides table, CASCADE delete)
  - Sequential stop number within ride (1, 2, 3... per ride)
  - GPS coordinates at stop confirmation (latitude, longitude in decimal degrees)
  - Start timestamp (Unix epoch milliseconds when stop confirmed)
  - End timestamp (Unix epoch milliseconds when movement detected, nullable during active stop)
  - Duration (calculated in seconds: end - start, nullable during active stop)
  - Cluster association (cluster_id integer, NULL until Feature 010 clustering runs)

- **Stop Detection State**: Transient runtime state (not persisted)
  - Current speed (from GPS location updates)
  - Speed-below-threshold consecutive seconds counter (0-3, resets when speed goes above threshold)
  - Stop timer (tracks duration since stop confirmed)
  - Current stop number for this ride (increments with each new stop)
  - Active stop ID (database ID of current in-progress stop, null when not stopped)
  - Is stop confirmed (boolean: false during first 0-15s below threshold, true after duration threshold met)

## Success Criteria

### Measurable Outcomes

- **SC-001**: Riders receive visual feedback within 1 second of a stop being confirmed (popup appears immediately when duration threshold is met)
- **SC-002**: 100% of confirmed stops (meeting both speed and duration thresholds) are persisted to the database with complete data (ride_id, timestamps, lat/long, duration, stop_number)
- **SC-003**: Stop detection accuracy is ≥95% for stops lasting 20+ seconds in urban riding conditions (speed < 3 km/h for 20s = confirmed stop with correct data)
- **SC-004**: False positive rate is <5% (stops detected when rider is actually moving slowly or briefly pausing)
- **SC-005**: Stop duration counters update with <500ms lag (UI shows 00:15, 00:16, 00:17... with no perceivable delay)
- **SC-006**: Stop popup auto-dismisses within 2 seconds of rider starting to move again (3s consecutive movement detection + 200ms animation)
- **SC-007**: Riders can complete a 30-minute urban commute with 10 stops without app crashes, ANR events, or missing stop data
- **SC-008**: Stop count display on Live tab matches actual number of confirmed stops recorded in database (count = SELECT COUNT(*) FROM stops WHERE ride_id = current_ride)
- **SC-009**: Settings thresholds are respected with 100% accuracy (e.g., if speed threshold = 2 km/h, riding at 2.1 km/h does not trigger stop detection)
- **SC-010**: Database foreign key CASCADE delete works correctly (deleting a ride with 15 stops removes all 15 stop records with no orphans)
- **SC-011**: Stop detection has negligible battery impact (<2% additional battery drain per hour of recording compared to Feature 007 baseline)
- **SC-012**: Stop data is queryable for clustering (Feature 010) with no missing required fields (all stops have lat, long, duration, ride_id)

### Assumptions

- GPS location updates are received at least every 1-3 seconds during ride recording (existing LocationRepository behavior from Feature 002)
- GPS speed values are sufficiently accurate to distinguish between stopped (0-1 km/h) and slow movement (3-5 km/h) in typical urban conditions
- Riders understand that "stop" means stationary for a configured duration, not just slowing down briefly
- Default thresholds (3 km/h, 15s) are appropriate for typical urban cycling based on roadmap analysis and Feature 008 specification
- The `stops` table schema will include all fields needed for clustering (Feature 010) even though cluster_id will be NULL initially
- Room database supports foreign key CASCADE delete (standard SQLite feature)
- Foreground service from Feature 002 continues running reliably during stops (no OS-level termination during stationary periods)
- Stop detection runs on the same background thread/coroutine as ride recording (no separate service needed)
- Manual ride pause/resume from Feature 002 provides clear state transitions (recording → paused → recording)
- Users will not manually delete individual stop records (deletion only via parent ride deletion or clustering algorithm in Feature 010)
- Stop popup UI is semi-transparent and does not block critical map/speed information on Live tab
- Riders will tolerate 3-second delay before stop detection starts (consecutive seconds filter for GPS noise)
- Duration threshold (5-30s range) is long enough to filter out brief traffic light green waves or rolling stops
- Clustering radius setting from Feature 008 is NOT consumed by this feature (only used by Feature 010 clustering algorithm)
