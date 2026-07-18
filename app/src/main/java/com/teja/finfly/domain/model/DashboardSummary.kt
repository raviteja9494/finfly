/* Domain-layer aggregate used by the dashboard presentation. */
package com.teja.finfly.domain.model

import java.math.BigDecimal

/** Summarizes current spending, the configured chart period, and the most recent transactions. */
data class DashboardSummary(
    val todaySpend: BigDecimal,
    val monthSpend: BigDecimal,
    val currency: String,
    val totalAssets: BigDecimal,
    val totalLiabilities: BigDecimal,
    val monthDailyAverage: BigDecimal,
    val chartSpending: List<DailySpend>,
    val chartPeriod: DashboardChartPeriod,
    val rangeMode: DashboardRangeMode,
    val categorySpending: List<CategorySpend>,
    val recentTransactions: List<Transaction>,
)
