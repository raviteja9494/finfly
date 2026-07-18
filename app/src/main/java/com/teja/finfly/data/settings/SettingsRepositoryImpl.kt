/* Data-layer DataStore implementation of local Firefly settings persistence. */
package com.teja.finfly.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.AppSettings
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.di.ApplicationScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    @ApplicationScope scope: CoroutineScope,
) : SettingsRepository {
    override val settings: StateFlow<AppSettings> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            AppSettings(
                serverUrl = preferences[SERVER_URL].orEmpty(),
                bearerToken = preferences[BEARER_TOKEN].orEmpty(),
                lastSyncTime = preferences[LAST_SYNC]?.let(Instant::ofEpochMilli),
            )
        }
        .stateIn(scope, SharingStarted.Eagerly, AppSettings())

    override suspend fun save(serverUrl: String, bearerToken: String): Result<Unit> = runCatching {
        dataStore.edit { preferences ->
            preferences[SERVER_URL] = serverUrl
            preferences[BEARER_TOKEN] = bearerToken
        }
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: it.javaClass.simpleName, it) }

    override suspend fun updateLastSyncTime(instant: Instant): Result<Unit> = runCatching {
        dataStore.edit { it[LAST_SYNC] = instant.toEpochMilli() }
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: it.javaClass.simpleName, it) }

    private companion object {
        val SERVER_URL = stringPreferencesKey("server_url")
        val BEARER_TOKEN = stringPreferencesKey("bearer_token")
        val LAST_SYNC = longPreferencesKey("last_sync_time")
    }
}
