/* Domain repository contract for cached and synchronized transactions. */
package com.teja.finflyiii.domain.repository

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.common.map
import com.teja.finflyiii.domain.model.Transaction
import com.teja.finflyiii.domain.model.Category
import com.teja.finflyiii.domain.model.DailySpend
import com.teja.finflyiii.domain.model.TransactionDraft
import com.teja.finflyiii.domain.model.TransactionFilter
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.math.BigDecimal
import java.time.Instant

/**
 * Provides offline-first transaction data.
 * Inputs are paging/date bounds; outputs are result-wrapped Flows or synchronization outcomes.
 */
interface TransactionRepository {
    fun observeTransactions(filter: TransactionFilter, limit: Int, offset: Int): Flow<Result<List<Transaction>>>
    fun observeTransactionCount(filter: TransactionFilter): Flow<Result<Int>>
    fun observeTransaction(id: String): Flow<Result<Transaction?>>
    fun observeTransactionGroup(id: String): Flow<Result<List<Transaction>>> = observeTransaction(id).map { result ->
        when (result) {
            is Result.Success -> Result.Success(listOfNotNull(result.value))
            is Result.Error -> result
        }
    }
    fun observeRecent(limit: Int): Flow<Result<List<Transaction>>>
    fun observeSpending(from: Instant, until: Instant): Flow<Result<BigDecimal>>
    fun observeDailySpending(from: Instant, until: Instant): Flow<Result<List<DailySpend>>>
    fun observeCategories(): Flow<Result<List<Category>>>
    suspend fun saveTransaction(draft: TransactionDraft): Result<Transaction>
    suspend fun saveTransactionGroup(
        drafts: List<TransactionDraft>,
        removedJournalIds: Set<String> = emptySet(),
    ): Result<List<Transaction>> = if (drafts.size == 1 && removedJournalIds.isEmpty()) {
        saveTransaction(drafts.single()).map { listOf(it) }
    } else {
        Result.Error("transaction_split_not_supported")
    }
    suspend fun deleteTransaction(remoteGroupId: String): Result<Unit>
    suspend fun sync(from: Instant? = null, until: Instant? = null): Result<Unit>
}
