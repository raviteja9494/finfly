/* Dependency-injection module creating the Room cache and DAO graph. */
package com.teja.finfly.di

import android.content.Context
import androidx.room.Room
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
        Room.databaseBuilder(context, FinFlyDatabase::class.java, DATABASE_NAME).build()

    private const val DATABASE_NAME = "finfly.db"
}
