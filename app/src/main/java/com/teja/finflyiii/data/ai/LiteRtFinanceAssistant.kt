/* LiteRT-LM implementation of the provider-neutral finance assistant boundary. */
package com.teja.finflyiii.data.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import com.teja.finflyiii.domain.assistant.FinanceAssistant
import com.teja.finflyiii.domain.model.AiConfig
import com.teja.finflyiii.domain.model.AiModelInfo
import com.teja.finflyiii.domain.model.AssistantResponseChunk
import com.teja.finflyiii.domain.repository.AiRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.Executors
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext

@Singleton
class LiteRtFinanceAssistant @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiRepository: AiRepository,
) : FinanceAssistant {
    private val inferenceDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
    private var engine: Engine? = null
    @Volatile private var conversation: Conversation? = null

    override suspend fun prepare() = withContext(inferenceDispatcher) { ensureEngine() }

    override fun streamResponse(prompt: String, config: AiConfig): Flow<AssistantResponseChunk> = flow {
        ensureEngine()
        val activeConversation = recreateConversation(config)
        activeConversation.sendMessageAsync(prompt).collect { partial ->
            val text = partial.toString()
            if (text.isNotEmpty()) emit(AssistantResponseChunk(text, false))
        }
        emit(AssistantResponseChunk("", true))
    }.flowOn(inferenceDispatcher)

    override suspend fun reset() = withContext(inferenceDispatcher) {
        conversation?.close()
        conversation = null
        engine?.close()
        engine = null
    }

    override fun cancel() {
        conversation?.takeIf { it.isAlive }?.cancelProcess()
    }

    override fun modelInfo(): AiModelInfo? {
        if (!aiRepository.isModelReady()) return null
        val file = File(aiRepository.modelPath())
        return AiModelInfo(
            name = GemmaModelSpec.NAME,
            provider = GemmaModelSpec.PROVIDER,
            quantization = GemmaModelSpec.QUANTIZATION,
            filePath = file.absolutePath,
            sizeGb = file.length().toDouble() / BYTES_PER_GIGABYTE,
        )
    }

    private suspend fun ensureEngine() {
        check(aiRepository.isModelReady()) { MODEL_MISSING_ERROR }
        if (engine != null) return
        val created = Engine(
            EngineConfig(
                modelPath = aiRepository.modelPath(),
                backend = Backend.CPU(),
                maxNumTokens = MAX_CONTEXT_TOKENS,
                cacheDir = context.cacheDir.absolutePath,
            )
        )
        try {
            created.initialize()
            check(created.isInitialized()) { INITIALIZATION_ERROR }
            engine = created
        } catch (error: Throwable) {
            if (created.isInitialized()) created.close()
            throw error
        }
    }

    private fun recreateConversation(config: AiConfig): Conversation {
        conversation?.close()
        return checkNotNull(engine).createConversation(
            ConversationConfig(
                samplerConfig = SamplerConfig(
                    topK = TOP_K,
                    topP = TOP_P,
                    temperature = config.temperature.toDouble(),
                )
            )
        ).also { conversation = it }
    }

    private companion object {
        const val MODEL_MISSING_ERROR = "ai_model_missing"
        const val INITIALIZATION_ERROR = "ai_initialization_error"
        const val MAX_CONTEXT_TOKENS = 2_048
        const val TOP_K = 64
        const val TOP_P = 0.95
        const val BYTES_PER_GIGABYTE = 1024.0 * 1024.0 * 1024.0
    }
}
