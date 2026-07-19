/* Presentation-layer state contract for the persistent app shell. */
package com.teja.finflyiii.presentation.navigation

import com.teja.finflyiii.domain.model.SyncState
import java.time.Instant

/** Connection metadata and shared synchronization state rendered by the app bar and drawer. */
sealed interface AppShellUiState {
    data object Loading : AppShellUiState
    data class Ready(
        val serverUrl: String,
        val lastSyncTime: Instant?,
        val syncState: SyncState,
        val useDeviceTimezone: Boolean,
    ) : AppShellUiState
}
