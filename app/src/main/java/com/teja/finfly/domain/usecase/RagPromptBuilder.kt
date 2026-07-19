/* Pure Gemma prompt composition for cached finance retrieval context. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.model.ChatMessage
import com.teja.finfly.domain.model.ChatRole

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
            appendLine("You are FinFly, a concise personal finance assistant.")
            appendLine("Use only the supplied cached finance data for account-specific facts.")
            appendLine("Do not invent balances or transactions. State data limitations plainly.")
            appendLine("For spending, count only rows marked expense; never count income or transfers.")
            appendLine("Respect the requested date period exactly and ignore rows outside it.")
            appendLine("Calculate totals before answering. Keep currencies separate and show the arithmetic briefly.")
            appendLine("Do not echo the transaction dataset or repeat labels, amounts, sentences, or list items.")
            appendLine("Answer in at most five short bullets, then stop.")
            appendLine()
            appendLine("CACHED FINANCE DATA")
            appendLine(financeContext)
            if (recentMessages.isNotEmpty()) {
                appendLine()
                appendLine("RECENT CONVERSATION")
                recentMessages.forEach { message ->
                    append(if (message.role == ChatRole.USER) "User: " else "FinFly: ")
                    appendLine(message.content.take(MAX_MESSAGE_CHARACTERS))
                }
            }
            appendLine()
            appendLine("CURRENT QUESTION")
            appendLine(question)
            append("Answer only the current question directly and keep amounts in the currencies shown in the data.")
        }
        return RagPrompt(
            text = text,
            historyWasTruncated = recentMessages.size < allConversationMessages.size ||
                recentMessages.any { it.content.length > MAX_MESSAGE_CHARACTERS },
            isTooLarge = text.length > MAX_PROMPT_CHARACTERS,
        )
    }

    private const val MAX_HISTORY_MESSAGES = 6
    private const val MAX_MESSAGE_CHARACTERS = 250
    private const val MAX_PROMPT_CHARACTERS = 6_000
}
