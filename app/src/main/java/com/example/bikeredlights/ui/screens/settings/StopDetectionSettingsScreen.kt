package com.example.bikeredlights.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
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
import com.example.bikeredlights.domain.model.settings.StopDetectionConfig
import com.example.bikeredlights.domain.model.settings.UnitsSystem
import com.example.bikeredlights.ui.components.settings.MultiOptionSegmentedButtonSetting
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme

/**
 * Stop Detection Settings screen (Feature 008 - v0.8.0).
 *
 * Allows users to configure stop detection parameters:
 * - Speed threshold (1-5 km/h): How slow is "stopped"
 * - Duration threshold (5-30s): How long to wait before recording a stop
 * - Clustering radius (10-50m): How close stops must be to group them
 *
 * These settings prepare the app for Feature 009 (Stop Detection) and
 * Feature 010 (Stop Clustering), which will use these thresholds during rides.
 *
 * @param config Current stop detection configuration
 * @param unitsSystem Current units system (for km/h ↔ mph conversion)
 * @param onConfigChange Callback when user changes any setting
 * @param onNavigateBack Callback when user presses back button
 * @param modifier Modifier for customizing layout and behavior
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StopDetectionSettingsScreen(
    config: StopDetectionConfig,
    unitsSystem: UnitsSystem,
    onConfigChange: (StopDetectionConfig) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stop Detection") },
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
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                windowInsets = WindowInsets(0, 0, 0, 0)
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
                // User Story 1 - Speed Threshold Section
                val kmhToMph = 0.621371f
                val speedOptions = StopDetectionConfig.VALID_SPEED_THRESHOLDS
                val displayedSpeed = if (unitsSystem == UnitsSystem.IMPERIAL) {
                    config.speedThresholdKmh * kmhToMph
                } else {
                    config.speedThresholdKmh
                }

                MultiOptionSegmentedButtonSetting(
                    label = "Speed Threshold",
                    options = speedOptions,
                    selectedValue = config.speedThresholdKmh,
                    onValueChange = { newSpeedKmh ->
                        onConfigChange(config.copy(speedThresholdKmh = newSpeedKmh))
                    },
                    valueFormatter = { speedKmh ->
                        val displayValue = if (unitsSystem == UnitsSystem.IMPERIAL) {
                            speedKmh * kmhToMph
                        } else {
                            speedKmh
                        }
                        val unit = if (unitsSystem == UnitsSystem.IMPERIAL) "mph" else "km/h"
                        String.format("%.1f %s", displayValue, unit)
                    }
                )

                // User Story 2 - Duration Threshold Section
                val durationOptions = StopDetectionConfig.VALID_DURATION_THRESHOLDS
                MultiOptionSegmentedButtonSetting(
                    label = "Duration Threshold",
                    options = durationOptions,
                    selectedValue = config.durationThresholdSeconds,
                    onValueChange = { newDurationSeconds ->
                        onConfigChange(config.copy(durationThresholdSeconds = newDurationSeconds))
                    },
                    valueFormatter = { seconds ->
                        "${seconds}s"
                    }
                )

                // TODO: User Story 3 - Clustering Radius Section (T039-T045)
            }
        }
    }
}

/**
 * Preview for StopDetectionSettingsScreen in light theme (Metric units).
 */
@Preview(showBackground = true)
@Composable
private fun StopDetectionSettingsScreenPreview() {
    BikeRedlightsTheme {
        StopDetectionSettingsScreen(
            config = StopDetectionConfig(),
            unitsSystem = UnitsSystem.METRIC,
            onConfigChange = {},
            onNavigateBack = {}
        )
    }
}

/**
 * Preview for StopDetectionSettingsScreen in light theme (Imperial units).
 */
@Preview(showBackground = true)
@Composable
private fun StopDetectionSettingsScreenPreviewImperial() {
    BikeRedlightsTheme {
        StopDetectionSettingsScreen(
            config = StopDetectionConfig(),
            unitsSystem = UnitsSystem.IMPERIAL,
            onConfigChange = {},
            onNavigateBack = {}
        )
    }
}

/**
 * Preview for StopDetectionSettingsScreen in dark theme.
 */
@Preview(showBackground = true, uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StopDetectionSettingsScreenPreviewDark() {
    BikeRedlightsTheme {
        StopDetectionSettingsScreen(
            config = StopDetectionConfig(),
            unitsSystem = UnitsSystem.METRIC,
            onConfigChange = {},
            onNavigateBack = {}
        )
    }
}
