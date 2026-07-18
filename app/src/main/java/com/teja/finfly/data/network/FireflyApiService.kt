/* Data-layer Retrofit declaration for independently extensible Firefly III endpoints. */
package com.teja.finfly.data.network

import com.teja.finfly.data.network.dto.AccountResource
import com.teja.finfly.data.network.dto.ApiListResponse
import com.teja.finfly.data.network.dto.ApiSingleResponse
import com.teja.finfly.data.network.dto.CategoryResource
import com.teja.finfly.data.network.dto.StoreTransactionRequest
import com.teja.finfly.data.network.dto.TransactionResource
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Defines the Phase 1 Firefly III REST surface.
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

    @POST("api/v1/transactions")
    suspend fun createTransaction(
        @Body request: StoreTransactionRequest,
    ): ApiSingleResponse<TransactionResource>
}
