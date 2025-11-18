/**
 * Stop Detection Settings Screen Contract
 *
 * Feature: 008-stop-detection-settings
 * Phase: 1 - Design
 *
 * This contract defines the Composable interface for the Stop Detection
 * settings detail screen.
 *
 * IMPORTANT: This is a CONTRACT DEFINITION, not implementation code.
 * It documents the expected UI component behavior for implementation.
 */

package com.example.bikeredlights.ui.screens.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.bikeredlights.domain.model.settings.StopDetectionConfig
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.ui.viewmodel.SettingsViewModel

/**
 * Settings detail screen for configuring stop detection parameters.
 *
 * **Layout**:
 * - Scaffold with top app bar ("Stop Detection" title, back button)
 * - Three settings sections (Speed Threshold, Duration Threshold, Clustering Radius)
 * - Each section: Label, description, SegmentedButtonSetting control
 * - Material 3 design (consistent with RideTrackingSettingsScreen)
 *
 * **State Management**:
 * - Reads `stopDetectionConfig` from ViewModel (StateFlow)
 * - Reads `unitsSystem` from ViewModel for speed conversion
 * - Calls `updateStopDetectionConfig()` on value changes
 * - Stateless composable (state hoisted to ViewModel)
 *
 * **User Interactions**:
 * 1. User taps segmented button for a setting
 * 2. Composable creates new StopDetectionConfig with updated value
 * 3. Calls viewModel.updateStopDetectionConfig(newConfig)
 * 4. Repository persists change
 * 5. StateFlow emits new value
 * 6. UI recomposes with updated selection
 *
 * **Unit Conversion** (Speed Threshold only):
 * - When unitsSystem == METRIC: Display km/h values (1, 2, 3, 4, 5)
 * - When unitsSystem == IMPERIAL: Display mph values (0.6, 1.2, 1.9, 2.5, 3.1)
 * - Internal storage always km/h (conversion only for display)
 * - Precision: 1 decimal place for mph
 *
 * **Accessibility**:
 * - All buttons have 48dp minimum touch targets
 * - Labels use appropriate typography scale
 * - Descriptions use secondary text color
 * - Dark mode supported (Material 3 dynamic color)
 *
 * **Performance**:
 * - Recomposition scoped to changed values only
 * - No heavy computation in composition
 * - Segmented buttons remembered across recompositions
 *
 * @param viewModel SettingsViewModel injected via Hilt
 * @param onBackClick Callback when user taps back button (navigates to SettingsHomeScreen)
 * @param modifier Modifier for customizing layout and behavior
 */
@Composable
fun StopDetectionSettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    TODO("Implementation in StopDetectionSettingsScreen.kt")
}

/**
 * UI Structure (Pseudocode)
 *
 * ```kotlin
 * Scaffold(
 *     topBar = {
 *         TopAppBar(
 *             title = "Stop Detection",
 *             navigationIcon = BackButton(onClick = onBackClick)
 *         )
 *     }
 * ) {
 *     Column(padding = 16.dp, spacing = 24.dp) {
 *
 *         // Setting 1: Speed Threshold
 *         Column {
 *             Text("Speed Threshold", style = titleMedium)
 *             Text("Consider stopped when speed drops below:", style = bodyMedium, color = secondary)
 *             SegmentedButtonSetting(
 *                 options = if (metric) [1, 2, 3, 4, 5] else [0.6, 1.2, 1.9, 2.5, 3.1],
 *                 selectedOption = currentSpeedThreshold,
 *                 unit = if (metric) "km/h" else "mph",
 *                 onOptionSelected = { newSpeed ->
 *                     viewModel.updateStopDetectionConfig(
 *                         config.copy(speedThresholdKmh = if (metric) newSpeed else newSpeed / 0.621371)
 *                     )
 *                 }
 *             )
 *         }
 *
 *         // Setting 2: Duration Threshold
 *         Column {
 *             Text("Duration Threshold", style = titleMedium)
 *             Text("Minimum time stationary to count as stop:", style = bodyMedium, color = secondary)
 *             SegmentedButtonSetting(
 *                 options = [5, 10, 15, 20, 25, 30],
 *                 selectedOption = config.durationThresholdSeconds,
 *                 unit = "s",
 *                 rows = 2,  // Two rows of 3 buttons each
 *                 onOptionSelected = { newDuration ->
 *                     viewModel.updateStopDetectionConfig(
 *                         config.copy(durationThresholdSeconds = newDuration)
 *                     )
 *                 }
 *             )
 *         }
 *
 *         // Setting 3: Clustering Radius
 *         Column {
 *             Text("Clustering Radius", style = titleMedium)
 *             Text("Group stops within this distance:", style = bodyMedium, color = secondary)
 *             SegmentedButtonSetting(
 *                 options = [10, 15, 20, 25, 30, 40, 50],
 *                 selectedOption = config.clusteringRadiusMeters,
 *                 unit = "m",
 *                 rows = 2,  // Two rows (4 + 3 buttons)
 *                 onOptionSelected = { newRadius ->
 *                     viewModel.updateStopDetectionConfig(
 *                         config.copy(clusteringRadiusMeters = newRadius)
 *                     )
 *                 }
 *             )
 *         }
 *     }
 * }
 * ```
 */

/**
 * Contract Guarantees (Testing Checklist)
 *
 * ✅ Screen displays three settings sections
 * ✅ Default values (3 km/h, 15s, 20m) shown on first launch
 * ✅ Selecting new value calls viewModel.updateStopDetectionConfig()
 * ✅ UI updates immediately after selection (via StateFlow)
 * ✅ Speed threshold converts to mph when Imperial units selected
 * ✅ Back button navigates to SettingsHomeScreen
 * ✅ Dark mode rendering supported
 * ✅ Accessibility: 48dp touch targets, semantic content descriptions
 * ✅ Recomposition only on changed values (performance)
 */

/**
 * Reusable Components (Existing)
 *
 * - **SettingCard**: Used on SettingsHomeScreen for navigation card
 * - **SegmentedButtonSetting**: Used for all three settings (discrete value selection)
 * - **Scaffold**: Material 3 scaffold with top bar
 * - **TopAppBar**: Material 3 top app bar with back navigation
 *
 * No new UI components needed - all exist in Feature 002.
 */

/**
 * Unit Conversion Reference
 *
 * **km/h to mph**: mph = kmh * 0.621371
 *
 * **Display Values**:
 * - 1 km/h → 0.6 mph
 * - 2 km/h → 1.2 mph
 * - 3 km/h → 1.9 mph (default)
 * - 4 km/h → 2.5 mph
 * - 5 km/h → 3.1 mph
 *
 * **Reverse Conversion** (user selects mph, store as km/h):
 * - kmh = mph / 0.621371
 *
 * **Precision**: Display to 1 decimal place (Success Criterion SC-003)
 */
