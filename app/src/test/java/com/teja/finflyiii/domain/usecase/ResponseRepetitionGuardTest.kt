/* Unit tests for bounded local-model repetition detection. */
package com.teja.finflyiii.domain.usecase

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ResponseRepetitionGuardTest {
    @Test
    fun detectsRepeatedSingleToken() {
        assertTrue(ResponseRepetitionGuard.isRunaway("withdrawal:100 withdrawal:100 withdrawal:100 withdrawal:100"))
    }

    @Test
    fun detectsRepeatedShortPhrase() {
        assertTrue(ResponseRepetitionGuard.isRunaway("total is 100 total is 100 total is 100 total is 100"))
    }

    @Test
    fun acceptsOrdinaryAnswer() {
        assertFalse(ResponseRepetitionGuard.isRunaway("You spent 100 INR today across two food transactions."))
    }
}
