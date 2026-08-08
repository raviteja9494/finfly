/* Domain use case coordinating account and recent transaction synchronization. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.SyncState
import com.teja.finflyiii.domain.repository.AccountRepository
import com.teja.finflyiii.domain.repository.SettingsRepository
import com.teja.finflyiii.domain.repository.TransactionRepository
import com.teja.finflyiii.domain.repository.TagRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import java.time.Clock
import java.time.Duration
import javax.inject.Inject
import javax.inject.Singleton

/** Runs an initial full sync, then overlapping incremental syncs, while explicit report ranges stay scoped. */
@Singleton
class SyncFinancesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
    private val tagRepository: TagRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) {
    private val mutableState = MutableStateFlow<SyncState>(SyncState.Idle)
    private val syncMutex = Mutex()
    val state: StateFlow<SyncState> = mutableState.asStateFlow()

    suspend operator fun invoke(from: java.time.Instant? = null, until: java.time.Instant? = null): Result<Unit> {
        if (!syncMutex.tryLock()) return Result.Success(Unit)
        mutableState.value = SyncState.Syncing
        return try {
            val result = if (from != null || until != null) {
                transactionRepository.sync(from = from, until = until)
            } else {
                val incrementalFrom = settingsRepository.settings.value.lastSyncTime
                    ?.minus(INCREMENTAL_OVERLAP)
                coroutineScope {
                    val accounts = async { accountRepository.sync() }
                    val transactions = async { transactionRepository.sync(from = incrementalFrom) }
                    val tags = async { tagRepository.refresh() }
                    listOf(accounts.await(), transactions.await(), tags.await())
                        .firstOrNull { it is Result.Error }
                        ?: Result.Success(Unit)
                }
            }
            mutableState.value = when (result) {
                is Result.Success -> SyncState.Success(clock.instant())
                is Result.Error -> SyncState.Error(result.message)
            }
            result
        } catch (cancelled: CancellationException) {
            mutableState.value = SyncState.Idle
            throw cancelled
        } catch (failure: Throwable) {
            Result.Error(UNEXPECTED_SYNC_ERROR, failure).also {
                mutableState.value = SyncState.Error(it.message)
            }
        } finally {
            syncMutex.unlock()
        }
    }

    private companion object {
        val INCREMENTAL_OVERLAP: Duration = Duration.ofDays(2)
        const val UNEXPECTED_SYNC_ERROR = "sync_error"
    }
}
