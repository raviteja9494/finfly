/* Presentation orchestration for private, on-device finance chat. */
package com.teja.finfly.presentation.assistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.assistant.FinanceAssistant
import com.teja.finfly.domain.model.AiModelState
import com.teja.finfly.domain.model.ChatMessage
import com.teja.finfly.domain.model.ChatRole
import com.teja.finfly.domain.model.AssistantSuggestion
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.domain.repository.AiRepository
import com.teja.finfly.domain.repository.AiSettingsRepository
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.domain.usecase.FinanceContextBuilder
import com.teja.finfly.domain.usecase.RagPromptBuilder
import com.teja.finfly.domain.common.Result
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.time.LocalDate

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
    private val transactionRepository: TransactionRepository,
    private val settingsRepository: SettingsRepository,
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
        viewModelScope.launch { refreshSuggestions() }
    }

    fun setInput(value: String) {
        mutableState.update { it.copy(input = value, error = null) }
    }

    fun downloadModel() {
        if (downloadJob?.isActive == true) return
        downloadJob = viewModelScope.launch {
            aiRepository.downloadModel().collect { modelState ->
                mutableState.update { it.copy(modelState = modelState, error = modelState.toAssistantError()) }
                if (modelState is AiModelState.Downloaded) prepareModel()
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
        if (question.isEmpty() || !aiRepository.isModelReady()) return
        if (snapshot.isGenerating) {
            mutableState.update { it.copy(error = AssistantError.WARMING_UP) }
            return
        }
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
                if (context.transactionCount == 0) throw NoTransactionsException()
                val promptData = RagPromptBuilder.build(context.prompt, previousMessages, question)
                if (promptData.isTooLarge) throw ContextTooLargeException()
                mutableState.update {
                    it.copy(
                        context = context,
                        contextWasTruncated = context.wasTruncated || promptData.historyWasTruncated,
                    )
                }
                var receivedAnyToken = false
                withTimeout(RESPONSE_TIMEOUT_MILLIS) {
                    financeAssistant.streamResponse(promptData.text, snapshot.config)
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
            } catch (_: NoTransactionsException) {
                finishWithError(AssistantError.NO_TRANSACTIONS, startedAt)
            } catch (_: ContextTooLargeException) {
                finishWithError(AssistantError.CONTEXT_TOO_LARGE, startedAt)
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

    private suspend fun prepareModel() {
        mutableState.update { it.copy(modelState = AiModelState.Loading, error = null) }
        try {
            withTimeout(RESPONSE_TIMEOUT_MILLIS) { financeAssistant.prepare() }
            mutableState.update { it.copy(modelState = AiModelState.Ready) }
        } catch (_: OutOfMemoryError) {
            mutableState.update {
                it.copy(modelState = AiModelState.Error("memory"), error = AssistantError.OUT_OF_MEMORY)
            }
        } catch (_: TimeoutCancellationException) {
            mutableState.update { it.copy(modelState = AiModelState.Error("timeout"), error = AssistantError.TIMEOUT) }
        } catch (_: Throwable) {
            mutableState.update {
                it.copy(modelState = AiModelState.Error("initialization"), error = AssistantError.MODEL_LOAD)
            }
        }
    }

    private suspend fun refreshSuggestions() {
        val zone = clock.zone
        val today = LocalDate.now(clock)
        val from = today.withDayOfMonth(1).atStartOfDay(zone).toInstant()
        val until = today.plusDays(1).atStartOfDay(zone).toInstant()
        val transactions = when (val result = transactionRepository.observeTransactions(
            TransactionFilter(from = from, until = until),
            limit = SUGGESTION_TRANSACTION_LIMIT,
            offset = 0,
        ).first()) {
            is Result.Success -> result.value
            is Result.Error -> emptyList()
        }
        val todayTransactions = transactions.filter { it.date.atZone(zone).toLocalDate() == today }
        val suggestions = buildList {
            if (transactions.any {
                    it.type == TransactionType.WITHDRAWAL &&
                        it.category.contains(FOOD_CATEGORY, ignoreCase = true)
                }) {
                add(AssistantSuggestion.FOOD_THIS_MONTH)
            }
            if (settingsRepository.settings.value.lastSyncTime?.atZone(zone)?.toLocalDate() == today) {
                add(AssistantSuggestion.SPEND_TODAY)
            }
            if (todayTransactions.any {
                    it.type == TransactionType.WITHDRAWAL &&
                        it.currency.equals(INR_CURRENCY, ignoreCase = true) &&
                        it.amount.abs() > LARGE_TRANSACTION_AMOUNT
                }) {
                add(AssistantSuggestion.BIG_EXPENSES_TODAY)
            }
            add(AssistantSuggestion.MONTH_SUMMARY)
            add(AssistantSuggestion.BALANCE)
        }.distinct()
        mutableState.update { it.copy(suggestions = suggestions) }
    }

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
        mutableState.update { state ->
            val messages = state.messages.toMutableList()
            if (messages.lastOrNull()?.let { it.role == ChatRole.ASSISTANT && it.content.isBlank() } == true) {
                messages.removeAt(messages.lastIndex)
            }
            state.copy(messages = messages, error = error, modelState = AiModelState.Error(error.name))
        }
    }

    private fun AiModelState.toAssistantError(): AssistantError? = when (this) {
        is AiModelState.Error -> when (message) {
            "ai_storage_error" -> AssistantError.STORAGE
            "ai_model_access_error" -> AssistantError.MODEL_ACCESS
            else -> AssistantError.DOWNLOAD
        }
        else -> null
    }

    override fun onCleared() {
        financeAssistant.cancel()
        super.onCleared()
    }

    private class NoTransactionsException : Exception()
    private class ContextTooLargeException : Exception()

    private companion object {
        const val MAX_HISTORY_MESSAGES = 40
        const val RESPONSE_TIMEOUT_MILLIS = 60_000L
        const val SUGGESTION_TRANSACTION_LIMIT = 1_000
        const val FOOD_CATEGORY = "food"
        const val INR_CURRENCY = "INR"
        val LARGE_TRANSACTION_AMOUNT = java.math.BigDecimal("1000")
    }
}
