package com.example.bikeredlights.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.bikeredlights.data.local.dao.RideDao
import com.example.bikeredlights.data.local.dao.TrackPointDao
import com.example.bikeredlights.data.local.entity.Ride
import com.example.bikeredlights.data.local.entity.TrackPoint

/**
 * Room database for BikeRedlights application.
 *
 * Entities:
 * - Ride: Cycling session metadata and statistics
 * - TrackPoint: GPS coordinates captured during rides
 *
 * Relationships:
 * - Ride (1) ----< TrackPoint (many) with CASCADE delete
 *
 * Version History:
 * - Version 1: Initial database schema (v0.3.0)
 *   * rides table with 11 fields
 *   * track_points table with 9 fields + foreign key
 *   * Indices on startTime, rideId, timestamp
 *
 * Migration Strategy:
 * - Version 1: Uses fallbackToDestructiveMigration() (acceptable for v0.3.0)
 * - Future versions: Implement proper migrations to preserve user data
 *
 * Thread Safety:
 * - Singleton pattern ensures single database instance
 * - All DAO methods are suspend functions or Flow-based
 * - Room handles thread safety internally
 */
@Database(
    entities = [
        Ride::class,
        TrackPoint::class
    ],
    version = 1,
    exportSchema = true  // Generates schema in app/schemas/ for version control
)
abstract class BikeRedlightsDatabase : RoomDatabase() {

    /**
     * Data Access Object for Ride operations.
     *
     * Provides CRUD operations and queries for rides table.
     */
    abstract fun rideDao(): RideDao

    /**
     * Data Access Object for TrackPoint operations.
     *
     * Provides insert and query operations for track_points table.
     */
    abstract fun trackPointDao(): TrackPointDao

    companion object {
        /**
         * Database file name on device filesystem.
         */
        private const val DATABASE_NAME = "bike_redlights.db"

        /**
         * Singleton instance of the database.
         *
         * Volatile ensures visibility across threads.
         */
        @Volatile
        private var INSTANCE: BikeRedlightsDatabase? = null

        /**
         * Get or create the database instance (thread-safe singleton).
         *
         * Uses double-checked locking pattern for performance.
         *
         * @param context Application context (don't pass Activity context!)
         * @return Singleton database instance
         */
        fun getDatabase(context: Context): BikeRedlightsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BikeRedlightsDatabase::class.java,
                    DATABASE_NAME
                )
                    .fallbackToDestructiveMigration()  // For v1 only - implement migrations later!
                    .build()

                INSTANCE = instance
                instance
            }
        }

        /**
         * Clear database instance (for testing only).
         *
         * WARNING: Only call this in test teardown!
         */
        @androidx.annotation.VisibleForTesting
        fun clearInstance() {
            INSTANCE = null
        }
    }
}
