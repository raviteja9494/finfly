/* Domain contract for user-initiated, date-bounded SMS inbox reads. */
package com.teja.finflyiii.domain.repository

import com.teja.finflyiii.domain.common.Result

data class InboxSms(val id: String, val sender: String, val message: String, val timestamp: Long)

/** Reads device SMS messages only after the user starts an on-demand scan. */
interface SmsInboxRepository {
    suspend fun scan(fromInclusive: Long, untilExclusive: Long): Result<List<InboxSms>>
}
