/* Presentation ViewModel for dynamic Firefly tag rule editing. */
package com.teja.finflyiii.presentation.smsrules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.TagRule
import com.teja.finflyiii.domain.model.TagRuleSource
import com.teja.finflyiii.domain.repository.SmsRulesRepository
import com.teja.finflyiii.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TagRuleEditorUiState(
    val id: String = "",
    val existing: Boolean = false,
    val name: String = "",
    val enabled: Boolean = true,
    val source: TagRuleSource = TagRuleSource.FULL_SMS,
    val keywords: List<String> = emptyList(),
    val excludeKeywords: List<String> = emptyList(),
    val fireflyTags: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val finished: Boolean = false,
    val error: TagRuleEditorError? = null,
)

enum class TagRuleEditorError { NAME, KEYWORDS, TAGS, SAVE }

/** Loads, validates, saves, and deletes one additive dynamic tag rule. */
@HiltViewModel
class TagRuleEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SmsRulesRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AppRoute.TagRuleEditor>()
    private val mutableState = MutableStateFlow(
        TagRuleEditorUiState(
            id = route.ruleId ?: UUID.randomUUID().toString(),
            existing = route.ruleId != null,
        )
    )
    val uiState = mutableState.asStateFlow()

    init {
        route.ruleId?.let { id ->
            viewModelScope.launch {
                (repository.getTagRules() as? Result.Success)?.value
                    ?.firstOrNull { it.id == id }?.let(::load)
            }
        }
    }

    fun setName(value: String) = update { copy(name = value, error = null) }
    fun setEnabled(value: Boolean) = update { copy(enabled = value) }
    fun setSource(value: TagRuleSource) = update { copy(source = value) }
    fun addKeyword(value: String) = addValues(value, { keywords }) {
        copy(keywords = keywords + it, error = null)
    }
    fun removeKeyword(value: String) = update { copy(keywords = keywords - value) }
    fun addExcludeKeyword(value: String) = addValues(value, { excludeKeywords }) {
        copy(excludeKeywords = excludeKeywords + it)
    }
    fun removeExcludeKeyword(value: String) = update { copy(excludeKeywords = excludeKeywords - value) }
    fun addTag(value: String) = addValues(value, { fireflyTags }) {
        copy(fireflyTags = fireflyTags + it, error = null)
    }
    fun removeTag(value: String) = update { copy(fireflyTags = fireflyTags - value) }

    fun save() {
        val state = mutableState.value
        val validation = when {
            state.name.isBlank() -> TagRuleEditorError.NAME
            state.keywords.isEmpty() -> TagRuleEditorError.KEYWORDS
            state.fireflyTags.isEmpty() -> TagRuleEditorError.TAGS
            else -> null
        }
        if (validation != null) {
            update { copy(error = validation) }
            return
        }
        viewModelScope.launch {
            update { copy(isSaving = true) }
            val result = repository.saveTagRule(
                TagRule(
                    id = state.id,
                    name = state.name.trim(),
                    enabled = state.enabled,
                    source = state.source,
                    keywords = state.keywords,
                    fireflyTags = state.fireflyTags,
                    excludeKeywords = state.excludeKeywords,
                )
            )
            update {
                copy(
                    isSaving = false,
                    finished = result is Result.Success,
                    error = if (result is Result.Error) TagRuleEditorError.SAVE else null,
                )
            }
        }
    }

    fun delete() {
        if (!mutableState.value.existing) return
        viewModelScope.launch {
            val result = repository.deleteTagRule(mutableState.value.id)
            update {
                copy(
                    finished = result is Result.Success,
                    error = if (result is Result.Error) TagRuleEditorError.SAVE else null,
                )
            }
        }
    }

    private fun load(rule: TagRule) = update {
        copy(
            name = rule.name,
            enabled = rule.enabled,
            source = rule.source,
            keywords = rule.keywords,
            excludeKeywords = rule.excludeKeywords,
            fireflyTags = rule.fireflyTags,
        )
    }

    private fun addValues(
        value: String,
        existing: TagRuleEditorUiState.() -> List<String>,
        append: TagRuleEditorUiState.(String) -> TagRuleEditorUiState,
    ) {
        value.split(',').map(String::trim).filter(String::isNotBlank).forEach { item ->
            update {
                if (existing().any { it.equals(item, true) }) this else append(item)
            }
        }
    }

    private fun update(transform: TagRuleEditorUiState.() -> TagRuleEditorUiState) {
        mutableState.value = mutableState.value.transform()
    }
}
