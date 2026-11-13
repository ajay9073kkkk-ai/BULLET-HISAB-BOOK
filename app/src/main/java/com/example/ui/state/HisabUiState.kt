package com.example.ui.state

import com.example.domain.model.DebtEntry
import com.example.domain.model.DebtSummary
import com.example.domain.model.DebtType
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.HisabTransaction
import com.example.domain.model.LedgerEntry
import com.example.domain.model.LedgerSummary
import com.example.domain.model.MonthlyNotebookSummary
import com.example.domain.model.PaymentAccount
import com.example.domain.model.RecurringEntry
import com.example.domain.model.ReportPeriod
import com.example.domain.model.ReportSummary
import com.example.domain.model.TransactionType
import com.example.domain.util.DateUtils

data class FilterState(
    val query: String = "",
    val typeFilter: TransactionType? = null,
    val accountFilter: PaymentAccount? = null,
    val categoryFilter: ExpenseCategory? = null,
    val monthKeyFilter: Int? = null,
    val dateKeyFilter: Int? = null
) {
    val hasActiveFilters: Boolean
        get() = query.isNotBlank() ||
            typeFilter != null ||
            accountFilter != null ||
            categoryFilter != null ||
            monthKeyFilter != null ||
            dateKeyFilter != null
}

data class HisabUiState(
    val isLoading: Boolean = true,
    val todayDateKey: Int = DateUtils.todayDateKey(),
    val summary: LedgerSummary = LedgerSummary(),
    val allEntries: List<LedgerEntry> = emptyList(),
    val filteredEntries: List<LedgerEntry> = emptyList(),
    val availableMonthKeys: List<Int> = emptyList(),
    val filterState: FilterState = FilterState(),
    // Monthly Notebook state
    val selectedNotebookMonthKey: Int = DateUtils.monthKeyFromDateKey(DateUtils.todayDateKey()),
    val showAllMonthsInDiary: Boolean = false,
    val monthlyNotebookSummary: MonthlyNotebookSummary = MonthlyNotebookSummary(
        monthKey = DateUtils.monthKeyFromDateKey(DateUtils.todayDateKey()),
        broughtForwardBalance = 0.0,
        monthIncome = 0.0,
        monthExpense = 0.0,
        closingBalance = 0.0
    ),
    // Reports
    val reportPeriod: ReportPeriod = ReportPeriod.DAILY,
    val reportSelectedYear: Int = DateUtils.yearOf(DateUtils.todayDateKey()),
    val reportSummary: ReportSummary = ReportSummary(
        period = ReportPeriod.DAILY,
        selectedYear = DateUtils.yearOf(DateUtils.todayDateKey()),
        totalReceived = 0.0,
        totalSpent = 0.0,
        periodNet = 0.0,
        remainingOverallBalance = 0.0,
        buckets = emptyList(),
        categoryTotals = emptyList()
    ),
    // Debt Tracker
    val debts: List<DebtEntry> = emptyList(),
    val debtSummary: DebtSummary = DebtSummary(),
    val debtTypeFilter: DebtType? = null,
    // Recurring Reminders
    val recurringEntries: List<RecurringEntry> = emptyList(),
    val dueRecurringEntries: List<RecurringEntry> = emptyList(),
    // Privacy PIN Lock
    val isPinEnabled: Boolean = false,
    val isAppLocked: Boolean = false
) {
    val recentEntries: List<LedgerEntry>
        get() = allEntries.take(5)

    val filteredDebts: List<DebtEntry>
        get() = if (debtTypeFilter == null) debts else debts.filter { it.type == debtTypeFilter }
}

data class TransactionSheetState(
    val isOpen: Boolean = false,
    val type: TransactionType = TransactionType.EXPENSE,
    val editingTransaction: HisabTransaction? = null
)

data class DebtSheetState(
    val isOpen: Boolean = false,
    val initialType: DebtType = DebtType.LENT,
    val editingDebt: DebtEntry? = null
)

data class RecurringSheetState(
    val isOpen: Boolean = false,
    val editingRecurring: RecurringEntry? = null
)
