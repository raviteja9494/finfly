/* Dependency-injection module creating the Room cache and DAO graph. */
package com.teja.finfly.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.teja.finfly.data.local.FinFlyDatabase
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
    fun provideDatabase(@ApplicationContext context: Context): FinFlyDatabase =
        Room.databaseBuilder(context, FinFlyDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_1_2)
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

    private const val DATABASE_NAME = "finfly.db"
}
