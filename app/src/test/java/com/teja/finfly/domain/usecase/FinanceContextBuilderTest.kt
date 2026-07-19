/* Unit tests for deterministic assistant token estimation. */
package com.teja.finfly.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test

class FinanceContextBuilderTest {
    @Test
    fun estimateTokensRoundsCharacterGroupsUp() {
        assertEquals(0, FinanceContextBuilder.estimateTokens(""))
        assertEquals(1, FinanceContextBuilder.estimateTokens("abc"))
        assertEquals(1, FinanceContextBuilder.estimateTokens("abcd"))
        assertEquals(2, FinanceContextBuilder.estimateTokens("abcde"))
    }
}
