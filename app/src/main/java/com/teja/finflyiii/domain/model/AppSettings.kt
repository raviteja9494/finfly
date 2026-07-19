/* Domain-layer model for local Firefly connection preferences. */
package com.teja.finflyiii.domain.model

import java.time.Instant

/** Connection settings persisted locally; [bearerToken] must never be logged. */
data class AppSettings(
    val serverUrl: String = "",
    val bearerToken: String = "",
    val lastSyncTime: Instant? = null,
    val showNetWorthSummary: Boolean = false,
    val recentTransactionsCount: Int = 10,
    val dashboardChartPeriod: DashboardChartPeriod = DashboardChartPeriod.WEEK,
    val dashboardRangeMode: DashboardRangeMode = DashboardRangeMode.CALENDAR,
    val showSpendingInsight: Boolean = true,
    val categoryChartStyle: CategoryChartStyle = CategoryChartStyle.BARS,
    val categoryChartPeriod: DashboardChartPeriod = DashboardChartPeriod.MONTH,
    val categoryRangeMode: DashboardRangeMode = DashboardRangeMode.CALENDAR,
    val smsParsingEnabled: Boolean = false,
    val useDeviceTimezone: Boolean = true,
)
