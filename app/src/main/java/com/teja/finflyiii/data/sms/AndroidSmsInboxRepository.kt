/* Android ContentResolver adapter for explicitly requested inbox scans. */
package com.teja.finflyiii.data.sms

import android.content.Context
import android.provider.Telephony
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.repository.InboxSms
import com.teja.finflyiii.domain.repository.SmsInboxRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidSmsInboxRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) : SmsInboxRepository {
    override suspend fun scan(fromInclusive: Long, untilExclusive: Long): Result<List<InboxSms>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val rows = mutableListOf<InboxSms>()
                context.contentResolver.query(
                    Telephony.Sms.Inbox.CONTENT_URI,
                    arrayOf(Telephony.Sms._ID, Telephony.Sms.ADDRESS, Telephony.Sms.BODY, Telephony.Sms.DATE),
                    "${Telephony.Sms.DATE} >= ? AND ${Telephony.Sms.DATE} < ?",
                    arrayOf(fromInclusive.toString(), untilExclusive.toString()),
                    "${Telephony.Sms.DATE} DESC",
                )?.use { cursor ->
                    val idIndex = cursor.getColumnIndexOrThrow(Telephony.Sms._ID)
                    val senderIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.ADDRESS)
                    val bodyIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.BODY)
                    val dateIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.DATE)
                    while (cursor.moveToNext()) {
                        rows += InboxSms(
                            id = cursor.getString(idIndex),
                            sender = cursor.getString(senderIndex).orEmpty(),
                            message = cursor.getString(bodyIndex).orEmpty(),
                            timestamp = cursor.getLong(dateIndex),
                        )
                    }
                }
                rows
            }.fold(
                onSuccess = { Result.Success(it) },
                onFailure = { Result.Error(it.message ?: READ_ERROR, it) },
            )
        }

    private companion object { const val READ_ERROR = "sms_inbox_read_error" }
}
