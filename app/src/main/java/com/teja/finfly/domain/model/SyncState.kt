/* Domain-layer state machine for Firefly synchronization. */
package com.teja.finfly.domain.model

import java.time.Instant

/** Describes the lifecycle and outcome of a remote-to-local synchronization. */
sealed interface SyncState {
    data object Idle : SyncState
    data object Syncing : SyncState
    data class Success(val completedAt: Instant) : SyncState
    data class Error(val message: String) : SyncState
}
