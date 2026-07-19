/* Presentation ViewModel for friendly bank-rule editing and testing. */
package com.teja.finfly.presentation.smsrules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Account
import com.teja.finfly.domain.model.BankRule
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.model.SmsParseResult
import com.teja.finfly.domain.repository.AccountRepository
import com.teja.finfly.domain.repository.SmsRulesRepository
import com.teja.finfly.domain.usecase.SmsParserEngine
import com.teja.finfly.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

data class BankRuleEditorUiState(
    val id: String = "",
    val existing: Boolean = false,
    val name: String = "",
    val enabled: Boolean = true,
    val accountId: String = "",
    val accounts: List<Account> = emptyList(),
    val senderIds: List<String> = emptyList(),
    val debitKeywords: List<String> = emptyList(),
    val creditKeywords: List<String> = emptyList(),
    val amountPatterns: List<String> = emptyList(),
    val descriptionPatterns: List<String> = emptyList(),
    val referencePatterns: List<String> = emptyList(),
    val sampleSms: String = "",
    val testResult: SmsParseResult? = null,
    val isSaving: Boolean = false,
    val finished: Boolean = false,
    val error: BankRuleEditorError? = null,
)

enum class BankRuleEditorError { NAME, ACCOUNT, SENDER, KEYWORDS, AMOUNT_PATTERN, DESCRIPTION_PATTERN, SAVE }

/** Loads cached accounts, edits friendly placeholder lists, tests unsaved values, and persists one BankRule. */
@HiltViewModel
class BankRuleEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SmsRulesRepository,
    accountRepository: AccountRepository,
    private val engine: SmsParserEngine,
    private val clock: Clock,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AppRoute.BankRuleEditor>()
    private var categories: List<CategoryRule> = emptyList()
    private var createdAt = clock.millis()
    private val mutableState = MutableStateFlow(
        BankRuleEditorUiState(
            id = route.ruleId ?: UUID.randomUUID().toString(),
            existing = route.ruleId != null,
            senderIds = route.prefillSender.takeIf(String::isNotBlank)?.let(::listOf) ?: emptyList(),
        )
    )
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            (repository.getCategoryRules() as? Result.Success)?.value?.let { categories = it }
            route.ruleId?.let { id ->
                (repository.getBankRules() as? Result.Success)?.value?.firstOrNull { it.id == id }?.let(::load)
            }
        }
        viewModelScope.launch {
            accountRepository.observeAccounts().collect { result ->
                if (result is Result.Success) update { copy(accounts = result.value.filter { it.isBalanceAccount }) }
            }
        }
    }

    fun setName(value: String) = update { copy(name = value, error = null) }
    fun setEnabled(value: Boolean) = update { copy(enabled = value) }
    fun setAccount(value: String) = update { copy(accountId = value, error = null) }
    fun setSample(value: String) = update { copy(sampleSms = value, testResult = null) }
    fun addSender(value: String) = add(value) { copy(senderIds = senderIds + it) }
    fun removeSender(value: String) = update { copy(senderIds = senderIds - value) }
    fun addDebit(value: String) = add(value) { copy(debitKeywords = debitKeywords + it) }
    fun removeDebit(value: String) = update { copy(debitKeywords = debitKeywords - value) }
    fun addCredit(value: String) = add(value) { copy(creditKeywords = creditKeywords + it) }
    fun removeCredit(value: String) = update { copy(creditKeywords = creditKeywords - value) }
    fun addAmount(value: String) = add(value) { copy(amountPatterns = amountPatterns + it) }
    fun removeAmount(value: String) = update { copy(amountPatterns = amountPatterns - value) }
    fun addDescription(value: String) = add(value) { copy(descriptionPatterns = descriptionPatterns + it) }
    fun removeDescription(value: String) = update { copy(descriptionPatterns = descriptionPatterns - value) }
    fun addReference(value: String) = add(value) { copy(referencePatterns = referencePatterns + it) }
    fun removeReference(value: String) = update { copy(referencePatterns = referencePatterns - value) }

    fun test() {
        val state = mutableState.value
        val rule = state.toRule(validateAccount = false) ?: return
        update { copy(testResult = engine.testRule(rule, sampleSms, categories)) }
    }

    fun save() {
        val rule = mutableState.value.toRule(validateAccount = true) ?: return
        viewModelScope.launch {
            update { copy(isSaving = true) }
            val result = repository.saveBankRule(rule)
            update {
                copy(
                    isSaving = false,
                    finished = result is Result.Success,
                    error = if (result is Result.Error) BankRuleEditorError.SAVE else null,
                )
            }
        }
    }

    fun delete() {
        if (!mutableState.value.existing) return
        viewModelScope.launch {
            val result = repository.deleteBankRule(mutableState.value.id)
            update { copy(finished = result is Result.Success, error = if (result is Result.Error) BankRuleEditorError.SAVE else null) }
        }
    }

    private fun load(rule: BankRule) {
        createdAt = rule.createdAt
        update {
            copy(
                name = rule.name, enabled = rule.enabled, accountId = rule.fireflyAccountId,
                senderIds = rule.senderIds, debitKeywords = rule.debitKeywords,
                creditKeywords = rule.creditKeywords, amountPatterns = rule.amountPatterns,
                descriptionPatterns = rule.descriptionPatterns, referencePatterns = rule.referencePatterns,
            )
        }
    }

    private fun BankRuleEditorUiState.toRule(validateAccount: Boolean): BankRule? {
        val validation = when {
            name.isBlank() -> BankRuleEditorError.NAME
            validateAccount && accountId.isBlank() -> BankRuleEditorError.ACCOUNT
            senderIds.isEmpty() -> BankRuleEditorError.SENDER
            debitKeywords.isEmpty() && creditKeywords.isEmpty() -> BankRuleEditorError.KEYWORDS
            amountPatterns.none { it.contains("{amount}") } -> BankRuleEditorError.AMOUNT_PATTERN
            descriptionPatterns.none { it.contains("{description}") } -> BankRuleEditorError.DESCRIPTION_PATTERN
            else -> null
        }
        if (validation != null) {
            update { copy(error = validation) }
            return null
        }
        val account = accounts.firstOrNull { it.id == accountId }
        return BankRule(
            id, name.trim(), enabled, senderIds, account?.name.orEmpty(), accountId,
            debitKeywords, creditKeywords, amountPatterns, descriptionPatterns, referencePatterns,
            createdAt, clock.millis(),
        )
    }

    private fun add(value: String, transform: BankRuleEditorUiState.(String) -> BankRuleEditorUiState) {
        value.split(',').map(String::trim).filter(String::isNotBlank).forEach { item ->
            if (item !in mutableState.value.senderIds && item !in mutableState.value.debitKeywords &&
                item !in mutableState.value.creditKeywords && item !in mutableState.value.amountPatterns &&
                item !in mutableState.value.descriptionPatterns && item !in mutableState.value.referencePatterns
            ) update { transform(item).copy(error = null) }
        }
    }

    private fun update(transform: BankRuleEditorUiState.() -> BankRuleEditorUiState) {
        mutableState.value = mutableState.value.transform()
    }
}
