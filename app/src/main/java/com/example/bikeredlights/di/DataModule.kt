package com.example.bikeredlights.di

import android.content.Context
import com.example.bikeredlights.data.repository.LocationRepositoryImpl
import com.example.bikeredlights.data.repository.SettingsRepository
import com.example.bikeredlights.data.repository.SettingsRepositoryImpl
import com.example.bikeredlights.domain.repository.LocationRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for data layer dependencies.
 *
 * Provides repository implementations to the dependency graph.
 * All repositories are singletons to ensure:
 * - Single source of truth for data
 * - Efficient resource usage (location services, database connections)
 * - Consistent state across the app
 *
 * Note: RideRepository, TrackPointRepository, and RideRecordingStateRepository
 * have @Inject constructors, so Hilt provides them automatically without
 * needing explicit @Provides methods here.
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    /**
     * Provides SettingsRepository implementation.
     *
     * Uses DataStore Preferences for persistent storage.
     *
     * @param context Application context for DataStore access
     * @return SettingsRepository instance
     */
    @Provides
    @Singleton
    fun provideSettingsRepository(
        @ApplicationContext context: Context
    ): SettingsRepository {
        return SettingsRepositoryImpl(context)
    }

    /**
     * Provides LocationRepository implementation.
     *
     * Uses Google Play Services FusedLocationProviderClient.
     * Depends on SettingsRepository for GPS accuracy configuration.
     *
     * @param context Application context for location services
     * @param settingsRepository Settings repository for GPS accuracy
     * @return LocationRepository instance
     */
    @Provides
    @Singleton
    fun provideLocationRepository(
        @ApplicationContext context: Context,
        settingsRepository: SettingsRepository
    ): LocationRepository {
        return LocationRepositoryImpl(context, settingsRepository)
    }
}
