/* Presentation state for model setup and the memory-only assistant conversation. */
package com.teja.finflyiii.presentation.assistant

import com.teja.finflyiii.domain.model.AiConfig
import com.teja.finflyiii.domain.model.AiModelState
import com.teja.finflyiii.domain.model.ChatMessage
import com.teja.finflyiii.domain.model.FinanceContext
import com.teja.finflyiii.domain.model.AssistantSuggestion

data class AssistantUiState(
    val modelState: AiModelState = AiModelState.NotDownloaded(0),
    val config: AiConfig = AiConfig(),
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val isGenerating: Boolean = false,
    val context: FinanceContext? = null,
    val contextWasTruncated: Boolean = false,
    val error: AssistantError? = null,
    val suggestions: List<AssistantSuggestion> = listOf(
        AssistantSuggestion.MONTH_SUMMARY,
        AssistantSuggestion.BALANCE,
    ),
)

enum class AssistantError {
    DOWNLOAD,
    MODEL_ACCESS,
    STORAGE,
    WARMING_UP,
    MODEL_LOAD,
    TIMEOUT,
    OUT_OF_MEMORY,
    NO_TRANSACTIONS,
    CONTEXT_TOO_LARGE,
    GENERATION,
}
