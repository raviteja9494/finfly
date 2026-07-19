/* Domain-layer aggregates rendered by the compact Reports experience. */
package com.teja.finfly.domain.model

import java.math.BigDecimal
import java.time.YearMonth

/** Income and spending totals for one calendar month. */
data class MonthlyCashFlow(
    val month: YearMonth,
    val income: BigDecimal,
    val expenses: BigDecimal,
)

/** A small reporting snapshot derived entirely from cached transactions. */
data class ReportsSummary(
    val currency: String,
    val monthIncome: BigDecimal,
    val monthExpenses: BigDecimal,
    val monthNetFlow: BigDecimal,
    val monthlyCashFlow: List<MonthlyCashFlow>,
    val categorySpending: List<CategorySpend>,
    val transactionCount: Int,
)
