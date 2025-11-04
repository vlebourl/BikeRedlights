package com.example.bikeredlights.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity representing a single cycling ride session.
 *
 * Stores aggregate statistics for completed rides including:
 * - Duration metrics (elapsed, moving, paused)
 * - Distance and speed statistics
 * - Temporal metadata (start/end timestamps)
 *
 * Relationships:
 * - One Ride has many TrackPoints (one-to-many with CASCADE delete)
 *
 * Data Storage:
 * - All durations stored in milliseconds
 * - All distances stored in meters
 * - All speeds stored in meters/second
 * - Timestamps stored as Unix epoch milliseconds
 *
 * State Management:
 * - endTime = null indicates incomplete/active ride
 * - endTime != null indicates completed ride
 */
@Entity(
    tableName = "rides",
    indices = [
        Index(value = ["start_time"], name = "idx_rides_start_time")
    ]
)
data class Ride(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "start_time")
    val startTime: Long,  // Unix timestamp in milliseconds

    @ColumnInfo(name = "end_time")
    val endTime: Long?,   // Nullable for incomplete rides

    @ColumnInfo(name = "elapsed_duration_millis")
    val elapsedDurationMillis: Long,

    @ColumnInfo(name = "moving_duration_millis")
    val movingDurationMillis: Long,

    @ColumnInfo(name = "manual_paused_duration_millis")
    val manualPausedDurationMillis: Long = 0,

    @ColumnInfo(name = "auto_paused_duration_millis")
    val autoPausedDurationMillis: Long = 0,

    @ColumnInfo(name = "distance_meters")
    val distanceMeters: Double,

    @ColumnInfo(name = "avg_speed_mps")
    val avgSpeedMetersPerSec: Double,

    @ColumnInfo(name = "max_speed_mps")
    val maxSpeedMetersPerSec: Double
)
