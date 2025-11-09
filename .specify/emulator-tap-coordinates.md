# Pixel 9 Pro Emulator - Tap Coordinate Reference

**Device**: Pixel 9 Pro (AVD)
**Screen Resolution**: 1280 x 2856 pixels
**Density**: 480 dpi (xxxhdpi)
**Aspect Ratio**: ~9:20

---

## Screen Layout

```
┌──────────────────────────┐  ← 0px (top)
│    Status Bar            │
│    (time, battery, etc)  │  ~100px
├──────────────────────────┤
│                          │
│                          │
│    Main Content Area     │
│                          │
│                          │  0-2650px
│                          │
│                          │
├──────────────────────────┤
│  Bottom Navigation Bar   │  2650-2856px
│  [Live] [Rides] [Settings]
└──────────────────────────┘  ← 2856px (bottom)
```

---

## BikeRedlights App - Bottom Navigation

**Navigation Bar Y-coordinate**: ~2710px (center of nav bar)
**Total Width**: 1280px

### Tab Positions (3 equally-spaced tabs):

| Tab      | X-coordinate | Y-coordinate | Notes                    |
|----------|--------------|--------------|--------------------------|
| Live     | 113          | 2710         | Left tab (1280 × 1/6 ≈ 213, icon centered ~113) |
| Rides    | 640          | 2710         | Center tab (1280 ÷ 2)    |
| Settings | 1067         | 2710         | Right tab (1280 × 5/6)   |

**Command examples**:
```bash
# Tap Live tab
adb shell input tap 113 2710

# Tap Rides tab
adb shell input tap 640 2710

# Tap Settings tab
adb shell input tap 1067 2710
```

---

## Live Tab Screen Elements

**When NOT recording** (Ready to ride state):

| Element          | X-coordinate | Y-coordinate | Notes                        |
|------------------|--------------|--------------|------------------------------|
| Start Ride Button| 640          | 1268         | Center of screen, blue button|
| My Location FAB  | 630          | 158          | Top-right floating button    |
| Zoom In (+)      | 652          | 562          | Top-right map control        |
| Zoom Out (-)     | 652          | 674          | Below zoom in button         |

**When recording**:

| Element          | X-coordinate | Y-coordinate | Notes                        |
|------------------|--------------|--------------|------------------------------|
| Pause Button     | 350          | 2024         | Left button in controls      |
| Stop Button      | 930          | 2024         | Right button in controls     |
| Center FAB       | 640          | 2156         | Re-center map on location    |

---

## Rides Tab Screen Elements

### Ride List (when rides exist):

| Element              | X-coordinate | Y-coordinate | Notes                        |
|---------------------|--------------|--------------|------------------------------|
| First Ride Card     | 640          | 450          | Full-width card, tap center  |
| Second Ride Card    | 640          | 750          | Approximate, depends on card height |
| Sort/Filter Icon    | 1150         | 200          | Top-right toolbar icon       |

### Ride Detail Screen:

| Element              | X-coordinate | Y-coordinate | Notes                        |
|---------------------|--------------|--------------|------------------------------|
| Back Button         | 64           | 150          | Top-left, arrow icon         |
| Map Area            | 640          | 450          | 300dp height map at top      |
| Zoom In (Map)       | 652          | 562          | Top-right of map             |
| Zoom Out (Map)      | 652          | 674          | Below zoom in                |

---

## Settings Tab Screen Elements

| Element                  | X-coordinate | Y-coordinate | Notes                        |
|--------------------------|--------------|--------------|------------------------------|
| Ride & Tracking Setting  | 640          | 400          | First item in list           |
| Units Setting            | 640          | 550          | Second item (approximate)    |

---

## Map Interaction Coordinates

### Live Tab Map (when visible):

**Map occupies**: Full width, top ~750px

| Action          | X-coordinate | Y-coordinate | Notes                        |
|-----------------|--------------|--------------|------------------------------|
| Map Center Tap  | 640          | 375          | Center of map area           |
| Map Pan (start) | 400          | 375          | Start point for swipe        |
| Map Pan (end)   | 800          | 375          | End point for horizontal pan |

### Ride Detail Map (300dp ≈ 900px height):

**Map occupies**: Full width, y: 200-1100px

| Action          | X-coordinate | Y-coordinate | Notes                        |
|-----------------|--------------|--------------|------------------------------|
| Map Center Tap  | 640          | 650          | Center of 300dp map          |

---

## GPS Location Simulation

### Set GPS coordinates:
```bash
# Format: adb emu geo fix <longitude> <latitude>
# Example: Annemasse, France
adb emu geo fix 6.2347 46.1942

# Example: Google Campus
adb emu geo fix -122.084 37.422
```

### Simulate route (multiple points):
```bash
adb emu geo fix 6.2347 46.1942  # Start
sleep 2
adb emu geo fix 6.2357 46.1945  # Mid
sleep 2
adb emu geo fix 6.2367 46.1948  # End
```

---

## Screenshot Capture

```bash
# Capture screenshot to file
adb exec-out screencap -p > /tmp/screenshot.png

# Capture with timestamp
adb exec-out screencap -p > /tmp/screenshot_$(date +%Y%m%d_%H%M%S).png
```

---

## Common Testing Workflows

### Test Ride Recording with Map:
```bash
# 1. Grant location permission
adb shell pm grant com.example.bikeredlights android.permission.ACCESS_FINE_LOCATION

# 2. Set initial location
adb emu geo fix 6.2347 46.1942

# 3. Launch app
adb shell am start -n com.example.bikeredlights/.MainActivity

# 4. Wait for UI
sleep 3

# 5. Tap Start Ride
adb shell input tap 640 1268

# 6. Simulate movement
sleep 2
adb emu geo fix 6.2357 46.1945
sleep 2
adb emu geo fix 6.2367 46.1948

# 7. Screenshot progress
adb exec-out screencap -p > /tmp/ride_recording.png

# 8. Stop ride
adb shell input tap 930 2024
```

### View Saved Ride with Map:
```bash
# 1. Navigate to Rides tab
adb shell input tap 640 2710

# 2. Wait for list
sleep 2

# 3. Tap first ride
adb shell input tap 640 450

# 4. Wait for detail screen with map
sleep 2

# 5. Screenshot
adb exec-out screencap -p > /tmp/ride_detail_map.png
```

---

## Notes

- **Y-coordinates may vary** slightly based on content height (e.g., keyboard visibility, list scroll position)
- **Safe tap zones**: Use center coordinates with ±20px tolerance for most buttons
- **Bottom nav is fixed**: Always at y=2710 regardless of content
- **Map zoom controls**: Consistent position in top-right when map is visible
- **Status bar height**: ~100px (varies by Android version)
- **Bottom nav height**: ~206px (2650-2856)

---

**Last Updated**: 2025-11-09
**Tested On**: Pixel 9 Pro API 34/35 emulator
