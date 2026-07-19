/* Domain repository contract for SMS processing history. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.SmsLog
import kotlinx.coroutines.flow.Flow

/** Stores and observes the latest 100 SMS parsing outcomes for diagnostics and rule creation. */
interface SmsLogRepository {
    fun observeLogs(): Flow<Result<List<SmsLog>>>
    fun observeLog(id: String): Flow<Result<SmsLog?>>
    suspend fun add(log: SmsLog): Result<Unit>
}
