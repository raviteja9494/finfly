/* Domain use case combining cached finance streams into Dashboard analytics. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.AccountGroup
import com.teja.finfly.domain.model.CategorySpend
import com.teja.finfly.domain.model.DailySpend
import com.teja.finfly.domain.model.DashboardSummary
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.Clock
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/** Enforces local calendar boundaries and builds weekly, category, and balance summaries. */
class ObserveDashboardUseCase @Inject constructor(
    private val transactionRepository: TransactionRepository,
    private val accountRepository: AccountRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<Result<DashboardSummary>> {
        val zone = ZoneId.systemDefault()
        val today = clock.instant().atZone(zone).toLocalDate()
        val todayStart = today.atStartOfDay(zone).toInstant()
        val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant()
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant()
        val weekStartDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val weekStart = weekStartDate.atStartOfDay(zone).toInstant()
        val weekEnd = weekStartDate.plusDays(7).atStartOfDay(zone).toInstant()

        val totalsAndRecent = combine(
            transactionRepository.observeSpending(todayStart, tomorrowStart),
            transactionRepository.observeSpending(monthStart, tomorrowStart),
            transactionRepository.observeRecent(RECENT_COUNT),
        ) { todayResult, monthResult, recentResult ->
            Triple(todayResult, monthResult, recentResult)
        }
        val analyticsAndAccounts = combine(
            transactionRepository.observeDailySpending(weekStart, weekEnd),
            transactionRepository.observeTransactions(
                TransactionFilter(from = monthStart, until = tomorrowStart),
                MONTH_TRANSACTION_LIMIT,
                0,
            ),
            accountRepository.observeAccounts(),
        ) { weeklyResult, transactionsResult, accountsResult ->
            Triple(weeklyResult, transactionsResult, accountsResult)
        }
        return combine(totalsAndRecent, analyticsAndAccounts) { totals, analytics ->
            buildSummary(totals, analytics, weekStartDate)
        }
    }

    private fun buildSummary(
        totals: Triple<Result<BigDecimal>, Result<BigDecimal>, Result<List<Transaction>>>,
        analytics: Triple<Result<List<DailySpend>>, Result<List<Transaction>>, Result<List<Account>>>,
        weekStartDate: java.time.LocalDate,
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
                weeklySpending = (0L..6L).map { offset ->
                    val date = weekStartDate.plusDays(offset)
                    weekly[date] ?: DailySpend(date, BigDecimal.ZERO)
                },
                categorySpending = monthTransactions
                    .filter { it.type == com.teja.finfly.domain.model.TransactionType.WITHDRAWAL }
                    .groupBy { it.category }
                    .map { (category, rows) ->
                        CategorySpend(
                            category,
                            rows.fold(BigDecimal.ZERO) { total, transaction -> total + transaction.amount },
                        )
                    }
                    .sortedByDescending(CategorySpend::amount)
                    .take(CATEGORY_COUNT),
                accounts = accounts.filter(Account::isBalanceAccount),
                recentTransactions = recent,
            )
        )
    }

    private fun <T> Result<T>.valueOrReturnError(): T? = when (this) {
        is Result.Success -> value
        is Result.Error -> null
    }
    private fun <T> Result<T>.asError(): Result.Error = this as? Result.Error ?: Result.Error(DASHBOARD_ERROR)

    private companion object {
        const val RECENT_COUNT = 10
        const val CATEGORY_COUNT = 5
        const val MONTH_TRANSACTION_LIMIT = 10_000
        const val DASHBOARD_ERROR = "dashboard_error"
    }
}
