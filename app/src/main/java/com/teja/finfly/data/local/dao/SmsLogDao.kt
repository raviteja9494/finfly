/* Data-layer Room access for capped SMS diagnostics. */
package com.teja.finfly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teja.finfly.data.local.entity.SmsLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SmsLogDao {
    @Query("SELECT * FROM sms_logs ORDER BY processedAt DESC LIMIT 100")
    fun observeAll(): Flow<List<SmsLogEntity>>

    @Query("SELECT * FROM sms_logs WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<SmsLogEntity?>

    @Upsert
    suspend fun upsert(log: SmsLogEntity)

    @Query("DELETE FROM sms_logs WHERE id NOT IN (SELECT id FROM sms_logs ORDER BY processedAt DESC LIMIT 100)")
    suspend fun trimToLatestHundred()
}
