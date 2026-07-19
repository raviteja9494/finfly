/* Domain gateway for the optional local language-model file. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.AiModelState
import kotlinx.coroutines.flow.Flow

/** Owns model discovery, cancellable download, deletion, and file readiness. */
interface AiRepository {
    fun getModelState(): Flow<AiModelState>
    fun downloadModel(): Flow<AiModelState>
    suspend fun deleteModel(): Result<Unit>
    fun isModelReady(): Boolean
    fun modelPath(): String
}
