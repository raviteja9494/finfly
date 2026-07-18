/* Data-layer offline-first implementation of the transaction repository. */
package com.teja.finfly.data.repository

import androidx.room.withTransaction
import com.teja.finfly.data.local.FinFlyDatabase
import com.teja.finfly.data.mapper.toDomain
import com.teja.finfly.data.mapper.toEntities
import com.teja.finfly.data.mapper.toEntity
import com.teja.finfly.data.local.entity.TagEntity
import com.teja.finfly.data.network.FireflyApiService
import com.teja.finfly.data.network.dto.StoreTransactionRequest
import com.teja.finfly.data.network.dto.StoreTransactionSplit
import com.teja.finfly.data.network.dto.UpdateTransactionRequest
import com.teja.finfly.data.network.dto.UpdateTransactionSplit
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.Category
import com.teja.finfly.domain.model.DailySpend
import com.teja.finfly.domain.model.TransactionDraft
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.LocalDate
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
    override fun observeTransactions(
        filter: TransactionFilter,
        limit: Int,
        offset: Int,
    ): Flow<Result<List<Transaction>>> = database.transactionDao().observeAll()
            .map { entities ->
                val transactions = entities.asSequence().map { it.toDomain() }
                    .filter { it.matches(filter) }
                    .drop(offset)
                    .take(limit)
                    .toList()
                Result.Success(transactions) as Result<List<Transaction>>
            }
            .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override fun observeTransaction(id: String): Flow<Result<Transaction?>> =
        database.transactionDao().observeById(id)
            .map { Result.Success(it?.toDomain()) as Result<Transaction?> }
            .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override fun observeTransactionCount(filter: TransactionFilter): Flow<Result<Int>> =
        database.transactionDao().observeAll()
            .map { entities ->
                Result.Success(entities.count { it.toDomain().matches(filter) }) as Result<Int>
            }
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

    override fun observeDailySpending(from: Instant, until: Instant): Flow<Result<List<DailySpend>>> =
        database.transactionDao().observeWithdrawals(from.toEpochMilli(), until.toEpochMilli())
            .map { rows ->
                val zone = ZoneId.systemDefault()
                val totals = rows.groupBy { Instant.ofEpochMilli(it.dateEpochMillis).atZone(zone).toLocalDate() }
                    .mapValues { (_, values) ->
                        values.fold(BigDecimal.ZERO) { total, row ->
                            total + (row.amount.toBigDecimalOrNull() ?: BigDecimal.ZERO)
                        }
                    }
                Result.Success(totals.map { DailySpend(it.key, it.value) }.sortedBy(DailySpend::date)) as Result<List<DailySpend>>
            }
            .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override fun observeCategories(): Flow<Result<List<Category>>> = database.categoryDao().observeAll()
        .map { rows -> Result.Success(rows.map { it.toDomain() }) as Result<List<Category>> }
        .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override suspend fun saveTransaction(draft: TransactionDraft): Result<Transaction> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            val split = StoreTransactionSplit(
                type = draft.type.name.lowercase(),
                date = draft.date.toString(),
                amount = draft.amount.abs().toPlainString(),
                description = draft.description.trim(),
                currencyCode = draft.currency.takeIf(String::isNotBlank),
                sourceId = draft.sourceAccountId,
                sourceName = draft.sourceAccount.takeIf { draft.sourceAccountId == null && it.isNotBlank() },
                destinationId = draft.destinationAccountId,
                destinationName = draft.destinationAccount.takeIf { draft.destinationAccountId == null && it.isNotBlank() },
                categoryName = draft.category.takeIf(String::isNotBlank),
                tags = draft.tags,
                notes = draft.notes?.takeIf(String::isNotBlank),
            )
            val response = if (draft.remoteGroupId != null && draft.journalId != null) {
                api.updateTransaction(
                    id = draft.remoteGroupId,
                    request = UpdateTransactionRequest(
                        transactions = listOf(
                            UpdateTransactionSplit(
                                transactionJournalId = draft.journalId,
                                type = split.type,
                                date = split.date,
                                amount = split.amount,
                                description = split.description,
                                currencyCode = split.currencyCode,
                                sourceId = split.sourceId,
                                sourceName = split.sourceName,
                                destinationId = split.destinationId,
                                destinationName = split.destinationName,
                                categoryName = split.categoryName,
                                tags = split.tags,
                                notes = split.notes,
                            )
                        )
                    ),
                )
            } else {
                api.createTransaction(StoreTransactionRequest(transactions = listOf(split)))
            }
            val entities = response.data.toEntities()
            database.withTransaction {
                database.transactionDao().upsertAll(entities)
                database.tagDao().upsertAll(draft.tags.map { TagEntity("local:$it", it) })
            }
            entities.first().toDomain()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: SAVE_ERROR, it) },
        )
    }

    override suspend fun sync(from: Instant?, until: Instant?): Result<Unit> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
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

    private fun isConfigured(): Boolean = settingsRepository.settings.value.run {
        serverUrl.isNotBlank() && bearerToken.isNotBlank()
    }

    private fun Transaction.matches(filter: TransactionFilter): Boolean {
        val normalizedQuery = filter.query.trim()
        val queryMatches = normalizedQuery.isBlank() || listOf(
            description, notes.orEmpty(),
        ).any { it.contains(normalizedQuery, ignoreCase = true) } ||
            tags.any { it.contains(normalizedQuery, ignoreCase = true) }
        val typeMatches = filter.types.isEmpty() || type in filter.types
        val categoryMatches = filter.categories.isEmpty() || category in filter.categories
        val tagMatches = filter.tags.isEmpty() || tags.any { it in filter.tags }
        val accountMatches = filter.accountIds.isEmpty() ||
            sourceAccountId?.let { it in filter.accountIds } == true ||
            destinationAccountId?.let { it in filter.accountIds } == true
        val fromMatches = filter.from == null || !date.isBefore(filter.from)
        val untilMatches = filter.until == null || date.isBefore(filter.until)
        return queryMatches && typeMatches && categoryMatches && tagMatches &&
            accountMatches && fromMatches && untilMatches
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 100
        const val CACHE_ERROR = "cache_error"
        const val SYNC_ERROR = "sync_error"
        const val SAVE_ERROR = "save_error"
        const val NOT_CONFIGURED = "not_configured"
    }
}
