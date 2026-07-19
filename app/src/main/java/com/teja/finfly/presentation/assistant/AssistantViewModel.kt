/* Presentation orchestration for private, on-device finance chat. */
package com.teja.finfly.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.assistant.FinanceAssistant
import com.teja.finfly.domain.model.AiModelState
import com.teja.finfly.domain.model.ChatMessage
import com.teja.finfly.domain.model.ChatRole
import com.teja.finfly.domain.repository.AiRepository
import com.teja.finfly.domain.repository.AiSettingsRepository
import com.teja.finfly.domain.usecase.FinanceContextBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Clock
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout

/**
 * Owns model setup, bounded in-memory history, finance-context assembly, and streamed response metrics.
 * Conversation content is never persisted and never leaves the device.
 */
@HiltViewModel
class AssistantViewModel @Inject constructor(
    private val aiRepository: AiRepository,
    private val aiSettingsRepository: AiSettingsRepository,
    private val financeContextBuilder: FinanceContextBuilder,
    private val financeAssistant: FinanceAssistant,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(AssistantUiState())
    val uiState: StateFlow<AssistantUiState> = mutableState.asStateFlow()
    private var downloadJob: Job? = null
    private var generationJob: Job? = null

    init {
        viewModelScope.launch {
            aiRepository.getModelState().collect { modelState ->
                mutableState.update { it.copy(modelState = modelState, error = modelState.toAssistantError()) }
            }
        }
        viewModelScope.launch {
            aiSettingsRepository.config.collect { config -> mutableState.update { it.copy(config = config) } }
        }
    }

    fun setInput(value: String) {
        mutableState.update { it.copy(input = value, error = null) }
    }

    fun downloadModel() {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            aiRepository.downloadModel().collect { modelState ->
                mutableState.update { it.copy(modelState = modelState, error = modelState.toAssistantError()) }
            }
        }
    }

    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
    }

    fun sendMessage(message: String = mutableState.value.input) {
        val question = message.trim()
        val snapshot = mutableState.value
        if (question.isEmpty() || snapshot.isGenerating || !aiRepository.isModelReady()) return
        generationJob = viewModelScope.launch {
            val previousMessages = snapshot.messages.takeLast(MAX_HISTORY_MESSAGES)
            val userMessage = ChatMessage(role = ChatRole.USER, content = question, timestamp = clock.instant())
            val assistantMessage = ChatMessage(role = ChatRole.ASSISTANT, content = "", timestamp = clock.instant())
            mutableState.update {
                it.copy(
                    messages = (previousMessages + userMessage + assistantMessage).takeLast(MAX_HISTORY_MESSAGES),
                    input = "",
                    isGenerating = true,
                    modelState = AiModelState.Loading,
                    error = null,
                )
            }
            val startedAt = clock.millis()
            try {
                val context = financeContextBuilder.buildContext(snapshot.config)
                val promptData = buildPrompt(context.prompt, previousMessages, question)
                mutableState.update {
                    it.copy(
                        context = context,
                        contextWasTruncated = context.wasTruncated || promptData.historyWasTruncated,
                    )
                }
                var receivedAnyToken = false
                withTimeout(RESPONSE_TIMEOUT_MILLIS) {
                    financeAssistant.streamResponse(promptData.prompt, snapshot.config)
                        .catch { throw it }
                        .collect { chunk ->
                            if (!receivedAnyToken) {
                                receivedAnyToken = true
                                mutableState.update { it.copy(modelState = AiModelState.Ready) }
                            }
                            if (chunk.text.isNotEmpty()) appendAssistantText(chunk.text)
                        }
                }
                finishAssistantMessage(context.estimatedTokens, startedAt)
            } catch (_: TimeoutCancellationException) {
                financeAssistant.cancel()
                finishWithError(AssistantError.TIMEOUT, startedAt)
            } catch (_: OutOfMemoryError) {
                financeAssistant.cancel()
                finishWithError(AssistantError.OUT_OF_MEMORY, startedAt)
            } catch (_: CancellationException) {
                finishAssistantMessage(mutableState.value.context?.estimatedTokens ?: 0, startedAt)
            } catch (_: Throwable) {
                finishWithError(AssistantError.MODEL_LOAD, startedAt)
            }
        }
    }

    fun stopGenerating() {
        financeAssistant.cancel()
        generationJob?.cancel()
        generationJob = null
        val contextTokens = mutableState.value.context?.estimatedTokens ?: 0
        finishAssistantMessage(contextTokens, clock.millis())
    }

    fun clearConversation() {
        generationJob?.cancel()
        financeAssistant.cancel()
        viewModelScope.launch { financeAssistant.reset() }
        mutableState.update {
            it.copy(
                messages = emptyList(),
                context = null,
                contextWasTruncated = false,
                error = null,
                modelState = if (aiRepository.isModelReady()) AiModelState.Ready else it.modelState,
            )
        }
    }

    fun retry() {
        if (aiRepository.isModelReady()) {
            mutableState.update { it.copy(error = null, modelState = AiModelState.Ready) }
        } else downloadModel()
    }

    fun modelInfo() = financeAssistant.modelInfo()

    private fun appendAssistantText(fragment: String) {
        mutableState.update { state ->
            val messages = state.messages.toMutableList()
            val index = messages.indexOfLast { it.role == ChatRole.ASSISTANT }
            if (index >= 0) messages[index] = messages[index].copy(content = messages[index].content + fragment)
            state.copy(messages = messages)
        }
    }

    private fun finishAssistantMessage(contextTokens: Int, startedAt: Long) {
        mutableState.update { state ->
            val messages = state.messages.toMutableList()
            val index = messages.indexOfLast { it.role == ChatRole.ASSISTANT }
            if (index >= 0) {
                val message = messages[index]
                val responseTokens = FinanceContextBuilder.estimateTokens(message.content)
                messages[index] = message.copy(
                    contextTokens = contextTokens,
                    responseTokens = responseTokens,
                    totalTokens = contextTokens + responseTokens,
                    processingTimeMs = (clock.millis() - startedAt).coerceAtLeast(0),
                )
            }
            state.copy(
                messages = messages.takeLast(MAX_HISTORY_MESSAGES),
                isGenerating = false,
                modelState = if (aiRepository.isModelReady()) AiModelState.Ready else state.modelState,
            )
        }
    }

    private fun finishWithError(error: AssistantError, startedAt: Long) {
        finishAssistantMessage(mutableState.value.context?.estimatedTokens ?: 0, startedAt)
        mutableState.update { it.copy(error = error, modelState = AiModelState.Error(error.name)) }
    }

    private fun buildPrompt(context: String, messages: List<ChatMessage>, question: String): PromptData {
        val pairs = messages.filter { it.role != ChatRole.SYSTEM }.takeLast(MAX_HISTORY_MESSAGES)
        var included = pairs
        fun historyText() = included.joinToString("\n") { message ->
            val role = if (message.role == ChatRole.USER) "user" else "assistant"
            "<|im_start|>$role\n${message.content}\n<|im_end|>"
        }
        while (included.isNotEmpty() && historyText().length > MAX_HISTORY_CHARACTERS) {
            included = included.drop(2.coerceAtMost(included.size))
        }
        val prompt = buildString {
            append("<|im_start|>system\n")
            append("You are FinFly, a concise personal finance assistant. Analyze only the supplied private cached data. ")
            append("Do not invent balances or transactions. Explain calculations clearly and note data limitations.\n\n")
            append(context)
            append("\n<|im_end|>\n")
            val history = historyText()
            if (history.isNotEmpty()) append(history).append('\n')
            append("<|im_start|>user\n").append(question).append("\n<|im_end|>\n<|im_start|>assistant\n")
        }
        return PromptData(prompt, included.size < pairs.size)
    }

    private fun AiModelState.toAssistantError(): AssistantError? = when (this) {
        is AiModelState.Error -> if (message == "ai_storage_error") AssistantError.STORAGE else AssistantError.DOWNLOAD
        else -> null
    }

    override fun onCleared() {
        financeAssistant.cancel()
        super.onCleared()
    }

    private data class PromptData(val prompt: String, val historyWasTruncated: Boolean)

    private companion object {
        const val MAX_HISTORY_MESSAGES = 40
        const val MAX_HISTORY_CHARACTERS = 6_000
        const val RESPONSE_TIMEOUT_MILLIS = 60_000L
    }
}
