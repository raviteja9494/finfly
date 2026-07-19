/* Presentation state for model setup and the memory-only assistant conversation. */
package com.teja.finfly.presentation.assistant

import com.teja.finfly.domain.model.AiConfig
import com.teja.finfly.domain.model.AiModelState
import com.teja.finfly.domain.model.ChatMessage
import com.teja.finfly.domain.model.FinanceContext

data class AssistantUiState(
    val modelState: AiModelState = AiModelState.NotDownloaded(0),
    val config: AiConfig = AiConfig(),
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val context: FinanceContext? = null,
    val contextWasTruncated: Boolean = false,
    val error: AssistantError? = null,
)

enum class AssistantError { DOWNLOAD, STORAGE, MODEL_LOAD, TIMEOUT, OUT_OF_MEMORY, GENERATION }
