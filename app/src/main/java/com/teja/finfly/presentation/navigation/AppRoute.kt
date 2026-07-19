/* Presentation-layer serializable route definitions for type-safe Compose Navigation. */
package com.teja.finfly.presentation.navigation

import kotlinx.serialization.Serializable

sealed interface AppRoute {
    @Serializable data object Dashboard : AppRoute
    @Serializable data class Transactions(
        val accountId: String? = null,
        val fromEpochMillis: Long? = null,
        val untilEpochMillis: Long? = null,
    ) : AppRoute
    @Serializable data object Reports : AppRoute
    @Serializable data object Settings : AppRoute
    @Serializable data object Assistant : AppRoute
    @Serializable data class TransactionEditor(val transactionId: String? = null) : AppRoute
    @Serializable data class TransactionDetail(val transactionId: String) : AppRoute
    @Serializable data object Accounts : AppRoute
    @Serializable data object AccountEditor : AppRoute
    @Serializable data class FeatureList(val feature: com.teja.finfly.domain.model.FireflyFeature) : AppRoute
    @Serializable data class FeatureEditor(val feature: com.teja.finfly.domain.model.FireflyFeature) : AppRoute
    @Serializable data object SmsParsing : AppRoute
    @Serializable data class BankRuleEditor(val ruleId: String? = null, val prefillSender: String = "") : AppRoute
    @Serializable data class CategoryRuleEditor(val ruleId: String? = null) : AppRoute
    @Serializable data object SmsLogs : AppRoute
    @Serializable data class SmsLogDetail(val id: String) : AppRoute
}
