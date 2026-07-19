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
    @SerializedName("budget_name") val budgetName: String? = null,
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
    @SerializedName("opening_balance") val openingBalance: String? = null,
    @SerializedName("opening_balance_date") val openingBalanceDate: String? = null,
)

data class CategoryResource(
    val id: String,
    val attributes: CategoryAttributes,
)

data class CategoryAttributes(val name: String, val notes: String? = null)

data class TagResource(
    val id: String,
    val attributes: TagAttributes,
)

data class TagAttributes(val tag: String, val description: String? = null)

data class CurrencyAmountDto(
    @SerializedName("currency_code") val currencyCode: String? = null,
    val sum: String = "0",
)

data class BudgetResource(val id: String, val attributes: BudgetAttributes)

data class BudgetAttributes(
    val name: String,
    val active: Boolean = true,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("auto_budget_amount") val autoBudgetAmount: String? = null,
    @SerializedName("auto_budget_period") val autoBudgetPeriod: String? = null,
    val notes: String? = null,
    val spent: List<CurrencyAmountDto> = emptyList(),
)

data class BudgetLimitResource(val id: String, val attributes: BudgetLimitAttributes)
data class BudgetLimitAttributes(
    @SerializedName("budget_id") val budgetId: String,
    val amount: String,
    @SerializedName("currency_code") val currencyCode: String? = null,
    val spent: List<CurrencyAmountDto> = emptyList(),
)

data class BillResource(val id: String, val attributes: BillAttributes)

data class BillAttributes(
    val name: String,
    @SerializedName("amount_min") val amountMin: String? = null,
    @SerializedName("amount_max") val amountMax: String? = null,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("repeat_freq") val repeatFrequency: String? = null,
    @SerializedName("next_expected_match_diff") val nextExpectedMatch: String? = null,
    val date: String? = null,
    val notes: String? = null,
)

data class PiggyBankResource(val id: String, val attributes: PiggyBankAttributes)

data class PiggyBankAttributes(
    val name: String,
    val percentage: Int? = null,
    @SerializedName("current_amount") val currentAmount: String? = null,
    @SerializedName("target_amount") val targetAmount: String? = null,
    @SerializedName("currency_code") val currencyCode: String? = null,
    @SerializedName("start_date") val startDate: String? = null,
    @SerializedName("target_date") val targetDate: String? = null,
    val notes: String? = null,
    val accounts: List<PiggyBankAccountDto> = emptyList(),
)

data class PiggyBankAccountDto(
    @SerializedName("account_id") val accountId: String = "",
    @SerializedName("current_amount") val currentAmount: String? = null,
)

data class RuleResource(val id: String, val attributes: RuleAttributes)
data class RuleAttributes(
    val title: String,
    val description: String? = null,
    @SerializedName("rule_group_id") val ruleGroupId: String,
    @SerializedName("rule_group_title") val ruleGroupTitle: String? = null,
    val active: Boolean = true,
    val strict: Boolean = true,
    @SerializedName("stop_processing") val stopProcessing: Boolean = false,
    val triggers: List<RuleClauseDto> = emptyList(),
    val actions: List<RuleClauseDto> = emptyList(),
)
data class RuleGroupResource(val id: String, val attributes: RuleGroupAttributes)
data class RuleGroupAttributes(val title: String, val active: Boolean = true)
data class RuleClauseDto(
    val id: String? = null,
    val type: String,
    val value: String? = null,
    val active: Boolean = true,
    val prohibited: Boolean = false,
    @SerializedName("stop_processing") val stopProcessing: Boolean = false,
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
    @SerializedName("budget_name") val budgetName: String? = null,
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
    @SerializedName("budget_name") val budgetName: String? = null,
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

data class UpdateAccountRequest(
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

data class UpdateCategoryRequest(val name: String, val notes: String? = null)

data class StoreTagRequest(
    val tag: String,
    val description: String? = null,
)
data class UpdateTagRequest(val tag: String, val description: String? = null)

data class StoreRuleRequest(
    val title: String,
    val description: String? = null,
    @SerializedName("rule_group_id") val ruleGroupId: String,
    val trigger: String = "store-journal",
    val active: Boolean = true,
    val strict: Boolean = true,
    @SerializedName("stop_processing") val stopProcessing: Boolean = false,
    val triggers: List<RuleClauseDto>,
    val actions: List<RuleClauseDto>,
)

typealias UpdateRuleRequest = StoreRuleRequest
typealias UpdateBudgetRequest = StoreBudgetRequest
typealias UpdateBillRequest = StoreBillRequest
typealias UpdatePiggyBankRequest = StorePiggyBankRequest

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
