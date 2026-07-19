/* Domain contracts for SMS parsing without Android dependencies. */
package com.teja.finflyiii.domain.sms

import com.teja.finflyiii.domain.model.BankRule
import com.teja.finflyiii.domain.model.CategoryRule
import com.teja.finflyiii.domain.model.SmsParseResult

/** Parses an SMS when its sender is supported by the configured rules. */
interface SmsParser {
    fun canParse(sender: String, message: String): Boolean
    fun parse(sender: String, message: String, timestamp: Long): SmsParseResult
}

/** Creates a parser from the current enabled rule snapshot. */
interface SmsParserFactory {
    fun create(
        bankRules: List<BankRule>,
        categoryRules: List<CategoryRule>,
        universalTags: List<String> = emptyList(),
    ): SmsParser
}
