/* Pure Gemma prompt composition for cached finance retrieval context. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.model.ChatMessage
import com.teja.finflyiii.domain.model.ChatRole

data class RagPrompt(
    val text: String,
    val historyWasTruncated: Boolean,
    val isTooLarge: Boolean,
)

/** Builds a structured plain-text prompt that LiteRT-LM formats for Gemma internally. */
object RagPromptBuilder {
    fun build(financeContext: String, history: List<ChatMessage>, question: String): RagPrompt {
        val allConversationMessages = history.filter { it.role != ChatRole.SYSTEM }
        val recentMessages = allConversationMessages.takeLast(MAX_HISTORY_MESSAGES)
        val text = buildString {
            appendLine("ROLE")
            appendLine("You are FinFly III, a concise personal finance assistant.")
            appendLine("Use only the supplied cached finance data for account-specific facts.")
            appendLine("Do not invent balances or transactions. State data limitations plainly.")
            appendLine("For spending, count only rows marked expense; never count income or transfers.")
            appendLine("Respect the requested date period exactly and ignore rows outside it.")
            appendLine("Keep currencies separate and state the relevant total directly.")
            appendLine("For general spending totals, use the supplied EXPENSE TOTALS instead of re-summing raw rows.")
            appendLine("Do not echo the transaction dataset or repeat labels, amounts, sentences, or list items.")
            appendLine("If the request is casual, answer in one short sentence without finance data.")
            appendLine("Otherwise answer in at most three short bullets, then stop.")
            appendLine()
            appendLine("CACHED FINANCE DATA")
            appendLine(financeContext)
            if (recentMessages.isNotEmpty()) {
                appendLine()
                appendLine("RECENT CONVERSATION")
                recentMessages.forEach { message ->
                    append(if (message.role == ChatRole.USER) "User: " else "FinFly III: ")
                    appendLine(message.content.take(MAX_MESSAGE_CHARACTERS))
                }
            }
            appendLine()
            appendLine("CURRENT QUESTION")
            appendLine(question)
            append("Answer only the current question directly. Never print internal headings or raw cached rows. Keep amounts in the currencies shown in the data.")
        }
        return RagPrompt(
            text = text,
            historyWasTruncated = recentMessages.size < allConversationMessages.size ||
                recentMessages.any { it.content.length > MAX_MESSAGE_CHARACTERS },
            isTooLarge = text.length > MAX_PROMPT_CHARACTERS,
        )
    }

    private const val MAX_HISTORY_MESSAGES = 6
    private const val MAX_MESSAGE_CHARACTERS = 80
    private const val MAX_PROMPT_CHARACTERS = 3_800
}
