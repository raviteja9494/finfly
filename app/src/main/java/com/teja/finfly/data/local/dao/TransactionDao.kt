/* Data-layer Room access contract for the offline transaction timeline. */
package com.teja.finfly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teja.finfly.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.Flow

/** Reads ordered pages and date-bounded withdrawals, and atomically upserts remote rows. */
@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC")
    fun observeAll(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<TransactionEntity?>

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC LIMIT :limit OFFSET :offset")
    fun observePage(limit: Int, offset: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions ORDER BY dateEpochMillis DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE type = 'WITHDRAWAL' AND dateEpochMillis >= :from AND dateEpochMillis < :until")
    fun observeWithdrawals(from: Long, until: Long): Flow<List<TransactionEntity>>

    @Upsert
    suspend fun upsertAll(transactions: List<TransactionEntity>)

    @Upsert
    suspend fun upsert(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE remoteGroupId = :remoteGroupId")
    suspend fun deleteByRemoteGroupId(remoteGroupId: String)

    @Query("DELETE FROM transactions WHERE sourceAccountId = :accountId OR destinationAccountId = :accountId")
    suspend fun deleteByAccountId(accountId: String)
}
