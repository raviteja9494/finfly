/* Data-layer Room implementation for capped SMS processing logs. */
package com.teja.finfly.data.repository

import androidx.room.withTransaction
import com.teja.finfly.data.local.FinFlyDatabase
import com.teja.finfly.data.local.entity.SmsLogEntity
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.SmsLog
import com.teja.finfly.domain.model.SmsLogResult
import com.teja.finfly.domain.repository.SmsLogRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmsLogRepositoryImpl @Inject constructor(
    private val database: FinFlyDatabase,
) : SmsLogRepository {
    override fun observeLogs(): Flow<Result<List<SmsLog>>> = database.smsLogDao().observeAll()
        .map { Result.Success(it.map(SmsLogEntity::toDomain)) as Result<List<SmsLog>> }
        .catch { emit(Result.Error(it.message ?: READ_ERROR, it)) }

    override fun observeLog(id: String): Flow<Result<SmsLog?>> = database.smsLogDao().observeById(id)
        .map { Result.Success(it?.toDomain()) as Result<SmsLog?> }
        .catch { emit(Result.Error(it.message ?: READ_ERROR, it)) }

    override suspend fun add(log: SmsLog): Result<Unit> = runCatching {
        database.withTransaction {
            database.smsLogDao().upsert(log.toEntity())
            database.smsLogDao().trimToLatestHundred()
        }
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: WRITE_ERROR, it) }

    private fun SmsLog.toEntity() = SmsLogEntity(
        id, sender, message.take(MAX_MESSAGE_LENGTH), timestamp, result.name, reason, matchedRule, processedAt,
    )
    private fun SmsLogEntity.toDomain() = SmsLog(
        id, sender, message, timestamp, SmsLogResult.valueOf(result), reason, matchedRule, processedAt,
    )

    private companion object {
        const val MAX_MESSAGE_LENGTH = 200
        const val READ_ERROR = "sms_log_read_error"
        const val WRITE_ERROR = "sms_log_write_error"
    }
}
