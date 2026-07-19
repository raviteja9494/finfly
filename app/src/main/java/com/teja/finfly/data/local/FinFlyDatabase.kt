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
import com.teja.finfly.data.local.dao.SmsRulesDao
import com.teja.finfly.data.local.dao.SmsLogDao
import com.teja.finfly.data.local.entity.BankRuleEntity
import com.teja.finfly.data.local.entity.CategoryRuleEntity
import com.teja.finfly.data.local.entity.SmsLogEntity

@Database(
    entities = [
        TransactionEntity::class, AccountEntity::class, CategoryEntity::class, TagEntity::class,
        BankRuleEntity::class, CategoryRuleEntity::class, SmsLogEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class FinFlyDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun smsRulesDao(): SmsRulesDao
    abstract fun smsLogDao(): SmsLogDao
}
