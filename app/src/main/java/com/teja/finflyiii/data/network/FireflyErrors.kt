package com.teja.finflyiii.data.network

import com.google.gson.JsonObject
import com.google.gson.JsonParser
import retrofit2.HttpException

/** Extracts Firefly's validation message instead of exposing only an HTTP status code. */
fun Throwable.fireflyMessage(fallback: String): String {
    val http = this as? HttpException ?: return message ?: fallback
    val body = runCatching { http.response()?.errorBody()?.string() }.getOrNull().orEmpty()
    val root = runCatching { JsonParser.parseString(body).asJsonObject }.getOrNull()
    val message = root?.get("message")?.takeUnless { it.isJsonNull }?.asString
    val validation = root?.getAsJsonObject("errors")?.firstValidationMessage()
    return listOfNotNull(message, validation).distinct().joinToString(" ").ifBlank {
        "${http.code()} ${http.message()}".trim().ifBlank { fallback }
    }
}

private fun JsonObject.firstValidationMessage(): String? = entrySet().asSequence().firstNotNullOfOrNull { (_, value) ->
    when {
        value.isJsonArray -> value.asJsonArray.firstOrNull()?.takeUnless { it.isJsonNull }?.asString
        value.isJsonPrimitive -> value.asString
        else -> null
    }
}
