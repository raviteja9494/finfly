/* Presentation ViewModel for SMS parsing settings and rule transfer. */
package com.teja.finfly.presentation.smsrules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.BankRule
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.model.RulesImportMode
import com.teja.finfly.domain.repository.RulesTransferRepository
import com.teja.finfly.domain.repository.SettingsRepository
import com.teja.finfly.domain.repository.SmsRulesRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.time.Clock
import javax.inject.Inject

/** Coordinates the master toggle, rule cards, JSON transfer, and default-rule loading. */
@HiltViewModel
class SmsRulesViewModel @Inject constructor(
    private val repository: SmsRulesRepository,
    private val settingsRepository: SettingsRepository,
    private val transferRepository: RulesTransferRepository,
    private val clock: Clock,
) : ViewModel() {
    private val mutableState = MutableStateFlow(SmsRulesUiState())
    val uiState = mutableState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.ensureDefaults()
            combine(
                repository.observeBankRules(),
                repository.observeCategoryRules(),
                settingsRepository.settings,
            ) { banks, categories, settings -> Triple(banks, categories, settings) }
                .collect { (banks, categories, settings) ->
                    val bankValues = (banks as? Result.Success)?.value
                    val categoryValues = (categories as? Result.Success)?.value
                    update {
                        copy(
                            loading = false,
                            error = bankValues == null || categoryValues == null,
                            enabled = settings.smsParsingEnabled,
                            bankRules = bankValues ?: bankRules,
                            categoryRules = categoryValues ?: categoryRules,
                        )
                    }
                }
        }
    }

    fun setEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setSmsParsingEnabled(enabled) }
    }

    fun toggleBankRule(rule: BankRule, enabled: Boolean) {
        viewModelScope.launch {
            repository.saveBankRule(rule.copy(enabled = enabled, updatedAt = clock.millis()))
        }
    }

    fun toggleCategoryRule(rule: CategoryRule, enabled: Boolean) {
        viewModelScope.launch { repository.saveCategoryRule(rule.copy(enabled = enabled)) }
    }

    fun exportRules() {
        viewModelScope.launch {
            update { copy(busy = true) }
            val config = repository.createConfig(clock.millis())
            val outcome: Result<String> = when (config) {
                is Result.Success -> transferRepository.export(config.value)
                is Result.Error -> Result.Error(config.message, config.cause)
            }
            update {
                copy(
                    busy = false,
                    feedback = if (outcome is Result.Success) SmsRulesFeedback.Exported(outcome.value)
                    else SmsRulesFeedback.ExportFailed,
                )
            }
        }
    }

    fun previewImport(uri: String) {
        viewModelScope.launch {
            update { copy(busy = true) }
            when (val result = transferRepository.read(uri)) {
                is Result.Success -> update { copy(busy = false, importPreview = result.value) }
                is Result.Error -> update { copy(busy = false, feedback = SmsRulesFeedback.ImportFailed) }
            }
        }
    }

    fun importRules(mode: RulesImportMode) {
        val config = mutableState.value.importPreview ?: return
        viewModelScope.launch {
            update { copy(busy = true, importPreview = null) }
            when (val result = repository.importConfig(config, mode)) {
                is Result.Success -> update {
                    copy(
                        busy = false,
                        feedback = SmsRulesFeedback.Imported(
                            result.value.bankRulesImported,
                            result.value.categoryRulesImported,
                        ),
                    )
                }
                is Result.Error -> update { copy(busy = false, feedback = SmsRulesFeedback.ImportFailed) }
            }
        }
    }

    fun dismissImport() = update { copy(importPreview = null) }
    fun consumeFeedback() = update { copy(feedback = null) }

    private fun update(transform: SmsRulesUiState.() -> SmsRulesUiState) {
        mutableState.value = mutableState.value.transform()
    }
}
