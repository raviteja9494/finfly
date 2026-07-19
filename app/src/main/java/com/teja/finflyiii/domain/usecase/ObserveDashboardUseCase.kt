/* Domain use case combining cached finance streams into Dashboard analytics. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.Account
import com.teja.finflyiii.domain.model.AccountGroup
import com.teja.finflyiii.domain.model.CategorySpend
import com.teja.finflyiii.domain.model.DailySpend
import com.teja.finflyiii.domain.model.DashboardSummary
import com.teja.finflyiii.domain.model.DashboardChartPeriod
import com.teja.finflyiii.domain.model.DashboardRangeMode
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.TransactionFilter
import com.teja.finflyiii.domain.repository.AccountRepository
import com.teja.finflyiii.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.time.temporal.ChronoUnit
import java.math.RoundingMode
import javax.inject.Inject

/** Enforces calendar or rolling chart boundaries and builds spending, category, and balance summaries. */
class ObserveDashboardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val clock: Clock,
) {
    operator fun invoke(
        recentTransactionCount: Int,
        chartPeriod: DashboardChartPeriod,
        rangeMode: DashboardRangeMode,
        categoryChartPeriod: DashboardChartPeriod,
        categoryRangeMode: DashboardRangeMode,
    ): Flow<Result<DashboardSummary>> {
        val zone = ZoneId.systemDefault()
        val today = clock.instant().atZone(zone).toLocalDate()
        val todayStart = today.atStartOfDay(zone).toInstant()
        val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant()
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant()
        val chartWindow = chartWindow(today, chartPeriod, rangeMode)
        val chartStart = chartWindow.start.atStartOfDay(zone).toInstant()
        val chartEnd = chartWindow.endExclusive.atStartOfDay(zone).toInstant()
        val categoryWindow = chartWindow(today, categoryChartPeriod, categoryRangeMode)
        val categoryStart = categoryWindow.start.atStartOfDay(zone).toInstant()
        val categoryEnd = categoryWindow.endExclusive.atStartOfDay(zone).toInstant()

        val totalsAndRecent = combine(
            transactionRepository.observeSpending(todayStart, tomorrowStart),
            transactionRepository.observeSpending(monthStart, tomorrowStart),
            transactionRepository.observeRecent(recentTransactionCount.coerceIn(MIN_RECENT_COUNT, MAX_RECENT_COUNT)),
        ) { todayResult, monthResult, recentResult ->
            Triple(todayResult, monthResult, recentResult)
        }
        val analyticsAndAccounts = combine(
            transactionRepository.observeDailySpending(chartStart, chartEnd),
            transactionRepository.observeTransactions(
                TransactionFilter(from = categoryStart, until = categoryEnd),
                MONTH_TRANSACTION_LIMIT,
                0,
            ),
            accountRepository.observeAccounts(),
        ) { weeklyResult, transactionsResult, accountsResult ->
            Triple(weeklyResult, transactionsResult, accountsResult)
        }
        return combine(totalsAndRecent, analyticsAndAccounts) { totals, analytics ->
            buildSummary(
                totals, analytics, today, chartWindow, chartPeriod, rangeMode,
                categoryChartPeriod, categoryRangeMode,
            )
        }
    }

    private fun buildSummary(
        totals: Triple<Result<BigDecimal>, Result<BigDecimal>, Result<List<Transaction>>>,
        analytics: Triple<Result<List<DailySpend>>, Result<List<Transaction>>, Result<List<Account>>>,
        today: java.time.LocalDate,
        chartWindow: ChartWindow,
        chartPeriod: DashboardChartPeriod,
        rangeMode: DashboardRangeMode,
        categoryChartPeriod: DashboardChartPeriod,
        categoryRangeMode: DashboardRangeMode,
    ): Result<DashboardSummary> {
        val todaySpend = totals.first.valueOrReturnError() ?: return totals.first.asError()
        val monthSpend = totals.second.valueOrReturnError() ?: return totals.second.asError()
        val recent = totals.third.valueOrReturnError() ?: return totals.third.asError()
        val weeklyRows = analytics.first.valueOrReturnError() ?: return analytics.first.asError()
        val monthTransactions = analytics.second.valueOrReturnError() ?: return analytics.second.asError()
        val accounts = analytics.third.valueOrReturnError() ?: return analytics.third.asError()
        val weekly = weeklyRows.associateBy(DailySpend::date)
        val currency = recent.firstOrNull()?.currency ?: accounts.firstOrNull()?.currency ?: "XXX"
        return Result.Success(
            DashboardSummary(
                todaySpend = todaySpend,
                monthSpend = monthSpend,
                currency = currency,
                totalAssets = accounts.filter { it.group == AccountGroup.ASSET }
                    .fold(BigDecimal.ZERO) { total, account -> total + account.balance },
                totalLiabilities = accounts.filter { it.group == AccountGroup.LIABILITY }
                    .fold(BigDecimal.ZERO) { total, account -> total + account.balance.abs() },
                monthDailyAverage = monthSpend.divide(
                    BigDecimal(today.dayOfMonth),
                    2,
                    RoundingMode.HALF_UP,
                ),
                chartSpending = (0 until chartWindow.dayCount).map { offset ->
                    val date = chartWindow.start.plusDays(offset.toLong())
                    weekly[date] ?: DailySpend(date, BigDecimal.ZERO)
                },
                chartPeriod = chartPeriod,
                rangeMode = rangeMode,
                categorySpending = monthTransactions
                    .filter { it.type == com.teja.finflyiii.domain.model.TransactionType.WITHDRAWAL }
                    .groupBy { it.category }
                    .map { (category, rows) ->
                        CategorySpend(
                            category,
                            rows.fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount },
                        )
                    }
                    .sortedByDescending(CategorySpend::amount)
                    .take(CATEGORY_COUNT),
                categoryChartPeriod = categoryChartPeriod,
                categoryRangeMode = categoryRangeMode,
                recentTransactions = recent,
            )
        )
    }

    private fun <T> Result<T>.valueOrReturnError(): T? = when (this) {
        is Result.Success -> value
        is Result.Error -> null
    }
    private fun <T> Result<T>.asError(): Result.Error = this as? Result.Error ?: Result.Error(DASHBOARD_ERROR)

    private fun chartWindow(
        today: java.time.LocalDate,
        period: DashboardChartPeriod,
        mode: DashboardRangeMode,
    ): ChartWindow = when (period to mode) {
        DashboardChartPeriod.WEEK to DashboardRangeMode.CALENDAR -> {
            val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            ChartWindow(start, start.plusDays(WEEK_DAYS))
        }
        DashboardChartPeriod.WEEK to DashboardRangeMode.ROLLING ->
            ChartWindow(today.minusDays(WEEK_DAYS - 1), today.plusDays(1))
        DashboardChartPeriod.MONTH to DashboardRangeMode.CALENDAR -> {
            val start = today.withDayOfMonth(1)
            ChartWindow(start, start.plusMonths(1))
        }
        else ->
            ChartWindow(today.minusDays(ROLLING_MONTH_DAYS - 1), today.plusDays(1))
    }

    private data class ChartWindow(
        val start: java.time.LocalDate,
        val endExclusive: java.time.LocalDate,
    ) {
        val dayCount: Int get() = ChronoUnit.DAYS.between(start, endExclusive).toInt()
    }

    private companion object {
        const val MIN_RECENT_COUNT = 5
        const val MAX_RECENT_COUNT = 20
        const val CATEGORY_COUNT = 5
        const val MONTH_TRANSACTION_LIMIT = 10_000
        const val DASHBOARD_ERROR = "dashboard_error"
        const val WEEK_DAYS = 7L
        const val ROLLING_MONTH_DAYS = 30L
    }
}
