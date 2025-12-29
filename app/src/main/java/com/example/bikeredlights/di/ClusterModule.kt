package com.example.bikeredlights.di

import com.example.bikeredlights.domain.repository.StopRepository
import com.example.bikeredlights.domain.usecase.CalculateClusterCenterUseCase
import com.example.bikeredlights.domain.usecase.CalculateClusterStatsUseCase
import com.example.bikeredlights.domain.usecase.FormatClusterAnalyticsUseCase
import com.example.bikeredlights.domain.usecase.GetClusteredStopsUseCase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.scopes.ViewModelScoped

/**
 * Hilt module providing cluster visualization use cases (Feature 011).
 *
 * Created by: Phase 3 - User Story 1 (View Clusters on Map)
 * Installed in: ViewModelComponent (lifecycle tied to ViewModel)
 *
 * Scope: ViewModelScoped (one instance per ViewModel instance)
 *
 * Provided Use Cases:
 * 1. GetClusteredStopsUseCase - Fetch clustered stops from repository
 * 2. CalculateClusterCenterUseCase - Calculate cluster center coordinates
 * 3. FormatClusterAnalyticsUseCase - Generate frequency text
 * 4. CalculateClusterStatsUseCase - Aggregate stops into ClusterSummary
 *
 * Dependency Graph:
 * ```
 * GetClusteredStopsUseCase
 * └── StopRepository (provided by DatabaseModule)
 *
 * CalculateClusterCenterUseCase
 * └── (no dependencies - pure function)
 *
 * FormatClusterAnalyticsUseCase
 * └── (no dependencies - pure function)
 *
 * CalculateClusterStatsUseCase
 * ├── CalculateClusterCenterUseCase
 * └── FormatClusterAnalyticsUseCase
 * ```
 *
 * Why ViewModelComponent:
 * - Use cases are stateless and lightweight
 * - Scoped to ViewModel lifecycle (created with ViewModel, destroyed with ViewModel)
 * - Avoids singleton overhead for feature-specific logic
 * - Allows different ViewModels to have independent use case instances if needed
 */
@Module
@InstallIn(ViewModelComponent::class)
object ClusterModule {

    /**
     * Provide GetClusteredStopsUseCase for fetching clustered stops.
     *
     * Dependencies:
     * - StopRepository: Provided by DatabaseModule
     *
     * @param stopRepository Repository for stop data access
     * @return GetClusteredStopsUseCase instance
     */
    @Provides
    @ViewModelScoped
    fun provideGetClusteredStopsUseCase(
        stopRepository: StopRepository
    ): GetClusteredStopsUseCase {
        return GetClusteredStopsUseCase(stopRepository)
    }

    /**
     * Provide CalculateClusterCenterUseCase for GPS coordinate averaging.
     *
     * Dependencies: None (pure computation)
     *
     * @return CalculateClusterCenterUseCase instance
     */
    @Provides
    @ViewModelScoped
    fun provideCalculateClusterCenterUseCase(): CalculateClusterCenterUseCase {
        return CalculateClusterCenterUseCase()
    }

    /**
     * Provide FormatClusterAnalyticsUseCase for frequency text generation.
     *
     * Dependencies: None (pure computation)
     *
     * @return FormatClusterAnalyticsUseCase instance
     */
    @Provides
    @ViewModelScoped
    fun provideFormatClusterAnalyticsUseCase(): FormatClusterAnalyticsUseCase {
        return FormatClusterAnalyticsUseCase()
    }

    /**
     * Provide CalculateClusterStatsUseCase for cluster statistics aggregation.
     *
     * Dependencies:
     * - CalculateClusterCenterUseCase: For cluster center calculation
     * - FormatClusterAnalyticsUseCase: For frequency text generation
     *
     * @param calculateClusterCenterUseCase Use case for center calculation
     * @param formatClusterAnalyticsUseCase Use case for analytics formatting
     * @return CalculateClusterStatsUseCase instance
     */
    @Provides
    @ViewModelScoped
    fun provideCalculateClusterStatsUseCase(
        calculateClusterCenterUseCase: CalculateClusterCenterUseCase,
        formatClusterAnalyticsUseCase: FormatClusterAnalyticsUseCase
    ): CalculateClusterStatsUseCase {
        return CalculateClusterStatsUseCase(
            calculateClusterCenterUseCase,
            formatClusterAnalyticsUseCase
        )
    }
}
