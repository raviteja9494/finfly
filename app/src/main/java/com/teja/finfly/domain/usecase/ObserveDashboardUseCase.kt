/* Domain use case combining cached transaction streams into dashboard totals. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.DashboardSummary
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import java.math.BigDecimal
import java.time.Clock
import java.time.ZoneId
import javax.inject.Inject

/** Enforces local-calendar boundaries for today's and this month's withdrawal totals. */
class ObserveDashboardUseCase @Inject constructor(
    private val repository: TransactionRepository,
    private val clock: Clock,
) {
    operator fun invoke(): Flow<Result<DashboardSummary>> {
        val zone = ZoneId.systemDefault()
        val today = clock.instant().atZone(zone).toLocalDate()
        val todayStart = today.atStartOfDay(zone).toInstant()
        val tomorrowStart = today.plusDays(1).atStartOfDay(zone).toInstant()
        val monthStart = today.withDayOfMonth(1).atStartOfDay(zone).toInstant()
        return combine(
            repository.observeSpending(todayStart, tomorrowStart),
            repository.observeSpending(monthStart, tomorrowStart),
            repository.observeRecent(5),
        ) { todayResult, monthResult, recentResult ->
            if (todayResult is Result.Error) return@combine todayResult
            if (monthResult is Result.Error) return@combine monthResult
            if (recentResult is Result.Error) return@combine recentResult
            val recent = (recentResult as Result.Success<List<Transaction>>).value
            Result.Success(
                DashboardSummary(
                    todaySpend = (todayResult as Result.Success<BigDecimal>).value,
                    monthSpend = (monthResult as Result.Success<BigDecimal>).value,
                    currency = recent.firstOrNull()?.currency ?: "XXX",
                    recentTransactions = recent,
                )
            )
        }
    }
}
