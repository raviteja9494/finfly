/* Domain use case aggregating filtered cached transactions into a compact report. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.CategorySpend
import com.teja.finflyiii.domain.model.MonthlyCashFlow
import com.teja.finflyiii.domain.model.ReportsFilter
import com.teja.finflyiii.domain.model.ReportsSummary
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.TransactionFilter
import com.teja.finflyiii.domain.model.TransactionType
import com.teja.finflyiii.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.YearMonth
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** Applies AND-combined report criteria and builds totals, monthly cash flow, and top categories. */
class ObserveReportsUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(filter: ReportsFilter): Flow<Result<ReportsSummary>> {
        val zone = ZoneId.systemDefault()
        val from = filter.fromDate.atStartOfDay(zone).toInstant()
        val until = filter.untilDate.plusDays(1).atStartOfDay(zone).toInstant()
        return repository.observeTransactions(
            filter = TransactionFilter(
                categories = filter.categories,
                tags = filter.tags,
                from = from,
                until = until,
            ),
            limit = REPORT_TRANSACTION_LIMIT,
            offset = 0,
        ).map { result ->
            when (result) {
                is Result.Error -> result
                is Result.Success -> Result.Success(buildSummary(result.value, filter, zone))
            }
        }
    }

    private fun buildSummary(
        transactions: List<Transaction>,
        filter: ReportsFilter,
        zone: ZoneId,
    ): ReportsSummary {
        val firstMonth = YearMonth.from(filter.fromDate)
        val lastMonth = YearMonth.from(filter.untilDate)
        val monthCount = ChronoUnit.MONTHS.between(firstMonth, lastMonth).toInt() + 1
        val rowsByMonth = transactions.groupBy { YearMonth.from(it.date.atZone(zone)) }
        val income = transactions.totalFor(TransactionType.DEPOSIT)
        val expenses = transactions.totalFor(TransactionType.WITHDRAWAL)
        val cashFlow = (0 until monthCount).map { offset ->
            val month = firstMonth.plusMonths(offset.toLong())
            val rows = rowsByMonth[month].orEmpty()
            MonthlyCashFlow(
                month = month,
                income = rows.totalFor(TransactionType.DEPOSIT),
                expenses = rows.totalFor(TransactionType.WITHDRAWAL),
            )
        }
        val categories = transactions.asSequence()
            .filter { it.type == TransactionType.WITHDRAWAL }
            .groupBy(Transaction::category)
            .map { (category, rows) ->
                CategorySpend(category, rows.fold(BigDecimal.ZERO) { total, row -> total + row.amount.abs() })
            }
            .sortedByDescending(CategorySpend::amount)
            .take(CATEGORY_COUNT)
        return ReportsSummary(
            currency = transactions.firstOrNull()?.currency ?: DEFAULT_CURRENCY,
            income = income,
            expenses = expenses,
            netFlow = income - expenses,
            monthlyCashFlow = cashFlow,
            categorySpending = categories,
            transactionCount = transactions.count { it.type != TransactionType.TRANSFER },
            fromDate = filter.fromDate,
            untilDate = filter.untilDate,
        )
    }

    private fun List<Transaction>.totalFor(type: TransactionType): BigDecimal = asSequence()
        .filter { it.type == type }
        .fold(BigDecimal.ZERO) { total, row -> total + row.amount.abs() }

    private companion object {
        const val REPORT_TRANSACTION_LIMIT = 10_000
        const val CATEGORY_COUNT = 5
        const val DEFAULT_CURRENCY = "XXX"
    }
}
