/* Domain-layer model for local Firefly connection preferences. */
package com.teja.finfly.domain.model

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
)
