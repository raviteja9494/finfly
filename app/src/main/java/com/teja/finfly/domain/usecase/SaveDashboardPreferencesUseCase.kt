/* Domain use case validating and saving Dashboard display preferences. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.repository.SettingsRepository
import javax.inject.Inject

/** Restricts recent-transaction choices to the supported Dashboard values before persistence. */
class SaveDashboardPreferencesUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(showNetWorthSummary: Boolean, recentTransactionsCount: Int): Result<Unit> =
        repository.saveDashboardPreferences(
            showNetWorthSummary = showNetWorthSummary,
            recentTransactionsCount = recentTransactionsCount.takeIf { it in SUPPORTED_COUNTS } ?: DEFAULT_COUNT,
        )

    private companion object {
        val SUPPORTED_COUNTS = setOf(5, 10, 20)
        const val DEFAULT_COUNT = 10
    }
}
