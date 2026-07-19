/* Immutable metadata and integrity checks for the selected official Gemma model. */
package com.teja.finfly.data.ai

import java.io.File

internal object GemmaModelSpec {
    const val FILE_NAME = "gemma3-1b-it-int4.litertlm"
    const val EXPECTED_SIZE_BYTES = 584_417_280L
    const val NAME = "Gemma 3 1B"
    const val QUANTIZATION = "INT4 QAT"
    const val PROVIDER = "LiteRT-LM 0.14.0"
    const val MODEL_PAGE = "https://huggingface.co/litert-community/Gemma3-1B-IT"
    const val DOWNLOAD_URL =
        "https://huggingface.co/litert-community/Gemma3-1B-IT/resolve/42d538a932e8d5b12e6b3b455f5572560bd60b2c/gemma3-1b-it-int4.litertlm"

    fun isComplete(file: File): Boolean = file.isFile && file.length() == EXPECTED_SIZE_BYTES
}
