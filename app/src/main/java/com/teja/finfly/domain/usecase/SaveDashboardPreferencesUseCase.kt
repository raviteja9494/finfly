/* Domain use case validating and saving Dashboard display preferences. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.model.DashboardChartPeriod
import com.teja.finfly.domain.model.DashboardRangeMode
import com.teja.finfly.domain.model.CategoryChartStyle
import javax.inject.Inject

/** Validates recent-item choices and persists Dashboard visibility and chart-window preferences. */
class SaveDashboardPreferencesUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(
        showNetWorthSummary: Boolean,
        recentTransactionsCount: Int,
        chartPeriod: DashboardChartPeriod,
        rangeMode: DashboardRangeMode,
        showSpendingInsight: Boolean,
        categoryChartStyle: CategoryChartStyle,
    ): Result<Unit> =
        repository.saveDashboardPreferences(
            showNetWorthSummary = showNetWorthSummary,
            recentTransactionsCount = recentTransactionsCount.takeIf { it in SUPPORTED_COUNTS } ?: DEFAULT_COUNT,
            chartPeriod = chartPeriod,
            rangeMode = rangeMode,
            showSpendingInsight = showSpendingInsight,
            categoryChartStyle = categoryChartStyle,
        )

    private companion object {
        val SUPPORTED_COUNTS = setOf(5, 10, 20)
        const val DEFAULT_COUNT = 10
    }
}
