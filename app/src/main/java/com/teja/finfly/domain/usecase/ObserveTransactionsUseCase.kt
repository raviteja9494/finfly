/* Domain use case exposing a validated page of cached transactions. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Enforces positive page sizes and non-negative offsets for the transaction timeline. */
class ObserveTransactionsUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(
        filter: TransactionFilter,
        limit: Int,
        offset: Int,
    ): Flow<Result<List<Transaction>>> = repository.observeTransactions(
        filter = filter,
        limit = limit.coerceIn(1, 200),
        offset = offset.coerceAtLeast(0),
    )
}
