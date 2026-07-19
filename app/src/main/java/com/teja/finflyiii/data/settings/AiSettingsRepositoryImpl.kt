/* DataStore implementation of assistant configuration persistence. */
package com.teja.finflyiii.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.AiConfig
import com.teja.finflyiii.domain.repository.AiSettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AiSettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : AiSettingsRepository {
    override val config: Flow<AiConfig> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { preferences ->
            AiConfig(
                maxTransactions = (preferences[MAX_TRANSACTIONS] ?: 50).coerceIn(10, 100),
                dateRangeDays = (preferences[DATE_RANGE_DAYS] ?: 30).takeIf { it in SUPPORTED_DAYS } ?: 30,
                includeBalances = preferences[INCLUDE_BALANCES] ?: true,
                includeCategories = preferences[INCLUDE_CATEGORIES] ?: true,
                includeSmsRules = preferences[INCLUDE_SMS_RULES] ?: false,
                temperature = (preferences[TEMPERATURE] ?: 0.7f).coerceIn(0.1f, 1f),
                maxResponseTokens = (preferences[MAX_RESPONSE_TOKENS] ?: 512)
                    .takeIf { it in SUPPORTED_RESPONSE_TOKENS } ?: 512,
            )
        }

    override val modelDownloaded: Flow<Boolean> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { it[MODEL_DOWNLOADED] ?: false }

    override val huggingFaceToken: Flow<String> = dataStore.data
        .catch { emit(androidx.datastore.preferences.core.emptyPreferences()) }
        .map { it[HUGGING_FACE_TOKEN].orEmpty() }

    override suspend fun saveConfig(config: AiConfig): Result<Unit> = runCatching {
        dataStore.edit { preferences ->
            preferences[MAX_TRANSACTIONS] = config.maxTransactions.coerceIn(10, 100)
            preferences[DATE_RANGE_DAYS] = config.dateRangeDays
            preferences[INCLUDE_BALANCES] = config.includeBalances
            preferences[INCLUDE_CATEGORIES] = config.includeCategories
            preferences[INCLUDE_SMS_RULES] = config.includeSmsRules
            preferences[TEMPERATURE] = config.temperature.coerceIn(0.1f, 1f)
            preferences[MAX_RESPONSE_TOKENS] = config.maxResponseTokens
        }
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: SETTINGS_ERROR, it) }

    override suspend fun setModelDownloaded(downloaded: Boolean): Result<Unit> = runCatching {
        dataStore.edit { it[MODEL_DOWNLOADED] = downloaded }
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: SETTINGS_ERROR, it) }

    override suspend fun saveHuggingFaceToken(token: String): Result<Unit> = runCatching {
        dataStore.edit { preferences ->
            if (token.isBlank()) preferences.remove(HUGGING_FACE_TOKEN)
            else preferences[HUGGING_FACE_TOKEN] = token.trim()
        }
        Result.Success(Unit)
    }.getOrElse { Result.Error(it.message ?: SETTINGS_ERROR, it) }

    private companion object {
        val MAX_TRANSACTIONS = intPreferencesKey("ai_max_transactions")
        val DATE_RANGE_DAYS = intPreferencesKey("ai_date_range_days")
        val INCLUDE_BALANCES = booleanPreferencesKey("ai_include_balances")
        val INCLUDE_CATEGORIES = booleanPreferencesKey("ai_include_categories")
        val INCLUDE_SMS_RULES = booleanPreferencesKey("ai_include_sms_rules")
        val TEMPERATURE = floatPreferencesKey("ai_temperature")
        val MAX_RESPONSE_TOKENS = intPreferencesKey("ai_max_response_tokens")
        val MODEL_DOWNLOADED = booleanPreferencesKey("ai_model_downloaded")
        val HUGGING_FACE_TOKEN = stringPreferencesKey("ai_hugging_face_token")
        val SUPPORTED_DAYS = setOf(7, 30, 90)
        val SUPPORTED_RESPONSE_TOKENS = setOf(256, 512, 1024)
        const val SETTINGS_ERROR = "ai_settings_error"
    }
}
