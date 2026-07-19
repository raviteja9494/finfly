/* Domain gateway for persisted assistant preferences. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.AiConfig
import kotlinx.coroutines.flow.Flow

/** Persists model-independent assistant configuration and the last verified download state. */
interface AiSettingsRepository {
    val config: Flow<AiConfig>
    val modelDownloaded: Flow<Boolean>
    suspend fun saveConfig(config: AiConfig): Result<Unit>
    suspend fun setModelDownloaded(downloaded: Boolean): Result<Unit>
}
