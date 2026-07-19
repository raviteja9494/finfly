/* Domain gateway for persisted assistant preferences. */
package com.teja.finflyiii.domain.repository

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.AiConfig
import kotlinx.coroutines.flow.Flow

/** Persists model-independent assistant configuration and the last verified download state. */
interface AiSettingsRepository {
    val config: Flow<AiConfig>
    val modelDownloaded: Flow<Boolean>
    val huggingFaceToken: Flow<String>
    suspend fun saveConfig(config: AiConfig): Result<Unit>
    suspend fun setModelDownloaded(downloaded: Boolean): Result<Unit>
    suspend fun saveHuggingFaceToken(token: String): Result<Unit>
}
