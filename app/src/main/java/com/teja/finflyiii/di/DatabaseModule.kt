/* Dependency-injection module creating the Room cache and DAO graph. */
package com.teja.finflyiii.di

import android.content.Context
import androidx.room.Room
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
            .fallbackToDestructiveMigration(true)
            .build()

    private const val DATABASE_NAME = "finfly_iii.db"
}
