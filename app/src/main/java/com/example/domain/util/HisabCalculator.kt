package com.example.domain.util

import com.example.domain.model.AccountBalanceSummary
import com.example.domain.model.AccountOpeningBalances
import com.example.domain.model.CategoryTotal
import com.example.domain.model.ChartBucket
import com.example.domain.model.DebtEntry
import com.example.domain.model.DebtStatus
import com.example.domain.model.DebtSummary
import com.example.domain.model.DebtType
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.HisabTransaction
import com.example.domain.model.LedgerEntry
import com.example.domain.model.LedgerSummary
import com.example.domain.model.MonthlyNotebookSummary
import com.example.domain.model.PaymentAccount
import com.example.domain.model.ReportPeriod
import com.example.domain.model.ReportSummary
import com.example.domain.model.TransactionType
import kotlin.math.abs
import kotlin.math.roundToLong

object HisabCalculator {

    private fun roundCurrency(value: Double): Double {
        if (!value.isFinite()) return 0.0
        return (value * 100.0).roundToLong() / 100.0
    }

    /**
     * Backward-compatible overload when only a single opening balance is provided.
     */
    fun calculateLedger(
        transactions: List<HisabTransaction>,
        openingBalance: Double,
        todayDateKey: Int
    ): Pair<LedgerSummary, List<LedgerEntry>> {
        return calculateLedger(
            transactions = transactions,
            accountOpenings = AccountOpeningBalances(cash = openingBalance),
            todayDateKey = todayDateKey
        )
    }

    /**
     * Computes exact total wealth summary, per-account balances (Cash, Bank, UPI),
     * and chronological running balances.
     *
     * Rules:
     * - Total Wealth = Total Opening Balance + Total Income - Total Expenses
     * - Transfers between own accounts do NOT count as income or expense and do NOT change total wealth.
     */
    fun calculateLedger(
        transactions: List<HisabTransaction>,
        accountOpenings: AccountOpeningBalances,
        todayDateKey: Int
    ): Pair<LedgerSummary, List<LedgerEntry>> {
        val safeCashOpen = roundCurrency(accountOpenings.cash.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0)
        val safeBankOpen = roundCurrency(accountOpenings.bank.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0)
        val safeUpiOpen = roundCurrency(accountOpenings.upi.takeIf { it.isFinite() && it >= 0.0 } ?: 0.0)
        val safeOpenings = AccountOpeningBalances(safeCashOpen, safeBankOpen, safeUpiOpen)
        val totalOpening = roundCurrency(safeOpenings.total)

        val chronological = transactions.sortedWith(
            compareBy<HisabTransaction>(
                { it.dateKey },
                { it.timeMinutes },
                { it.createdAtMillis },
                { it.id }
            )
        )

        var runningTotal = totalOpening
        val accountRunning = mutableMapOf(
            PaymentAccount.CASH to safeCashOpen,
            PaymentAccount.BANK to safeBankOpen,
            PaymentAccount.UPI to safeUpiOpen
        )
        val accountIncome = PaymentAccount.entries.associateWith { 0.0 }.toMutableMap()
        val accountExpense = PaymentAccount.entries.associateWith { 0.0 }.toMutableMap()
        val accountTransfersIn = PaymentAccount.entries.associateWith { 0.0 }.toMutableMap()
        val accountTransfersOut = PaymentAccount.entries.associateWith { 0.0 }.toMutableMap()

        var totalIncome = 0.0
        var totalExpense = 0.0
        var todayIncome = 0.0
        var todayExpense = 0.0
        var monthIncome = 0.0
        var monthExpense = 0.0

        val currentMonthKey = DateUtils.monthKeyFromDateKey(todayDateKey)
        val computedEntries = ArrayList<LedgerEntry>(chronological.size)

        for (tx in chronological) {
            val safeAmount = if (tx.amount.isFinite() && tx.amount > 0.0) roundCurrency(tx.amount) else 0.0
            when (tx.type) {
                TransactionType.INCOME -> {
                    totalIncome = roundCurrency(totalIncome + safeAmount)
                    runningTotal = roundCurrency(runningTotal + safeAmount)
                    accountIncome[tx.account] = roundCurrency((accountIncome[tx.account] ?: 0.0) + safeAmount)
                    accountRunning[tx.account] = roundCurrency((accountRunning[tx.account] ?: 0.0) + safeAmount)

                    if (tx.dateKey == todayDateKey) {
                        todayIncome = roundCurrency(todayIncome + safeAmount)
                    }
                    if (DateUtils.monthKeyFromDateKey(tx.dateKey) == currentMonthKey) {
                        monthIncome = roundCurrency(monthIncome + safeAmount)
                    }
                }
                TransactionType.EXPENSE -> {
                    totalExpense = roundCurrency(totalExpense + safeAmount)
                    runningTotal = roundCurrency(runningTotal - safeAmount)
                    accountExpense[tx.account] = roundCurrency((accountExpense[tx.account] ?: 0.0) + safeAmount)
                    accountRunning[tx.account] = roundCurrency((accountRunning[tx.account] ?: 0.0) - safeAmount)

                    if (tx.dateKey == todayDateKey) {
                        todayExpense = roundCurrency(todayExpense + safeAmount)
                    }
                    if (DateUtils.monthKeyFromDateKey(tx.dateKey) == currentMonthKey) {
                        monthExpense = roundCurrency(monthExpense + safeAmount)
                    }
                }
                TransactionType.TRANSFER -> {
                    val dest = tx.toAccount
                    if (dest != null && dest != tx.account) {
                        accountTransfersOut[tx.account] =
                            roundCurrency((accountTransfersOut[tx.account] ?: 0.0) + safeAmount)
                        accountTransfersIn[dest] =
                            roundCurrency((accountTransfersIn[dest] ?: 0.0) + safeAmount)
                        accountRunning[tx.account] =
                            roundCurrency((accountRunning[tx.account] ?: 0.0) - safeAmount)
                        accountRunning[dest] =
                            roundCurrency((accountRunning[dest] ?: 0.0) + safeAmount)
                    }
                }
            }

            val acctBalAfter = accountRunning[tx.account] ?: 0.0
            computedEntries.add(
                LedgerEntry(
                    transaction = tx,
                    balanceAfter = runningTotal,
                    accountBalanceAfter = acctBalAfter
                )
            )
        }

        val currentBalance = roundCurrency(totalOpening + totalIncome - totalExpense)
        val accountSummaries = PaymentAccount.entries.map { acct ->
            AccountBalanceSummary(
                account = acct,
                openingBalance = safeOpenings.forAccount(acct),
                income = accountIncome[acct] ?: 0.0,
                expense = accountExpense[acct] ?: 0.0,
                transfersIn = accountTransfersIn[acct] ?: 0.0,
                transfersOut = accountTransfersOut[acct] ?: 0.0,
                currentBalance = accountRunning[acct] ?: 0.0
            )
        }

        val summary = LedgerSummary(
            openingBalance = totalOpening,
            accountOpenings = safeOpenings,
            accountBalances = accountSummaries,
            totalIncome = totalIncome,
            totalExpense = totalExpense,
            currentBalance = currentBalance,
            todayIncome = todayIncome,
            todayExpense = todayExpense,
            monthIncome = monthIncome,
            monthExpense = monthExpense
        )

        return summary to computedEntries.asReversed()
    }

    fun calculateMonthlyNotebook(
        allTransactions: List<HisabTransaction>,
        totalOpeningBalance: Double,
        monthKey: Int
    ): MonthlyNotebookSummary {
        val safeOpening = roundCurrency(if (totalOpeningBalance.isFinite() && totalOpeningBalance >= 0.0) totalOpeningBalance else 0.0)
        var beforeIncome = 0.0
        var beforeExpense = 0.0
        var mIncome = 0.0
        var mExpense = 0.0

        for (tx in allTransactions) {
            val txMonth = DateUtils.monthKeyFromDateKey(tx.dateKey)
            val amt = if (tx.amount.isFinite() && tx.amount > 0.0) roundCurrency(tx.amount) else 0.0
            when {
                txMonth < monthKey -> {
                    if (tx.type == TransactionType.INCOME) beforeIncome = roundCurrency(beforeIncome + amt)
                    if (tx.type == TransactionType.EXPENSE) beforeExpense = roundCurrency(beforeExpense + amt)
                }
                txMonth == monthKey -> {
                    if (tx.type == TransactionType.INCOME) mIncome = roundCurrency(mIncome + amt)
                    if (tx.type == TransactionType.EXPENSE) mExpense = roundCurrency(mExpense + amt)
                }
            }
        }

        val broughtForward = roundCurrency(safeOpening + beforeIncome - beforeExpense)
        val closing = roundCurrency(broughtForward + mIncome - mExpense)
        return MonthlyNotebookSummary(
            monthKey = monthKey,
            broughtForwardBalance = broughtForward,
            monthIncome = mIncome,
            monthExpense = mExpense,
            closingBalance = closing
        )
    }

    fun filterEntries(
        entries: List<LedgerEntry>,
        query: String,
        typeFilter: TransactionType?,
        categoryFilter: ExpenseCategory?,
        monthKeyFilter: Int?,
        dateKeyFilter: Int?,
        accountFilter: PaymentAccount? = null
    ): List<LedgerEntry> {
        val trimmedQuery = query.trim().lowercase()
        val numericQuery = trimmedQuery.replace("₹", "").replace(",", "").trim()
        return entries.filter { entry ->
            val tx = entry.transaction
            val matchesType = typeFilter == null || tx.type == typeFilter
            val matchesCategory = categoryFilter == null ||
                (tx.type == TransactionType.EXPENSE && tx.category == categoryFilter)
            val matchesAccount = accountFilter == null ||
                tx.account == accountFilter ||
                tx.toAccount == accountFilter
            val matchesMonth = monthKeyFilter == null ||
                DateUtils.monthKeyFromDateKey(tx.dateKey) == monthKeyFilter
            val matchesDate = dateKeyFilter == null || tx.dateKey == dateKeyFilter
            val matchesQuery = if (trimmedQuery.isEmpty()) {
                true
            } else {
                val rawAmt = if (tx.amount % 1.0 == 0.0) tx.amount.toLong().toString() else tx.amount.toString()
                val formattedAmt = CurrencyFormatter.formatRupees(tx.amount).lowercase()
                tx.partyName.lowercase().contains(trimmedQuery) ||
                    tx.notes.lowercase().contains(trimmedQuery) ||
                    tx.displayCategoryLabel.lowercase().contains(trimmedQuery) ||
                    tx.account.labelHi.lowercase().contains(trimmedQuery) ||
                    (tx.toAccount?.labelHi?.lowercase()?.contains(trimmedQuery) == true) ||
                    tx.type.labelHi.lowercase().contains(trimmedQuery) ||
                    DateUtils.formatDateHindi(tx.dateKey).lowercase().contains(trimmedQuery) ||
                    DateUtils.formatIsoDate(tx.dateKey).contains(trimmedQuery) ||
                    formattedAmt.contains(trimmedQuery) ||
                    (numericQuery.isNotEmpty() && rawAmt.contains(numericQuery))
            }
            matchesType && matchesCategory && matchesAccount && matchesMonth && matchesDate && matchesQuery
        }
    }

    fun calculateReport(
        transactions: List<HisabTransaction>,
        openingBalance: Double,
        period: ReportPeriod,
        todayDateKey: Int,
        selectedYear: Int = DateUtils.yearOf(todayDateKey)
    ): ReportSummary {
        val safeOpening = roundCurrency(if (openingBalance >= 0.0 && openingBalance.isFinite()) openingBalance else 0.0)
        val overallIncome = transactions
            .filter { it.type == TransactionType.INCOME }
            .sumOf { it.amount }
        val overallExpense = transactions
            .filter { it.type == TransactionType.EXPENSE }
            .sumOf { it.amount }
        val remainingOverallBalance = roundCurrency(safeOpening + overallIncome - overallExpense)

        val periodTransactions: List<HisabTransaction>
        val buckets: List<ChartBucket>

        when (period) {
            ReportPeriod.DAILY -> {
                val startKey = DateUtils.shiftDays(todayDateKey, -6)
                periodTransactions = transactions.filter { it.dateKey in startKey..todayDateKey }
                buckets = (-6..0).map { offset ->
                    val dayKey = DateUtils.shiftDays(todayDateKey, offset)
                    val dayTxs = transactions.filter { it.dateKey == dayKey }
                    ChartBucket(
                        label = DateUtils.formatShortDayMonth(dayKey),
                        income = roundCurrency(dayTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }),
                        expense = roundCurrency(dayTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
                    )
                }
            }
            ReportPeriod.WEEKLY -> {
                val earliestKey = DateUtils.shiftDays(todayDateKey, -27)
                periodTransactions = transactions.filter { it.dateKey in earliestKey..todayDateKey }
                buckets = (3 downTo 0).mapIndexed { index, weekOffset ->
                    val endDay = DateUtils.shiftDays(todayDateKey, -(weekOffset * 7))
                    val startDay = DateUtils.shiftDays(endDay, -6)
                    val weekTxs = transactions.filter { it.dateKey in startDay..endDay }
                    val label = if (weekOffset == 0) "यह सप्ताह" else "सप्ताह ${index + 1}"
                    ChartBucket(
                        label = label,
                        income = roundCurrency(weekTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }),
                        expense = roundCurrency(weekTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
                    )
                }
            }
            ReportPeriod.MONTHLY -> {
                val currentMonthKey = DateUtils.monthKeyFromDateKey(todayDateKey)
                val earliestMonthKey = DateUtils.shiftMonths(currentMonthKey, -5)
                periodTransactions = transactions.filter {
                    val mKey = DateUtils.monthKeyFromDateKey(it.dateKey)
                    mKey in earliestMonthKey..currentMonthKey
                }
                buckets = (-5..0).map { offset ->
                    val mKey = DateUtils.shiftMonths(currentMonthKey, offset)
                    val monthTxs = transactions.filter { DateUtils.monthKeyFromDateKey(it.dateKey) == mKey }
                    ChartBucket(
                        label = DateUtils.formatShortMonthHindi(mKey),
                        income = roundCurrency(monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }),
                        expense = roundCurrency(monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
                    )
                }
            }
            ReportPeriod.YEARLY -> {
                periodTransactions = transactions.filter { DateUtils.yearOf(it.dateKey) == selectedYear }
                buckets = (1..12).map { month1Based ->
                    val mKey = selectedYear * 100 + month1Based
                    val monthTxs = periodTransactions.filter { DateUtils.monthKeyFromDateKey(it.dateKey) == mKey }
                    ChartBucket(
                        label = DateUtils.formatShortMonthIndexHindi(month1Based),
                        income = roundCurrency(monthTxs.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }),
                        expense = roundCurrency(monthTxs.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount })
                    )
                }
            }
        }

        val totalReceived = roundCurrency(
            periodTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        )
        val totalSpent = roundCurrency(
            periodTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        )
        val periodNet = roundCurrency(totalReceived - totalSpent)

        val categoryTotals = if (totalSpent > 0.0) {
            ExpenseCategory.entries.mapNotNull { cat ->
                val catAmount = roundCurrency(
                    periodTransactions
                        .filter { it.type == TransactionType.EXPENSE && it.category == cat }
                        .sumOf { it.amount }
                )
                if (catAmount > 0.0) {
                    CategoryTotal(
                        category = cat,
                        amount = catAmount,
                        percentage = (catAmount / totalSpent).toFloat().coerceIn(0f, 1f)
                    )
                } else null
            }.sortedByDescending { it.amount }
        } else {
            emptyList()
        }

        return ReportSummary(
            period = period,
            selectedYear = selectedYear,
            totalReceived = totalReceived,
            totalSpent = totalSpent,
            periodNet = periodNet,
            remainingOverallBalance = remainingOverallBalance,
            buckets = buckets,
            categoryTotals = categoryTotals
        )
    }

    fun calculateDebtSummary(debts: List<DebtEntry>): DebtSummary {
        var toReceive = 0.0
        var toPay = 0.0
        var settledCount = 0
        var pendingCount = 0

        for (d in debts) {
            val amt = if (d.amount.isFinite() && d.amount > 0.0) roundCurrency(d.amount) else 0.0
            if (d.status == DebtStatus.SETTLED) {
                settledCount++
            } else {
                pendingCount++
                when (d.type) {
                    DebtType.LENT -> toReceive = roundCurrency(toReceive + amt)
                    DebtType.BORROWED -> toPay = roundCurrency(toPay + amt)
                }
            }
        }
        return DebtSummary(
            pendingToReceive = toReceive,
            pendingToPay = toPay,
            settledCount = settledCount,
            pendingCount = pendingCount
        )
    }

    fun isDuplicateTransaction(
        candidate: HisabTransaction,
        existing: List<HisabTransaction>
    ): Boolean {
        val normParty = candidate.partyName.trim().lowercase()
        val normNotes = candidate.notes.trim().lowercase()
        return existing.any { item ->
            item.id != candidate.id &&
                item.type == candidate.type &&
                abs(item.amount - candidate.amount) < 0.005 &&
                item.dateKey == candidate.dateKey &&
                item.timeMinutes == candidate.timeMinutes &&
                item.account == candidate.account &&
                item.toAccount == candidate.toAccount &&
                item.category == candidate.category &&
                item.incomeCategory == candidate.incomeCategory &&
                item.partyName.trim().lowercase() == normParty &&
                item.notes.trim().lowercase() == normNotes
        }
    }
}
