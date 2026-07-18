/* Data-layer Retrofit DTOs mirroring the Firefly III JSON:API envelopes. */
package com.teja.finfly.data.network.dto

import com.google.gson.annotations.SerializedName

data class ApiListResponse<T>(
    val data: List<T> = emptyList(),
    val meta: ApiMeta? = null,
)

data class ApiSingleResponse<T>(val data: T)

data class ApiMeta(val pagination: ApiPagination? = null)

data class ApiPagination(
    @SerializedName("current_page") val currentPage: Int = 1,
    @SerializedName("total_pages") val totalPages: Int = 1,
)

data class TransactionResource(
    val id: String,
    val attributes: TransactionJournalAttributes,
)

data class TransactionJournalAttributes(
    val transactions: List<TransactionSplitDto> = emptyList(),
)

data class TransactionSplitDto(
    val type: String,
    val date: String,
    val amount: String,
    val description: String,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("destination_name") val destinationName: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
)

data class AccountResource(
    val id: String,
    val attributes: AccountAttributes,
)

data class AccountAttributes(
    val name: String,
    val type: String,
    @SerializedName("current_balance") val currentBalance: String? = null,
    @SerializedName("currency_code") val currencyCode: String? = null,
)

data class CategoryResource(
    val id: String,
    val attributes: CategoryAttributes,
)

data class CategoryAttributes(val name: String)

data class StoreTransactionRequest(
    @SerializedName("apply_rules") val applyRules: Boolean = true,
    @SerializedName("fire_webhooks") val fireWebhooks: Boolean = true,
    val transactions: List<StoreTransactionSplit>,
)

data class StoreTransactionSplit(
    val type: String,
    val date: String,
    val amount: String,
    val description: String,
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("destination_name") val destinationName: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
)
