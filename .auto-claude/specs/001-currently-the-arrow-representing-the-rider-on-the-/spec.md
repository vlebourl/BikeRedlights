# Quick Spec: Rider Position Lower on Map

## Overview
Move the rider's arrow marker from center to lower third of map, matching Google Maps/Waze navigation mode. This standard navigation UX pattern shows more road ahead by keeping user position lower, providing better forward visibility for route planning.

## Workflow Type
Simple - Single file modification using existing GoogleMap composable parameters.

## Task Scope

### Files to Modify
- `app/src/main/java/com/example/bikeredlights/ui/components/map/BikeMap.kt` - Add contentPadding to GoogleMap

### Change Details
Add `contentPadding` parameter to the `GoogleMap` composable with bottom padding to offset the camera center point. This shifts the logical center of the map upward, causing the rider position to appear in the lower portion of the screen.

Add a new parameter `navigationMode: Boolean = true` to BikeMap to enable this behavior, then apply `PaddingValues(bottom = 200.dp)` to the GoogleMap when navigation mode is active.

### Implementation

```kotlin
// Add parameter to BikeMap
navigationMode: Boolean = true,

// In GoogleMap composable, add:
contentPadding = if (navigationMode) PaddingValues(bottom = 200.dp) else PaddingValues()
```

## Success Criteria
- [ ] Rider arrow appears in lower third of map (not center)
- [ ] More road visible ahead of the rider
- [ ] Map still tracks and rotates correctly
- [ ] Build succeeds without errors

## Notes
- The 200.dp value provides approximately 1/3 offset; can be adjusted based on testing
- contentPadding offsets the camera center without affecting map controls
