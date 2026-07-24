/* Unit coverage for the four built-in Indian bank rules. */
package com.teja.finflyiii.domain.usecase

import com.teja.finflyiii.data.sms.DefaultSmsRules
import com.teja.finflyiii.data.sms.RuleBasedSmsParserFactory
import com.teja.finflyiii.domain.common.Result
import com.teja.finflyiii.domain.model.BankRule
import com.teja.finflyiii.domain.model.CategoryRule
import com.teja.finflyiii.domain.model.RulesConfig
import com.teja.finflyiii.domain.model.RulesImportMode
import com.teja.finflyiii.domain.model.RulesImportSummary
import com.teja.finflyiii.domain.model.SmsParseResult
import com.teja.finflyiii.domain.model.TransactionType
import com.teja.finflyiii.domain.repository.SmsRulesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmsParserEngineTest {
    private val banks = DefaultSmsRules.bankRules(1L).map {
        it.copy(accountName = "Mapped account", fireflyAccountId = "42")
    }
    private val categories = DefaultSmsRules.categoryRules()
    private val engine = SmsParserEngine(FakeRulesRepository(banks, categories), RuleBasedSmsParserFactory())

    @Test
    fun `parses HDFC savings debit and category`() = runBlocking {
        val result = engine.process(
            "AD-HDFCBK-S",
            "A/c debited Rs.1,234.50 To SWIGGY On 19-07 Ref ABCDEF123",
            10L,
        ).success()
        assertEquals(1234.50, result.amount, 0.001)
        assertEquals(TransactionType.WITHDRAWAL, result.type)
        assertEquals("SWIGGY", result.description)
        assertEquals("Food & Dining", result.category)
    }

    @Test
    fun `parses HDFC credit card despite shared sender`() = runBlocking {
        val result = engine.process(
            "AD-HDFCBK-S",
            "Rs.899 spent At AMAZON On your HDFC card Ref CARDREF99",
            20L,
        ).success()
        assertEquals("HDFC Credit Card", result.matchedRule)
        assertEquals("AMAZON", result.description)
        assertEquals("Shopping", result.category)
    }

    @Test
    fun `parses ICICI savings debit`() = runBlocking {
        val result = engine.process(
            "VM-ICICIB",
            "Account debited Rs 500 from UBER. UPI Ref ABCDEF123",
            30L,
        ).success()
        assertEquals("ICICI Savings", result.matchedRule)
        assertEquals("UBER", result.description)
        assertEquals("ABCDEF123", result.reference)
    }

    @Test
    fun `parses Jupiter unicode amount`() = runBlocking {
        val result = engine.process(
            "VM-JTEDGE-S",
            "₹250 paid from your account to ZOMATO on 20 Jul UPI Ref no.JUPITER99",
            40L,
        ).success()
        assertEquals("Edge CSB / Jupiter", result.matchedRule)
        assertEquals(250.0, result.amount, 0.001)
        assertEquals("ZOMATO", result.description)
    }

    @Test
    fun `parses HDFC savings credit`() = runBlocking {
        val result = engine.process(
            "AX-HDFCBK-S",
            "Your account credited INR 1000 To EMPLOYER Ref SALARY99",
            50L,
        ).success()
        assertEquals(TransactionType.DEPOSIT, result.type)
        assertEquals("SALARY99", result.reference)
    }

    @Test
    fun `applies matching and global tags`() = runBlocking {
        val taggedRules = categories + CategoryRule(
            "merchant-tag", "Delivery", listOf("SWIGGY"), "", 1, true, listOf("delivery")
        )
        val taggedEngine = SmsParserEngine(
            FakeRulesRepository(
                banks.map { if (it.name == "HDFC Savings") it.copy(fireflyTags = listOf("bank-tag")) else it },
                taggedRules,
                listOf("parsed"),
            ),
            RuleBasedSmsParserFactory(),
        )
        val result = taggedEngine.process(
            "AD-HDFCBK-S",
            "A/c debited Rs.100 To SWIGGY On 19-07 Ref ABCDEF123",
            60L,
        ).success()
        assertEquals(listOf("bank-tag", "delivery", "parsed"), result.tags)
    }

    @Test
    fun `global test without sender applies categories and every tag source`() = runBlocking {
        val taggedRules = categories + CategoryRule(
            "merchant-tag", "Delivery", listOf("SWIGGY"), "", 1, true, listOf("delivery")
        )
        val taggedEngine = SmsParserEngine(
            FakeRulesRepository(
                banks.map { if (it.name == "HDFC Savings") it.copy(fireflyTags = listOf("bank-tag")) else it },
                taggedRules,
                listOf("parsed"),
            ),
            RuleBasedSmsParserFactory(),
        )

        val report = taggedEngine.testAllRules(
            "",
            "A/c debited Rs.100 To SWIGGY On 19-07 Ref ABCDEF123",
            70L,
        )

        assertTrue(report.inferredSender)
        assertEquals(1, report.matches.size)
        assertEquals("HDFC Savings", report.matches.single().matchedRule)
        assertEquals("Food & Dining", report.matches.single().category)
        assertEquals(listOf("bank-tag", "delivery", "parsed"), report.matches.single().tags)
    }

    @Test
    fun `global test with sender mirrors production rule selection`() = runBlocking {
        val report = engine.testAllRules(
            "AD-HDFCBK-S",
            "A/c debited Rs.100 To SWIGGY On 19-07 Ref ABCDEF123",
            75L,
        )

        assertFalse(report.inferredSender)
        assertEquals(1, report.matches.size)
        assertEquals("HDFC Savings", report.matches.single().matchedRule)
    }

    @Test
    fun `global test reports overlapping bank rules`() = runBlocking {
        val savings = banks.first { it.name == "HDFC Savings" }
        val overlappingEngine = SmsParserEngine(
            FakeRulesRepository(
                banks + savings.copy(id = "duplicate", name = "Duplicate HDFC"),
                categories,
            ),
            RuleBasedSmsParserFactory(),
        )

        val report = overlappingEngine.testAllRules(
            "",
            "A/c debited Rs.100 To SWIGGY On 19-07 Ref ABCDEF123",
            80L,
        )

        assertEquals(2, report.matches.size)
        assertEquals(setOf("HDFC Savings", "Duplicate HDFC"), report.matches.map { it.matchedRule }.toSet())
    }

    private fun SmsParseResult.success() = (this as SmsParseResult.Success).transaction

    private class FakeRulesRepository(
        private val banks: List<BankRule>,
        private val categories: List<CategoryRule>,
        private val universalTags: List<String> = emptyList(),
    ) : SmsRulesRepository {
        override fun observeBankRules(): Flow<Result<List<BankRule>>> = flowOf(Result.Success(banks))
        override fun observeCategoryRules(): Flow<Result<List<CategoryRule>>> = flowOf(Result.Success(categories))
        override fun observeUniversalTags(): Flow<Result<List<String>>> = flowOf(Result.Success(universalTags))
        override suspend fun getBankRules() = Result.Success(banks)
        override suspend fun getCategoryRules() = Result.Success(categories)
        override suspend fun getUniversalTags() = Result.Success(universalTags)
        override suspend fun saveBankRule(rule: BankRule) = Result.Success(Unit)
        override suspend fun deleteBankRule(id: String) = Result.Success(Unit)
        override suspend fun saveCategoryRule(rule: CategoryRule) = Result.Success(Unit)
        override suspend fun deleteCategoryRule(id: String) = Result.Success(Unit)
        override suspend fun saveUniversalTags(tags: List<String>) = Result.Success(Unit)
        override suspend fun ensureDefaults() = Result.Success(Unit)
        override suspend fun createConfig(exportedAt: Long) =
            Result.Success(RulesConfig(exportedAt = exportedAt, bankRules = banks, categoryRules = categories))
        override suspend fun importConfig(config: RulesConfig, mode: RulesImportMode) =
            Result.Success(RulesImportSummary(0, 0))
    }
}
