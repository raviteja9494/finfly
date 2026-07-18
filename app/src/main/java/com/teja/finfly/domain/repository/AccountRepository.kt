/* Domain repository contract for cached and synchronized accounts. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import kotlinx.coroutines.flow.Flow

/**
 * Provides cached Firefly accounts and refresh behavior.
 * It accepts no account-specific input and emits result-wrapped account collections.
 */
interface AccountRepository {
    fun observeAccounts(): Flow<Result<List<Account>>>
    suspend fun sync(): Result<Unit>
}
