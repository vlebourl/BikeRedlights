package com.example.bikeredlights.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for stop persistence (Feature 009: Stop Detection & Recording).
 *
 * Represents a single stationary period detected during a ride. Stops are automatically
 * detected when speed drops below configured threshold for configured duration.
 *
 * Database Schema:
 * - Table: stops
 * - Foreign Keys: ride_id (CASCADE delete), cluster_id (SET NULL delete)
 * - Indexes: ride_id, cluster_id, start_timestamp
 * - Constraints: UNIQUE (ride_id, stop_number)
 *
 * Immutability:
 * - All fields except endTimestamp and durationSeconds are immutable after creation
 * - endTimestamp and durationSeconds set once when stop ends
 * - clusterId set by Feature 010 clustering algorithm, never by this feature
 *
 * @property id Auto-generated primary key
 * @property rideId Foreign key to rides table (CASCADE delete when ride deleted)
 * @property stopNumber Sequential number within ride (1, 2, 3...), unique per ride
 * @property latitude GPS latitude at stop confirmation (decimal degrees, -90 to 90)
 * @property longitude GPS longitude at stop confirmation (decimal degrees, -180 to 180)
 * @property startTimestamp Unix epoch milliseconds when stop confirmed (duration threshold met)
 * @property endTimestamp Unix epoch milliseconds when movement resumed (null during active stop)
 * @property durationSeconds Calculated duration in seconds (null during active stop)
 * @property clusterId Foreign key to clusters table (Feature 010), NULL until clustering runs
 */
@Entity(
    tableName = "stops",
    foreignKeys = [
        ForeignKey(
            entity = Ride::class,
            parentColumns = ["id"],
            childColumns = ["ride_id"],
            onDelete = ForeignKey.CASCADE
        )
        // Note: cluster foreign key not enforced yet (clusters table from Feature 010)
    ],
    indices = [
        Index(value = ["ride_id"]),
        Index(value = ["cluster_id"]),
        Index(value = ["start_timestamp"]),
        Index(value = ["ride_id", "stop_number"], unique = true) // UNIQUE constraint
    ]
)
data class StopEntity(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    val id: Long = 0,

    @ColumnInfo(name = "ride_id")
    val rideId: Long,

    @ColumnInfo(name = "stop_number")
    val stopNumber: Int,

    @ColumnInfo(name = "latitude")
    val latitude: Double,

    @ColumnInfo(name = "longitude")
    val longitude: Double,

    @ColumnInfo(name = "start_timestamp")
    val startTimestamp: Long,

    @ColumnInfo(name = "end_timestamp")
    val endTimestamp: Long? = null,

    @ColumnInfo(name = "duration_seconds")
    val durationSeconds: Int? = null,

    @ColumnInfo(name = "cluster_id")
    val clusterId: Long? = null
)
