# BikeRedlights Feature Roadmap

> **Last Updated**: 2025-11-04 (FINAL ANALYSIS - Implementation order defined)
> **Status**: Ready for Implementation - 8 atomic features defined
> **Purpose**: Comprehensive roadmap with dependencies, atomicity review, and optimal implementation order

---

## 🎯 App Mission

**BikeRedlights** is a bike commuting/delivery tracking app designed for:
- **Primary Users**: Daily commuters + professional delivery riders
- **Core Value**: Ride logging, analytics, and route awareness
- **Future Vision**: Red light awareness features (TBD)

---

## 🎨 UI/UX Design Philosophy

**Core Vision:** Modern, easy looking, efficient - don't lose the user in useless details.

---

### **1. Glanceable First 👀**

**Problem:** Cyclists need information at a glance while riding - no time to read paragraphs.

**Principles:**
- **Large, bold numbers** for critical data (speed, duration, distance)
- **Minimal text** - use icons and visual indicators wherever possible
- **High contrast** - readable in direct sunlight
- **Single screen focus** - one primary purpose per screen
- **3-second rule** - user should understand screen purpose within 3 seconds
- **Map-centric** - visual context is faster to parse than text

**Examples:**

✅ **GOOD:**
```
25 km/h          (large, bold - displayLarge)
5.2 km • 12:34   (medium, single line - headlineMedium)
```

❌ **BAD:**
```
Current Speed: 25.4 km/h
Distance Traveled: 5.2 kilometers
Elapsed Time: 12 minutes, 34 seconds
```

---

### **2. Bike-Friendly Interactions 🚴**

**Problem:** Users are on bikes - bumpy roads, gloves, one-handed operation, safety-critical focus on the road.

**Principles:**
- **Extra-large touch targets** (minimum 56dp, prefer 64dp+ for primary actions)
- **Bottom-accessible controls** (thumb-friendly, one-handed use)
- **No precision gestures** (no small buttons, no required pinch-zoom, no tiny text)
- **Haptic feedback** for all interactions (users may not be looking at screen)
- **Auto-dismiss popups** (don't require manual closing during rides)
- **Map zoom locked** during rides (prevent accidental gestures)
- **Always-on screen** (no auto-lock while app is open)

**Why This Matters:**
- Safety first - minimize distraction from road
- Gloves reduce touch precision
- Bumps/vibration cause accidental taps
- One hand on handlebars, one hand free

---

### **3. Information Hierarchy 📊**

**Problem:** Too much detail overwhelms users and distracts from riding.

**Three-tier system:**

#### **Tier 1: Critical (Always Visible, Large)**
- **Current speed** (hero element, displayLarge 57sp)
- **Map with live location** (visual context, appropriately zoomed)
- **Recording status** (Start/Stop button, 64dp height)
- **GPS status indicator** (color-coded: red/yellow/green)

#### **Tier 2: Contextual (Visible When Relevant, Medium)**
- **Duration** (when recording)
- **Distance** (when recording)
- **Average speed** (when recording)
- **Stop count** (when recording)
- **Stop popup** (when stopped, auto-dismiss on move)
- **Battery level** (when low < 20%)

#### **Tier 3: Secondary (Buried in Settings/History, Small)**
- GPS update interval setting
- Auto-pause duration configuration
- Clustering radius setting
- Historical averages and trends
- Individual stop durations
- Detailed statistics breakdowns

**Rule:** If it's not Tier 1 or Tier 2, it doesn't belong on the Live tab. Settings and History tabs exist for details.

---

### **4. Modern Minimalism 🎨**

**Problem:** Cluttered UIs feel outdated and overwhelming.

**Principles:**
- **Generous white space** (or dark space in dark mode) - breathing room between elements
- **Single accent color** per screen (Material 3 dynamic color system)
- **Flat design** with subtle elevation (minimal shadows/gradients)
- **Sans-serif typography** (Material 3 Roboto/system fonts)
- **Iconography over text** where clear (🚴 not "Rides", 🚦 not "Stops")
- **Progressive disclosure** (show more on tap, not all at once)
- **No decorative elements** (every pixel serves a purpose)

**Screen Density Limits:**
- **Live tab:** 3-5 main elements maximum
- **Settings:** 3-4 cards maximum per screen
- **History:** List items with 2-3 data points each

**Visual Weight:**
- Primary action: Bold, full-width button
- Secondary actions: Icon buttons or text links
- Tertiary actions: Hidden in menus

---

### **5. Consistent Patterns 🔄**

**Problem:** Inconsistent UX creates confusion and increases cognitive load.

**Standardized Interactions:**
- **Navigation:** Bottom nav for top-level destinations, back button for sub-screens
- **Primary action:** Always bottom-center or full-width bottom button
- **Deletion:** Always long-press → confirmation dialog (prevent accidents)
- **Settings:** Always segmented buttons or toggles (no dropdowns, avoid scrolling pickers)
- **Stats display:** Always icon + number + unit (🚴 12 rides, not "Total rides: 12")
- **Time periods:** Always segmented buttons (All Time | This Week | This Month)
- **Lists:** Always grouped by meaningful categories (Today, Yesterday, etc.)

**Button Styles:**
- **Primary action:** Filled button (Start Ride, Save)
- **Secondary action:** Outlined button (Discard, Cancel)
- **Tertiary action:** Text button (Skip, Learn More)

---

### **6. Motion with Purpose 🎬**

**Problem:** Excessive animations distract, slow down the app, and drain battery.

**Principles:**
- **Fast transitions** (200-300ms maximum, prefer 250ms)
- **No decorative animations** (only functional feedback)
- **Haptic feedback preferred** over visual-only feedback when riding
- **Auto-dismiss popups** with smooth fade-out (no user action needed)
- **60fps minimum** for all animations (no jank)

**When to Animate:**
- ✅ Screen transitions (slide in/out, 250ms)
- ✅ Button presses (scale down 95% + haptic, 150ms)
- ✅ List item deletion (fade + slide, 300ms)
- ✅ Stop popup appearing/disappearing (fade, 200ms)
- ✅ Bottom sheet slide up/down (300ms)

**When NOT to Animate:**
- ❌ Real-time numbers updating (speed, duration) - just change value instantly
- ❌ Map polyline drawing - instant rendering
- ❌ Settings toggles - instant state change
- ❌ GPS indicator color changes - instant feedback

---

### **7. Accessibility ♿**

**Problem:** App must work for all users, including those with visual, motor, or cognitive disabilities.

**Requirements:**
- **TalkBack support** (all interactive elements have semantic content descriptions)
- **Minimum contrast ratios** (WCAG AA: 4.5:1 for text, 3:1 for large text)
- **No color-only indicators** (use icons + color, shapes + color)
- **Scalable text** (respect system font size settings, test up to 200%)
- **Touch target size** (minimum 48dp, 56dp for primary actions)
- **Focus indicators** (visible keyboard navigation for accessibility devices)
- **Meaningful labels** ("Start ride recording" not just "Start")

**Future Enhancements:**
- Voice commands ("OK Google, start ride", "Hey Siri, stop ride")
- Voice feedback for critical alerts (speed warnings)

---

## 🖼️ Visual Design System

### **Color Palette**

#### **Primary Colors (Material 3 Dynamic)**
- **System-adaptive:** App theme adapts to user's wallpaper automatically
- **Default theme:** Red accent (BikeRedlights branding)
- **Fallback:** Red-based palette when dynamic color unavailable

#### **Semantic Colors**
- 🔴 **Red:** Stop/Warning/Danger (red lights detected, errors, stop popup)
- 🟢 **Green:** Go/Active/Success (GPS active, recording, ride saved)
- 🟡 **Yellow:** Caution/Transitional (GPS acquiring, moderate speed warnings)
- ⚫ **Gray:** Inactive/Disabled (not recording, disabled buttons)

#### **Color Usage Guidelines**
- **Live tab recording state:** Green accent on Start/Stop button when recording
- **Stop popup:** Red border and icon when stopped
- **GPS indicator:** Red (unavailable) → Yellow (acquiring) → Green (active) progression
- **Map markers:** Red traffic lights (stop clusters), Blue current location

### **Typography**

#### **Material 3 Type Scale**
- **displayLarge (57sp):** Current speed on Live tab - THE hero element
- **headlineMedium (28sp):** Screen titles, primary statistics
- **titleLarge (22sp):** Card titles, section headers
- **bodyLarge (16sp):** List items, descriptions, settings labels
- **bodyMedium (14sp):** Secondary information
- **labelMedium (12sp):** Captions, timestamps, helper text

#### **Font Weights**
- **Bold (700):** Primary data (speed, distance, duration)
- **Medium (500):** Screen titles, card headers
- **Regular (400):** Labels, descriptions, secondary info

#### **Typography Rules**
- Never more than 3 font sizes per screen
- Keep line height generous (1.5x minimum)
- Use ALL CAPS sparingly (only for labels/buttons)

### **Spacing & Layout**

#### **8dp Grid System**
- **8dp:** Tight spacing (icon + label, inline elements)
- **16dp:** Standard padding (card content, list item padding)
- **24dp:** Section spacing (between cards, between groups)
- **32dp:** Screen margins (left/right edge padding)

#### **Component Sizing**
- **Touch targets:** Minimum 48dp, prefer 56dp for important actions, 64dp for primary
- **Bottom nav height:** 80dp (Material 3 standard)
- **Top app bar height:** 64dp
- **Card elevation:** 1dp (subtle shadow)
- **Button height:** 48dp (secondary), 56dp (primary)

---

## 📱 Screen-Specific Design Guidelines

### **Live Tab - Primary Screen (Glanceable Bike Computer)**

**Layout Priority: Map-First Design**

```
┌─────────────────────────────────────┐
│                                     │ ← Top bar: GPS status (compact)
│           [MAP AREA]                │
│      Live location marker           │ ← Map: 60% of screen
│      Route polyline (if recording)  │   Zoom: City block level
│      Zoomed appropriately for       │   (50-200m radius)
│      bike riding speed              │
│                                     │
├─────────────────────────────────────┤
│         25 km/h                     │ ← displayLarge (Tier 1)
│                                     │   Hero element, always visible
├─────────────────────────────────────┤
│    🚴 5.2 km  ⏱ 12:34  Stops: 2    │ ← headlineMedium (Tier 2)
│                                     │   Contextual stats (when recording)
├─────────────────────────────────────┤
│                                     │
│     [  START RIDE  ]                │ ← Large button (64dp height)
│                                     │   Bottom-accessible
└─────────────────────────────────────┘
```

#### **Map Requirements (Critical)**
- **Always visible:** Map occupies 50-60% of screen height
- **Live location:** Blue dot marker at rider's current position
- **Zoom level:** Appropriate for cycling speed (50-200 meter radius)
  - City block level: Enough context to see upcoming intersections
  - Not too zoomed in: Can see 2-3 blocks ahead
  - Not too zoomed out: Individual streets clearly visible
- **Map follows rider:** Center updates as rider moves (smooth panning, no jarring)
- **Route polyline:** Drawn in real-time when recording (contrasting color)
- **Locked zoom during ride:** Prevent accidental pinch-zoom gestures
- **Rotation:** Map rotates to match heading (north-up or heading-up toggle in settings)

#### **Principles:**
- Speed is hero element (largest text)
- Map provides spatial context (where am I? where have I been?)
- Secondary stats in single row below speed (scannable at a glance)
- Button at bottom (thumb-friendly, one-handed access)
- No clutter above the fold

#### **When Not Recording:**
- Duration: 00:00:00 (frozen)
- Distance: 0.0 km (frozen)
- Stops: 0 (hidden or frozen)
- Map: Shows current location only (no polyline)
- Button: "START RIDE"

#### **When Recording:**
- Duration: Live timer (updating every second)
- Distance: Real-time updates
- Stops: Count visible if > 0
- Map: Growing polyline showing path traveled
- Button: "STOP RIDE" (different color - red accent)

### **Rides Tab (History)**

```
┌─────────────────────────────────────┐
│ 📊 Rides                            │ ← Screen title
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ This Week                       │ │
│ │ 🚴 12 rides • 156 km • 8h 23m   │ │ ← Stats card (Tier 2)
│ │                                 │ │
│ │ [All Time] [Week] [Month]       │ │ ← Segmented buttons
│ └─────────────────────────────────┘ │
│                                     │
│ Today                               │ ← Group header
│ • Morning ride        8.2 km       │ ← Minimal list item
│   8:30 AM            25 min         │   (2 lines max)
│                                     │
│ Yesterday                           │
│ • Evening ride       12.1 km        │
│   6:15 PM            35 min         │
│                                     │
│ 2 days ago                          │
│ • Lunch delivery     4.5 km         │
│   12:45 PM           18 min         │
└─────────────────────────────────────┘
```

#### **Principles:**
- Stats card at top (quick overview, most important)
- Time period filters (easy comparison)
- Minimal list items (2 lines maximum per ride)
- Grouped by relative date (scannable, chronological)
- Tap to see full details (progressive disclosure)

### **Stops Tab (Red Light Map)**

```
┌─────────────────────────────────────┐
│ 🚦 Stops                 [Filter]   │ ← Top bar with filter icon
│                                     │
│           [MAP AREA]                │
│    Red light markers                │ ← Full screen map
│    Color-coded by duration          │   City-wide zoom
│    Sized by frequency               │   All clusters visible
│                                     │
│                                     │
│  [Re-center] [Search]               │ ← FAB controls (floating)
│                                     │
└─────────────────────────────────────┘

(Tap marker → Bottom sheet slides up)

┌─────────────────────────────────────┐
│ 🚦 Main St & 5th Ave               │ ← Bottom sheet
│ 38.8977° N, 77.0365° W              │
│                                     │
│ 47 stops • Median: 45s              │ ← Key stats
│ Range: 12s - 2m 18s                 │
│                                     │
│ [ Dismiss ]                         │
└─────────────────────────────────────┘
```

#### **Principles:**
- Map takes full screen (context is visual)
- Markers are primary interface (tap to learn more)
- Bottom sheet for details (progressive disclosure)
- Filters accessible but not intrusive (icon in top bar)

### **Settings Tab**

```
┌─────────────────────────────────────┐
│ ⚙️ Settings                         │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 🚴 Ride & Tracking              │ │ ← Card 1 (max 4 cards)
│ │ Units, GPS, Auto-pause          │ │   Subtitle shows what's inside
│ │                              >  │ │   Chevron indicates navigation
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ 🚦 Stop Detection               │ │ ← Card 2
│ │ Thresholds, Clustering          │ │
│ │                              >  │ │
│ └─────────────────────────────────┘ │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ ℹ️ About                        │ │ ← Card 3 (future)
│ │ Version, Privacy, Licenses      │ │
│ │                              >  │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

#### **Principles:**
- Card-based (easy to scan, clear separation)
- Maximum 4 cards per screen (avoid overwhelming)
- Descriptive subtitles (preview what's inside)
- Detail screens for complex settings (progressive disclosure)

---

## 🎯 Design System Impact on Features

All 8 features must follow these UI/UX principles:

### **Feature 1A (Core Recording) - Design Requirements:**
- ✅ Live tab follows map-first glanceable layout
- ✅ Large speed display (displayLarge 57sp)
- ✅ Map with live location, appropriate zoom (50-200m radius)
- ✅ Bottom button (64dp height, full-width or centered)
- ✅ No clutter, maximum 5 elements on screen

### **Feature 1B (Maps Integration) - Design Requirements:**
- ✅ Map zoom locked during recording (prevent accidental gestures)
- ✅ Route polyline in contrasting color (red or dynamic accent)
- ✅ Smooth center following (no jarring jumps)
- ✅ Heading-up rotation option (map rotates with direction)

### **Feature 2A (Basic Settings) - Design Requirements:**
- ✅ Card-based settings home (maximum 4 cards)
- ✅ Segmented buttons for binary choices (Metric/Imperial)
- ✅ Toggles for on/off settings (Auto-pause)
- ✅ Number pickers with clear labels and units

### **Feature 3 (Ride History) - Design Requirements:**
- ✅ Stats card at top (quick overview)
- ✅ Minimal list items (2 lines maximum)
- ✅ Grouped by relative date (Today, Yesterday, etc.)
- ✅ Long-press for deletion (consistent pattern)

### **Feature 4 (Stop Detection) - Design Requirements:**
- ✅ Auto-dismiss popup (fade out when moving)
- ✅ Red border/accent (semantic color)
- ✅ Live duration counter (updates every second)
- ✅ Semi-transparent (doesn't block map)

### **Feature 6 (Stops Map) - Design Requirements:**
- ✅ Full-screen map (city-wide zoom)
- ✅ Color-coded markers (green/yellow/red by duration)
- ✅ Sized by frequency (larger = more stops)
- ✅ Bottom sheet for details (progressive disclosure)

---

## 📐 Implementation Guidelines for Developers

### **Before Starting UI Work:**
1. Review Material 3 guidelines: https://m3.material.io/
2. Use Material 3 components from Jetpack Compose (don't reinvent)
3. Test on multiple screen sizes (phones, tablets)
4. Test in both light and dark modes
5. Test with system font scaling (up to 200%)

### **During Development:**
- Use `Preview` annotations extensively (test different states)
- Measure touch target sizes (use Layout Inspector)
- Test on physical device with gloves (if possible)
- Verify contrast ratios (use Accessibility Scanner)

### **Before Release:**
- Emulator testing with TalkBack enabled
- Test in bright sunlight (if possible, or simulate)
- Test one-handed use (thumb reach)
- Test with poor GPS signal (simulated)
- Record screen for motion review (check animation smoothness)

---

**This design system applies to ALL features across the roadmap. Consistency is key to a polished, professional app.**

---

## 🔍 Feature Atomicity Review & Proposed Splits

After comprehensive analysis, two features were split for more atomic development:

### **Split 1: Feature 2 (Settings) → Features 2A + 2B**

**Rationale:** Stop Detection settings not needed until Feature 4, can be deferred.

**Feature 2A: Basic Settings Infrastructure** (2-3 days)
- Settings home with card layout
- Ride & Tracking card: Units, GPS Accuracy, Auto-pause
- DataStore persistence, Bottom nav integration
- **Dependencies:** DataStore (available)
- **Enables:** F1A, F2B

**Feature 2B: Stop Detection Settings** (1-2 days)
- Add Stop Detection card: Speed threshold, Duration threshold, Clustering radius
- **Dependencies:** F2A (settings infrastructure)
- **Enables:** F4, F5

### **Split 2: Feature 1 (Ride Tracking) → Features 1A + 1B**

**Rationale:** Maps integration is complex (Google Cloud setup). Separate allows faster MVP without maps.

**Feature 1A: Core Ride Recording** (4-5 days)
- Start/Stop controls, Foreground service
- Room database (rides + track_points)
- Live stats on Live tab (NO MAPS)
- Basic Review Screen with stats (NO MAPS)
- Settings integration (units, GPS accuracy, auto-pause)
- **Dependencies:** F2A
- **Enables:** F1B, F3, F4

**Feature 1B: Maps Integration** (3-4 days)
- Google Maps SDK setup
- Add map to Live tab with route polyline
- Add map to Review Screen with complete track
- Start/end markers
- **Dependencies:** F1A, Google Maps SDK
- **Enables:** F6 (map infrastructure)

### **Final Feature Count: 8 Atomic Features**
- Original 6 features → 8 features after splits
- All features now 1-7 days (average 3-4 days)
- Better suited for iterative development

---

## ✅ Foundation (Released)

### v0.2.0 - Basic Settings Infrastructure
**Status**: ✅ Released (2025-11-04)
- Settings tab with card-based layout
- Ride & Tracking settings: Units (Metric/Imperial), GPS Accuracy (High/Battery Saver), Auto-Pause
- DataStore persistence across app restarts
- Bottom navigation bar with 3 tabs (Live, Rides, Settings)
- Reusable Material 3 UI components (SegmentedButtonSetting, ToggleWithPickerSetting)
- 57 unit tests + 12+ instrumented tests passing
- Full accessibility support (48dp touch targets, TalkBack)
- Bug fix: Auto-pause toggle race condition resolved

### v0.1.0 - Real-Time Speed Tracking
**Status**: ✅ Released (2025-11-03)
- Real-time speedometer (km/h)
- GPS position display (lat/long with accuracy)
- GPS status indicator (unavailable/acquiring/active)
- Material 3 UI with dark mode support
- 90%+ test coverage

---

## 📋 Features in Brainstorming Queue

### Feature 1: Ride Tracking & Review ⭐
**Status**: Fully scoped, ready for implementation (next in sequence)
**Complexity**: High (7-10 days)
**Target Users**: Commuters + delivery riders
**Core Value**: Personal/professional ride logging with detailed analytics

#### Capabilities

1. **Unified Home Screen (Live Tab - Default Landing Page)**

   **Key Design**: Single screen with two modes - fields always visible, updates controlled by recording state

   **Always Displayed (regardless of recording state):**
   - Current speed (large display, from v0.1.0)
   - GPS position (lat/long with accuracy)
   - GPS status indicator (unavailable/acquiring/active)
   - Google Maps view with current location marker
   - Duration display (00:00:00 when not recording)
   - Distance display (0.0 km/0.0 mi when not recording)
   - Average speed display (0 km/h/mph when not recording)
   - Route polyline on map (empty when not recording)
   - **[Start Ride]** button (when not recording) OR **[Stop Ride]** button (when recording)

   **When Recording Active:**
   - Duration: Live timer (00:23:45)
   - Distance: Real-time updates (5.2 km)
   - Average speed: Continuously calculated (18.5 km/h)
   - Route polyline: Growing trail showing path traveled
   - Map: Follows user location
   - Button changes to: **[Stop Ride]**

   **When NOT Recording:**
   - Duration: Frozen at 00:00:00
   - Distance: Frozen at 0.0 km
   - Average speed: Frozen at 0 km/h
   - Route polyline: Empty (no trail)
   - Map: Still shows current location (live)
   - Button shows: **[Start Ride]**

2. **Ride Control (Manual)**
   - Tap **[Start Ride]** → begins recording
   - Tap **[Stop Ride]** → dialog: "Save ride?" with [Discard] [Save] buttons
   - Discard → resets all fields to zero, clears route, deletes data
   - Save → stores to local database, resets UI for next ride

3. **Settings Integration (from Feature 2)**

   **Units Setting:**
   - Speed display respects km/h vs mph setting
   - Distance respects km vs miles setting
   - Conversion logic in domain layer

   **GPS Accuracy Setting:**
   - LocationRepository accepts configurable update interval
   - High Accuracy: 1 second updates (current behavior)
   - Battery Saver: 3-5 second updates
   - ViewModel reads setting from DataStore

   **Auto-Pause Setting:**
   - When enabled: Ride pauses if stationary (< 1 km/h) for X minutes
   - UI shows "⏸️ Paused" indicator
   - Duration and distance freeze during pause
   - Auto-resumes when speed > 1 km/h
   - TrackLocationUseCase handles detection logic

4. **Background Recording (Foreground Service)**
   - Persistent notification: "🚴 Ride in progress: 5.2 km, 23:45"
   - Continues recording when:
     - App is backgrounded
     - Screen is off
     - User switches to other apps (navigation, calls)
     - App is swiped from recents
   - Tap notification → returns to Live tab (Unified Home Screen)
   - Service keeps GPS tracking active

5. **Post-Ride Review Screen**
   - Accessed from Rides tab → tap a saved ride
   - **Statistics panel:**
     - Total duration (HH:MM:SS)
     - Total distance (km or miles based on setting)
     - Average speed
     - Max speed
     - Speed over time (line chart/graph)
     - Date/time of ride
   - **Map visualization:**
     - Complete GPS track (polyline)
     - Start/end markers (green pin → red flag)
     - Route color-coding by speed (optional enhancement)
   - **Actions:** [Delete Ride] button

6. **Ride History (Rides Tab)**
   - Bottom nav → **📊 Rides** tab
   - List of all saved rides (newest first)
   - Each list item shows:
     - Date/time (e.g., "Nov 4, 2025 8:30 AM")
     - Duration (e.g., "45:23")
     - Distance (e.g., "12.5 km")
   - Tap ride → opens Review Screen
   - Swipe-to-delete or delete button
   - Empty state: "No rides yet. Start your first ride!"

7. **Local Storage (Room Database)**
   - **Rides table (metadata):**
     - id, startTime, endTime, duration, distance, avgSpeed, maxSpeed
   - **Track points table (GPS coordinates):**
     - id, rideId (foreign key), latitude, longitude, timestamp, speed, accuracy
   - Efficient indexing for hundreds of rides
   - No automatic deletion (removed storage limits per user request)

8. **Google Maps SDK Setup**
   - Google Cloud Console project creation
   - Maps SDK for Android API enablement
   - API key configuration in local.properties
   - Billing setup (free tier sufficient: 28,000 map loads/month free)
   - *(Implementation guidance will be provided)*

9. **Screen Keep-Alive**
   - App prevents screen from dimming/locking when in foreground
   - Uses `WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON`
   - Applies to all screens (Live, Rides, Settings)
   - Simple implementation (always-on, no conditional logic)

10. **Bottom Navigation Structure**
    - 3 tabs: **[🚴 Live] [📊 Rides] [⚙️ Settings]**
    - Live tab = Unified Home Screen (default)
    - Rides tab = History list
    - Settings tab = App configuration
    - Material 3 NavigationBar component
    - Can migrate to drawer/hamburger if we exceed 5 top-level sections

#### Technical Requirements
- Google Maps SDK for Android
- Room database (rides + track_points tables)
- Foreground Service with notification channel
- DataStore for reading settings (units, GPS accuracy, auto-pause)
- Charts library for speed-over-time graph (e.g., MPAndroidChart or Vico)
- Bottom Navigation Compose
- Material 3 components

#### Dependencies
- **Critical:** Feature 2 (Settings) - must be implemented first or in parallel
- Google Maps API key setup
- Room database schema design
- v0.1.0 location tracking foundation
- DataStore for settings persistence

#### Open Questions
- Max speed display: yes (included)
- Route color-coding by speed: optional enhancement (defer to v2?)
- Auto-save interval during ride: every 30s for crash recovery?

#### Priority Ranking
- **TBD** (will rank against Feature 2 after brainstorming more features)

### Feature 2: Settings Screen ⚙️
**Status**: Fully scoped, ready for prioritization
**Complexity**: Medium (3-5 days)
**Target Users**: All users (commuters + delivery riders)
**Core Value**: User-configurable preferences for personalized ride tracking experience

#### Capabilities

1. **Settings Home Screen (Card-Based Layout)**

   Accessed via bottom nav → **⚙️ Settings** tab (3rd position)

   **Material 3 Card Layout:**
   ```
   ⚙️ Settings

   ┌────────────────────────────────┐
   │ 🚴 Ride & Tracking             │
   │ Units, GPS, Auto-pause         │
   │                             >  │
   └────────────────────────────────┘

   (Future category cards will appear here)
   ```

   - Card component with elevation
   - Tappable → navigates to detail screen
   - Scalable: Easy to add new category cards (App Behavior, Data Management, etc.)

2. **Ride & Tracking Detail Screen**

   Tap "🚴 Ride & Tracking" card → opens detail screen with 3 settings:

   **A. Units (Measurement System)**
   - **Control**: Segmented button (Material 3)
   - **Options**: `[Metric] [Imperial]`
   - **Metric**: km/h for speed, km for distance
   - **Imperial**: mph for speed, miles for distance
   - **Default**: Metric
   - **Impact**: Affects all speed/distance displays (Live tab, Review, History)

   **B. GPS Accuracy**
   - **Control**: Segmented button (Material 3)
   - **Options**: `[Battery Saver] [High Accuracy]`
   - **Battery Saver**: 3-5 second GPS update interval
   - **High Accuracy**: 1 second GPS update interval (current v0.1.0 behavior)
   - **Default**: High Accuracy
   - **Impact**: LocationRepository update frequency
   - **User benefit**: Delivery riders can extend battery life on long shifts

   **C. Auto-Pause Rides**
   - **Control**: Toggle switch + number picker
   - **UI**: `[🔘 OFF | ON] Pause after [5▼] minutes stationary`
   - **Number picker options**: 1, 2, 3, 5, 10, 15 minutes
   - **Behavior when ON**:
     - Ride pauses if speed < 1 km/h for X minutes
     - UI shows "⏸️ Paused" indicator on Live tab
     - Duration and distance freeze during pause
     - Auto-resumes when speed > 1 km/h
   - **Default**: OFF, 5 minutes
   - **Impact**: TrackLocationUseCase handles stationary detection
   - **User benefit**: Commuters don't accumulate stopped time at red lights or in traffic

3. **Data Persistence (DataStore Preferences)**
   - Settings stored locally using Jetpack DataStore
   - Persists across app restarts and updates
   - Key-value pairs:
     - `units_system`: "metric" | "imperial"
     - `gps_accuracy`: "battery_saver" | "high_accuracy"
     - `auto_pause_enabled`: true | false
     - `auto_pause_minutes`: 1-15 (integer)
   - No cloud sync (local only)
   - Defaults applied on first launch

4. **Material Design 3 Compliance**
   - Card components with appropriate elevation and padding
   - Segmented buttons (Material 3 SegmentedButton component)
   - Toggle switches with 48dp minimum touch targets
   - Number picker with dropdown or stepper controls
   - Dynamic color scheme support (adapts to wallpaper)
   - Dark mode fully supported
   - Proper semantic accessibility labels

5. **Navigation Integration**
   - **Bottom nav bar position**: 3rd tab (⚙️ Settings)
   - **Tab structure**: `[🚴 Live] [📊 Rides] [⚙️ Settings]`
   - **Navigation flow**:
     - Settings tab → Settings Home (card list)
     - Tap "Ride & Tracking" card → Detail screen
     - Back button → returns to Settings Home
     - Tapping Settings tab again → returns to Settings Home

#### Technical Requirements
- Jetpack DataStore Preferences (already configured in v0.1.0)
- Material 3 components:
  - Card
  - SegmentedButton
  - Switch
  - Dropdown menu or NumberPicker
- Bottom Navigation Compose (shared with Feature 1)
- ViewModel with StateFlow for reactive UI
- Repository pattern for DataStore access

#### Dependencies
- DataStore (already in v0.1.0) ✅
- Bottom Navigation structure (shared with Feature 1)
- None - can be implemented standalone

#### Impact on Other Features

**Feature 1 (Ride Tracking) - CRITICAL INTEGRATION:**

1. **Units conversion required**:
   - Domain layer: Create unit conversion utilities
   - Speed: km/h ↔ mph conversion (factor: 0.621371)
   - Distance: km ↔ miles conversion (factor: 0.621371)
   - ViewModel reads units setting and applies conversion before display

2. **GPS accuracy integration**:
   - LocationRepository must accept dynamic update interval parameter
   - Read GPS accuracy setting from DataStore on app start
   - Update interval when setting changes (restart location tracking)

3. **Auto-pause integration**:
   - TrackLocationUseCase: Add stationary detection logic
   - Timer: Track time spent at < 1 km/h
   - When threshold reached: Emit "paused" state to ViewModel
   - ViewModel: Freeze duration/distance updates, show "⏸️ Paused" indicator
   - Resume logic: When speed > 1 km/h, emit "active" state

**v0.1.0 (Current Speed Tracking) - IMMEDIATE IMPACT:**
- Current speed display must respect units setting
- LocationRepository may need to switch from fixed 1s interval to configurable

#### Open Questions
- None - fully scoped

#### Priority Ranking
- **CRITICAL**: Must be implemented BEFORE or IN PARALLEL with Feature 1
- Feature 1 depends on settings infrastructure (units, GPS accuracy, auto-pause)
- Should this be the FIRST feature after v0.1.0?

---

### Feature 3: Ride History Panel 📊
**Status**: Fully scoped, ready for prioritization
**Complexity**: Medium (3-5 days)
**Target Users**: Commuters + delivery riders
**Core Value**: Browse and manage saved rides with summary statistics

#### Capabilities

1. **Statistics Summary Card**

   Displayed at top of Rides tab, above ride list:

   **Metrics displayed:**
   - Total rides count (in selected time period)
   - Total distance (sum, respects units setting from Feature 2)
   - Total duration (sum, formatted as Xh Xm)
   - Average speed (calculated, respects units setting)

   **Time period filters (segmented buttons):**
   - All Time: All saved rides since app installation
   - This Week: Monday-Sunday of current week
   - This Month: 1st to last day of current month
   - Tapping filter → recalculates stats and filters list below

2. **Rides List (Grouped by Relative Date)**

   Below summary card, scrollable list:

   **Grouping logic:**
   - "Today" - rides from today
   - "Yesterday" - rides from yesterday
   - "X days ago" - rides 2-6 days old
   - "Nov 3, 2025" - rides 7+ days old (absolute date)

   **List item content:**
   - Ride title (auto-generated: "Morning ride", "Evening ride")
   - Duration (MM min or Hh MMm)
   - Distance (km or miles, respects units setting)
   - Time of day (8:30 AM)

   **Sorting:**
   - Default: Newest to oldest (most recent at top)
   - Optional: Sort toggle (newest/oldest) - icon button
   - Sorting applies within date groups

3. **User Actions**

   **Tap ride:**
   - Opens Review Screen (from Feature 1)
   - Shows full ride details (stats, map, speed chart)

   **Long-press ride:**
   - Context menu appears with "Delete ride" option
   - Confirmation dialog: "Delete this ride? This cannot be undone."
   - On delete → removes from database, updates list and summary stats

   **Bulk actions (Nice-to-have / Optional):**
   - "Select" button in top bar
   - Tap rides → checkboxes appear
   - Select multiple rides
   - Bottom action bar: [Delete Selected (X)]
   - *Can be deferred to v2 if time-constrained*

4. **Empty State**

   When no rides exist in selected time period:
   ```
   📊 Rides

   This Week
   No rides yet

   [All Time] [This Week] [This Month]

   ---

   No rides recorded yet.
   Start your first ride from the Live tab!
   ```

5. **Bottom Navigation Integration**
   - Tab position: 2nd tab (📊 Rides)
   - Tab structure: `[🚴 Live] [📊 Rides] [⚙️ Settings]`
   - Tapping Rides tab → shows History Panel

6. **Material Design 3 Compliance**
   - Statistics card: Material 3 Card with elevation
   - Time period filters: SegmentedButton component
   - Ride list: LazyColumn with Card items
   - Date group headers: Typography.titleMedium
   - Long-press: DropdownMenu or ModalBottomSheet
   - Dynamic color scheme + dark mode support

#### Technical Requirements
- Room database queries (read-only)
- SQL aggregation: SUM, COUNT, AVG for statistics
- Date/time calculations for relative grouping
- Kotlin Flows for reactive list updates
- LazyColumn for performant scrolling
- Material 3 components: Card, SegmentedButton, DropdownMenu
- ViewModel with StateFlow
- Repository pattern for database access

#### Dependencies
**Critical:**
- **Feature 1 (Ride Tracking)**: Must be implemented first
  - Requires rides table in Room database
  - Requires Review Screen to navigate to
  - Feature 3 reads data written by Feature 1

**Optional:**
- **Feature 2 (Settings)**: Units setting affects display
  - Can hardcode metric if Feature 2 not ready
  - Add units integration later

**No external dependencies:**
- Uses Room database (already configured in v0.1.0)
- No Google Maps needed (list view only)
- No external APIs

#### Impact on Other Features

**Feature 1 (Ride Tracking):**
- Feature 1 must populate rides database with proper schema
- Review Screen must be accessible from History Panel
- Delete action removes data written by Feature 1
- Shared bottom nav structure

**Feature 2 (Settings):**
- Units setting affects distance/speed display
- If implemented: read from DataStore
- If not: default to metric

**Future Enhancements (Deferred):**
- Ride sharing/export
- Ride renaming (custom titles)
- Notes/tags on rides
- Favorite/starred rides
- Advanced filtering (distance/duration ranges, custom dates)

#### Open Questions
- Bulk actions: Include in v1 or defer to v2?
- Sort toggle: Essential or default newest-first only?

#### Priority Ranking
- **TBD** - Will rank against Features 1 & 2
- **Hard dependency on Feature 1** - Cannot implement standalone
- Likely sequence: Feature 2 → Feature 1 → Feature 3

---

### Feature 4: Stop Detection & Recording 🛑
**Status**: Fully scoped, ready for prioritization
**Complexity**: Medium (4-6 days)
**Target Users**: Commuters + delivery riders
**Core Value**: Detect and record stops during rides for later analysis

#### Capabilities

1. **Settings Integration (Feature 2 - New Card)**

   Add "🚦 Stop Detection" card with 2 settings:

   **A. Speed Threshold**
   - Range: 1-5 km/h (or mph if imperial)
   - Default: 3 km/h
   - Description: "Consider stopped when speed drops below this value"

   **B. Duration Threshold**
   - Options: 5s, 10s, 15s, 20s, 25s, 30s
   - Default: 15s
   - Description: "Minimum time stationary to count as a stop"

2. **Real-Time Stop Detection During Rides**

   **Detection Logic:**
   - Monitor speed continuously during active ride
   - When speed < threshold for duration threshold → stop detected
   - Assign sequential stop number (#1, #2, #3...)
   - Record location and timestamps

   **Stop Data Captured:**
   - Stop ID, Ride ID, Lat/Long
   - Start/end timestamps, Duration
   - Stop number in ride

3. **Live UI During Stop**

   **Popup overlay on Live tab:**
   - Shows: "🛑 Stop #3" + live duration (00:15... 00:16...)
   - Auto-dismisses when rider starts moving
   - Semi-transparent, doesn't block map/stats

4. **Ride Statistics Integration**

   **Live tab during recording:**
   - "Stops: 5" (cumulative count)
   - "Stop time: 2m 15s" (total time stopped)

   **Post-Ride Review Screen:**
   - Total stops, total stop time
   - Average stop duration, longest stop

5. **Database Schema**

   **New table: `stops`**
   - id, ride_id, latitude, longitude
   - start_timestamp, end_timestamp, duration
   - stop_number, cluster_id (NULL initially, set by Feature 5)
   - Foreign key to rides, CASCADE delete

#### Technical Requirements
- Room database (new `stops` table)
- StopDetectionUseCase or TrackLocationUseCase enhancement
- Settings DataStore integration
- Compose overlay/dialog for popup
- Material 3 components

#### Dependencies
- **Feature 1 (Ride Tracking)**: Active ride recording
- **Feature 2 (Settings)**: Settings screen for thresholds
- Room database (already configured)

#### Impact on Other Features

**Feature 2 (Settings) - NEW CARD:**
- Add "🚦 Stop Detection" card to Settings home

**Feature 1 (Ride Tracking) - INTEGRATION:**
- Live tab displays stop count/time
- Review screen shows stop statistics
- Database CASCADE on ride delete

**Feature 5 (Stop Clustering):**
- Reads `stops` table for clustering
- Populates `cluster_id` field

#### Open Questions
- Should stop popup be manually dismissible or auto-dismiss only?
- Should stops be shown as markers on Live tab map during ride?

#### Priority Ranking
- **Depends on Features 1 & 2**
- **Enables Feature 5** (provides raw data)
- Sequence: F2 → F1 → F3 → **F4** → F5 → F6

---

### Feature 5: Stop Clustering & Statistics 📈
**Status**: Fully scoped, ready for prioritization
**Complexity**: Medium-High (5-7 days)
**Target Users**: Data analysis + red light inference
**Core Value**: Identify red light locations by clustering stop patterns

#### Capabilities

1. **Settings Integration (Add to Feature 2 Stop Detection Card)**

   **C. Clustering Radius**
   - Range: 10m, 15m, 20m, 25m, 30m, 40m, 50m
   - Default: 20m
   - Description: "Group stops within this distance as same location"

2. **Post-Ride Clustering Algorithm**

   **Trigger:** After user taps "Save" on completed ride

   **Algorithm (DBSCAN-like):**
   - For each new stop in ride:
     - Calculate distance to all existing cluster centers (Haversine)
     - Find nearest cluster within clustering radius
     - If found: Assign to cluster, update center/stats
     - If not found: Create new cluster
   - Processing time: < 1 second typically
   - Progress indicator: "Analyzing ride data..."

3. **Global Stop Clusters Database**

   **New table: `stop_clusters`**
   - id, center_latitude, center_longitude
   - stop_count (total stops in cluster)
   - first_seen, last_seen timestamps
   - median_duration, min_duration, max_duration, avg_duration
   - created_at, updated_at

4. **Statistics Calculation**

   For each cluster:
   - **Stop count**: Total stops at this location (all rides)
   - **Median duration**: Most representative value
   - **Min/Max/Avg duration**: Duration range and mean
   - **First/last seen**: Cluster age tracking
   - **Confidence score** (optional): Based on count and consistency

5. **Cluster Center Recalculation**

   Weighted average as new stops added:
   ```
   new_center = (old_center * old_count + new_stop) / (old_count + 1)
   ```

6. **No UI in This Feature**

   Pure backend logic - Feature 6 adds visualization

#### Technical Requirements
- Room database (new `stop_clusters` table)
- Haversine distance calculation
- Clustering algorithm (DBSCAN-like or custom)
- SQL aggregation (MIN, MAX, AVG, MEDIAN)
- Kotlin coroutines for async processing
- Transaction handling

#### Dependencies
- **Feature 4 (Stop Detection)**: Needs `stops` table with raw data
- **Feature 2 (Settings)**: Clustering radius setting
- Room database

#### Impact on Other Features

**Feature 4:**
- Updates `stops.cluster_id` after clustering

**Feature 6:**
- Provides `stop_clusters` data for map visualization

**Feature 2:**
- Add clustering radius to "Stop Detection" settings card

#### Open Questions
- Should clustering be re-run periodically to refine clusters?
- Should stale clusters (no stops in 6+ months) be auto-deleted?
- Should radius changes trigger re-clustering of all historical data?

#### Priority Ranking
- **Depends on Feature 4** (raw stop data)
- **Enables Feature 6** (clustered data for visualization)
- Sequence: F2 → F1 → F3 → F4 → **F5** → F6

---

### Feature 6: Stop Clusters Map Visualization 🗺️
**Status**: Fully scoped, ready for prioritization
**Complexity**: Medium (4-6 days)
**Target Users**: Commuters + delivery riders
**Core Value**: Visual map of identified red light locations with statistics

#### Capabilities

1. **New Bottom Navigation Tab**

   **4-tab structure:**
   ```
   [🚴 Live] [📊 Rides] [🚦 Stops] [⚙️ Settings]
   ```
   - Stops tab added as 3rd position
   - Settings moves to 4th position

2. **Stops Map Screen**

   **Full-screen Google Maps:**
   - Centered on user's current location
   - City-wide zoom level
   - All stop clusters displayed as markers

   **Marker Design:**
   - Red traffic light icon or custom stop sign
   - Size scales with stop count (more stops = larger)
   - Color gradient based on average duration:
     - Green: Quick stops (< 30s avg)
     - Yellow: Medium stops (30-60s avg)
     - Red: Long stops (> 60s avg)
   - Opacity: Higher for more stops (confidence)

3. **Cluster Details Bottom Sheet**

   **Tap marker → Bottom sheet:**
   - Location coordinates + reverse geocoded address
   - Statistics:
     - Total stops: 47
     - Median duration: 45s
     - Range: 12s - 2m 18s
     - Avg duration: 52s
     - First/last seen dates
   - Optional: "View All Stops" button

4. **Map Controls & Filters**

   **Top bar:**
   - Search box (address/location search)
   - Filter icon → options:
     - Minimum stop count (e.g., ≥ 10 stops)
     - Duration range (e.g., > 30s avg)
     - Date range (e.g., last 30 days)
   - Current location button (re-center)

   **Clustering on Map** (optional):
   - Zoomed out: Group nearby markers ("12")
   - Tap → zoom in to see individuals

5. **Empty State**

   No clusters yet:
   ```
   🚦 Stop Locations

   No stop data yet.

   Complete rides with stops to see
   identified red light locations here.

   [Go to Live Tab]
   ```

6. **Material Design 3 Compliance**
   - Google Maps with M3 theming
   - ModalBottomSheet component
   - SearchBar, FilterChip components
   - Dark mode: Dark map style
   - Dynamic color scheme

7. **Performance Optimization**
   - Load clusters within current viewport only
   - Lazy loading on pan/zoom
   - Cache geocoded addresses
   - Limit to 200 visible markers

#### Technical Requirements
- Google Maps SDK (already setup from Feature 1)
- Google Maps Geocoding API (optional, for addresses)
- Room queries (`stop_clusters` table)
- Kotlin Flows for reactive data
- Material 3: BottomSheet, SearchBar, FilterChip
- ViewModel with StateFlow
- Repository pattern
- Marker clustering library (optional)

#### Dependencies
- **Feature 5 (Stop Clustering)**: Needs `stop_clusters` table
- Google Maps SDK (from Feature 1)
- Room database

#### Impact on Other Features

**Bottom Navigation (All Features):**
- All features adapt to 4-tab layout
- Settings moves from 3rd to 4th position

**Google Maps (Feature 1):**
- Reuses existing Maps SDK configuration
- May need Geocoding API key

**Feature 5:**
- Reads `stop_clusters` data (read-only)

#### Open Questions
- Should users manually mark/unmark "confirmed red light"?
- Should confidence scores be displayed?
- Should there be "Report incorrect location" feature?

#### Priority Ranking
- **Depends on Feature 5** (needs clustered data)
- **Final piece** of stop detection system
- Sequence: F2 → F1 → F3 → F4 → F5 → **F6**

---

## 🎯 Recommended Implementation Order: MVP Fast Track

After atomicity review and dependency analysis, this is the optimal sequence:

---

### **📊 Implementation Summary Table**

| # | Feature | Release | Days | Cumulative | Dependencies | Deliverable |
|---|---------|---------|------|------------|--------------|-------------|
| 1 | F2A: Basic Settings | v0.2.0 ✅ | 2-3 | 2-3 | DataStore | Settings foundation |
| 2 | F1A: Core Recording | v0.3.0 | 4-5 | 6-8 | F2A | Ride tracking (no maps) |
| 3 | F3: Ride History | v0.4.0 | 3-5 | 9-13 | F1A | **MVP COMPLETE** ✅ |
| 4 | F1B: Maps Integration | v0.5.0 | 3-4 | 12-17 | F1A, Google Maps | Enhanced MVP with maps |
| 5 | F2B: Stop Settings | v0.6.0 | 1-2 | 13-19 | F2A | Stop detection ready |
| 6 | F4: Stop Detection | v0.7.0 | 4-6 | 17-25 | F1A, F2B | Stops recorded |
| 7 | F5: Stop Clustering | v0.8.0 | 5-7 | 22-32 | F4, F2B | Red lights inferred |
| 8 | F6: Stops Map | v0.9.0 | 4-6 | 26-38 | F5, F1B | **MISSION COMPLETE** 🎉 |

**Total Time: 26-38 days** (slightly faster than original 26-39 due to better atomicity)

---

### **Phase 1: MVP (Ride Management) - v0.2.0 to v0.4.0**

**Goal:** Deliver usable ride tracking product ASAP

**v0.2.0 - Basic Settings** (2-3 days) ✅ **COMPLETE - Released 2025-11-04**
- First feature after v0.1.0
- Settings tab with Ride & Tracking card
- Units, GPS accuracy, auto-pause settings
- Bottom navigation bar with 3 tabs
- Reusable Material 3 UI components
- 57 unit tests + 12+ instrumented tests
- ✅ **Milestone:** Settings infrastructure complete

**v0.3.0 - Core Ride Recording** (4-5 days, 6-8 cumulative)
- Start/Stop rides with foreground service
- Database: rides + track_points tables
- Live tab: duration, distance, avg speed (NO MAPS)
- Review screen: stats only (NO MAPS)
- ✅ **Milestone:** Ride tracking functional

**v0.4.0 - Ride History Panel** (3-5 days, 9-13 cumulative)
- Browse all saved rides
- Statistics summary with time filters
- Tap to view, long-press delete
- ✅ **MAJOR MILESTONE: MVP COMPLETE!**
  - Users can track rides, view history, manage data
  - Fully functional bike computer (no maps yet)

**Phase 1 Duration: 9-13 days**

---

### **Phase 2: Maps Enhancement - v0.5.0**

**Goal:** Add visual richness to existing features

**v0.5.0 - Maps Integration** (3-4 days, 12-17 cumulative)
- Google Maps SDK setup
- Add map to Live tab with route polyline
- Add map to Review screen with complete track
- ✅ **Milestone:** Enhanced MVP with beautiful route visualization

**Phase 2 Duration: 3-4 days**

---

### **Phase 3: Red Light Detection System - v0.6.0 to v0.9.0**

**Goal:** Complete BikeRedlights core mission

**v0.6.0 - Stop Detection Settings** (1-2 days, 13-19 cumulative)
- Add Stop Detection card to Settings
- Speed/duration thresholds, clustering radius
- ✅ **Milestone:** Ready for stop detection implementation

**v0.7.0 - Stop Detection & Recording** (4-6 days, 17-25 cumulative)
- Real-time stop detection during rides
- Popup UI with live duration
- Stops database with location data
- ✅ **Milestone:** All stops recorded

**v0.8.0 - Stop Clustering & Statistics** (5-7 days, 22-32 cumulative)
- Post-ride clustering algorithm (DBSCAN-like)
- Global stop clusters database
- Calculate median/min/max/avg duration per cluster
- ✅ **Milestone:** Red light locations identified!

**v0.9.0 - Stop Clusters Map Visualization** (4-6 days, 26-38 cumulative)
- New Stops tab (4th tab in bottom nav)
- Full-screen map with red light markers
- Color-coded by duration, sized by frequency
- Bottom sheet with detailed statistics
- ✅ **MAJOR MILESTONE: BIKEREDLIGHTS MISSION COMPLETE!** 🎉
  - Red light locations inferred from stop patterns
  - Visual map of all identified red lights
  - Complete safety awareness system

**Phase 3 Duration: 14-21 days**

---

### **📈 Milestone Timeline**

```
Day 0:    v0.1.0 (Speed tracking) ✅
Day 2-3:  v0.2.0 (Settings) ✅ COMPLETE
Day 6-8:  v0.3.0 (Core recording) ← NEXT
Day 9-13: v0.4.0 (MVP COMPLETE) ✅✅✅
Day 12-17: v0.5.0 (Maps enhancement)
Day 13-19: v0.6.0 (Stop settings)
Day 17-25: v0.7.0 (Stop detection)
Day 22-32: v0.8.0 (Clustering)
Day 26-38: v0.9.0 (MISSION COMPLETE) 🎉🎉🎉
```

---

### **🔄 Alternative Orders (Not Recommended)**

**Alt 1: Red Light First (Mission-Focused)**
- F2A → F2B → F1A → F4 → F5 → F6 → F3 → F1B
- Prioritizes core mission but no usable product until day 17-25
- History comes very late (less useful)
- **NOT RECOMMENDED** - Too slow to value

**Alt 2: Sequential No Splits (Monolithic)**
- F2 → F1 → F3 → F4 → F5 → F6
- Waits longer for MVP (13-20 days vs 9-13 days)
- Bundles maps with F1 (more complex, longer to test)
- **NOT RECOMMENDED** - Less flexible

**Why MVP Fast Track is Best:**
- ✅ Fastest time to usable product (9-13 days)
- ✅ Incremental value delivery (8 releases)
- ✅ Defers Google Maps complexity until v0.5.0
- ✅ Allows early user testing and feedback
- ✅ Clear phase boundaries (MVP → Enhancement → Mission)

---

## 🔗 Complete Dependency Graph

```
v0.1.0 (Speed Tracking)
    ↓
F2A: Basic Settings (2-3d)
    ├─→ F1A: Core Recording (4-5d)
    │       ├─→ F1B: Maps Integration (3-4d)
    │       │       └─→ F6: Stops Map (4-6d) [also needs F5]
    │       │
    │       ├─→ F3: Ride History (3-5d) [MVP COMPLETE HERE]
    │       │
    │       └─→ F4: Stop Detection (4-6d) [also needs F2B]
    │               └─→ F5: Stop Clustering (5-7d)
    │                       └─→ F6: Stops Map [MISSION COMPLETE HERE]
    │
    └─→ F2B: Stop Settings (1-2d)
            ├─→ F4: Stop Detection [already shown above]
            └─→ F5: Stop Clustering [already shown above]
```

**Critical Path:** F2A → F1A → F3 (MVP) → F2B → F4 → F5 → F6 (Mission Complete)

---

## 📝 Brainstorming Notes

### Session 1 (2025-11-04)

**App Mission Clarification:**
- BikeRedlights = bike commuting/delivery tracking app (NOT pure safety app)
- Primary users: Daily commuters + professional delivery riders
- Red light features = future awareness features (not warnings)

**Feature 1 (Ride Tracking) - Key Decisions:**
- Manual start/stop (no auto-detection for v1)
- Google Maps SDK chosen (most "androidesque", over OpenStreetMap/Mapbox)
- Foreground service with persistent notification (required for delivery riders)
- Screen always-on when app is open (bike computer experience)
- **Critical design insight**: Unified home screen with two modes
  - All fields always visible (no appearing/disappearing elements)
  - Recording state controls which fields update vs. stay frozen at zero
  - Single screen = Live tab (replaces v0.1.0 speed screen)

**Feature 2 (Settings) - Key Decisions:**
- Bottom navigation bar chosen (3 tabs: Live | Rides | Settings)
  - Scales well for 3-5 top-level sections
  - Can migrate to hamburger menu if needed later
- Card-based settings layout (Material 3 compliant)
- 4 settings initially: Units, GPS accuracy, Auto-pause, ~~Storage limits~~
  - Storage limits REMOVED per user request (no automatic deletion)
- Settings controls finalized:
  - Units: Segmented button `[Metric] [Imperial]`
  - GPS accuracy: Segmented button `[Battery Saver] [High Accuracy]`
  - Auto-pause: Toggle switch + number picker
  - Storage limits: Removed

**Cross-Feature Impacts Identified:**
- Feature 2 must integrate with Feature 1 (units conversion, GPS frequency, auto-pause logic)
- Feature 1 depends on Feature 2 settings infrastructure
- Bottom nav structure shared between both features
- v0.1.0 speed display may need immediate units setting integration

**Feature 3 (Ride History Panel) - Key Decisions:**
- Split from Feature 1 to keep features atomic (easier sequential development)
- Statistics summary card with basic metrics: rides count, distance, duration, avg speed
- Time period filters: All Time, This Week, This Month (no custom date ranges)
- Rides list grouped by relative date (Today, Yesterday, X days ago)
- Actions: Tap to view (opens Review Screen), Long-press to delete
- Bulk actions: Nice-to-have, can be deferred to v2
- Sorting: By date only (newest/oldest toggle optional)
- Empty state: Simple text-based (Option A)
- Future enhancements deferred: Share, rename, notes, favorites, advanced filtering

**Cross-Feature Dependencies Clarified:**
- **Strict sequence**: Feature 2 → Feature 1 → Feature 3
- Feature 3 has **hard dependency** on Feature 1 (reads ride data from database)
- Feature 3 has **soft dependency** on Feature 2 (units setting for display)
- All three form cohesive "Ride Management" capability

**Features 4-6 (Stop Detection System) - Key Decisions:**
- **Atomic split**: Separated into 3 features for clean sequential development:
  - F4: Detection & recording (raw data)
  - F5: Clustering & statistics (analysis)
  - F6: Map visualization (UI)
- **Settings thresholds** (in Feature 2):
  - Speed threshold: 1-5 km/h, default 3 km/h
  - Duration threshold: 5-30s, default 15s
  - Clustering radius: 10-50m, default 20m
- **Live UI during stop**: Popup with stop number and live duration counter
- **Post-ride processing**: Clustering happens when ride is saved (not real-time)
- **Clustering algorithm**: DBSCAN-like distance-based clustering with Haversine
- **Statistics per cluster**: Median, min, max, avg duration + stop count
- **Visualization**: New "🚦 Stops" tab (4th tab in bottom nav)
- **Map markers**: Color-coded by avg duration (green/yellow/red), sized by stop count
- **Purpose**: Infer red light locations from stop patterns (core BikeRedlights mission!)

**Cross-Feature Dependencies Updated:**
- **Two dependency chains** identified:
  - Chain 1: F2 → F1 → F3 (Ride Management)
  - Chain 2: F2 → F1 → F4 → F5 → F6 (Stop Detection System)
- Feature 1 is critical bottleneck (enables both chains)
- Feature 2 (Settings) must come first (foundation for all)
- Bottom nav expands to 4 tabs in Feature 6 (impacts all features)

**FINAL ANALYSIS RESULTS:**
- **Atomicity Review Completed**: 2 features split (F1 → F1A+F1B, F2 → F2A+F2B)
- **Final feature count**: 8 atomic features (all 1-7 days, avg 3-4 days)
- **Dependencies verified**: Complete dependency graph created
- **Implementation order defined**: MVP Fast Track (Phase 1 → Phase 2 → Phase 3)
- **MVP timeline**: 9-13 days (v0.4.0)
- **Full system timeline**: 26-38 days (v0.9.0)
- **8 releases planned**: v0.2.0 through v0.9.0

**Architectural Decisions Finalized:**
- ✅ Feature 2A (Settings) FIRST - foundation approach
- ✅ Feature splits enable faster MVP without maps
- ✅ Google Maps deferred to v0.5.0 (after MVP complete)
- ✅ Stop Detection system built sequentially after MVP
- ✅ Incremental releases every 1-7 days (rapid iterations)

**Why This Order:**
- Fastest path to usable product (MVP in 9-13 days)
- Clear phase boundaries (Ride Management → Maps → Red Light System)
- Defers external complexity (Google Maps setup) until after MVP
- Allows early user testing and feedback
- Each release delivers tangible value

---

## 🚀 Next Steps

### **Brainstorming Complete! ✅**

All features have been:
- ✅ Scoped and defined
- ✅ Reviewed for atomicity (2 features split)
- ✅ Dependencies verified and documented
- ✅ Prioritized with implementation order

### **Ready to Begin Implementation:**

**Step 1: Start Feature 2A (Basic Settings) - v0.2.0**
- Run `/speckit.specify` for Feature 2A
- Create detailed specification document
- Generate implementation tasks
- Begin development following MVP Fast Track sequence

**Step 2: Follow Release Schedule**
- Complete each feature in order (F2A → F1A → F3 → F1B → F2B → F4 → F5 → F6)
- Test on emulator after each feature
- Tag releases: v0.2.0, v0.3.0, v0.4.0, etc.
- Update TODO.md and RELEASE.md per constitution

**Step 3: Key Milestones**
- Day 9-13: MVP Complete (v0.4.0) - Celebrate! 🎉
- Day 12-17: Maps Added (v0.5.0) - Enhanced MVP
- Day 26-38: Mission Complete (v0.9.0) - Red light system fully operational

### **Future Feature Ideas (Post-v0.9.0)**
Additional features can be brainstormed later:
- Data export/sharing
- Social features
- Bike maintenance tracking
- Weather integration
- Route planning
- Achievements/gamification

**For now: Focus on the 8 defined features to reach v0.9.0!** 🚴

---

**Maintained by**: Feature brainstorming session
**Review Frequency**: After each new feature discussion
