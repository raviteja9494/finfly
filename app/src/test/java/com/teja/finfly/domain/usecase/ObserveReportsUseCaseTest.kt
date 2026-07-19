/* Unit coverage for compact report aggregation semantics. */
package com.teja.finfly.domain.usecase

import com.teja.finfly.domain.common.Result
import com.teja.finfly.domain.model.Category
import com.teja.finfly.domain.model.DailySpend
import com.teja.finfly.domain.model.Transaction
import com.teja.finfly.domain.model.ReportsFilter
import com.teja.finfly.domain.model.TransactionDraft
import com.teja.finfly.domain.model.TransactionFilter
import com.teja.finfly.domain.model.TransactionType
import com.teja.finfly.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

class ObserveReportsUseCaseTest {
    @Test
    fun `builds current totals and excludes transfers from cash flow`() = runBlocking {
        val rows = listOf(
            transaction("income", "2026-07-03", "1000", TransactionType.DEPOSIT),
            transaction("food", "2026-07-05", "200", TransactionType.WITHDRAWAL, "Food"),
            transaction("shopping", "2026-07-07", "100", TransactionType.WITHDRAWAL, "Shopping"),
            transaction("transfer", "2026-07-08", "500", TransactionType.TRANSFER),
            transaction("previous", "2026-06-12", "50", TransactionType.WITHDRAWAL, "Food"),
        )
        val result = ObserveReportsUseCase(FakeTransactionRepository(rows))(
            ReportsFilter(LocalDate.parse("2026-05-01"), LocalDate.parse("2026-07-19"))
        ).first()
        val summary = (result as Result.Success).value

        assertEquals(BigDecimal("1000"), summary.income)
        assertEquals(BigDecimal("350"), summary.expenses)
        assertEquals(BigDecimal("650"), summary.netFlow)
        assertEquals(3, summary.monthlyCashFlow.size)
        assertEquals(BigDecimal("50"), summary.monthlyCashFlow[1].expenses)
        assertEquals("Food", summary.categorySpending.first().category)
        assertEquals(BigDecimal("250"), summary.categorySpending.first().amount)
    }

    @Test
    fun `supports multiple categories with tag groups combined using AND semantics`() = runBlocking {
        val rows = listOf(
            transaction("matching", "2026-07-05", "200", TransactionType.WITHDRAWAL, "Food", listOf("Essential")),
            transaction("wrong-tag", "2026-07-06", "100", TransactionType.WITHDRAWAL, "Food", listOf("Fun")),
            transaction("wrong-category", "2026-07-07", "300", TransactionType.WITHDRAWAL, "Shopping", listOf("Essential")),
        )
        val result = ObserveReportsUseCase(FakeTransactionRepository(rows))(
            ReportsFilter(
                LocalDate.parse("2026-07-01"),
                LocalDate.parse("2026-07-31"),
                categories = setOf("Food", "Shopping"),
                tags = setOf("Essential"),
            )
        ).first()
        val summary = (result as Result.Success).value

        assertEquals(BigDecimal("500"), summary.expenses)
        assertEquals(2, summary.transactionCount)
    }

    private fun transaction(
        id: String,
        date: String,
        amount: String,
        type: TransactionType,
        category: String = "",
        tags: List<String> = emptyList(),
    ) = Transaction(
        id = id,
        remoteGroupId = id,
        journalId = id,
        amount = BigDecimal(amount),
        description = id,
        category = category,
        budget = "",
        account = "Account",
        sourceAccountId = null,
        sourceAccount = "Source",
        destinationAccountId = null,
        destinationAccount = "Destination",
        date = LocalDate.parse(date).atStartOfDay(ZoneId.systemDefault()).toInstant(),
        type = type,
        tags = tags,
        notes = null,
        rawSms = null,
        currency = "INR",
    )

    private class FakeTransactionRepository(
        private val transactions: List<Transaction>,
    ) : TransactionRepository {
        override fun observeTransactions(filter: TransactionFilter, limit: Int, offset: Int) = flowOf(
            Result.Success(
                transactions.filter { row ->
                    (filter.from == null || !row.date.isBefore(filter.from)) &&
                        (filter.until == null || row.date.isBefore(filter.until)) &&
                        (filter.categories.isEmpty() || row.category in filter.categories) &&
                        (filter.tags.isEmpty() || row.tags.any { it in filter.tags })
                }
            )
        )
        override fun observeTransactionCount(filter: TransactionFilter): Flow<Result<Int>> = error("unused")
        override fun observeTransaction(id: String): Flow<Result<Transaction?>> = error("unused")
        override fun observeRecent(limit: Int): Flow<Result<List<Transaction>>> = error("unused")
        override fun observeSpending(from: Instant, until: Instant): Flow<Result<BigDecimal>> = error("unused")
        override fun observeDailySpending(from: Instant, until: Instant): Flow<Result<List<DailySpend>>> = error("unused")
        override fun observeCategories(): Flow<Result<List<Category>>> = error("unused")
        override suspend fun saveTransaction(draft: TransactionDraft): Result<Transaction> = error("unused")
        override suspend fun deleteTransaction(remoteGroupId: String): Result<Unit> = error("unused")
        override suspend fun sync(from: Instant?, until: Instant?): Result<Unit> = error("unused")
    }
}
