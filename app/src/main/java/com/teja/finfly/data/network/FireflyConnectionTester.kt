/* Data-layer implementation for probing Firefly III with unsaved credentials. */
package com.teja.finfly.data.network

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.repository.ConnectionTester
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Named

/** Uses a DI-provided bare client so preview credentials cannot be replaced by saved settings. */
class FireflyConnectionTester @Inject constructor(
    @Named("bare") private val client: OkHttpClient,
) : ConnectionTester {
    override suspend fun test(serverUrl: String, bearerToken: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url(serverUrl.trimEnd('/') + ABOUT_PATH)
                    .header(AUTHORIZATION, "$BEARER $bearerToken")
                    .get()
                    .build()
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) Result.Success(Unit)
                    else Result.Error("HTTP ${response.code}")
                }
            }.getOrElse { Result.Error(it.message ?: it.javaClass.simpleName, it) }
        }

    private companion object {
        const val ABOUT_PATH = "/api/v1/about/user"
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer"
    }
}
