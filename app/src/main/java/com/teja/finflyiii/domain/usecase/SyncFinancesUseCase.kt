/* Domain use case coordinating account and recent transaction synchronization. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.SyncState
import com.teja.finflyiii.domain.repository.AccountRepository
import com.teja.finflyiii.domain.repository.TransactionRepository
import com.teja.finflyiii.domain.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

/** Synchronizes accounts and the full transaction history unless an explicit report range is supplied. */
@Singleton
class SyncFinancesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
    private val clock: Clock,
) {
    private val mutableState = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = mutableState.asStateFlow()

    suspend operator fun invoke(from: java.time.Instant? = null, until: java.time.Instant? = null): Result<Unit> {
        if (mutableState.value is SyncState.Syncing) return Result.Success(Unit)
        mutableState.value = SyncState.Syncing
        val result = when (val accounts = accountRepository.sync()) {
            is Result.Error -> accounts
            is Result.Success -> when (val transactions = transactionRepository.sync(
                from = from,
                until = until,
            )) {
                is Result.Error -> transactions
                is Result.Success -> tagRepository.refresh()
            }
        }
        mutableState.value = when (result) {
            is Result.Success -> SyncState.Success(clock.instant())
            is Result.Error -> SyncState.Error(result.message)
        }
        return result
    }
}
