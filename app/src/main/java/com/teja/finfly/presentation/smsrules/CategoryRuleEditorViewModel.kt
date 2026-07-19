/* Presentation ViewModel for category rule editing. */
package com.teja.finfly.presentation.smsrules

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.repository.SmsRulesRepository
import com.teja.finfly.presentation.navigation.AppRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class CategoryRuleEditorUiState(
    val id: String = "",
    val existing: Boolean = false,
    val name: String = "",
    val fireflyCategory: String = "",
    val priority: String = "100",
    val enabled: Boolean = true,
    val keywords: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val finished: Boolean = false,
    val error: CategoryRuleEditorError? = null,
)

enum class CategoryRuleEditorError { NAME, CATEGORY, PRIORITY, KEYWORDS, SAVE }

/** Loads, validates, saves, and deletes one priority-ordered merchant category rule. */
@HiltViewModel
class CategoryRuleEditorViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val repository: SmsRulesRepository,
) : ViewModel() {
    private val route = savedStateHandle.toRoute<AppRoute.CategoryRuleEditor>()
    private val mutableState = MutableStateFlow(
        CategoryRuleEditorUiState(
            id = route.ruleId ?: UUID.randomUUID().toString(),
            existing = route.ruleId != null,
        )
    )
    val uiState = mutableState.asStateFlow()

    init {
        route.ruleId?.let { id ->
            viewModelScope.launch {
                (repository.getCategoryRules() as? Result.Success)?.value
                    ?.firstOrNull { it.id == id }?.let(::load)
            }
        }
    }

    fun setName(value: String) = update { copy(name = value, error = null) }
    fun setCategory(value: String) = update { copy(fireflyCategory = value, error = null) }
    fun setPriority(value: String) = update { copy(priority = value, error = null) }
    fun setEnabled(value: Boolean) = update { copy(enabled = value) }
    fun addKeyword(value: String) {
        value.split(',').map(String::trim).filter(String::isNotBlank).forEach { keyword ->
            update {
                if (keywords.any { it.equals(keyword, true) }) this
                else copy(keywords = keywords + keyword, error = null)
            }
        }
    }
    fun removeKeyword(value: String) = update { copy(keywords = keywords - value) }

    fun save() {
        val state = mutableState.value
        val priority = state.priority.toIntOrNull()
        val validation = when {
            state.name.isBlank() -> CategoryRuleEditorError.NAME
            state.fireflyCategory.isBlank() -> CategoryRuleEditorError.CATEGORY
            priority == null || priority < 0 -> CategoryRuleEditorError.PRIORITY
            state.keywords.isEmpty() -> CategoryRuleEditorError.KEYWORDS
            else -> null
        }
        if (validation != null) {
            update { copy(error = validation) }
            return
        }
        viewModelScope.launch {
            update { copy(isSaving = true) }
            val result = repository.saveCategoryRule(
                CategoryRule(
                    state.id, state.name.trim(), state.keywords,
                    state.fireflyCategory.trim(), priority!!, state.enabled,
                )
            )
            update {
                copy(
                    isSaving = false,
                    finished = result is Result.Success,
                    error = if (result is Result.Error) CategoryRuleEditorError.SAVE else null,
                )
            }
        }
    }

    fun delete() {
        if (!mutableState.value.existing) return
        viewModelScope.launch {
            val result = repository.deleteCategoryRule(mutableState.value.id)
            update { copy(finished = result is Result.Success, error = if (result is Result.Error) CategoryRuleEditorError.SAVE else null) }
        }
    }

    private fun load(rule: CategoryRule) = update {
        copy(
            name = rule.name,
            fireflyCategory = rule.fireflyCategory,
            priority = rule.priority.toString(),
            enabled = rule.enabled,
            keywords = rule.keywords,
        )
    }

    private fun update(transform: CategoryRuleEditorUiState.() -> CategoryRuleEditorUiState) {
        mutableState.value = mutableState.value.transform()
    }
}
