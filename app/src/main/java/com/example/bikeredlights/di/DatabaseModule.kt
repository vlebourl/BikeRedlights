package com.example.bikeredlights.di

import android.content.Context
import com.example.bikeredlights.data.local.BikeRedlightsDatabase
import com.example.bikeredlights.data.local.dao.RideDao
import com.example.bikeredlights.data.local.dao.StopDao
import com.example.bikeredlights.data.local.dao.TrackPointDao
import com.example.bikeredlights.data.repository.StopRepositoryImpl
import com.example.bikeredlights.domain.repository.StopRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for database dependencies.
 *
 * Provides Room database and DAOs to the dependency graph.
 * All database dependencies are singletons to ensure:
 * - Single database instance across the app
 * - Thread-safe database operations
 * - Efficient resource usage
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides the Room database instance.
     *
     * Uses the factory method from BikeRedlightsDatabase companion object
     * to ensure singleton pattern and proper database initialization.
     *
     * @param context Application context
     * @return Singleton database instance
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): BikeRedlightsDatabase {
        return BikeRedlightsDatabase.getDatabase(context)
    }

    /**
     * Provides the Ride DAO.
     *
     * @param database Database instance
     * @return RideDao for ride database operations
     */
    @Provides
    @Singleton
    fun provideRideDao(database: BikeRedlightsDatabase): RideDao {
        return database.rideDao()
    }

    /**
     * Provides the TrackPoint DAO.
     *
     * @param database Database instance
     * @return TrackPointDao for track point database operations
     */
    @Provides
    @Singleton
    fun provideTrackPointDao(database: BikeRedlightsDatabase): TrackPointDao {
        return database.trackPointDao()
    }

    /**
     * Provides the Stop DAO (Feature 009).
     *
     * @param database Database instance
     * @return StopDao for stop database operations
     */
    @Provides
    @Singleton
    fun provideStopDao(database: BikeRedlightsDatabase): StopDao {
        return database.stopDao()
    }
}

/**
 * Hilt module for repository bindings.
 *
 * Uses @Binds to bind repository implementations to their interfaces.
 * Must be abstract class (Dagger requirement for @Binds).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    /**
     * Binds StopRepositoryImpl to StopRepository interface (Feature 009).
     *
     * @param impl Implementation provided by Hilt (StopRepositoryImpl)
     * @return Interface bound to implementation (StopRepository)
     */
    @Binds
    @Singleton
    abstract fun bindStopRepository(impl: StopRepositoryImpl): StopRepository
}
