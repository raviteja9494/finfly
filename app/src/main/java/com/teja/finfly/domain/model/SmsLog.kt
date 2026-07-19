/* Pure domain model for one SMS processing attempt. */
package com.teja.finfly.domain.model

data class SmsLog(
    val id: String,
    val sender: String,
    val message: String,
    val timestamp: Long,
    val result: SmsLogResult,
    val reason: String,
    val matchedRule: String,
    val processedAt: Long,
)

enum class SmsLogResult { SUCCESS, SKIPPED, NO_RULE }
