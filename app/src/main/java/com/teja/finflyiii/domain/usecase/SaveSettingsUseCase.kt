/* Domain use case validating and saving Firefly connection settings. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.repository.SettingsRepository
import java.net.URI
import javax.inject.Inject

/** Enforces an HTTP(S) URL with a host and a non-blank token before persistence. */
class SaveSettingsUseCase @Inject constructor(
    private val repository: SettingsRepository,
) {
    suspend operator fun invoke(serverUrl: String, bearerToken: String): Result<Unit> {
        if (!isValidServerUrl(serverUrl)) return Result.Error("invalid_url")
        if (bearerToken.isBlank()) return Result.Error("token_required")
        return repository.save(normalizeServerUrl(serverUrl), bearerToken.trim())
    }

    private fun isValidServerUrl(value: String): Boolean = runCatching {
        val uri = URI(value.trim())
        (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
    }.getOrDefault(false)

    private fun normalizeServerUrl(value: String): String = value.trim().trimEnd('/') + "/"
}
