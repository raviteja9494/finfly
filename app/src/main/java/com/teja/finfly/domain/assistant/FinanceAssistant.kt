/* Provider-neutral domain boundary for finance question answering. */
package com.teja.finfly.domain.assistant

import com.teja.finfly.domain.model.AiConfig
import com.teja.finfly.domain.model.AiModelInfo
import com.teja.finfly.domain.model.AssistantResponseChunk
import kotlinx.coroutines.flow.Flow

/** Streams local assistant output without exposing a concrete inference runtime to domain callers. */
interface FinanceAssistant {
    suspend fun prepare()
    fun streamResponse(prompt: String, config: AiConfig): Flow<AssistantResponseChunk>
    suspend fun reset()
    fun cancel()
    fun modelInfo(): AiModelInfo?
}
