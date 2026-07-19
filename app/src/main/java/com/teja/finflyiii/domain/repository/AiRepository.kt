/* Domain gateway for the optional local language-model file. */
package com.teja.finflyiii.domain.repository

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.AiModelState
import kotlinx.coroutines.flow.Flow

/** Owns model discovery, cancellable download, local backup/import, deletion, and file readiness. */
interface AiRepository {
    fun getModelState(): Flow<AiModelState>
    fun downloadModel(): Flow<AiModelState>
    suspend fun importModel(sourceUri: String): Result<Unit>
    suspend fun exportModel(destinationUri: String): Result<Unit>
    suspend fun deleteModel(): Result<Unit>
    fun isModelReady(): Boolean
    fun modelPath(): String
}
