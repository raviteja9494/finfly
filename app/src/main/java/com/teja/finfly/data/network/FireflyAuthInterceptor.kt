/* Data-layer OkHttp interceptor injecting the current Firefly bearer token. */
package com.teja.finfly.data.network

import com.teja.finfly.domain.repository.SettingsRepository
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

/** Adds an Authorization header from settings without ever logging the token. */
class FireflyAuthInterceptor @Inject constructor(
    private val settingsRepository: SettingsRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val token = settingsRepository.settings.value.bearerToken
        val request = if (token.isBlank() || original.header(AUTHORIZATION) != null) {
            original
        } else {
            original.newBuilder().header(AUTHORIZATION, "$BEARER $token").build()
        }
        return chain.proceed(request)
    }

    private companion object {
        const val AUTHORIZATION = "Authorization"
        const val BEARER = "Bearer"
    }
}
