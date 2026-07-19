/* Domain use case exposing the complete count for a transaction filter. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.TransactionFilter
import com.teja.finflyiii.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/** Returns the full cached result count independently from visible timeline pagination. */
class ObserveTransactionCountUseCase @Inject constructor(
    private val repository: TransactionRepository,
) {
    operator fun invoke(filter: TransactionFilter): Flow<Result<Int>> =
        repository.observeTransactionCount(filter)
}
