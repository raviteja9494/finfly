/* Data-layer Room access contract for Firefly accounts. */
package com.teja.finfly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teja.finfly.data.local.entity.AccountEntity
import kotlinx.coroutines.flow.Flow

/** Observes all cached accounts and upserts server snapshots. */
@Dao
interface AccountDao {
    @Query("SELECT * FROM accounts ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<AccountEntity>>

    @Upsert
    suspend fun upsertAll(accounts: List<AccountEntity>)
}
