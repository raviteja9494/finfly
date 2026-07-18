/* Data-layer Room access contract for Firefly transaction tags. */
package com.teja.finfly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teja.finfly.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

/** Observes tag choices and upserts snapshots returned by Firefly. */
@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<TagEntity>>

    @Upsert
    suspend fun upsertAll(tags: List<TagEntity>)

    @Query("DELETE FROM tags")
    suspend fun clear()
}
