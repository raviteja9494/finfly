/* MediaPipe implementation of the provider-neutral finance assistant boundary. */
package com.teja.finfly.data.ai

import android.content.Context
import com.google.common.util.concurrent.ListenableFuture
import com.google.mediapipe.tasks.genai.llminference.LlmInference
import com.google.mediapipe.tasks.genai.llminference.LlmInferenceSession
import com.teja.finfly.domain.assistant.FinanceAssistant
import com.teja.finfly.domain.model.AiConfig
import com.teja.finfly.domain.model.AiModelInfo
import com.teja.finfly.domain.model.AssistantResponseChunk
import com.teja.finfly.domain.repository.AiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class MediaPipeFinanceAssistant @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiRepository: AiRepository,
) : FinanceAssistant {
    private val inferenceDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var inference: LlmInference? = null
    private var session: LlmInferenceSession? = null
    @Volatile private var activeFuture: ListenableFuture<*>? = null
    private var loadedResponseTokens: Int? = null

    override fun streamResponse(prompt: String, config: AiConfig): Flow<AssistantResponseChunk> = callbackFlow {
        try {
            ensureEngine(config)
            recreateSession(config)
            val currentSession = checkNotNull(session)
            currentSession.addQueryChunk(prompt)
            activeFuture = currentSession.generateResponseAsync { partialResult, done ->
                trySend(AssistantResponseChunk(partialResult, done))
                if (done) close()
            }
        } catch (error: Throwable) {
            close(error)
        }
        awaitClose { activeFuture?.cancel(true) }
    }.flowOn(inferenceDispatcher)

    override suspend fun reset() = withContext(inferenceDispatcher) {
        activeFuture?.cancel(true)
        activeFuture = null
        session?.close()
        session = null
        inference?.close()
        inference = null
        loadedResponseTokens = null
    }

    override fun cancel() {
        activeFuture?.cancel(true)
    }

    override fun modelInfo(): AiModelInfo? {
        if (!aiRepository.isModelReady()) return null
        val file = File(aiRepository.modelPath())
        return AiModelInfo(
            name = MODEL_NAME,
            provider = PROVIDER_NAME,
            filePath = file.absolutePath,
            sizeGb = file.length().toDouble() / BYTES_PER_GIGABYTE,
        )
    }

    private fun ensureEngine(config: AiConfig) {
        check(aiRepository.isModelReady()) { MODEL_MISSING_ERROR }
        if (inference != null && loadedResponseTokens == config.maxResponseTokens) return
        session?.close()
        inference?.close()
        val options = LlmInference.LlmInferenceOptions.builder()
            .setModelPath(aiRepository.modelPath())
            .setMaxTokens((INPUT_TOKEN_BUDGET + config.maxResponseTokens).coerceAtMost(MAX_TOTAL_TOKENS))
            .build()
        inference = LlmInference.createFromOptions(context, options)
        loadedResponseTokens = config.maxResponseTokens
    }

    private fun recreateSession(config: AiConfig) {
        session?.close()
        val options = LlmInferenceSession.LlmInferenceSessionOptions.builder()
            .setTemperature(config.temperature)
            .setTopK(TOP_K)
            .setTopP(TOP_P)
            .build()
        session = LlmInferenceSession.createFromOptions(checkNotNull(inference), options)
    }

    private companion object {
        const val MODEL_NAME = "Qwen 2.5 3B Instruct Q4"
        const val PROVIDER_NAME = "MediaPipe LLM Inference"
        const val MODEL_MISSING_ERROR = "ai_model_missing"
        const val INPUT_TOKEN_BUDGET = 3_072
        const val MAX_TOTAL_TOKENS = 4_096
        const val TOP_K = 40
        const val TOP_P = 0.95f
        const val BYTES_PER_GIGABYTE = 1024.0 * 1024.0 * 1024.0
    }
}
