/* Provider-neutral domain boundary for finance question answering. */
package com.teja.finflyiii.domain.assistant

import com.teja.finflyiii.domain.model.AiConfig
import com.teja.finflyiii.domain.model.AiModelInfo
import com.teja.finflyiii.domain.model.AssistantResponseChunk
import kotlinx.coroutines.flow.Flow

/** Streams local assistant output without exposing a concrete inference runtime to domain callers. */
interface FinanceAssistant {
    suspend fun prepare()
    fun streamResponse(prompt: String, config: AiConfig): Flow<AssistantResponseChunk>
    suspend fun reset()
    fun cancel()
    fun modelInfo(): AiModelInfo?
}
