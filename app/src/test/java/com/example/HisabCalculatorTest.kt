package com.example

import com.example.domain.model.AccountOpeningBalances
import com.example.domain.model.DebtEntry
import com.example.domain.model.DebtStatus
import com.example.domain.model.DebtType
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.HisabTransaction
import com.example.domain.model.IncomeCategory
import com.example.domain.model.PaymentAccount
import com.example.domain.model.ReportPeriod
import com.example.domain.model.TransactionType
import com.example.domain.util.BackupSerializer
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.HisabCalculator
import com.example.domain.util.PinHashUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HisabCalculatorTest {

    @Test
    fun `balance equals opening plus income minus expense and running balance is chronological`() {
        val opening = 1000.0
        val tx1 = HisabTransaction(
            id = 1L,
            type = TransactionType.INCOME,
            amount = 500.0,
            dateKey = 20261110,
            partyName = "रमेश",
            createdAtMillis = 100L,
            timeMinutes = 600
        )
        val tx2 = HisabTransaction(
            id = 2L,
            type = TransactionType.EXPENSE,
            amount = 200.0,
            dateKey = 20261111,
            partyName = "किराना",
            category = ExpenseCategory.FOOD,
            createdAtMillis = 200L,
            timeMinutes = 720
        )
        val tx3 = HisabTransaction(
            id = 3L,
            type = TransactionType.EXPENSE,
            amount = 150.0,
            dateKey = 20261112,
            partyName = "ऑटो",
            category = ExpenseCategory.TRAVEL,
            createdAtMillis = 300L,
            timeMinutes = 900
        )

        val (summary, entries) = HisabCalculator.calculateLedger(
            transactions = listOf(tx3, tx1, tx2), // out of order input
            openingBalance = opening,
            todayDateKey = 20261112
        )

        assertEquals(1000.0, summary.openingBalance, 0.001)
        assertEquals(500.0, summary.totalIncome, 0.001)
        assertEquals(350.0, summary.totalExpense, 0.001)
        assertEquals(1150.0, summary.currentBalance, 0.001)
        assertEquals(0.0, summary.todayIncome, 0.001)
        assertEquals(150.0, summary.todayExpense, 0.001)
        assertEquals(500.0, summary.monthIncome, 0.001)
        assertEquals(350.0, summary.monthExpense, 0.001)

        // Newest-first display order: tx3, tx2, tx1
        assertEquals(3L, entries[0].transaction.id)
        assertEquals(1150.0, entries[0].balanceAfter, 0.001) // 1000 + 500 - 200 - 150
        assertEquals(2L, entries[1].transaction.id)
        assertEquals(1300.0, entries[1].balanceAfter, 0.001) // 1000 + 500 - 200
        assertEquals(1L, entries[2].transaction.id)
        assertEquals(1500.0, entries[2].balanceAfter, 0.001) // 1000 + 500
    }

    @Test
    fun `multi account balances and transfers preserve total wealth without counting transfers as income or expense`() {
        val openings = AccountOpeningBalances(cash = 1000.0, bank = 5000.0, upi = 500.0)
        val txs = listOf(
            HisabTransaction(
                id = 1L,
                type = TransactionType.INCOME,
                amount = 2000.0,
                dateKey = 20261110,
                partyName = "सैलरी",
                account = PaymentAccount.BANK,
                incomeCategory = IncomeCategory.SALARY,
                timeMinutes = 540
            ),
            HisabTransaction(
                id = 2L,
                type = TransactionType.TRANSFER,
                amount = 1500.0,
                dateKey = 20261111,
                partyName = "बैंक से UPI",
                account = PaymentAccount.BANK,
                toAccount = PaymentAccount.UPI,
                timeMinutes = 600
            ),
            HisabTransaction(
                id = 3L,
                type = TransactionType.EXPENSE,
                amount = 300.0,
                dateKey = 20261112,
                partyName = "सब्जी",
                category = ExpenseCategory.FOOD,
                account = PaymentAccount.UPI,
                timeMinutes = 700
            )
        )

        val (summary, entries) = HisabCalculator.calculateLedger(txs, openings, 20261112)

        assertEquals(6500.0, summary.openingBalance, 0.001)
        assertEquals(2000.0, summary.totalIncome, 0.001)
        assertEquals(300.0, summary.totalExpense, 0.001)
        assertEquals(8200.0, summary.currentBalance, 0.001) // 6500 + 2000 - 300 (transfer excluded!)

        val cashBal = summary.accountBalances.first { it.account == PaymentAccount.CASH }
        val bankBal = summary.accountBalances.first { it.account == PaymentAccount.BANK }
        val upiBal = summary.accountBalances.first { it.account == PaymentAccount.UPI }

        assertEquals(1000.0, cashBal.currentBalance, 0.001)
        assertEquals(5500.0, bankBal.currentBalance, 0.001) // 5000 + 2000 - 1500
        assertEquals(1700.0, upiBal.currentBalance, 0.001)  // 500 + 1500 - 300
        assertEquals(8200.0, cashBal.currentBalance + bankBal.currentBalance + upiBal.currentBalance, 0.001)

        // Transfer entry keeps running total wealth unchanged (8500 -> 8500)
        val transferEntry = entries.first { it.transaction.id == 2L }
        assertEquals(8500.0, transferEntry.balanceAfter, 0.001)
    }

    @Test
    fun `monthly notebook computes brought forward and closing balances accurately`() {
        val txs = listOf(
            HisabTransaction(1L, TransactionType.INCOME, 4000.0, 20261005, "अक्टूबर सैलरी"),
            HisabTransaction(2L, TransactionType.EXPENSE, 1500.0, 20261020, "किराया", ExpenseCategory.HOME),
            HisabTransaction(3L, TransactionType.INCOME, 2000.0, 20261102, "नवंबर बोनस"),
            HisabTransaction(4L, TransactionType.EXPENSE, 500.0, 20261108, "राशन", ExpenseCategory.FOOD)
        )

        val novNotebook = HisabCalculator.calculateMonthlyNotebook(txs, totalOpeningBalance = 1000.0, monthKey = 202611)
        assertEquals(3500.0, novNotebook.broughtForwardBalance, 0.001) // 1000 + 4000 - 1500
        assertEquals(2000.0, novNotebook.monthIncome, 0.001)
        assertEquals(500.0, novNotebook.monthExpense, 0.001)
        assertEquals(5000.0, novNotebook.closingBalance, 0.001)
    }

    @Test
    fun `invalid and negative amounts are rejected`() {
        assertNull(CurrencyFormatter.parseValidTransactionAmount(""))
        assertNull(CurrencyFormatter.parseValidTransactionAmount("0"))
        assertNull(CurrencyFormatter.parseValidTransactionAmount("-50"))
        assertNull(CurrencyFormatter.parseValidTransactionAmount("abc"))
        assertEquals(250.5, CurrencyFormatter.parseValidTransactionAmount("250.50")!!, 0.001)

        assertNull(CurrencyFormatter.parseValidOpeningBalance("-1"))
        assertEquals(0.0, CurrencyFormatter.parseValidOpeningBalance("0")!!, 0.001)
        assertEquals(5000.0, CurrencyFormatter.parseValidOpeningBalance("5,000")!!, 0.001)
    }

    @Test
    fun `filtering works across name amount date account category and month`() {
        val txs = listOf(
            HisabTransaction(1L, TransactionType.INCOME, 1000.0, 20261112, "वेतन", notes = "नवंबर वेतन", account = PaymentAccount.BANK),
            HisabTransaction(2L, TransactionType.EXPENSE, 120.0, 20261112, "शर्मा ढाबा", ExpenseCategory.FOOD, "दोपहर का खाना", account = PaymentAccount.UPI),
            HisabTransaction(3L, TransactionType.EXPENSE, 300.0, 20261025, "किताब घर", ExpenseCategory.EDUCATION, "नोटबुक", account = PaymentAccount.CASH)
        )
        val (_, entries) = HisabCalculator.calculateLedger(txs, 0.0, 20261112)

        val onlyFood = HisabCalculator.filterEntries(entries, "", TransactionType.EXPENSE, ExpenseCategory.FOOD, null, null)
        assertEquals(1, onlyFood.size)
        assertEquals(2L, onlyFood.first().transaction.id)

        val octoberOnly = HisabCalculator.filterEntries(entries, "", null, null, 202610, null)
        assertEquals(1, octoberOnly.size)
        assertEquals(3L, octoberOnly.first().transaction.id)

        val queryByAmount = HisabCalculator.filterEntries(entries, "120", null, null, null, null)
        assertEquals(1, queryByAmount.size)
        assertEquals(2L, queryByAmount.first().transaction.id)

        val queryByIsoDate = HisabCalculator.filterEntries(entries, "2026-10-25", null, null, null, null)
        assertEquals(1, queryByIsoDate.size)
        assertEquals(3L, queryByIsoDate.first().transaction.id)

        val upiOnly = HisabCalculator.filterEntries(entries, "", null, null, null, null, accountFilter = PaymentAccount.UPI)
        assertEquals(1, upiOnly.size)
        assertEquals(2L, upiOnly.first().transaction.id)
    }

    @Test
    fun `reports compute daily and yearly 12-month totals accurately`() {
        val txs = listOf(
            HisabTransaction(1L, TransactionType.INCOME, 2000.0, 20261112, "ग्राहक"),
            HisabTransaction(2L, TransactionType.EXPENSE, 400.0, 20261112, "बाजार", ExpenseCategory.FOOD),
            HisabTransaction(3L, TransactionType.EXPENSE, 100.0, 20260310, "मेट्रो", ExpenseCategory.TRAVEL),
            HisabTransaction(4L, TransactionType.TRANSFER, 999.0, 20261112, "बैंक->नकद", account = PaymentAccount.BANK, toAccount = PaymentAccount.CASH)
        )

        val daily = HisabCalculator.calculateReport(txs, 500.0, ReportPeriod.DAILY, 20261112)
        assertEquals(2000.0, daily.totalReceived, 0.001)
        assertEquals(400.0, daily.totalSpent, 0.001)
        assertEquals(1600.0, daily.periodNet, 0.001)
        assertEquals(2000.0, daily.remainingOverallBalance, 0.001) // 500 + 2000 - 500
        assertEquals(7, daily.buckets.size)

        val yearly = HisabCalculator.calculateReport(txs, 500.0, ReportPeriod.YEARLY, 20261112, selectedYear = 2026)
        assertEquals(12, yearly.buckets.size)
        assertEquals(2000.0, yearly.totalReceived, 0.001)
        assertEquals(500.0, yearly.totalSpent, 0.001)
        assertEquals(100.0, yearly.buckets[2].expense, 0.001) // March = index 2
        assertEquals(2000.0, yearly.buckets[10].income, 0.001) // Nov = index 10
    }

    @Test
    fun `debt summary and duplicate transaction detection and pin hashing work correctly`() {
        val debts = listOf(
            DebtEntry(1L, DebtType.LENT, "अमित", 500.0, 20261110, status = DebtStatus.PENDING),
            DebtEntry(2L, DebtType.LENT, "सुमित", 300.0, 20261111, status = DebtStatus.SETTLED),
            DebtEntry(3L, DebtType.BORROWED, "रवि", 700.0, 20261112, status = DebtStatus.PENDING)
        )
        val debtSummary = HisabCalculator.calculateDebtSummary(debts)
        assertEquals(500.0, debtSummary.pendingToReceive, 0.001)
        assertEquals(700.0, debtSummary.pendingToPay, 0.001)
        assertEquals(1, debtSummary.settledCount)
        assertEquals(2, debtSummary.pendingCount)

        val existing = listOf(
            HisabTransaction(1L, TransactionType.EXPENSE, 150.0, 20261112, "चाय", ExpenseCategory.FOOD, "नाश्ता", timeMinutes = 540, account = PaymentAccount.UPI)
        )
        val duplicate = HisabTransaction(0L, TransactionType.EXPENSE, 150.0, 20261112, " चाय ", ExpenseCategory.FOOD, "नाश्ता", timeMinutes = 540, account = PaymentAccount.UPI)
        val distinctTime = duplicate.copy(timeMinutes = 545)
        assertTrue(HisabCalculator.isDuplicateTransaction(duplicate, existing))
        assertFalse(HisabCalculator.isDuplicateTransaction(distinctTime, existing))

        assertFalse(PinHashUtil.isValidPinFormat("123"))
        assertFalse(PinHashUtil.isValidPinFormat("12ab"))
        assertTrue(PinHashUtil.isValidPinFormat("4829"))
        val salt = PinHashUtil.generateSalt()
        val hash = PinHashUtil.hashPin("4829", salt)
        assertTrue(PinHashUtil.verifyPin("4829", salt, hash))
        assertFalse(PinHashUtil.verifyPin("0000", salt, hash))
    }

    @Test
    fun `csv v2 backup round trip preserves accounts transfers hindi text and skips malformed rows`() {
        val openings = AccountOpeningBalances(cash = 1000.0, bank = 2500.0, upi = 500.0)
        val txs = listOf(
            HisabTransaction(1L, TransactionType.INCOME, 3500.0, 20261112, "राजू भाई", null, "दुकान हिसाब, नकद", timeMinutes = 615, account = PaymentAccount.BANK, incomeCategory = IncomeCategory.BUSINESS),
            HisabTransaction(2L, TransactionType.EXPENSE, 240.5, 20261112, "अग्रवाल स्टोर", ExpenseCategory.SHOPPING, "सब्जी \"ताजा\"", timeMinutes = 845, account = PaymentAccount.UPI),
            HisabTransaction(3L, TransactionType.TRANSFER, 500.0, 20261112, "कैश निकासी", account = PaymentAccount.BANK, toAccount = PaymentAccount.CASH, timeMinutes = 900)
        )

        val csv = BackupSerializer.toCsv(openings, txs) + "\ninvalid-date,10:00 AM,EXPENSE,CASH,,FOOD,bad,-50,skip,0"
        val parsedCsv = BackupSerializer.fromCsv(csv)
        assertEquals(4000.0, parsedCsv.openingBalance!!, 0.001)
        assertEquals(2500.0, parsedCsv.accountOpenings!!.bank, 0.001)
        assertEquals(3, parsedCsv.transactions.size)
        assertEquals(1, parsedCsv.skippedInvalidRows)
        assertEquals(PaymentAccount.UPI, parsedCsv.transactions[1].account)
        assertEquals(TransactionType.TRANSFER, parsedCsv.transactions[2].type)
        assertEquals(PaymentAccount.CASH, parsedCsv.transactions[2].toAccount)
        assertTrue(parsedCsv.transactions[0].notes.contains(","))
        assertTrue(parsedCsv.transactions[1].notes.contains("\"ताजा\""))
    }
}
