/* Domain repository contract for local connection configuration. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.AppSettings
import kotlinx.coroutines.flow.StateFlow
import java.time.Instant

/**
 * Persists and exposes Firefly connection settings.
 * Inputs are complete settings or sync timestamps; outputs are observable settings and wrapped outcomes.
 */
interface SettingsRepository {
    val settings: StateFlow<AppSettings>
    suspend fun save(serverUrl: String, bearerToken: String): Result<Unit>
    suspend fun updateLastSyncTime(instant: Instant): Result<Unit>
}
