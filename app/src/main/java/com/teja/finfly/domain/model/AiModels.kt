/* Pure domain models for the optional on-device finance assistant. */
package com.teja.finfly.domain.model

import java.time.Instant
import java.util.UUID

/** One in-memory conversation entry, including optional local inference metrics. */
data class ChatMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: ChatRole,
    val content: String,
    val timestamp: Instant = Instant.now(),
    val contextTokens: Int? = null,
    val responseTokens: Int? = null,
    val totalTokens: Int? = null,
    val processingTimeMs: Long? = null,
)

enum class ChatRole { USER, ASSISTANT, SYSTEM }

/** Download and initialization lifecycle for the local model file. */
sealed interface AiModelState {
    data class NotDownloaded(val availableMb: Long) : AiModelState
    data class Downloading(
        val progressPercent: Int,
        val downloadedMb: Long,
        val totalMb: Long,
    ) : AiModelState
    data class Downloaded(val modelSizeGb: Double, val filePath: String) : AiModelState
    data object Loading : AiModelState
    data object Ready : AiModelState
    data class Error(val message: String) : AiModelState
}

/** User-controlled limits for finance context and local response generation. */
data class AiConfig(
    val maxTransactions: Int = 50,
    val dateRangeDays: Int = 30,
    val includeBalances: Boolean = true,
    val includeCategories: Boolean = true,
    val includeSmsRules: Boolean = false,
    val temperature: Float = 0.7f,
    val maxResponseTokens: Int = 512,
)

/** A bounded, displayable snapshot of the cached finance context sent to the model. */
data class FinanceContext(
    val prompt: String,
    val estimatedTokens: Int,
    val transactionCount: Int,
    val dateRangeDays: Int,
    val wasTruncated: Boolean,
)

/** Static information about the active local model provider. */
data class AiModelInfo(
    val name: String,
    val provider: String,
    val filePath: String,
    val sizeGb: Double,
)

/** A streamed response fragment and its completion marker. */
data class AssistantResponseChunk(val text: String, val isComplete: Boolean)
