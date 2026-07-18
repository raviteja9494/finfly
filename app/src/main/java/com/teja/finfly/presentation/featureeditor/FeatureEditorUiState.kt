/* Presentation-layer form state for creating secondary Firefly resources. */
package com.teja.finfly.presentation.featureeditor

import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.FireflyFeature

data class FeatureEditorUiState(
    val feature: FireflyFeature,
    val name: String = "",
    val notes: String = "",
    val minimumAmount: String = "",
    val maximumAmount: String = "",
    val currencyCode: String = "",
    val startDate: String = "",
    val targetDate: String = "",
    val accountId: String = "",
    val repeatFrequency: String = "monthly",
    val accounts: List<Account> = emptyList(),
    val isSaving: Boolean = false,
    val saved: Boolean = false,
    val error: FeatureEditorError? = null,
)

enum class FeatureEditorError {
    NAME_REQUIRED,
    INVALID_AMOUNT,
    MAXIMUM_BELOW_MINIMUM,
    CURRENCY_REQUIRED,
    INVALID_DATE,
    TARGET_DATE_BEFORE_START,
    ACCOUNT_REQUIRED,
    SAVE_FAILED,
}
