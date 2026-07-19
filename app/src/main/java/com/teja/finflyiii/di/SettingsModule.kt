/* Dependency-injection module creating DataStore and binding the settings gateway. */
package com.teja.finflyiii.di

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStoreFile
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.teja.finflyiii.data.settings.SettingsRepositoryImpl
import com.teja.finflyiii.domain.repository.SettingsRepository
import com.teja.finflyiii.data.settings.AiSettingsRepositoryImpl
import com.teja.finflyiii.domain.repository.AiSettingsRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SettingsBindingsModule {
    @Binds
    @Singleton
    abstract fun bindSettingsRepository(implementation: SettingsRepositoryImpl): SettingsRepository

    @Binds
    @Singleton
    abstract fun bindAiSettingsRepository(implementation: AiSettingsRepositoryImpl): AiSettingsRepository
}

@Module
@InstallIn(SingletonComponent::class)
object SettingsDataStoreModule {
    @Provides
    @Singleton
    fun provideDataStore(@ApplicationContext context: Context): DataStore<Preferences> =
        PreferenceDataStoreFactory.create { context.preferencesDataStoreFile(FILE_NAME) }

    private const val FILE_NAME = "finfly_iii_settings.preferences_pb"
}
