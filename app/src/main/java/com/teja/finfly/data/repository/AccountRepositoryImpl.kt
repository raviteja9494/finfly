/* Data-layer offline-first implementation of the account repository. */
package com.teja.finfly.data.repository

import androidx.room.withTransaction
import com.teja.finfly.data.local.FinFlyDatabase
import com.teja.finfly.data.mapper.toDomain
import com.teja.finfly.data.mapper.toEntity
import com.teja.finfly.data.network.FireflyApiService
import com.teja.finfly.data.network.dto.StoreAccountRequest
import com.teja.finfly.data.network.dto.UpdateAccountRequest
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.AccountDraft
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

    override fun observeAccount(id: String): Flow<Result<Account?>> = database.accountDao().observeById(id)
        .map { Result.Success(it?.toDomain()) as Result<Account?> }
        .catch { emit(Result.Error(it.message ?: CACHE_ERROR, it)) }

    override suspend fun saveAccount(draft: AccountDraft): Result<Account> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            val response = if (draft.id == null) api.createAccount(
                StoreAccountRequest(
                    name = draft.name.trim(),
                    type = draft.type,
                    currencyCode = draft.currency.takeIf(String::isNotBlank),
                    openingBalance = draft.openingBalance?.toPlainString(),
                    openingBalanceDate = draft.openingBalanceDate?.toString(),
                )
            ) else api.updateAccount(
                draft.id,
                UpdateAccountRequest(
                    name = draft.name.trim(),
                    type = draft.type,
                    currencyCode = draft.currency.takeIf(String::isNotBlank),
                    openingBalance = draft.openingBalance?.toPlainString(),
                    openingBalanceDate = draft.openingBalanceDate?.toString(),
                ),
            )
            val entity = response.data.toEntity()
            database.accountDao().upsertAll(listOf(entity))
            entity.toDomain()
        }.fold(
            onSuccess = { Result.Success(it) },
            onFailure = { Result.Error(it.message ?: SAVE_ERROR, it) },
        )
    }

    override suspend fun deleteAccount(id: String): Result<Unit> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
        return runCatching {
            check(api.deleteAccount(id).isSuccessful)
            database.withTransaction {
                database.accountDao().delete(id)
                database.transactionDao().deleteByAccountId(id)
            }
            Result.Success(Unit)
        }.getOrElse { Result.Error(it.message ?: DELETE_ERROR, it) }
    }

    override suspend fun sync(): Result<Unit> {
        if (!isConfigured()) return Result.Error(NOT_CONFIGURED)
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

    private fun isConfigured(): Boolean = settingsRepository.settings.value.run {
        serverUrl.isNotBlank() && bearerToken.isNotBlank()
    }

    private companion object {
        const val PAGE_SIZE = 100
        const val MAX_PAGES = 100
        const val CACHE_ERROR = "cache_error"
        const val SYNC_ERROR = "sync_error"
        const val SAVE_ERROR = "save_error"
        const val DELETE_ERROR = "account_delete_error"
        const val NOT_CONFIGURED = "not_configured"
    }
}
