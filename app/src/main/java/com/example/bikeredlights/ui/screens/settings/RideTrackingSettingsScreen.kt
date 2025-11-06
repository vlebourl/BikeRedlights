package com.example.bikeredlights.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.settings.GpsAccuracy
import com.example.bikeredlights.domain.model.settings.AutoPauseConfig
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.ui.components.settings.SegmentedButtonSetting
import com.example.bikeredlights.ui.components.settings.ToggleWithPickerSetting
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Ride & Tracking settings detail screen.
 *
 * Currently displays (User Story 1 - Phase 3):
 * - Units setting (Metric/Imperial)
 *
 * Future settings (User Story 2 & 3):
 * - GPS Accuracy (High Accuracy/Battery Saver)
 * - Auto-Pause Rides (toggle + duration picker)
 *
 * @param unitsSystem Current units system selection
 * @param gpsAccuracy Current GPS accuracy selection (future - not displayed yet)
 * @param autoPauseConfig Current auto-pause configuration (future - not displayed yet)
 * @param onUnitsChange Callback when user changes units setting
 * @param onGpsAccuracyChange Callback when user changes GPS accuracy (future)
 * @param onAutoPauseChange Callback when user changes auto-pause (future)
 * @param onNavigateBack Callback when user taps back button
 * @param modifier Modifier for customizing layout and behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RideTrackingSettingsScreen(
    unitsSystem: UnitsSystem,
    gpsAccuracy: GpsAccuracy,
    autoPauseConfig: AutoPauseConfig,
    onUnitsChange: (UnitsSystem) -> Unit,
    onGpsAccuracyChange: (GpsAccuracy) -> Unit,
    onAutoPauseChange: (AutoPauseConfig) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Ride & Tracking") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        Surface(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // User Story 1: Units setting (Metric/Imperial)
                SegmentedButtonSetting(
                    label = "Units",
                    option1Label = "Metric",
                    option2Label = "Imperial",
                    selectedOption = if (unitsSystem == UnitsSystem.METRIC) 0 else 1,
                    onOptionSelected = { selectedIndex ->
                        onUnitsChange(
                            if (selectedIndex == 0) UnitsSystem.METRIC else UnitsSystem.IMPERIAL
                        )
                    }
                )

                // User Story 2: GPS Accuracy (High Accuracy/Battery Saver)
                SegmentedButtonSetting(
                    label = "GPS Accuracy",
                    option1Label = "High Accuracy",
                    option2Label = "Battery Saver",
                    selectedOption = if (gpsAccuracy == GpsAccuracy.HIGH_ACCURACY) 0 else 1,
                    onOptionSelected = { selectedIndex ->
                        onGpsAccuracyChange(
                            if (selectedIndex == 0) GpsAccuracy.HIGH_ACCURACY else GpsAccuracy.BATTERY_SAVER
                        )
                    }
                )

                // User Story 3: Auto-Pause Rides (toggle + duration picker)
                ToggleWithPickerSetting(
                    label = "Auto-Pause Rides",
                    enabled = autoPauseConfig.enabled,
                    selectedValue = autoPauseConfig.thresholdSeconds,
                    options = AutoPauseConfig.VALID_THRESHOLDS,
                    valueFormatter = { seconds ->
                        if (seconds == 1) "1 second" else "$seconds seconds"
                    },
                    onEnabledChange = { enabled ->
                        onAutoPauseChange(autoPauseConfig.copy(enabled = enabled))
                    },
                    onValueChange = { seconds ->
                        onAutoPauseChange(autoPauseConfig.copy(thresholdSeconds = seconds))
                    }
                )
            }
        }
    }
}

/**
 * Preview for RideTrackingSettingsScreen with Metric units selected (light theme).
 */
@Preview(showBackground = true)
@Composable
private fun RideTrackingSettingsScreenPreview() {
    BikeRedlightsTheme {
        RideTrackingSettingsScreen(
            unitsSystem = UnitsSystem.METRIC,
            gpsAccuracy = GpsAccuracy.HIGH_ACCURACY,
            autoPauseConfig = AutoPauseConfig.default(),
            onUnitsChange = {},
            onGpsAccuracyChange = {},
            onAutoPauseChange = {},
            onNavigateBack = {}
        )
    }
}

/**
 * Preview for RideTrackingSettingsScreen with Imperial units selected (dark theme).
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun RideTrackingSettingsScreenPreviewImperial() {
    BikeRedlightsTheme {
        RideTrackingSettingsScreen(
            unitsSystem = UnitsSystem.IMPERIAL,
            gpsAccuracy = GpsAccuracy.BATTERY_SAVER,
            autoPauseConfig = AutoPauseConfig(enabled = true, thresholdSeconds = 30),
            onUnitsChange = {},
            onGpsAccuracyChange = {},
            onAutoPauseChange = {},
            onNavigateBack = {}
        )
    }
}
