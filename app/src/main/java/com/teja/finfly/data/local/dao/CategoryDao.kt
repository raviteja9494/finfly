/* Data-layer Room access contract for Firefly categories. */
package com.teja.finfly.data.local.dao

import androidx.room.Dao
import androidx.room.Upsert
import com.teja.finfly.data.local.entity.CategoryEntity

/** Upserts category metadata fetched during transaction synchronization. */
@Dao
interface CategoryDao {
    @Upsert
    suspend fun upsertAll(categories: List<CategoryEntity>)
}
