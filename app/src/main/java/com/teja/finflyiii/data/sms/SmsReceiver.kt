/* Android adapter receiving SMS broadcasts and delegating to the pure parser engine. */
package com.teja.finflyiii.data.sms

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.provider.Telephony
import com.teja.finflyiii.di.ApplicationScope
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.ParsedTransaction
import com.teja.finflyiii.domain.model.SmsLog
import com.teja.finflyiii.domain.model.SmsLogResult
import com.teja.finflyiii.domain.model.SmsParseResult
import com.teja.finflyiii.domain.repository.SettingsRepository
import com.teja.finflyiii.domain.repository.SmsLogRepository
import com.teja.finflyiii.domain.usecase.SmsParserEngine
import com.teja.finflyiii.domain.usecase.SubmitParsedTransactionUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.UUID
import javax.inject.Inject

@AndroidEntryPoint
class SmsReceiver : BroadcastReceiver() {
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var parserEngine: SmsParserEngine
    @Inject lateinit var submitParsedTransaction: SubmitParsedTransactionUseCase
    @Inject lateinit var logRepository: SmsLogRepository
    @Inject lateinit var clock: Clock
    @Inject @ApplicationScope lateinit var applicationScope: CoroutineScope

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return
        if (!settingsRepository.settings.value.smsParsingEnabled) return
        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent)
        if (messages.isEmpty()) return
        val sender = messages.first().originatingAddress.orEmpty()
        val body = messages.joinToString(separator = "") { it.messageBody.orEmpty() }
        val timestamp = messages.first().timestampMillis
        val pendingResult = goAsync()
        applicationScope.launch {
            try {
                process(sender, body, timestamp)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private suspend fun process(sender: String, body: String, timestamp: Long) {
        when (val parsed = parserEngine.process(sender, body, timestamp)) {
            is SmsParseResult.NoRuleMatched -> log(
                sender, body, timestamp, SmsLogResult.NO_RULE, REASON_NO_RULE, "",
            )
            is SmsParseResult.Skipped -> log(
                sender, body, timestamp, SmsLogResult.SKIPPED, parsed.reason, "",
            )
            is SmsParseResult.Success -> submit(parsed.transaction)
        }
    }

    private suspend fun submit(parsed: ParsedTransaction) {
        if (parsed.fireflyAccountId.isBlank() && parsed.accountName.isBlank()) {
            log(
                parsed.sender, parsed.rawSms, parsed.timestamp, SmsLogResult.SKIPPED,
                REASON_ACCOUNT_NOT_MAPPED, parsed.matchedRule,
            )
            return
        }
        val result = submitParsedTransaction(parsed)
        log(
            parsed.sender,
            parsed.rawSms,
            parsed.timestamp,
            if (result is Result.Success) SmsLogResult.SUCCESS else SmsLogResult.SKIPPED,
            if (result is Result.Success) REASON_CREATED else REASON_FIREFLY_SAVE_FAILED,
            parsed.matchedRule,
        )
    }

    private suspend fun log(
        sender: String,
        message: String,
        timestamp: Long,
        result: SmsLogResult,
        reason: String,
        matchedRule: String,
    ) {
        logRepository.add(
            SmsLog(
                id = UUID.randomUUID().toString(),
                sender = sender,
                message = message,
                timestamp = timestamp,
                result = result,
                reason = reason,
                matchedRule = matchedRule,
                processedAt = clock.millis(),
            )
        )
    }

    companion object {
        const val REASON_NO_RULE = "no_rule_matched"
        const val REASON_ACCOUNT_NOT_MAPPED = "account_not_mapped"
        const val REASON_FIREFLY_SAVE_FAILED = "firefly_save_failed"
        const val REASON_CREATED = "transaction_created"
    }
}
