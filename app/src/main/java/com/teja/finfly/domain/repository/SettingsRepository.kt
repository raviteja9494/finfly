/* Domain repository contract for local connection configuration. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.AppSettings
import com.teja.finfly.domain.model.DashboardChartPeriod
import com.teja.finfly.domain.model.DashboardRangeMode
import com.teja.finfly.domain.model.CategoryChartStyle
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * Persists and exposes Firefly connection settings.
 * Inputs are complete settings or sync timestamps; outputs are observable settings and wrapped outcomes.
 */
interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    suspend fun save(serverUrl: String, bearerToken: String): Result<Unit>
    suspend fun logout(): Result<Unit>
    suspend fun saveDashboardPreferences(
        showNetWorthSummary: Boolean,
        recentTransactionsCount: Int,
        chartPeriod: DashboardChartPeriod,
        rangeMode: DashboardRangeMode,
        showSpendingInsight: Boolean,
        categoryChartStyle: CategoryChartStyle,
    ): Result<Unit>
    suspend fun updateLastSyncTime(instant: Instant): Result<Unit>
    suspend fun setSmsParsingEnabled(enabled: Boolean): Result<Unit>
    suspend fun setUseDeviceTimezone(enabled: Boolean): Result<Unit>
}
