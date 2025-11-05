package com.example.bikeredlights.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for application-level dependencies.
 *
 * Provides dependencies that live as long as the application:
 * - Application Context
 * - Other app-wide singletons
 *
 * @InstallIn(SingletonComponent::class) means these dependencies
 * are available throughout the app lifecycle.
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    /**
     * Provides the application context.
     *
     * @ApplicationContext qualifier ensures we get the app context,
     * not an activity context, preventing memory leaks.
     */
    @Provides
    @Singleton
    fun provideContext(@ApplicationContext context: Context): Context {
        return context
    }
}
