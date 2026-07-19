/* Data-layer Room access contract for Firefly accounts. */
package com.teja.finflyiii.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teja.finflyiii.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/** Observes all cached accounts and upserts server snapshots. */
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<AccountEntity>>

    @Query("SELECT * FROM accounts WHERE id = :id LIMIT 1")
    fun observeById(id: String): Flow<AccountEntity?>

    @Upsert
    suspend fun upsertAll(accounts: List<AccountEntity>)

    @Query("DELETE FROM accounts WHERE id = :id")
    suspend fun delete(id: String)
}
