/* Data-layer interceptor routing Retrofit requests to the configured Firefly server. */
package com.teja.finflyiii.data.network

import com.teja.finflyiii.BuildConfig
import com.teja.finflyiii.domain.repository.SettingsRepository
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Rewrites scheme, authority, and optional installation path using current settings. */
class ServerUrlInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val configured = settingsRepository.settings.value.serverUrl
            .ifBlank { BuildConfig.DEFAULT_SERVER_URL }
            .toHttpUrlOrNull()
            ?: return chain.proceed(request)
        val apiPath = request.url.encodedPath.trimStart('/')
        val basePath = configured.encodedPath.trimEnd('/')
        val rewritten = request.url.newBuilder()
            .scheme(configured.scheme)
            .host(configured.host)
            .port(configured.port)
            .encodedPath("$basePath/$apiPath")
            .build()
        return chain.proceed(request.newBuilder().url(rewritten).build())
    }
}
