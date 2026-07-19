/* Domain use case validating inputs before probing a Firefly server. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.repository.ConnectionTester
import java.net.URI
import javax.inject.Inject

/** Enforces a safe HTTP(S) endpoint and a non-empty token before network access. */
class TestConnectionUseCase @Inject constructor(
    private val tester: ConnectionTester,
) {
    suspend operator fun invoke(serverUrl: String, bearerToken: String): Result<Unit> {
        val normalized = serverUrl.trim().trimEnd('/') + "/"
        val valid = runCatching {
            val uri = URI(normalized)
            (uri.scheme == "http" || uri.scheme == "https") && !uri.host.isNullOrBlank()
        }.getOrDefault(false)
        if (!valid) return Result.Error("invalid_url")
        if (bearerToken.isBlank()) return Result.Error("token_required")
        return tester.test(normalized, bearerToken.trim())
    }
}
