/* Data-layer Room access contract for Firefly categories. */
package com.teja.finfly.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.teja.finfly.data.local.entity.CategoryEntity
import kotlinx.coroutines.flow.Flow

/** Upserts category metadata fetched during transaction synchronization. */
@Dao
interface CategoryDao {
    @Query("SELECT * FROM categories ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<CategoryEntity>>

    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)
}
