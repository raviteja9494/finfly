/* Data-layer offline-first implementation of the transaction repository. */
package com.teja.finfly.data.repository

import androidx.room.withTransaction
import com.teja.finfly.data.local.FinFlyDatabase
import com.teja.finfly.data.mapper.toDomain
import com.teja.finfly.data.mapper.toEntities
import com.teja.finfly.data.mapper.toEntity
import com.teja.finfly.data.network.FireflyApiService
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepositoryImpl @Inject constructor(
    private val api: FireflyApiService,
    private val database: FinFlyDatabase,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : TransactionRepository {
    override fun observeTransactions(limit: Int, offset: Int): Flow<Result<List<Transaction>>> =
        database.transactionDao().observePage(limit, offset)
            .map { entities -> Result.Success(entities.map { it.toDomain() }) as Result<List<Transaction>> }
            .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override fun observeRecent(limit: Int): Flow<Result<List<Transaction>>> =
        database.transactionDao().observeRecent(limit)
            .map { entities -> Result.Success(entities.map { it.toDomain() }) as Result<List<Transaction>> }
            .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override fun observeSpending(from: Instant, until: Instant): Flow<Result<BigDecimal>> =
        database.transactionDao().observeWithdrawals(from.toEpochMilli(), until.toEpochMilli())
            .map { rows ->
                Result.Success(rows.fold(BigDecimal.ZERO) { total, row ->
                    total + (row.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                }) as Result<BigDecimal>
            }
            .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override suspend fun sync(from: Instant?, until: Instant?): Result<Unit> {
        if (settingsRepository.settings.value.serverUrl.isBlank() ||
            settingsRepository.settings.value.bearerToken.isBlank()
        ) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            val transactions = mutableListOf<com.teja.finfly.data.local.entity.TransactionEntity>()
            var page = 1
            var totalPages: Int
            do {
                val response = api.getTransactions(
                    page = page,
                    limit = PAGE_SIZE,
                    start = from?.toLocalDateString(),
                    end = until?.toLocalDateString(),
                )
                transactions += response.data.flatMap { it.toEntities() }
                totalPages = response.meta?.pagination?.totalPages ?: page
                page++
            } while (page <= totalPages && page <= MAX_PAGES)

            val categories = mutableListOf<com.teja.finfly.data.local.entity.CategoryEntity>()
            page = 1
            do {
                val response = api.getCategories(page, PAGE_SIZE)
                categories += response.data.map { it.toEntity() }
                totalPages = response.meta?.pagination?.totalPages ?: page
                page++
            } while (page <= totalPages && page <= MAX_PAGES)

            database.withTransaction {
                database.transactionDao().upsertAll(transactions)
                database.categoryDao().upsertAll(categories)
            }
            settingsRepository.updateLastSyncTime(clock.instant())
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: SYNC_ERROR, it) }
    }

    private fun Instant.toLocalDateString(): String =
        atZone(ZoneId.systemDefault()).toLocalDate().format(DateTimeFormatter.ISO_LOCAL_DATE)

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 100
        const val CACHE_ERROR = "cache_error"
        const val SYNC_ERROR = "sync_error"
        const val NOT_CONFIGURED = "not_configured"
    }
}
