/* Presentation-layer ViewModel for the global drawer, app bar, and sync indicator. */
package com.teja.finflyiii.presentation.navigation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.teja.finflyiii.domain.repository.SettingsRepository
import com.teja.finflyiii.domain.usecase.SyncFinancesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Shares connection metadata and synchronization actions across every navigation destination. */
@HiltViewModel
class AppShellViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val syncFinances: SyncFinancesUseCase,
) : ViewModel() {
    val uiState = combine(settingsRepository.settings, syncFinances.state) { settings, syncState ->
        AppShellUiState.Ready(
            settings.serverUrl,
            settings.lastSyncTime,
            syncState,
            settings.useDeviceTimezone,
        ) as AppShellUiState
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppShellUiState.Loading)

    init {
        viewModelScope.launch {
            settingsRepository.settings
                .filter { it.serverUrl.isNotBlank() && it.bearerToken.isNotBlank() }
                .take(1)
                .collect { syncFinances() }
        }
    }

    fun sync() {
        viewModelScope.launch { syncFinances() }
    }
}
