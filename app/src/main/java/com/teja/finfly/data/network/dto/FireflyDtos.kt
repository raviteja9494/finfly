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
    @SerializedName("transaction_journal_id") val transactionJournalId: String = "",
    val type: String,
    val date: String,
    val amount: String,
    val description: String,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("source_id") val sourceId: String? = null,
    @SerializedName("destination_name") val destinationName: String? = null,
    @SerializedName("destination_id") val destinationId: String? = null,
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

data class TagResource(
    val id: String,
    val attributes: TagAttributes,
)

data class TagAttributes(val tag: String)

data class BudgetResource(val id: String, val attributes: BudgetAttributes)

data class BudgetAttributes(
    val name: String,
    val active: Boolean = true,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("auto_budget_amount") val autoBudgetAmount: String? = null,
)

data class BillResource(val id: String, val attributes: BillAttributes)

data class BillAttributes(
    val name: String,
    @SerializedName("amount_min") val amountMin: String? = null,
    @SerializedName("amount_max") val amountMax: String? = null,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("repeat_freq") val repeatFrequency: String? = null,
    @SerializedName("next_expected_match_diff") val nextExpectedMatch: String? = null,
)

data class PiggyBankResource(val id: String, val attributes: PiggyBankAttributes)

data class PiggyBankAttributes(
    val name: String,
    val percentage: Int? = null,
    @SerializedName("current_amount") val currentAmount: String? = null,
    @SerializedName("target_amount") val targetAmount: String? = null,
    @SerializedName("currency_code") val currencyCode: String? = null,
)

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
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("source_id") val sourceId: String? = null,
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("destination_id") val destinationId: String? = null,
    @SerializedName("destination_name") val destinationName: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
)

data class UpdateTransactionRequest(
    @SerializedName("apply_rules") val applyRules: Boolean = true,
    @SerializedName("fire_webhooks") val fireWebhooks: Boolean = true,
    val transactions: List<UpdateTransactionSplit>,
)

data class UpdateTransactionSplit(
    @SerializedName("transaction_journal_id") val transactionJournalId: String,
    val type: String,
    val date: String,
    val amount: String,
    val description: String,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("source_id") val sourceId: String? = null,
    @SerializedName("source_name") val sourceName: String? = null,
    @SerializedName("destination_id") val destinationId: String? = null,
    @SerializedName("destination_name") val destinationName: String? = null,
    @SerializedName("category_name") val categoryName: String? = null,
    val tags: List<String> = emptyList(),
    val notes: String? = null,
)

data class StoreAccountRequest(
    val name: String,
    val type: String,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("opening_balance") val openingBalance: String? = null,
    @SerializedName("opening_balance_date") val openingBalanceDate: String? = null,
    val active: Boolean = true,
    @SerializedName("include_net_worth") val includeNetWorth: Boolean = true,
)

data class StoreBudgetRequest(
    val name: String,
    val active: Boolean = true,
    val notes: String? = null,
    @SerializedName("fire_webhooks") val fireWebhooks: Boolean = true,
    @SerializedName("auto_budget_type") val autoBudgetType: String? = null,
    @SerializedName("auto_budget_currency_code") val autoBudgetCurrencyCode: String? = null,
    @SerializedName("auto_budget_amount") val autoBudgetAmount: String? = null,
    @SerializedName("auto_budget_period") val autoBudgetPeriod: String? = null,
)

data class StoreCategoryRequest(
    val name: String,
    val notes: String? = null,
)

data class StoreTagRequest(
    val tag: String,
    val description: String? = null,
)

data class StoreBillRequest(
    val name: String,
    @SerializedName("amount_min") val amountMin: String,
    @SerializedName("amount_max") val amountMax: String,
    val date: String,
    @SerializedName("repeat_freq") val repeatFrequency: String,
    @SerializedName("currency_code") val currencyCode: String,
    val active: Boolean = true,
    val notes: String? = null,
)

data class StorePiggyBankRequest(
    val name: String,
    val accounts: List<StorePiggyBankAccountRequest>,
    @SerializedName("target_amount") val targetAmount: String,
    @SerializedName("current_amount") val currentAmount: String? = null,
    @SerializedName("start_date") val startDate: String,
    @SerializedName("target_date") val targetDate: String? = null,
    val notes: String? = null,
)

data class StorePiggyBankAccountRequest(
    val id: String,
    @SerializedName("current_amount") val currentAmount: String? = null,
)
