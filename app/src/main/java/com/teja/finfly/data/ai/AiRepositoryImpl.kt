/* Data-layer storage and streaming download for the optional MediaPipe model. */
package com.teja.finfly.data.ai

import android.content.Context
import android.os.StatFs
import com.teja.finfly.di.ApplicationScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.AiModelState
import com.teja.finfly.domain.repository.AiRepository
import com.teja.finfly.domain.repository.AiSettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class AiRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("bare") private val client: OkHttpClient,
    private val settingsRepository: AiSettingsRepository,
    @ApplicationScope private val applicationScope: CoroutineScope,
) : AiRepository {
    private val modelDirectory = File(context.getExternalFilesDir(null) ?: context.filesDir, MODEL_DIRECTORY)
    private val modelFile = File(modelDirectory, MODEL_FILE_NAME)
    private val partialFile = File(modelDirectory, "$MODEL_FILE_NAME.part")
    private val downloadMutex = Mutex()
    private val state = MutableStateFlow(discoverState())

    init {
        applicationScope.launch {
            settingsRepository.setModelDownloaded(isModelReady())
        }
    }

    override fun getModelState(): Flow<AiModelState> = state.asStateFlow()

    override fun downloadModel(): Flow<AiModelState> = channelFlow {
        suspend fun sendAndStore(value: AiModelState) {
            state.value = value
            send(value)
        }
        downloadMutex.withLock {
            if (isModelReady()) {
                sendAndStore(downloadedState())
                return@withLock
            }
            withContext(Dispatchers.IO) {
                modelDirectory.mkdirs()
                partialFile.delete()
                if (modelFile.exists()) modelFile.delete()
            }

            try {
                sendAndStore(AiModelState.Downloading(0, 0, 0))
                val request = Request.Builder().url(MODEL_URL).build()
                withContext(Dispatchers.IO) {
                    client.newCall(request).execute().use { response ->
                        check(response.isSuccessful) { DOWNLOAD_HTTP_ERROR }
                        val body = response.body
                        val totalBytes = body.contentLength().coerceAtLeast(0L)
                        if (totalBytes > 0 && availableBytes() < totalBytes + STORAGE_HEADROOM_BYTES) {
                            error(STORAGE_ERROR)
                        }
                        body.byteStream().use { input ->
                            partialFile.outputStream().buffered().use { output ->
                                val buffer = ByteArray(BUFFER_SIZE)
                                var downloaded = 0L
                                var lastPercent = -1
                                while (true) {
                                    coroutineContext.ensureActive()
                                    val count = input.read(buffer)
                                    if (count < 0) break
                                    output.write(buffer, 0, count)
                                    downloaded += count
                                    val percent = if (totalBytes > 0) {
                                        ((downloaded * 100) / totalBytes).toInt().coerceIn(0, 99)
                                    } else 0
                                    if (percent != lastPercent) {
                                        lastPercent = percent
                                        sendAndStore(
                                            AiModelState.Downloading(
                                                progressPercent = percent,
                                                downloadedMb = downloaded.toMegabytes(),
                                                totalMb = totalBytes.toMegabytes(),
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                    check(partialFile.length() >= MIN_VALID_MODEL_BYTES) { DOWNLOAD_INCOMPLETE_ERROR }
                    check(partialFile.renameTo(modelFile)) { DOWNLOAD_MOVE_ERROR }
                }
                settingsRepository.setModelDownloaded(true)
                sendAndStore(downloadedState())
            } catch (cancelled: CancellationException) {
                withContext(Dispatchers.IO) { partialFile.delete() }
                sendAndStore(discoverState())
                throw cancelled
            } catch (error: Throwable) {
                withContext(Dispatchers.IO) { partialFile.delete() }
                settingsRepository.setModelDownloaded(false)
                sendAndStore(AiModelState.Error(error.message ?: DOWNLOAD_ERROR))
            }
        }
    }

    override suspend fun deleteModel(): Result<Unit> = withContext(Dispatchers.IO) {
        runCatching {
            partialFile.delete()
            if (modelFile.exists()) check(modelFile.delete())
            settingsRepository.setModelDownloaded(false)
            state.value = discoverState()
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: DELETE_ERROR, it) }
    }

    override fun isModelReady(): Boolean = modelFile.isFile && modelFile.length() >= MIN_VALID_MODEL_BYTES

    override fun modelPath(): String = modelFile.absolutePath

    private fun discoverState(): AiModelState = if (isModelReady()) {
        downloadedState()
    } else {
        AiModelState.NotDownloaded(availableBytes().toMegabytes())
    }

    private fun downloadedState(): AiModelState.Downloaded = AiModelState.Downloaded(
        modelSizeGb = modelFile.length().toDouble() / BYTES_PER_GIGABYTE,
        filePath = modelFile.absolutePath,
    )

    private fun availableBytes(): Long = runCatching {
        StatFs(modelDirectory.parentFile?.absolutePath ?: context.filesDir.absolutePath).availableBytes
    }.getOrDefault(0L)

    private fun Long.toMegabytes(): Long = this / BYTES_PER_MEGABYTE

    private companion object {
        const val MODEL_DIRECTORY = "models"
        const val MODEL_FILE_NAME = "qwen2.5-3b-instruct-q4.bin"
        const val MODEL_URL =
            "https://huggingface.co/litaotju/qwen2.5-3b-instruct-mediapipe/resolve/main/qwen2.5-3b-instruct-q4.bin"
        const val BUFFER_SIZE = 64 * 1024
        const val BYTES_PER_MEGABYTE = 1024L * 1024L
        const val BYTES_PER_GIGABYTE = 1024.0 * 1024.0 * 1024.0
        const val STORAGE_HEADROOM_BYTES = 200L * BYTES_PER_MEGABYTE
        const val MIN_VALID_MODEL_BYTES = 100L * BYTES_PER_MEGABYTE
        const val DOWNLOAD_HTTP_ERROR = "ai_download_http_error"
        const val STORAGE_ERROR = "ai_storage_error"
        const val DOWNLOAD_INCOMPLETE_ERROR = "ai_download_incomplete"
        const val DOWNLOAD_MOVE_ERROR = "ai_download_move_error"
        const val DOWNLOAD_ERROR = "ai_download_error"
        const val DELETE_ERROR = "ai_delete_error"
    }
}
