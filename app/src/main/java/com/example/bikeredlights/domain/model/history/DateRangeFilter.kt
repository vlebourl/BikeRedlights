package com.example.bikeredlights.domain.model.history

/**
 * Date range filter for ride history queries.
 *
 * **Usage**:
 * - `None`: No date filtering (show all rides)
 * - `Custom`: Filter rides between start and end dates (inclusive)
 *
 * **Validation**:
 * - Start date must be <= end date
 * - Dates are represented as epoch milliseconds (UTC)
 *
 * @property startMillis Start of date range (inclusive), epoch milliseconds
 * @property endMillis End of date range (inclusive), epoch milliseconds
 */
sealed class DateRangeFilter {
    /**
     * No date filtering - show all rides.
     */
    object None : DateRangeFilter()

    /**
     * Custom date range filter.
     *
     * @param startMillis Start date (inclusive) in epoch milliseconds
     * @param endMillis End date (inclusive) in epoch milliseconds
     * @throws IllegalArgumentException if start > end
     */
    data class Custom(
        val startMillis: Long,
        val endMillis: Long
    ) : DateRangeFilter() {
        init {
            require(startMillis <= endMillis) {
                "Start date ($startMillis) must be <= end date ($endMillis)"
            }
        }
    }
}
