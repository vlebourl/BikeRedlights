package com.example.bikeredlights.di

import com.example.bikeredlights.data.repository.RideRecordingStateRepositoryImpl
import com.example.bikeredlights.data.repository.RideRepositoryImpl
import com.example.bikeredlights.data.repository.TrackPointRepositoryImpl
import com.example.bikeredlights.domain.repository.RideRecordingStateRepository
import com.example.bikeredlights.domain.repository.RideRepository
import com.example.bikeredlights.domain.repository.TrackPointRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for binding repository interfaces to implementations.
 *
 * Uses @Binds to tell Hilt which implementation class to use for each
 * repository interface. This module handles repositories that have
 * @Inject constructors - Hilt will automatically instantiate them
 * using their injected dependencies.
 *
 * @Binds vs @Provides:
 * - @Binds: Used when you just need to map an interface to an implementation
 * - @Provides: Used when you need custom instantiation logic
 *
 * Must be an abstract class (not object) to use @Binds methods.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryBindingsModule {

    /**
     * Binds RideRepository interface to RideRepositoryImpl.
     *
     * RideRepositoryImpl has @Inject constructor with RideDao parameter.
     * Hilt will automatically provide the RideDao from DatabaseModule.
     */
    @Binds
    @Singleton
    abstract fun bindRideRepository(
        impl: RideRepositoryImpl
    ): RideRepository

    /**
     * Binds TrackPointRepository interface to TrackPointRepositoryImpl.
     *
     * TrackPointRepositoryImpl has @Inject constructor with TrackPointDao parameter.
     * Hilt will automatically provide the TrackPointDao from DatabaseModule.
     */
    @Binds
    @Singleton
    abstract fun bindTrackPointRepository(
        impl: TrackPointRepositoryImpl
    ): TrackPointRepository

    /**
     * Binds RideRecordingStateRepository interface to RideRecordingStateRepositoryImpl.
     *
     * RideRecordingStateRepositoryImpl has @Inject constructor with @ApplicationContext Context.
     * Hilt will automatically provide the application context.
     */
    @Binds
    @Singleton
    abstract fun bindRideRecordingStateRepository(
        impl: RideRecordingStateRepositoryImpl
    ): RideRecordingStateRepository
}
