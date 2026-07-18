/* Domain-layer aggregate used by the dashboard presentation. */
package com.teja.finfly.domain.model

import java.math.BigDecimal

/** Summarizes current spending periods and the most recent transactions. */
data class DashboardSummary(
    val todaySpend: BigDecimal,
    val monthSpend: BigDecimal,
    val currency: String,
    val weeklySpending: List<DailySpend>,
    val accounts: List<Account>,
    val recentTransactions: List<Transaction>,
)
