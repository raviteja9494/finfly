/* Unit tests for Gemma RAG prompt history and formatting. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.domain.model.ChatMessage
import com.teja.finflyiii.domain.model.ChatRole
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RagPromptBuilderTest {
    @Test
    fun promptUsesOnlyLastThreePairsAndNoQwenMarkers() {
        val messages = (1..4).flatMap { index ->
            listOf(
                ChatMessage(id = "u$index", role = ChatRole.USER, content = "question $index"),
                ChatMessage(id = "a$index", role = ChatRole.ASSISTANT, content = "answer $index"),
            )
        }

        val prompt = RagPromptBuilder.build("cached data", messages, "And last month?")

        assertFalse(prompt.text.contains("question 1"))
        assertTrue(prompt.text.contains("question 2"))
        assertTrue(prompt.text.contains("answer 4"))
        assertTrue(prompt.text.contains("And last month?"))
        assertFalse(prompt.text.contains("<|im_start|>"))
        assertTrue(prompt.text.contains("count only rows marked expense"))
        assertTrue(prompt.text.contains("Do not echo the transaction dataset"))
        assertTrue(prompt.text.contains("at most three short bullets"))
        assertTrue(prompt.text.contains("Never print internal headings or raw cached rows"))
        assertTrue(prompt.historyWasTruncated)
        assertFalse(prompt.isTooLarge)
    }

    @Test
    fun promptRespectsConfiguredConversationHistoryPairs() {
        val messages = (1..3).flatMap { index ->
            listOf(
                ChatMessage(id = "u$index", role = ChatRole.USER, content = "question $index"),
                ChatMessage(id = "a$index", role = ChatRole.ASSISTANT, content = "answer $index"),
            )
        }

        val onePair = RagPromptBuilder.build("cached data", messages, "Current?", historyPairs = 1)
        val noHistory = RagPromptBuilder.build("cached data", messages, "Current?", historyPairs = 0)

        assertFalse(onePair.text.contains("question 2"))
        assertTrue(onePair.text.contains("question 3"))
        assertTrue(onePair.text.contains("answer 3"))
        assertFalse(noHistory.text.contains("RECENT CONVERSATION"))
        assertFalse(noHistory.text.contains("question 3"))
    }
}
