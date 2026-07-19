/* Presentation-layer ViewModel for Firefly feature creation. */
package com.teja.finfly.presentation.featureeditor

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.common.isBlankOrIsoCurrencyCode
import com.teja.finfly.domain.model.AccountGroup
import com.teja.finfly.domain.model.FireflyFeature
import com.teja.finfly.domain.model.FireflyFeatureDraft
import com.teja.finfly.domain.model.FireflyRuleClause
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.FireflyFeatureRepository
import com.teja.finfly.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Validates and creates one Budget, Category, Bill, or Piggy Bank using its required Firefly fields. */
@HiltViewModel
class FeatureEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: FireflyFeatureRepository,
    accountRepository: AccountRepository,
    clock: Clock,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AppRoute.FeatureEditor>()
    private val feature = route.feature
    private val today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
    private val mutableState = MutableStateFlow(
        FeatureEditorUiState(
            feature = feature,
            itemId = route.itemId,
            isLoading = route.itemId != null,
            startDate = today.toString(),
        )
    )
    val uiState = mutableState.asStateFlow()

    init {
        if (feature == FireflyFeature.PIGGY_BANKS) {
            viewModelScope.launch {
                accountRepository.observeAccounts().collect { result ->
                    if (result is Result.Success) {
                        update {
                            val available = result.value.filter { it.group == AccountGroup.ASSET }
                            copy(
                                accounts = available,
                                accountId = accountId.ifBlank { available.firstOrNull()?.id.orEmpty() },
                            )
                        }
                    }
                }
            }
        }
        if (feature == FireflyFeature.RULES) {
            viewModelScope.launch {
                when (val result = repository.loadRuleGroups()) {
                    is Result.Success -> update {
                        copy(
                            ruleGroups = result.value,
                            ruleGroupId = ruleGroupId.ifBlank { result.value.firstOrNull()?.id.orEmpty() },
                        )
                    }
                    is Result.Error -> update { copy(error = FeatureEditorError.LOAD_FAILED) }
                }
            }
        }
        route.itemId?.let { id ->
            viewModelScope.launch {
                when (val result = repository.loadDraft(feature, id)) {
                    is Result.Success -> applyDraft(result.value)
                    is Result.Error -> update { copy(isLoading = false, error = FeatureEditorError.LOAD_FAILED) }
                }
            }
        }
    }

    fun setName(value: String) = update { copy(name = value, error = null) }
    fun setNotes(value: String) = update { copy(notes = value, error = null) }
    fun setMinimumAmount(value: String) = update { copy(minimumAmount = value, error = null) }
    fun setMaximumAmount(value: String) = update { copy(maximumAmount = value, error = null) }
    fun setCurrencyCode(value: String) = update { copy(currencyCode = value.uppercase(), error = null) }
    fun setStartDate(value: String) = update { copy(startDate = value, error = null) }
    fun setTargetDate(value: String) = update { copy(targetDate = value, error = null) }
    fun setAccountId(value: String) = update { copy(accountId = value, error = null) }
    fun setRepeatFrequency(value: String) = update { copy(repeatFrequency = value, error = null) }
    fun setRuleGroupId(value: String) = update { copy(ruleGroupId = value, error = null) }
    fun setActive(value: Boolean) = update { copy(active = value) }
    fun setStrict(value: Boolean) = update { copy(strict = value) }
    fun setStopProcessing(value: Boolean) = update { copy(stopProcessing = value) }
    fun addRuleTrigger() = update { copy(ruleTriggers = ruleTriggers + FireflyRuleClause(type = "description_contains")) }
    fun addRuleAction() = update { copy(ruleActions = ruleActions + FireflyRuleClause(type = "set_category")) }
    fun removeRuleTrigger(index: Int) = update { copy(ruleTriggers = ruleTriggers.filterIndexed { i, _ -> i != index }) }
    fun removeRuleAction(index: Int) = update { copy(ruleActions = ruleActions.filterIndexed { i, _ -> i != index }) }
    fun updateRuleTrigger(index: Int, type: String? = null, value: String? = null) = update {
        copy(ruleTriggers = ruleTriggers.mapIndexed { i, clause ->
            if (i == index) clause.copy(type = type ?: clause.type, value = value ?: clause.value) else clause
        }, error = null)
    }
    fun updateRuleAction(index: Int, type: String? = null, value: String? = null) = update {
        copy(ruleActions = ruleActions.mapIndexed { i, clause ->
            if (i == index) clause.copy(type = type ?: clause.type, value = value ?: clause.value) else clause
        }, error = null)
    }

    fun save() {
        val state = mutableState.value
        val draft = state.toDraftOrNull()
        if (draft == null) return
        viewModelScope.launch {
            update { copy(isSaving = true, error = null) }
            val result = route.itemId?.let { repository.update(it, draft) } ?: repository.create(draft)
            update {
                copy(
                    isSaving = false,
                    saved = result is Result.Success,
                    error = if (result is Result.Error) FeatureEditorError.SAVE_FAILED else null,
                    errorDetails = (result as? Result.Error)?.message,
                )
            }
        }
    }

    private fun FeatureEditorUiState.toDraftOrNull(): FireflyFeatureDraft? {
        if (name.isBlank()) return invalid(FeatureEditorError.NAME_REQUIRED)
        val minimum = minimumAmount.takeIf(String::isNotBlank)?.toBigDecimalOrNull()
        val maximum = maximumAmount.takeIf(String::isNotBlank)?.toBigDecimalOrNull()
        val start = runCatching { LocalDate.parse(startDate) }.getOrNull()
        val target = targetDate.takeIf(String::isNotBlank)?.let {
            runCatching { LocalDate.parse(it) }.getOrNull()
        }
        return when (feature) {
            FireflyFeature.BUDGETS -> {
                if (!currencyCode.isBlankOrIsoCurrencyCode()) {
                    invalid(FeatureEditorError.INVALID_CURRENCY)
                } else if (minimumAmount.isNotBlank() && (minimum == null || minimum.signum() <= 0)) {
                    invalid(FeatureEditorError.INVALID_AMOUNT)
                } else FireflyFeatureDraft.Budget(name, notes, minimum, currencyCode)
            }
            FireflyFeature.CATEGORIES -> FireflyFeatureDraft.Category(name, notes)
            FireflyFeature.TAGS -> FireflyFeatureDraft.Tag(name, notes)
            FireflyFeature.BILLS -> when {
                minimum == null || maximum == null || minimum.signum() <= 0 || maximum.signum() <= 0 ->
                    invalid(FeatureEditorError.INVALID_AMOUNT)
                maximum < minimum -> invalid(FeatureEditorError.MAXIMUM_BELOW_MINIMUM)
                currencyCode.isBlank() -> invalid(FeatureEditorError.CURRENCY_REQUIRED)
                !currencyCode.isBlankOrIsoCurrencyCode() -> invalid(FeatureEditorError.INVALID_CURRENCY)
                start == null -> invalid(FeatureEditorError.INVALID_DATE)
                else -> FireflyFeatureDraft.Bill(
                    name, notes, minimum, maximum, currencyCode, start, repeatFrequency,
                )
            }
            FireflyFeature.PIGGY_BANKS -> when {
                accountId.isBlank() -> invalid(FeatureEditorError.ACCOUNT_REQUIRED)
                minimum == null || minimum.signum() <= 0 -> invalid(FeatureEditorError.INVALID_AMOUNT)
                maximumAmount.isNotBlank() && (maximum == null || maximum.signum() < 0) ->
                    invalid(FeatureEditorError.INVALID_AMOUNT)
                start == null || (targetDate.isNotBlank() && target == null) ->
                    invalid(FeatureEditorError.INVALID_DATE)
                target != null && target < start -> invalid(FeatureEditorError.TARGET_DATE_BEFORE_START)
                else -> FireflyFeatureDraft.PiggyBank(
                    name, notes, accountId, minimum, maximum, start, target,
                )
            }
            FireflyFeature.RULES -> when {
                ruleGroupId.isBlank() -> invalid(FeatureEditorError.RULE_GROUP_REQUIRED)
                ruleTriggers.isEmpty() || ruleActions.isEmpty() ||
                    ruleTriggers.any { it.type.isBlank() } || ruleActions.any { it.type.isBlank() } ->
                    invalid(FeatureEditorError.RULE_CLAUSE_REQUIRED)
                else -> FireflyFeatureDraft.Rule(
                    name, notes, ruleGroupId, active, strict, stopProcessing, ruleTriggers, ruleActions,
                )
            }
        }
    }

    private fun applyDraft(draft: FireflyFeatureDraft) {
        update {
            when (draft) {
                is FireflyFeatureDraft.Budget -> copy(
                    isLoading = false, name = draft.name, notes = draft.notes,
                    minimumAmount = draft.monthlyAmount?.toPlainString().orEmpty(), currencyCode = draft.currencyCode,
                )
                is FireflyFeatureDraft.Category -> copy(isLoading = false, name = draft.name, notes = draft.notes)
                is FireflyFeatureDraft.Tag -> copy(isLoading = false, name = draft.name, notes = draft.notes)
                is FireflyFeatureDraft.Bill -> copy(
                    isLoading = false, name = draft.name, notes = draft.notes,
                    minimumAmount = draft.minimumAmount.toPlainString(), maximumAmount = draft.maximumAmount.toPlainString(),
                    currencyCode = draft.currencyCode, startDate = draft.firstDueDate.toString(),
                    repeatFrequency = draft.repeatFrequency,
                )
                is FireflyFeatureDraft.PiggyBank -> copy(
                    isLoading = false, name = draft.name, notes = draft.notes, accountId = draft.accountId,
                    minimumAmount = draft.targetAmount.toPlainString(),
                    maximumAmount = draft.currentAmount?.toPlainString().orEmpty(),
                    startDate = draft.startDate.toString(), targetDate = draft.targetDate?.toString().orEmpty(),
                )
                is FireflyFeatureDraft.Rule -> copy(
                    isLoading = false, name = draft.name, notes = draft.notes, ruleGroupId = draft.groupId,
                    active = draft.active, strict = draft.strict, stopProcessing = draft.stopProcessing,
                    ruleTriggers = draft.triggers, ruleActions = draft.actions,
                )
            }
        }
    }

    private fun invalid(error: FeatureEditorError): Nothing? {
        update { copy(error = error) }
        return null
    }

    private fun update(transform: FeatureEditorUiState.() -> FeatureEditorUiState) {
        mutableState.value = mutableState.value.transform()
    }
}
