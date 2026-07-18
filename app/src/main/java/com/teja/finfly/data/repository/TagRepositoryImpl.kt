/* Data-layer offline-first implementation of the Firefly tag repository. */
package com.teja.finfly.data.repository

import androidx.room.withTransaction
import com.teja.finfly.data.local.FinFlyDatabase
import com.teja.finfly.data.mapper.toDomain
import com.teja.finfly.data.mapper.toEntity
import com.teja.finfly.data.network.FireflyApiService
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Tag
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.repository.TagRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** Keeps the Room tag cache aligned with every paginated tag returned by Firefly III. */
@Singleton
class TagRepositoryImpl @Inject constructor(
    private val api: FireflyApiService,
    private val database: FinFlyDatabase,
    private val settingsRepository: SettingsRepository,
) : TagRepository {
    override fun observeTags(): Flow<Result<List<Tag>>> = database.tagDao().observeAll()
        .map { rows -> Result.Success(rows.map { it.toDomain() }.distinctBy(Tag::name)) as Result<List<Tag>> }
        .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override suspend fun refresh(): Result<Unit> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            val tags = mutableListOf<com.teja.finfly.data.local.entity.TagEntity>()
            var page = 1
            var totalPages: Int
            do {
                val response = api.getTags(page, PAGE_SIZE)
                tags += response.data.map { it.toEntity() }
                totalPages = response.meta?.pagination?.totalPages ?: page
                page++
            } while (page <= totalPages && page <= MAX_PAGES)
            database.withTransaction {
                database.tagDao().clear()
                database.tagDao().upsertAll(tags)
            }
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: REFRESH_ERROR, it) }
    }

    private fun isConfigured(): Boolean = settingsRepository.settings.value.run {
        serverUrl.isNotBlank() && bearerToken.isNotBlank()
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 100
        const val CACHE_ERROR = "cache_error"
        const val REFRESH_ERROR = "tag_refresh_error"
        const val NOT_CONFIGURED = "not_configured"
    }
}
