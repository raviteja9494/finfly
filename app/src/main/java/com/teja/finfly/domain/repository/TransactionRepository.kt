/* Domain repository contract for cached and synchronized transactions. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.Category
import com.teja.finfly.domain.model.DailySpend
import com.teja.finfly.domain.model.TransactionDraft
import com.teja.finfly.domain.model.TransactionFilter
import kotlinx.coroutines.flow.Flow
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
    fun observeRecent(limit: Int): Flow<Result<List<Transaction>>>
    fun observeSpending(from: Instant, until: Instant): Flow<Result<BigDecimal>>
    fun observeDailySpending(from: Instant, until: Instant): Flow<Result<List<DailySpend>>>
    fun observeCategories(): Flow<Result<List<Category>>>
    suspend fun saveTransaction(draft: TransactionDraft): Result<Transaction>
    suspend fun sync(from: Instant? = null, until: Instant? = null): Result<Unit>
}
