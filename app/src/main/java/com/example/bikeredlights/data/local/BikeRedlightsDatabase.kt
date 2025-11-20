package com.example.bikeredlights.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.bikeredlights.data.local.dao.RideDao
import com.example.bikeredlights.data.local.dao.StopDao
import com.example.bikeredlights.data.local.dao.TrackPointDao
import com.example.bikeredlights.data.local.entity.Ride
import com.example.bikeredlights.data.local.entity.StopEntity
import com.example.bikeredlights.data.local.entity.TrackPoint

/**
 * Room database for BikeRedlights application.
 *
 * Entities:
 * - Ride: Cycling session metadata and statistics
 * - TrackPoint: GPS coordinates captured during rides
 * - StopEntity: Stationary periods detected during rides (Feature 009)
 *
 * Relationships:
 * - Ride (1) ----< TrackPoint (many) with CASCADE delete
 * - Ride (1) ----< StopEntity (many) with CASCADE delete
 *
 * Version History:
 * - Version 1: Initial database schema (v0.3.0)
 *   * rides table with 11 fields
 *   * track_points table with 9 fields + foreign key
 *   * Indices on startTime, rideId, timestamp
 * - Version 2: Add stops table (v0.9.0 - Feature 009)
 *   * stops table with 9 fields + foreign keys
 *   * Indices on ride_id, cluster_id, start_timestamp
 *   * UNIQUE constraint on (ride_id, stop_number)
 *
 * Migration Strategy:
 * - Version 1→2 (MIGRATION_1_2): Add stops table with foreign keys and indexes
 * - Proper migrations preserve user data (no destructive migration after v1)
 *
 * Thread Safety:
 * - Singleton pattern ensures single database instance
 * - All DAO methods are suspend functions or Flow-based
 * - Room handles thread safety internally
 */
@Database(
    entities = [
        Ride::class,
        TrackPoint::class,
        StopEntity::class
    ],
    version = 2,
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

    /**
     * Data Access Object for Stop operations (Feature 009).
     *
     * Provides insert, update, and query operations for stops table.
     */
    abstract fun stopDao(): StopDao

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
         * Migration from version 1 to 2: Add stops table (Feature 009).
         *
         * Changes:
         * - Create stops table with 9 columns
         * - Add foreign key to rides table with CASCADE delete
         * - Add indexes for ride_id, cluster_id, start_timestamp
         * - Add UNIQUE constraint on (ride_id, stop_number)
         *
         * Data Preservation:
         * - No existing data affected (new table only)
         * - All rides and track_points preserved
         *
         * Testing:
         * - Validated via MigrationTestHelper in BikeRedlightsDatabaseTest
         * - Verified foreign key CASCADE delete behavior
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Create stops table with foreign keys
                database.execSQL("""
                    CREATE TABLE stops (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        ride_id INTEGER NOT NULL,
                        stop_number INTEGER NOT NULL,
                        latitude REAL NOT NULL,
                        longitude REAL NOT NULL,
                        start_timestamp INTEGER NOT NULL,
                        end_timestamp INTEGER,
                        duration_seconds INTEGER,
                        cluster_id INTEGER,
                        FOREIGN KEY (ride_id) REFERENCES rides(id) ON DELETE CASCADE
                    )
                """)

                // Critical: Create indexes for foreign keys (avoids performance warnings)
                // IMPORTANT: Index names MUST match Room's auto-generated names exactly
                // Room naming pattern: "index_{table}_{column}" or "index_{table}_{col1}_{col2}"
                database.execSQL("CREATE INDEX index_stops_ride_id ON stops(ride_id)")
                database.execSQL("CREATE INDEX index_stops_cluster_id ON stops(cluster_id)")
                database.execSQL("CREATE INDEX index_stops_start_timestamp ON stops(start_timestamp)")

                // UNIQUE constraint on (ride_id, stop_number) prevents duplicate stop numbers per ride
                database.execSQL(
                    "CREATE UNIQUE INDEX index_stops_ride_id_stop_number ON stops(ride_id, stop_number)"
                )
            }
        }

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
                    .addMigrations(MIGRATION_1_2)  // Apply migration v1→v2
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
