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
        assertTrue(prompt.text.contains("at most five short bullets"))
        assertTrue(prompt.historyWasTruncated)
        assertFalse(prompt.isTooLarge)
    }
}
