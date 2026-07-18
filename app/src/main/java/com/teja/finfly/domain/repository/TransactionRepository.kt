/* Domain repository contract for cached and synchronized transactions. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Transaction
import kotlinx.coroutines.flow.Flow
import java.math.BigDecimal
import java.time.Instant

/**
 * Provides offline-first transaction data.
 * Inputs are paging/date bounds; outputs are result-wrapped Flows or synchronization outcomes.
 */
interface TransactionRepository {
    fun observeTransactions(limit: Int, offset: Int): Flow<Result<List<Transaction>>>
    fun observeRecent(limit: Int): Flow<Result<List<Transaction>>>
    fun observeSpending(from: Instant, until: Instant): Flow<Result<BigDecimal>>
    suspend fun sync(from: Instant? = null, until: Instant? = null): Result<Unit>
}
