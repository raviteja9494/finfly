/* Domain repository contract for cached and synchronized Firefly tags. */
package com.teja.finflyiii.domain.repository

import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.Tag
import kotlinx.coroutines.flow.Flow

/**
 * Exposes reusable Firefly tags from the local cache and refreshes them from the server.
 * The observable cache remains available when a network refresh fails.
 */
interface TagRepository {
    fun observeTags(): Flow<Result<List<Tag>>>
    suspend fun refresh(): Result<Unit>
}
