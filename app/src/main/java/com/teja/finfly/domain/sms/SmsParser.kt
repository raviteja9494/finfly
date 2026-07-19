/* Domain contracts for SMS parsing without Android dependencies. */
package com.teja.finfly.domain.sms

import com.teja.finfly.domain.model.BankRule
import com.teja.finfly.domain.model.CategoryRule
import com.teja.finfly.domain.model.SmsParseResult

/** Parses an SMS when its sender is supported by the configured rules. */
interface SmsParser {
    fun canParse(sender: String, message: String): Boolean
    fun parse(sender: String, message: String, timestamp: Long): SmsParseResult
}

/** Creates a parser from the current enabled rule snapshot. */
interface SmsParserFactory {
    fun create(bankRules: List<BankRule>, categoryRules: List<CategoryRule>): SmsParser
}
