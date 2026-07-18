/* Data-layer offline-first implementation of the account repository. */
package com.teja.finfly.data.repository

import com.teja.finfly.data.local.FinFlyDatabase
import com.teja.finfly.data.mapper.toDomain
import com.teja.finfly.data.mapper.toEntity
import com.teja.finfly.data.network.FireflyApiService
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepositoryImpl @Inject constructor(
    private val api: FireflyApiService,
    private val database: FinFlyDatabase,
    private val settingsRepository: SettingsRepository,
) : AccountRepository {
    override fun observeAccounts(): Flow<Result<List<Account>>> = database.accountDao().observeAll()
        .map { rows -> Result.Success(rows.map { it.toDomain() }) as Result<List<Account>> }
        .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override suspend fun sync(): Result<Unit> {
        if (settingsRepository.settings.value.serverUrl.isBlank() ||
            settingsRepository.settings.value.bearerToken.isBlank()
        ) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            val accounts = mutableListOf<com.teja.finfly.data.local.entity.AccountEntity>()
            var page = 1
            var totalPages: Int
            do {
                val response = api.getAccounts(page, PAGE_SIZE)
                accounts += response.data.map { it.toEntity() }
                totalPages = response.meta?.pagination?.totalPages ?: page
                page++
            } while (page <= totalPages && page <= MAX_PAGES)
            database.accountDao().upsertAll(accounts)
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: SYNC_ERROR, it) }
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 100
        const val CACHE_ERROR = "cache_error"
        const val SYNC_ERROR = "sync_error"
        const val NOT_CONFIGURED = "not_configured"
    }
}
