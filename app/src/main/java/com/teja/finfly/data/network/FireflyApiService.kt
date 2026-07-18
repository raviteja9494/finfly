/* Data-layer Retrofit declaration for independently extensible Firefly III endpoints. */
package com.teja.finfly.data.network

import com.teja.finfly.data.network.dto.AccountResource
import com.teja.finfly.data.network.dto.ApiListResponse
import com.teja.finfly.data.network.dto.ApiSingleResponse
import com.teja.finfly.data.network.dto.CategoryResource
import com.teja.finfly.data.network.dto.StoreTransactionRequest
import com.teja.finfly.data.network.dto.StoreAccountRequest
import com.teja.finfly.data.network.dto.TagResource
import com.teja.finfly.data.network.dto.TransactionResource
import com.teja.finfly.data.network.dto.UpdateTransactionRequest
import com.teja.finfly.data.network.dto.BudgetResource
import com.teja.finfly.data.network.dto.BillResource
import com.teja.finfly.data.network.dto.PiggyBankResource
import com.teja.finfly.data.network.dto.StoreBudgetRequest
import com.teja.finfly.data.network.dto.StoreCategoryRequest
import com.teja.finfly.data.network.dto.StoreBillRequest
import com.teja.finfly.data.network.dto.StorePiggyBankRequest
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Defines the Firefly III REST surface used through Phase 3.
 * Query inputs control pagination/date bounds; outputs retain the server's JSON:API envelopes.
 */
interface FireflyApiService {
    @GET("api/v1/transactions")
    suspend fun getTransactions(
        @Query("page") page: Int,
        @Query("limit") limit: Int,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
    ): ApiListResponse<TransactionResource>

    @GET("api/v1/accounts")
    suspend fun getAccounts(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): ApiListResponse<AccountResource>

    @GET("api/v1/categories")
    suspend fun getCategories(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): ApiListResponse<CategoryResource>

    @GET("api/v1/tags")
    suspend fun getTags(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): ApiListResponse<TagResource>

    @GET("api/v1/budgets")
    suspend fun getBudgets(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
    ): ApiListResponse<BudgetResource>

    @GET("api/v1/bills")
    suspend fun getBills(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
        @Query("start") start: String? = null,
        @Query("end") end: String? = null,
    ): ApiListResponse<BillResource>

    @GET("api/v1/piggy-banks")
    suspend fun getPiggyBanks(
        @Query("page") page: Int = 1,
        @Query("limit") limit: Int = 100,
    ): ApiListResponse<PiggyBankResource>

    @POST("api/v1/transactions")
    suspend fun createTransaction(
        @Body request: StoreTransactionRequest,
    ): ApiSingleResponse<TransactionResource>

    @PUT("api/v1/transactions/{id}")
    suspend fun updateTransaction(
        @Path("id") id: String,
        @Body request: UpdateTransactionRequest,
    ): ApiSingleResponse<TransactionResource>

    @POST("api/v1/accounts")
    suspend fun createAccount(
        @Body request: StoreAccountRequest,
    ): ApiSingleResponse<AccountResource>

    @POST("api/v1/budgets")
    suspend fun createBudget(@Body request: StoreBudgetRequest): ApiSingleResponse<BudgetResource>

    @POST("api/v1/categories")
    suspend fun createCategory(@Body request: StoreCategoryRequest): ApiSingleResponse<CategoryResource>

    @POST("api/v1/bills")
    suspend fun createBill(@Body request: StoreBillRequest): ApiSingleResponse<BillResource>

    @POST("api/v1/piggy-banks")
    suspend fun createPiggyBank(@Body request: StorePiggyBankRequest): ApiSingleResponse<PiggyBankResource>
}
