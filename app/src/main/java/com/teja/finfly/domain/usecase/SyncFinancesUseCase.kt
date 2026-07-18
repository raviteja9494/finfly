/* Domain use case coordinating account and recent transaction synchronization. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.SyncState
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.time.Clock
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** Synchronizes accounts then the latest 90 days and exposes one app-wide operation state. */
@Singleton
class SyncFinancesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val clock: Clock,
) {
    private val mutableState = MutableStateFlow<SyncState>(SyncState.Idle)
    val state: StateFlow<SyncState> = mutableState.asStateFlow()

    suspend operator fun invoke(): Result<Unit> {
        if (mutableState.value is SyncState.Syncing) return Result.Success(Unit)
        mutableState.value = SyncState.Syncing
        val now = clock.instant()
        val result = when (val accounts = accountRepository.sync()) {
            is Result.Error -> accounts
            is Result.Success -> transactionRepository.sync(
                from = now.minus(SYNC_DAYS, ChronoUnit.DAYS),
                until = now,
            )
        }
        mutableState.value = when (result) {
            is Result.Success -> SyncState.Success(clock.instant())
            is Result.Error -> SyncState.Error(result.message)
        }
        return result
    }

    private companion object { const val SYNC_DAYS = 90L }
}
