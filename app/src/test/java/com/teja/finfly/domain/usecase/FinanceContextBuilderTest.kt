/* Unit tests for deterministic assistant token estimation. */
package com.teja.finfly.domain.usecase

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Instant
import java.time.ZoneOffset

class FinanceContextBuilderTest {
    @Test
    fun estimateTokensRoundsCharacterGroupsUp() {
        assertEquals(0, FinanceContextBuilder.estimateTokens(""))
        assertEquals(1, FinanceContextBuilder.estimateTokens("abc"))
        assertEquals(1, FinanceContextBuilder.estimateTokens("abcd"))
        assertEquals(2, FinanceContextBuilder.estimateTokens("abcde"))
    }

    @Test
    fun todayQuestionUsesOnlyCurrentCalendarDay() {
        val now = Instant.parse("2026-07-19T12:30:00Z")

        val period = FinanceContextBuilder.resolvePeriod("What did I spend today?", now, ZoneOffset.UTC, 30)

        assertEquals(Instant.parse("2026-07-19T00:00:00Z"), period.from)
        assertEquals(now, period.until)
        assertEquals(1, period.dayCount)
    }

    @Test
    fun lastMonthQuestionUsesExactPreviousCalendarMonth() {
        val now = Instant.parse("2026-07-19T12:30:00Z")

        val period = FinanceContextBuilder.resolvePeriod("And last month?", now, ZoneOffset.UTC, 30)

        assertEquals(Instant.parse("2026-06-01T00:00:00Z"), period.from)
        assertEquals(Instant.parse("2026-07-01T00:00:00Z"), period.until)
        assertEquals(30, period.dayCount)
    }
}
