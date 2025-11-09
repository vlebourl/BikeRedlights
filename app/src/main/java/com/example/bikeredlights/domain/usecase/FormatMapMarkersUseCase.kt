package com.example.bikeredlights.domain.usecase

import com.example.bikeredlights.domain.model.MarkerData
import com.example.bikeredlights.domain.model.MarkerType
import com.example.bikeredlights.domain.model.TrackPoint
import com.example.bikeredlights.domain.util.endLocation
import com.example.bikeredlights.domain.util.startLocation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Use case to generate start and end marker data from a list of TrackPoints.
 *
 * **Responsibilities**:
 * - Extract start location (first track point) and create green START marker
 * - Extract end location (last track point) and create red END marker
 * - Format timestamps for marker titles/snippets
 *
 * **Edge Cases**:
 * - Empty route: Returns empty list
 * - Single point: Returns only START marker (no END marker, same location)
 * - Two+ points: Returns START and END markers
 *
 * @property No dependencies required (pure function)
 */
class FormatMapMarkersUseCase @Inject constructor() {

    private val timeFormatter = SimpleDateFormat("h:mm a", Locale.getDefault())

    /**
     * Generates start and end marker data for a ride.
     *
     * @param trackPoints The list of GPS track points from the ride
     * @return List of MarkerData containing START and END markers (empty if no track points)
     *
     * Example usage in ViewModel:
     * ```
     * val trackPoints: List<TrackPoint> = trackPointRepository.getTrackPoints(rideId)
     * val markers: List<MarkerData> = formatMapMarkersUseCase(trackPoints)
     * // markers[0] = START marker (green)
     * // markers[1] = END marker (red, if more than 1 point)
     * ```
     */
    operator fun invoke(trackPoints: List<TrackPoint>): List<MarkerData> {
        if (trackPoints.isEmpty()) return emptyList()

        val markers = mutableListOf<MarkerData>()

        // Add START marker (first track point)
        val startLocation = trackPoints.startLocation()
        if (startLocation != null) {
            val startPoint = trackPoints.first()
            markers.add(
                MarkerData(
                    position = startLocation,
                    type = MarkerType.START,
                    title = "Ride Start",
                    snippet = "Started at ${formatTimestamp(startPoint.timestamp)}",
                    visible = true
                )
            )
        }

        // Add END marker (last track point) only if route has multiple points
        if (trackPoints.size > 1) {
            val endLocation = trackPoints.endLocation()
            if (endLocation != null) {
                val endPoint = trackPoints.last()
                markers.add(
                    MarkerData(
                        position = endLocation,
                        type = MarkerType.END,
                        title = "Ride End",
                        snippet = "Ended at ${formatTimestamp(endPoint.timestamp)}",
                        visible = true
                    )
                )
            }
        }

        return markers
    }

    /**
     * Formats a Unix epoch timestamp in milliseconds to a human-readable time string.
     *
     * @param timestamp Unix epoch time in milliseconds
     * @return Formatted time string (e.g., "10:30 AM")
     */
    private fun formatTimestamp(timestamp: Long): String {
        return timeFormatter.format(Date(timestamp))
    }
}
