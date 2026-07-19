/* Domain repository contract for cached and synchronized accounts. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.AccountDraft
import kotlinx.coroutines.flow.Flow

/**
 * Provides cached Firefly accounts and refresh behavior.
 * It accepts no account-specific input and emits result-wrapped account collections.
 */
interface AccountRepository {
    fun observeAccounts(): Flow<Result<List<Account>>>
    fun observeAccount(id: String): Flow<Result<Account?>>
    suspend fun saveAccount(draft: AccountDraft): Result<Account>
    suspend fun deleteAccount(id: String): Result<Unit>
    suspend fun sync(): Result<Unit>
}
