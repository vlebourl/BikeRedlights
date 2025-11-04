package com.example.bikeredlights.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single GPS coordinate point captured during a ride.
 *
 * TrackPoints are children of Ride entities in a one-to-many relationship.
 * Deleting a parent Ride will CASCADE delete all its TrackPoints.
 *
 * Data Quality:
 * - Only points with accuracy <= 50 meters should be inserted (spec FR-022)
 * - Points are captured at intervals determined by GPS accuracy setting:
 *   * High Accuracy: Every 1 second
 *   * Battery Saver: Every 4 seconds
 *
 * Pause State:
 * - isManually Paused: User explicitly paused the ride
 * - isAutoPaused: System auto-paused due to low speed threshold
 * - Both flags cannot be true simultaneously
 *
 * Data Storage:
 * - Coordinates in decimal degrees (WGS 84)
 * - Speed in meters/second
 * - Accuracy in meters
 * - Timestamp as Unix epoch milliseconds
 */
@Entity(
    tableName = "track_points",
    foreignKeys = [
        ForeignKey(
            entity = Ride::class,
            parentColumns = ["id"],
            childColumns = ["ride_id"],
            onDelete = ForeignKey.CASCADE  // Auto-delete when parent Ride deleted
        )
    ],
    indices = [
        Index(value = ["ride_id"], name = "idx_track_points_ride_id"),
        Index(value = ["timestamp"], name = "idx_track_points_timestamp")
    ]
)
data class TrackPoint(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "ride_id")
    val rideId: Long,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long,  // Unix timestamp in milliseconds

    @ColumnInfo(name = "latitude")
    val latitude: Double,  // Decimal degrees, range: -90.0 to 90.0

    @ColumnInfo(name = "longitude")
    val longitude: Double,  // Decimal degrees, range: -180.0 to 180.0

    @ColumnInfo(name = "speed_mps")
    val speedMetersPerSec: Double,  // Meters per second, >= 0.0

    @ColumnInfo(name = "accuracy")
    val accuracy: Float,  // GPS accuracy radius in meters, must be <= 50.0

    @ColumnInfo(name = "is_manually_paused")
    val isManuallyPaused: Boolean = false,

    @ColumnInfo(name = "is_auto_paused")
    val isAutoPaused: Boolean = false
)
