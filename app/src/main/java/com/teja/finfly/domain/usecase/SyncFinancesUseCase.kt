/* Domain use case coordinating account and transaction synchronization. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.TransactionRepository
import javax.inject.Inject

/** Enforces account refresh before transaction refresh so cached references remain coherent. */
class SyncFinancesUseCase @Inject constructor(
    private val accountRepository: AccountRepository,
    private val transactionRepository: TransactionRepository,
) {
    suspend operator fun invoke(): Result<Unit> {
        val accounts = accountRepository.sync()
        if (accounts is Result.Error) return accounts
        return transactionRepository.sync()
    }
}
