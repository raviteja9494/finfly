/* Data-layer Room database assembling FinFly's offline cache. */
package com.teja.finfly.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.teja.finfly.data.local.dao.AccountDao
import com.teja.finfly.data.local.dao.CategoryDao
import com.teja.finfly.data.local.dao.TransactionDao
import com.teja.finfly.data.local.dao.TagDao
import com.teja.finfly.data.local.entity.AccountEntity
import com.teja.finfly.data.local.entity.CategoryEntity
import com.teja.finfly.data.local.entity.TransactionEntity
import com.teja.finfly.data.local.entity.TagEntity

@Database(
    entities = [TransactionEntity::class, AccountEntity::class, CategoryEntity::class, TagEntity::class],
    version = 2,
    exportSchema = true,
)
abstract class FinFlyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
}
