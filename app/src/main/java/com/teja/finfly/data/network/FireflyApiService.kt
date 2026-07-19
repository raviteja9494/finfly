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
import com.teja.finfly.data.network.dto.StoreTagRequest
import com.teja.finfly.data.network.dto.RuleResource
import com.teja.finfly.data.network.dto.RuleGroupResource
import com.teja.finfly.data.network.dto.BudgetLimitResource
import com.teja.finfly.data.network.dto.UpdateAccountRequest
import com.teja.finfly.data.network.dto.UpdateBudgetRequest
import com.teja.finfly.data.network.dto.UpdateCategoryRequest
import com.teja.finfly.data.network.dto.UpdateTagRequest
import com.teja.finfly.data.network.dto.UpdateBillRequest
import com.teja.finfly.data.network.dto.UpdatePiggyBankRequest
import com.teja.finfly.data.network.dto.StoreRuleRequest
import com.teja.finfly.data.network.dto.UpdateRuleRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.Response

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

    @GET("api/v1/rules")
    suspend fun getRules(@Query("page") page: Int = 1, @Query("limit") limit: Int = 100): ApiListResponse<RuleResource>

    @GET("api/v1/rule-groups")
    suspend fun getRuleGroups(@Query("page") page: Int = 1, @Query("limit") limit: Int = 100): ApiListResponse<RuleGroupResource>

    @GET("api/v1/budget-limits")
    suspend fun getBudgetLimits(@Query("start") start: String, @Query("end") end: String): ApiListResponse<BudgetLimitResource>

    @GET("api/v1/budgets/{id}") suspend fun getBudget(@Path("id") id: String): ApiSingleResponse<BudgetResource>
    @GET("api/v1/categories/{id}") suspend fun getCategory(@Path("id") id: String): ApiSingleResponse<CategoryResource>
    @GET("api/v1/tags/{id}") suspend fun getTag(@Path("id") id: String): ApiSingleResponse<TagResource>
    @GET("api/v1/bills/{id}") suspend fun getBill(@Path("id") id: String): ApiSingleResponse<BillResource>
    @GET("api/v1/piggy-banks/{id}") suspend fun getPiggyBank(@Path("id") id: String): ApiSingleResponse<PiggyBankResource>
    @GET("api/v1/rules/{id}") suspend fun getRule(@Path("id") id: String): ApiSingleResponse<RuleResource>

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

    @POST("api/v1/tags")
    suspend fun createTag(@Body request: StoreTagRequest): ApiSingleResponse<TagResource>

    @POST("api/v1/rules")
    suspend fun createRule(@Body request: StoreRuleRequest): ApiSingleResponse<RuleResource>

    @PUT("api/v1/accounts/{id}") suspend fun updateAccount(@Path("id") id: String, @Body request: UpdateAccountRequest): ApiSingleResponse<AccountResource>
    @PUT("api/v1/budgets/{id}") suspend fun updateBudget(@Path("id") id: String, @Body request: UpdateBudgetRequest): ApiSingleResponse<BudgetResource>
    @PUT("api/v1/categories/{id}") suspend fun updateCategory(@Path("id") id: String, @Body request: UpdateCategoryRequest): ApiSingleResponse<CategoryResource>
    @PUT("api/v1/tags/{id}") suspend fun updateTag(@Path("id") id: String, @Body request: UpdateTagRequest): ApiSingleResponse<TagResource>
    @PUT("api/v1/bills/{id}") suspend fun updateBill(@Path("id") id: String, @Body request: UpdateBillRequest): ApiSingleResponse<BillResource>
    @PUT("api/v1/piggy-banks/{id}") suspend fun updatePiggyBank(@Path("id") id: String, @Body request: UpdatePiggyBankRequest): ApiSingleResponse<PiggyBankResource>
    @PUT("api/v1/rules/{id}") suspend fun updateRule(@Path("id") id: String, @Body request: UpdateRuleRequest): ApiSingleResponse<RuleResource>

    @DELETE("api/v1/transactions/{id}")
    suspend fun deleteTransaction(@Path("id") id: String): Response<Unit>

    @DELETE("api/v1/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: String): Response<Unit>

    @DELETE("api/v1/budgets/{id}")
    suspend fun deleteBudget(@Path("id") id: String): Response<Unit>

    @DELETE("api/v1/categories/{id}")
    suspend fun deleteCategory(@Path("id") id: String): Response<Unit>

    @DELETE("api/v1/bills/{id}")
    suspend fun deleteBill(@Path("id") id: String): Response<Unit>

    @DELETE("api/v1/piggy-banks/{id}")
    suspend fun deletePiggyBank(@Path("id") id: String): Response<Unit>

    @DELETE("api/v1/tags/{id}")
    suspend fun deleteTag(@Path("id") id: String): Response<Unit>

    @DELETE("api/v1/rules/{id}")
    suspend fun deleteRule(@Path("id") id: String): Response<Unit>
}
