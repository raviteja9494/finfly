/* Domain use case aggregating cached transactions into a compact report. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.CategorySpend
import com.teja.finfly.domain.model.MonthlyCashFlow
import com.teja.finfly.domain.model.ReportsSummary
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Clock
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

/** Builds current-month totals, a three-month cash-flow trend, and top spending categories. */
class ObserveReportsUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<Result<ReportsSummary>> {
        val zone = ZoneId.systemDefault()
        val today = clock.instant().atZone(zone).toLocalDate()
        val currentMonth = YearMonth.from(today)
        val firstMonth = currentMonth.minusMonths(REPORT_MONTHS - 1L)
        val from = firstMonth.atDay(1).atStartOfDay(zone).toInstant()
        val until = today.plusDays(1).atStartOfDay(zone).toInstant()
        return repository.observeTransactions(
            filter = TransactionFilter(from = from, until = until),
            limit = REPORT_TRANSACTION_LIMIT,
            offset = 0,
        ).map { result ->
            when (result) {
                is Result.Error -> result
                is Result.Success -> Result.Success(buildSummary(result.value, currentMonth, firstMonth, zone))
            }
        }
    }

    private fun buildSummary(
        transactions: List<Transaction>,
        currentMonth: YearMonth,
        firstMonth: YearMonth,
        zone: ZoneId,
    ): ReportsSummary {
        val rowsByMonth = transactions.groupBy { YearMonth.from(it.date.atZone(zone)) }
        val currentRows = rowsByMonth[currentMonth].orEmpty()
        val income = currentRows.totalFor(TransactionType.DEPOSIT)
        val expenses = currentRows.totalFor(TransactionType.WITHDRAWAL)
        val cashFlow = (0 until REPORT_MONTHS).map { offset ->
            val month = firstMonth.plusMonths(offset.toLong())
            val rows = rowsByMonth[month].orEmpty()
            MonthlyCashFlow(
                month = month,
                income = rows.totalFor(TransactionType.DEPOSIT),
                expenses = rows.totalFor(TransactionType.WITHDRAWAL),
            )
        }
        val categories = currentRows.asSequence()
            .filter { it.type == TransactionType.WITHDRAWAL }
            .groupBy(Transaction::category)
            .map { (category, rows) ->
                CategorySpend(category, rows.fold(BigDecimal.ZERO) { total, row -> total + row.amount.abs() })
            }
            .sortedByDescending(CategorySpend::amount)
            .take(CATEGORY_COUNT)
        return ReportsSummary(
            currency = currentRows.firstOrNull()?.currency ?: transactions.firstOrNull()?.currency ?: DEFAULT_CURRENCY,
            monthIncome = income,
            monthExpenses = expenses,
            monthNetFlow = income - expenses,
            monthlyCashFlow = cashFlow,
            categorySpending = categories,
            transactionCount = transactions.count { it.type != TransactionType.TRANSFER },
        )
    }

    private fun List<Transaction>.totalFor(type: TransactionType): BigDecimal = asSequence()
        .filter { it.type == type }
        .fold(BigDecimal.ZERO) { total, row -> total + row.amount.abs() }

    private companion object {
        const val REPORT_MONTHS = 3
        const val REPORT_TRANSACTION_LIMIT = 10_000
        const val CATEGORY_COUNT = 5
        const val DEFAULT_CURRENCY = "XXX"
    }
}
