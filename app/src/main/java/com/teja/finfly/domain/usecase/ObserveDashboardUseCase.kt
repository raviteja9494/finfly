/* Domain use case combining cached transaction streams into dashboard totals. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.DashboardSummary
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.DailySpend
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.Clock
import java.time.ZoneId
import java.time.DayOfWeek
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

/** Enforces local-calendar boundaries for today's and this month's withdrawal totals. */
class ObserveDashboardUseCase @Inject constructor(
    private val repository: TransactionRepository,
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
        return combine(
            repository.observeSpending(todayStart, tomorrowStart),
            repository.observeSpending(monthStart, tomorrowStart),
            repository.observeRecent(5),
            repository.observeDailySpending(weekStart, weekEnd),
            accountRepository.observeAccounts(),
        ) { todayResult, monthResult, recentResult, weeklyResult, accountsResult ->
            if (todayResult is Result.Error) return@combine todayResult
            if (monthResult is Result.Error) return@combine monthResult
            if (recentResult is Result.Error) return@combine recentResult
            if (weeklyResult is Result.Error) return@combine weeklyResult
            if (accountsResult is Result.Error) return@combine accountsResult
            val recent = (recentResult as Result.Success<List<Transaction>>).value
            val weekly = (weeklyResult as Result.Success<List<DailySpend>>).value.associateBy(DailySpend::date)
            val accounts = (accountsResult as Result.Success<List<Account>>).value.filter(Account::isBalanceAccount)
            Result.Success(
                DashboardSummary(
                    todaySpend = (todayResult as Result.Success<BigDecimal>).value,
                    monthSpend = (monthResult as Result.Success<BigDecimal>).value,
                    currency = recent.firstOrNull()?.currency ?: accounts.firstOrNull()?.currency ?: "XXX",
                    weeklySpending = (0L..6L).map { offset ->
                        weekly[weekStartDate.plusDays(offset)] ?: DailySpend(
                            date = weekStartDate.plusDays(offset),
                            amount = BigDecimal.ZERO,
                        )
                    },
                    accounts = accounts,
                    recentTransactions = recent,
                )
            )
        }
    }
}
