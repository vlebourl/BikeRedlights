# Feature Specification: Map UX Improvements (v0.6.1 Patch)

**Feature Branch**: `007-map-ux-improvements`
**Created**: 2025-11-10
**Status**: Draft
**Release Type**: Patch (v0.6.0 → v0.6.1)
**Input**: User description: "there are some small adjustments to make on v0.6: I would like the map to follow the riders direction instead of pointing north all the time, the pin to be an arrow head (or a bike???) instead of just a map pin, the pause counter (not immobile, that's not implemented yet) to auto update while paused instead of updating when un-pausing, and the auto-pause timings in settings to be 1s 2s 5s 10s 15s 30s instead of what it is currently. This should be a patched release, not minor."

## User Scenarios & Testing

### User Story 1 - Directional Map Orientation (Priority: P1)

As a cyclist, when I'm navigating using the live ride map, I want the map to rotate to match my heading direction so that "forward" on the map matches my actual forward direction, making navigation more intuitive.

**Why this priority**: Core navigation improvement that directly affects usability during active rides. Users currently must mentally rotate the map when traveling in non-north directions, causing cognitive load and potential navigation errors.

**Independent Test**: Can be fully tested by starting a live ride, moving in various directions (N, S, E, W, NE, etc.), and verifying the map rotates to keep the user's direction pointing upward on screen. Delivers immediate value by making map orientation match user perspective.

**Acceptance Scenarios**:

1. **Given** I am on an active live ride, **When** I move north, **Then** the map displays with north at the top
2. **Given** I am on an active live ride, **When** I move east, **Then** the map rotates 90° clockwise so east points up
3. **Given** I am on an active live ride, **When** I move southeast, **Then** the map rotates 135° clockwise so southeast points up
4. **Given** I am on an active live ride, **When** I make a turn from north to west, **Then** the map smoothly rotates to follow my new heading
5. **Given** I am stationary, **When** my heading/bearing is unavailable or unreliable, **Then** the map maintains the last known bearing

---

### User Story 2 - Directional Location Marker (Priority: P1)

As a cyclist viewing the live ride map, I want my location marker to show which direction I'm heading (using an arrow or bike icon) instead of a generic pin, so I can instantly see my orientation on the map.

**Why this priority**: Critical visual feedback for navigation. A directional marker eliminates ambiguity about which way the rider is facing, especially useful when stationary at intersections or when the map bearing update hasn't stabilized yet.

**Independent Test**: Can be fully tested by observing the location marker during a live ride. When moving in different directions, the marker should point in the direction of travel. Delivers immediate value by providing clear orientation feedback.

**Acceptance Scenarios**:

1. **Given** I am on an active live ride moving north, **When** I view the map, **Then** the location marker displays as an arrow/bike icon pointing upward (north)
2. **Given** I am moving east, **When** I view the map, **Then** the arrow/bike icon points to the right (east)
3. **Given** I am stationary, **When** bearing data is unavailable, **Then** the marker displays without directional indication (fallback to pin or non-rotated icon)
4. **Given** I change direction from north to south, **When** the heading updates, **Then** the arrow/bike icon smoothly rotates to point in the new direction
5. **Given** I am viewing a completed ride on the ride detail/review screen, **When** I look at the map, **Then** start/end markers remain as distinct pins (not directional arrows)

---

### User Story 3 - Real-Time Pause Counter (Priority: P2)

As a cyclist who has auto-paused during a ride, I want to see the paused time counter update in real-time (every second) while paused, so I can monitor how long I've been stopped without needing to resume riding.

**Why this priority**: Quality-of-life improvement for ride tracking accuracy. Currently, users don't see pause duration until they resume, making it unclear how long they've been paused. Real-time updates provide transparency and help users decide when to manually end a ride vs. wait for auto-resume.

**Independent Test**: Can be fully tested by triggering auto-pause (by remaining stationary for the configured threshold), then watching the pause counter increment every second without resuming movement. Delivers value by improving awareness of pause state and duration.

**Acceptance Scenarios**:

1. **Given** I am on an active ride that has auto-paused, **When** I remain paused, **Then** the paused time counter updates every second
2. **Given** I have been paused for 15 seconds, **When** I remain paused for 5 more seconds, **Then** the counter displays 20 seconds (or formatted as MM:SS)
3. **Given** I am paused and viewing the live ride screen, **When** the pause counter updates, **Then** no other UI elements flicker or re-render unnecessarily
4. **Given** the app is paused and I lock my phone screen, **When** I unlock and return to the app, **Then** the pause counter shows the accurate elapsed paused time
5. **Given** I am paused for 2 minutes, **When** I resume riding, **Then** the final paused time statistics match what was displayed during the pause

---

### User Story 4 - Granular Auto-Pause Timing Options (Priority: P3)

As a cyclist configuring auto-pause settings, I want more granular timing options (1s, 2s, 5s, 10s, 15s, 30s) instead of the current limited set, so I can fine-tune auto-pause sensitivity to match my riding style (e.g., quick stops at traffic lights vs. longer rest breaks).

**Why this priority**: Configuration enhancement that improves user control but doesn't affect core functionality. Users with different riding patterns (urban vs. rural, aggressive vs. casual) benefit from flexible timing, but the existing feature already works with the current options.

**Independent Test**: Can be fully tested by navigating to Settings > Auto-Pause, verifying all six timing options appear, selecting each option, and confirming the setting persists and correctly triggers auto-pause at the specified threshold. Delivers value by accommodating diverse user preferences.

**Acceptance Scenarios**:

1. **Given** I navigate to Settings > Auto-Pause Timing, **When** I view the options, **Then** I see six choices: 1s, 2s, 5s, 10s, 15s, 30s
2. **Given** I select "1 second", **When** I save and return to settings, **Then** "1 second" is displayed as the current selection
3. **Given** I have set auto-pause to 5 seconds, **When** I remain stationary for 5 seconds during a ride, **Then** the ride auto-pauses
4. **Given** I have set auto-pause to 30 seconds, **When** I remain stationary for only 20 seconds, **Then** the ride does NOT auto-pause
5. **Given** I change the setting from 10s to 2s, **When** I start a new ride, **Then** the new 2-second threshold is applied

---

### Edge Cases

- What happens when GPS bearing data is noisy or jumps erratically (e.g., in urban canyons)? The map should smooth/debounce bearing updates to avoid disorienting rotation jitter.
- How does the system handle bearing when the user is stationary? The map should maintain the last known bearing or revert to north-up if bearing becomes stale.
- What if the user's device doesn't provide accurate bearing/heading data? The system should fall back gracefully to a non-directional marker and north-up orientation.
- How does pause counter behavior interact with device sleep/background modes? The counter should continue tracking elapsed pause time even when the app is backgrounded or screen is locked.
- What happens if the user changes auto-pause timing during an active ride? The new threshold should apply immediately to future pause detection (existing pause state should not be affected).
- How should the directional marker icon appear on small vs. large screens? The marker should scale appropriately to remain visible but not obscure surrounding map details.

## Requirements

### Functional Requirements

- **FR-001**: Map view MUST rotate its bearing to align with the user's current heading/direction of travel during an active live ride
- **FR-002**: Map bearing rotation MUST animate smoothly when heading changes to avoid disorienting jumps
- **FR-003**: Map MUST maintain the last known bearing when stationary or when bearing data is unavailable, falling back to north-up if bearing becomes stale (staleness threshold: 60 seconds without GPS bearing updates)
- **FR-004**: Location marker on live ride map MUST display as a directional indicator (arrow or bike icon) that rotates to show the user's heading
- **FR-005**: Location marker MUST rotate smoothly when heading changes during active rides
- **FR-006**: Location marker MUST fall back to a non-directional indicator (pin or static icon) when bearing data is unavailable or unreliable
- **FR-007**: Start and end markers on ride review/detail maps MUST remain as distinct pin markers (not directional arrows)
- **FR-008**: Paused time counter MUST update in real-time (every 1000ms ±200ms) while a ride is in paused state
- **FR-009**: Paused time counter MUST continue updating accurately even when the app is backgrounded or device screen is locked
- **FR-010**: Paused time statistics MUST match the real-time counter value when the ride is resumed
- **FR-011**: Auto-pause timing settings MUST offer exactly six options: 1 second, 2 seconds, 5 seconds, 10 seconds, 15 seconds, 30 seconds
- **FR-012**: Selected auto-pause timing setting MUST persist across app restarts
- **FR-013**: Auto-pause timing setting MUST apply immediately to new rides started after the setting is changed
- **FR-014**: Changing auto-pause timing during an active ride MUST apply the new threshold to future pause detection without affecting current pause state

### Key Entities

This feature primarily enhances existing UI components (map, marker, counters, settings) and does not introduce new data entities. It modifies presentation and real-time update behavior of:

- **Map View State**: Bearing/rotation angle, camera orientation
- **Location Marker State**: Icon type (arrow/bike/pin), rotation angle
- **Pause Timing State**: Real-time counter value, last update timestamp
- **Settings**: Auto-pause timing threshold (integer value in seconds)

## Success Criteria

### Measurable Outcomes

- **SC-001**: Users can navigate during active rides with the map oriented to their heading direction within 2 seconds of heading change
- **SC-002**: Location marker orientation updates within 1 second of heading change, providing clear directional feedback
- **SC-003**: Pause counter displays accurate elapsed time (within ±1 second) and updates visibly every second without UI lag
- **SC-004**: 100% of auto-pause timing changes persist correctly and apply to subsequent rides
- **SC-005**: Map bearing rotation is smooth and non-disorienting, maintaining 60 fps during animation, using 300ms transition duration, and applying 5° debounce threshold (bearing changes < 5° are ignored to prevent jitter)
- **SC-006**: Users can distinguish ride direction at a glance without needing to observe movement on the map

### Assumptions

- GPS/location hardware provides bearing/heading data on most modern Android devices (API 34+)
- Bearing data may be unavailable or unreliable when stationary or in poor GPS conditions (urban canyons, indoors)
- Real-time pause counter updates are acceptable with ~1-second granularity (no need for sub-second precision)
- Map bearing updates can be smoothed/debounced without introducing unacceptable lag for user experience
- Existing ride recording service can provide bearing/heading data to the UI layer
- Device performance is sufficient to update UI counters every second without battery drain concerns
- The visual distinction between arrow/bike icon vs. pin marker is sufficient for users to understand context (active ride vs. completed ride review)
- Auto-pause timing setting is stored in DataStore Preferences (existing settings infrastructure)
