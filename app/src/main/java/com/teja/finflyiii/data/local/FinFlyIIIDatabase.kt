/* Data-layer Room database assembling FinFly III's offline cache. */
package com.teja.finflyiii.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.teja.finflyiii.data.local.dao.AccountDao
import com.teja.finflyiii.data.local.dao.CategoryDao
import com.teja.finflyiii.data.local.dao.TransactionDao
import com.teja.finflyiii.data.local.dao.TagDao
import com.teja.finflyiii.data.local.entity.AccountEntity
import com.teja.finflyiii.data.local.entity.CategoryEntity
import com.teja.finflyiii.data.local.entity.TransactionEntity
import com.teja.finflyiii.data.local.entity.TagEntity
import com.teja.finflyiii.data.local.dao.SmsRulesDao
import com.teja.finflyiii.data.local.dao.SmsLogDao
import com.teja.finflyiii.data.local.entity.BankRuleEntity
import com.teja.finflyiii.data.local.entity.CategoryRuleEntity
import com.teja.finflyiii.data.local.entity.SmsLogEntity

@Database(
    entities = [
        TransactionEntity::class, AccountEntity::class, CategoryEntity::class, TagEntity::class,
        BankRuleEntity::class, CategoryRuleEntity::class, SmsLogEntity::class,
    ],
    version = 4,
    exportSchema = true,
)
abstract class FinFlyIIIDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun accountDao(): AccountDao
    abstract fun categoryDao(): CategoryDao
    abstract fun tagDao(): TagDao
    abstract fun smsRulesDao(): SmsRulesDao
    abstract fun smsLogDao(): SmsLogDao
}
