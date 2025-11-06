package com.example.bikeredlights.ui.components.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.bikeredlights.domain.model.history.DateRangeFilter
import com.example.bikeredlights.ui.theme.BikeRedlightsTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Dialog for selecting a date range filter.
 *
 * **Features**:
 * - Material 3 DatePicker for start and end dates
 * - "Show All" button to clear filter
 * - "Apply" button to apply custom date range
 * - Validates start <= end before allowing apply
 * - Displays selected dates in readable format
 *
 * **UX Flow**:
 * 1. User clicks "Select Start Date" → DatePicker opens
 * 2. User selects start date → returns to dialog
 * 3. User clicks "Select End Date" → DatePicker opens
 * 4. User selects end date → returns to dialog
 * 5. User clicks "Apply" → filter applied, dialog closes
 * 6. OR User clicks "Show All" → filter cleared, dialog closes
 *
 * @param currentFilter Current date range filter
 * @param onFilterSelected Callback when filter is applied (None or Custom)
 * @param onDismiss Callback when dialog is dismissed/cancelled
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateRangeFilterDialog(
    currentFilter: DateRangeFilter,
    onFilterSelected: (DateRangeFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var startDateMillis by remember {
        mutableStateOf(
            if (currentFilter is DateRangeFilter.Custom) {
                currentFilter.startMillis
            } else {
                null
            }
        )
    }

    var endDateMillis by remember {
        mutableStateOf(
            if (currentFilter is DateRangeFilter.Custom) {
                currentFilter.endMillis
            } else {
                null
            }
        )
    }

    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }

    val dateFormatter = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter by Date Range") },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Select a date range to filter your rides:",
                    style = MaterialTheme.typography.bodyMedium
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Start date selection
                OutlinedButton(
                    onClick = { showStartDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (startDateMillis != null) {
                            "Start: ${dateFormatter.format(Date(startDateMillis!!))}"
                        } else {
                            "Select Start Date"
                        }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // End date selection
                OutlinedButton(
                    onClick = { showEndDatePicker = true },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (endDateMillis != null) {
                            "End: ${dateFormatter.format(Date(endDateMillis!!))}"
                        } else {
                            "Select End Date"
                        }
                    )
                }
            }
        },
        confirmButton = {
            Row {
                // Show All button - clears filter
                TextButton(
                    onClick = {
                        onFilterSelected(DateRangeFilter.None)
                        onDismiss()
                    }
                ) {
                    Text("Show All")
                }

                // Apply button - applies custom filter
                Button(
                    onClick = {
                        val start = startDateMillis
                        val end = endDateMillis

                        if (start != null && end != null) {
                            try {
                                onFilterSelected(DateRangeFilter.Custom(start, end))
                                onDismiss()
                            } catch (e: IllegalArgumentException) {
                                // Validation failed (start > end), do nothing
                            }
                        }
                    },
                    enabled = startDateMillis != null && endDateMillis != null
                ) {
                    Text("Apply")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )

    // Start date picker dialog
    if (showStartDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = startDateMillis ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showStartDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        startDateMillis = datePickerState.selectedDateMillis
                        showStartDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showStartDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    // End date picker dialog
    if (showEndDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = endDateMillis ?: System.currentTimeMillis()
        )

        DatePickerDialog(
            onDismissRequest = { showEndDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        endDateMillis = datePickerState.selectedDateMillis
                        showEndDatePicker = false
                    }
                ) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEndDatePicker = false }) {
                    Text("Cancel")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }
}

// ===== Previews =====

@Preview(name = "Date Range Filter Dialog - No Filter", showBackground = true)
@Composable
private fun DateRangeFilterDialogNoFilterPreview() {
    BikeRedlightsTheme {
        DateRangeFilterDialog(
            currentFilter = DateRangeFilter.None,
            onFilterSelected = {},
            onDismiss = {}
        )
    }
}

@Preview(name = "Date Range Filter Dialog - Custom Filter", showBackground = true)
@Composable
private fun DateRangeFilterDialogWithFilterPreview() {
    BikeRedlightsTheme {
        DateRangeFilterDialog(
            currentFilter = DateRangeFilter.Custom(
                startMillis = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000, // 7 days ago
                endMillis = System.currentTimeMillis()
            ),
            onFilterSelected = {},
            onDismiss = {}
        )
    }
}
