package com.teja.finflyiii.domain.common

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyCodesTest {
    @Test fun `accepts blank optional or ISO code`() {
        assertTrue("".isBlankOrIsoCurrencyCode())
        assertTrue("INR".isBlankOrIsoCurrencyCode())
        assertTrue("usd".isBlankOrIsoCurrencyCode())
    }

    @Test fun `rejects symbols and informal abbreviations`() {
        assertFalse("Rs".isBlankOrIsoCurrencyCode())
        assertFalse("₹".isBlankOrIsoCurrencyCode())
        assertFalse("EURO".isBlankOrIsoCurrencyCode())
    }
}
