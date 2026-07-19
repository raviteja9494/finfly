/* Data-layer Android storage implementation for rule JSON backup and restore. */
package com.teja.finfly.data.repository

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.google.gson.Gson
import com.google.gson.JsonParser
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.RulesConfig
import com.teja.finfly.domain.repository.RulesTransferRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RulesTransferRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val gson: Gson,
) : RulesTransferRepository {
    override suspend fun export(config: RulesConfig): Result<String> = runCatching {
        val fileName = "rules_export_${config.exportedAt}.json"
        val json = gson.toJson(config)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, JSON_MIME)
                put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/FinFly")
            }
            val uri = requireNotNull(
                context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
            )
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(json) }
                ?: error(WRITE_ERROR)
            "${Environment.DIRECTORY_DOWNLOADS}/FinFly/$fileName"
        } else {
            val directory = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "FinFly")
            directory.mkdirs()
            File(directory, fileName).apply { writeText(json) }.absolutePath
        }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(it.message ?: WRITE_ERROR, it) },
    )

    override suspend fun read(uri: String): Result<RulesConfig> = runCatching {
        val json = context.contentResolver.openInputStream(Uri.parse(uri))?.bufferedReader()?.use { it.readText() }
            ?: error(READ_ERROR)
        val root = JsonParser.parseString(json).asJsonObject
        require(root.has("version") && root.get("version").isJsonPrimitive) { INVALID_CONFIG }
        require(root.has("bankRules") && root.get("bankRules").isJsonArray) { INVALID_CONFIG }
        require(root.get("version").asInt == RulesConfig.CURRENT_VERSION) { UNSUPPORTED_VERSION }
        gson.fromJson(json, RulesConfig::class.java).also {
            requireNotNull(it.bankRules) { INVALID_CONFIG }
        }
    }.fold(
        onSuccess = { Result.Success(it) },
        onFailure = { Result.Error(it.message ?: READ_ERROR, it) },
    )

    private companion object {
        const val JSON_MIME = "application/json"
        const val WRITE_ERROR = "rules_export_error"
        const val READ_ERROR = "rules_import_read_error"
        const val INVALID_CONFIG = "rules_invalid_config"
        const val UNSUPPORTED_VERSION = "rules_unsupported_version"
    }
}
