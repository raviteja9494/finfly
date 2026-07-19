/* Dependency-injection module creating the Room cache and DAO graph. */
package com.teja.finflyiii.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teja.finflyiii.data.local.FinFlyIIIDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): FinFlyIIIDatabase =
        Room.databaseBuilder(context, FinFlyIIIDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .build()

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN remoteGroupId TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE transactions ADD COLUMN journalId TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE transactions ADD COLUMN sourceAccountId TEXT")
            database.execSQL("ALTER TABLE transactions ADD COLUMN sourceAccount TEXT NOT NULL DEFAULT ''")
            database.execSQL("ALTER TABLE transactions ADD COLUMN destinationAccountId TEXT")
            database.execSQL("ALTER TABLE transactions ADD COLUMN destinationAccount TEXT NOT NULL DEFAULT ''")
            database.execSQL("CREATE TABLE IF NOT EXISTS tags (id TEXT NOT NULL, name TEXT NOT NULL, PRIMARY KEY(id))")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS bank_rules (id TEXT NOT NULL, name TEXT NOT NULL, enabled INTEGER NOT NULL, configJson TEXT NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS category_rules (id TEXT NOT NULL, name TEXT NOT NULL, enabled INTEGER NOT NULL, priority INTEGER NOT NULL, configJson TEXT NOT NULL, PRIMARY KEY(id))")
            database.execSQL("CREATE TABLE IF NOT EXISTS sms_logs (id TEXT NOT NULL, sender TEXT NOT NULL, message TEXT NOT NULL, timestamp INTEGER NOT NULL, result TEXT NOT NULL, reason TEXT NOT NULL, matchedRule TEXT NOT NULL, processedAt INTEGER NOT NULL, PRIMARY KEY(id))")
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE transactions ADD COLUMN budget TEXT NOT NULL DEFAULT ''")
        }
    }

    private const val DATABASE_NAME = "finfly_iii.db"
}
