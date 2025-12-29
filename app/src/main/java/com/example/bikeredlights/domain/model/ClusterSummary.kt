package com.example.bikeredlights.domain.model

import androidx.compose.runtime.Immutable
import com.google.android.gms.maps.model.LatLng

/**
 * Domain model representing aggregate statistics for a stop cluster.
 *
 * Created by Feature 011: Stop Cluster Visualization.
 * Used to display cluster information on map markers and detail bottom sheet.
 *
 * Business Rules:
 * - Each cluster must have at least 2 stops (DBSCAN minimum)
 * - Center position calculated as arithmetic mean of GPS coordinates
 * - Average duration excludes active stops (endTime == null)
 * - Frequency text varies based on stop timestamps (week/month/total)
 *
 * @property clusterId Unique identifier from stops.cluster_id (NOT NULL)
 * @property centerPosition Geographic center for marker placement (arithmetic mean of lat/lng)
 * @property stopCount Total number of stops in this cluster (>= 2)
 * @property averageDuration Average stop duration in seconds (0 if no completed stops)
 * @property frequencyText Human-readable frequency ("You stopped here 15 times this month")
 * @property stops List of Stop entities belonging to this cluster (for detail popup)
 */
@Immutable
data class ClusterSummary(
    val clusterId: Long,
    val centerPosition: LatLng,
    val stopCount: Int,
    val averageDuration: Long,
    val frequencyText: String,
    val stops: List<Stop>
)
