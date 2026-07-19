/* Domain gateway for Android-backed rules file transfer. */
package com.teja.finfly.domain.repository

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.RulesConfig

/** Writes rule backups to Downloads and reads JSON selected through the system file picker. */
interface RulesTransferRepository {
    suspend fun export(config: RulesConfig): Result<String>
    suspend fun read(uri: String): Result<RulesConfig>
}
