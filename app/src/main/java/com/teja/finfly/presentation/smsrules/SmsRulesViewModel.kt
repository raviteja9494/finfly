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
import com.teja.finfly.domain.repository.SmsInboxRepository
import com.teja.finfly.domain.repository.TransactionRepository
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.model.SmsParseResult
import com.teja.finfly.domain.usecase.SmsParserEngine
import com.teja.finfly.domain.usecase.SubmitParsedTransactionUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

/** Coordinates the master toggle, rule cards, JSON transfer, and default-rule loading. */
@HiltViewModel
class SmsRulesViewModel @Inject constructor(
    private val repository: SmsRulesRepository,
    private val settingsRepository: SettingsRepository,
    private val transferRepository: RulesTransferRepository,
    private val clock: Clock,
    private val inboxRepository: SmsInboxRepository,
    private val parserEngine: SmsParserEngine,
    private val submitParsedTransaction: SubmitParsedTransactionUseCase,
    private val transactionRepository: TransactionRepository,
) : ViewModel() {
    private val today = clock.instant().atZone(ZoneId.systemDefault()).toLocalDate()
    private val mutableState = MutableStateFlow(
        SmsRulesUiState(
            scanFromDate = today.withDayOfMonth(1).toString(),
            scanUntilDate = today.toString(),
        )
    )
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
    fun setScanFromDate(value: String) = update { copy(scanFromDate = value, scanError = null) }
    fun setScanUntilDate(value: String) = update { copy(scanUntilDate = value, scanError = null) }
    fun clearScanPreview() = update { copy(scanPreview = emptyList(), scanError = null) }
    fun toggleScanPreview(id: String) = update {
        copy(scanPreview = scanPreview.map {
            if (it.id == id && it.status != SmsPreviewStatus.PUSHED) {
                it.copy(selected = !it.selected)
            } else it
        })
    }

    fun scanInbox() {
        if (mutableState.value.isScanning) return
        val state = mutableState.value
        val from = runCatching { LocalDate.parse(state.scanFromDate) }.getOrNull()
        val until = runCatching { LocalDate.parse(state.scanUntilDate) }.getOrNull()
        val error = when {
            from == null || until == null -> OnDemandScanError.INVALID_DATE
            until < from -> OnDemandScanError.INVALID_RANGE
            else -> null
        }
        if (error != null) {
            update { copy(scanError = error) }
            return
        }
        val zone = ZoneId.systemDefault()
        viewModelScope.launch {
            update { copy(isScanning = true, scanError = null, scanPreview = emptyList()) }
            when (val result = inboxRepository.scan(
                from!!.atStartOfDay(zone).toInstant().toEpochMilli(),
                until!!.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli(),
            )) {
                is Result.Error -> update { copy(isScanning = false, scanError = OnDemandScanError.READ_FAILED) }
                is Result.Success -> {
                    val existing = transactionRepository.observeTransactions(
                        TransactionFilter(
                            from = from.atStartOfDay(zone).toInstant(),
                            until = until.plusDays(1).atStartOfDay(zone).toInstant(),
                        ),
                        limit = MAX_DUPLICATE_CANDIDATES,
                        offset = 0,
                    ).first().let { (it as? Result.Success)?.value.orEmpty() }
                    val seen = mutableSetOf<String>()
                    val previews = result.value.mapNotNull { sms ->
                        when (val parsed = parserEngine.process(sms.sender, sms.message, sms.timestamp)) {
                            is SmsParseResult.Success -> {
                                val signature = parsed.transaction.duplicateSignature()
                                val duplicate = !seen.add(signature) || existing.any {
                                    it.matchesParsedTransaction(parsed.transaction)
                                }
                                OnDemandSmsPreview(
                                    id = sms.id,
                                    transaction = parsed.transaction,
                                    selected = !duplicate,
                                    status = if (duplicate) SmsPreviewStatus.DUPLICATE else SmsPreviewStatus.READY,
                                )
                            }
                            else -> null
                        }
                    }
                    update {
                        copy(
                            isScanning = false,
                            scanPreview = previews,
                            feedback = SmsRulesFeedback.ScanComplete(previews.size),
                        )
                    }
                }
            }
        }
    }

    fun pushSelectedPreview() {
        val selected = mutableState.value.scanPreview.filter {
            it.selected && it.status != SmsPreviewStatus.PUSHED
        }
        if (selected.isEmpty() || mutableState.value.isPushingPreview) return
        viewModelScope.launch {
            update { copy(isPushingPreview = true, scanError = null) }
            var pushed = 0
            selected.forEach { preview ->
                when (val result = submitParsedTransaction(preview.transaction)) {
                    is Result.Success -> {
                        pushed++
                        updatePreview(preview.id) {
                            copy(selected = false, status = SmsPreviewStatus.PUSHED, errorMessage = null)
                        }
                    }
                    is Result.Error -> updatePreview(preview.id) {
                        copy(status = SmsPreviewStatus.FAILED, errorMessage = result.message)
                    }
                }
            }
            update {
                val failed = scanPreview.any { it.status == SmsPreviewStatus.FAILED }
                copy(
                    isPushingPreview = false,
                    scanError = if (failed) OnDemandScanError.PUSH_FAILED else null,
                    feedback = if (pushed > 0) SmsRulesFeedback.TransactionsPushed(pushed) else feedback,
                )
            }
        }
    }

    private fun update(transform: SmsRulesUiState.() -> SmsRulesUiState) {
        mutableState.value = mutableState.value.transform()
    }

    private fun updatePreview(id: String, transform: OnDemandSmsPreview.() -> OnDemandSmsPreview) = update {
        copy(scanPreview = scanPreview.map { if (it.id == id) it.transform() else it })
    }

    private fun com.teja.finfly.domain.model.ParsedTransaction.duplicateSignature(): String =
        listOf(sender, rawSms, amount.toString(), timestamp.toString()).joinToString("|").lowercase()

    private fun Transaction.matchesParsedTransaction(
        parsed: com.teja.finfly.domain.model.ParsedTransaction,
    ): Boolean {
        val sameRawSms = notes?.contains(parsed.rawSms, ignoreCase = true) == true
        val sameReference = parsed.reference.isNotBlank() && notes?.contains(parsed.reference, ignoreCase = true) == true
        val sameLedgerFields = amount.compareTo(java.math.BigDecimal.valueOf(parsed.amount)) == 0 &&
            description.equals(parsed.description, ignoreCase = true) &&
            kotlin.math.abs(date.toEpochMilli() - parsed.timestamp) <= DUPLICATE_TIME_WINDOW_MILLIS
        return sameRawSms || (sameReference && sameLedgerFields) || sameLedgerFields
    }

    private companion object {
        const val MAX_DUPLICATE_CANDIDATES = 5_000
        const val DUPLICATE_TIME_WINDOW_MILLIS = 120_000L
    }
}
