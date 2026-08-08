/* Data-layer offline-first implementation of the transaction repository. */
package com.teja.finflyiii.data.repository

import androidx.room.withTransaction
import com.teja.finflyiii.data.local.FinFlyIIIDatabase
import com.teja.finflyiii.data.mapper.toDomain
import com.teja.finflyiii.data.mapper.toEntities
import com.teja.finflyiii.data.mapper.toEntity
import com.teja.finflyiii.data.local.entity.TagEntity
import com.teja.finflyiii.data.network.FireflyApiService
import com.teja.finflyiii.data.network.fireflyMessage
import com.teja.finflyiii.data.network.dto.StoreTransactionRequest
import com.teja.finflyiii.data.network.dto.StoreTransactionSplit
import com.teja.finflyiii.data.network.dto.UpdateTransactionRequest
import com.teja.finflyiii.data.network.dto.UpdateTransactionSplit
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.DailySpend
import com.teja.finflyiii.domain.model.TransactionDraft
import com.teja.finflyiii.domain.model.TransactionFilter
import com.teja.finflyiii.domain.repository.SettingsRepository
import com.teja.finflyiii.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
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
    private val database: FinFlyIIIDatabase,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : TransactionRepository {
    override fun observeTransactions(
        filter: TransactionFilter,
        limit: Int,
        offset: Int,
    ): Flow<Result<List<Transaction>>> = (if (filter.canUseDateQuery()) {
        database.transactionDao().observeDatePage(
            filter.from?.toEpochMilli(),
            filter.until?.toEpochMilli(),
            limit,
            offset,
        ).map { entities ->
            Result.Success(entities.map { it.toDomain() }) as Result<List<Transaction>>
        }
    } else {
        database.transactionDao().observeAll()
            .map { entities ->
                val transactions = entities.asSequence().map { it.toDomain() }
                    .filter { it.matches(filter) }
                    .drop(offset)
                    .take(limit)
                    .toList()
                Result.Success(transactions) as Result<List<Transaction>>
            }
    }).catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }
        .flowOn(Dispatchers.Default)

    override fun observeTransaction(id: String): Flow<Result<Transaction?>> =
        database.transactionDao().observeById(id)
            .map { Result.Success(it?.toDomain()) as Result<Transaction?> }
            .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override fun observeTransactionGroup(id: String): Flow<Result<List<Transaction>>> =
        database.transactionDao().observeGroupForTransaction(id)
            .map { rows -> Result.Success(rows.map { it.toDomain() }) as Result<List<Transaction>> }
            .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override fun observeTransactionCount(filter: TransactionFilter): Flow<Result<Int>> = (if (filter.canUseDateQuery()) {
        database.transactionDao().observeDateCount(filter.from?.toEpochMilli(), filter.until?.toEpochMilli())
            .map { Result.Success(it) as Result<Int> }
    } else {
        database.transactionDao().observeAll()
            .map { entities ->
                Result.Success(entities.count { it.toDomain().matches(filter) }) as Result<Int>
            }
    }).catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }
        .flowOn(Dispatchers.Default)

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

    override suspend fun saveTransaction(draft: TransactionDraft): Result<Transaction> =
        when (val result = saveTransactionGroup(listOf(draft))) {
            is Result.Success -> Result.Success(result.value.first())
            is Result.Error -> result
        }

    override suspend fun saveTransactionGroup(
        drafts: List<TransactionDraft>,
        removedJournalIds: Set<String>,
    ): Result<List<Transaction>> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        if (drafts.isEmpty()) return Result.Error(EMPTY_SPLITS)
        return runCatching {
            val splits = drafts.map(TransactionDraft::toStoreSplit)
            val remoteGroupId = drafts.mapNotNull(TransactionDraft::remoteGroupId)
                .distinct()
                .singleOrNull()
            val response = if (remoteGroupId != null) {
                api.updateTransaction(
                    id = remoteGroupId,
                    request = UpdateTransactionRequest(
                        transactions = drafts.zip(splits) { draft, split -> split.toUpdate(draft.journalId) }
                    ),
                )
            } else {
                api.createTransaction(StoreTransactionRequest(transactions = splits))
            }
            removedJournalIds.forEach { journalId ->
                check(api.deleteTransactionJournal(journalId).isSuccessful)
            }
            val entities = response.data.toEntities()
                .filterNot { it.journalId in removedJournalIds }
            database.withTransaction {
                if (remoteGroupId != null) database.transactionDao().deleteByRemoteGroupId(remoteGroupId)
                database.transactionDao().upsertAll(entities)
                database.tagDao().upsertAll(
                    drafts.flatMap(TransactionDraft::tags).distinct().map { TagEntity("local:$it", it) }
                )
            }
            entities.map { it.toDomain() }
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.fireflyMessage(SAVE_ERROR), it) },
        )
    }

    private fun TransactionDraft.toStoreSplit() = StoreTransactionSplit(
        type = type.name.lowercase(),
        date = date.toString(),
        amount = amount.abs().toPlainString(),
        description = description.trim(),
        currencyCode = currency.takeIf(String::isNotBlank),
        sourceId = sourceAccountId,
        sourceName = sourceAccount.takeIf { sourceAccountId == null && it.isNotBlank() },
        destinationId = destinationAccountId,
        destinationName = destinationAccount.takeIf { destinationAccountId == null && it.isNotBlank() },
        categoryName = category.takeIf(String::isNotBlank),
        budgetName = budget.takeIf(String::isNotBlank),
        tags = tags,
        notes = notes?.takeIf(String::isNotBlank),
    )

    private fun StoreTransactionSplit.toUpdate(journalId: String?) = UpdateTransactionSplit(
        transactionJournalId = journalId,
        type = type,
        date = date,
        amount = amount,
        description = description,
        currencyCode = currencyCode,
        sourceId = sourceId,
        sourceName = sourceName,
        destinationId = destinationId,
        destinationName = destinationName,
        categoryName = categoryName,
        budgetName = budgetName,
        tags = tags,
        notes = notes,
    )

    override suspend fun deleteTransaction(remoteGroupId: String): Result<Unit> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            check(api.deleteTransaction(remoteGroupId).isSuccessful)
            database.transactionDao().deleteByRemoteGroupId(remoteGroupId)
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: DELETE_ERROR, it) }
    }

    override suspend fun sync(from: Instant?, until: Instant?): Result<Unit> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            val (transactions, categories) = coroutineScope {
                val transactionRequest = async { fetchTransactions(from, until) }
                val categoryRequest = async { fetchCategories() }
                transactionRequest.await() to categoryRequest.await()
            }

            database.withTransaction {
                database.transactionDao().upsertAll(transactions)
                database.categoryDao().upsertAll(categories)
            }
            settingsRepository.updateLastSyncTime(clock.instant())
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: SYNC_ERROR, it) }
    }

    private suspend fun fetchTransactions(from: Instant?, until: Instant?):
        List<com.teja.finflyiii.data.local.entity.TransactionEntity> {
        val transactions = mutableListOf<com.teja.finflyiii.data.local.entity.TransactionEntity>()
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
            totalPages = response.meta?.pagination?.totalPages
                ?: if (response.data.size >= PAGE_SIZE) page + 1 else page
            page++
        } while (page <= totalPages && page <= MAX_TRANSACTION_PAGES)
        return transactions
    }

    private suspend fun fetchCategories(): List<com.teja.finflyiii.data.local.entity.CategoryEntity> {
        val categories = mutableListOf<com.teja.finflyiii.data.local.entity.CategoryEntity>()
        var page = 1
        var totalPages: Int
        do {
            val response = api.getCategories(page, PAGE_SIZE)
            categories += response.data.map { it.toEntity() }
            totalPages = response.meta?.pagination?.totalPages ?: page
            page++
        } while (page <= totalPages && page <= MAX_PAGES)
        return categories
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

    private fun TransactionFilter.canUseDateQuery(): Boolean = query.isBlank() &&
        types.isEmpty() && categories.isEmpty() && tags.isEmpty() && accountIds.isEmpty()

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 100
        const val MAX_TRANSACTION_PAGES = 1_000
        const val CACHE_ERROR = "cache_error"
        const val SYNC_ERROR = "sync_error"
        const val SAVE_ERROR = "save_error"
        const val DELETE_ERROR = "transaction_delete_error"
        const val NOT_CONFIGURED = "not_configured"
        const val EMPTY_SPLITS = "transaction_splits_empty"
    }
}
