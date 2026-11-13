package com.example.domain.model

enum class TransactionType(val labelHi: String) {
    INCOME("आमदनी"),
    EXPENSE("खर्च"),
    TRANSFER("ट्रांसफ़र")
}

enum class PaymentAccount(val labelHi: String, val key: String) {
    CASH("नकद", "CASH"),
    BANK("बैंक खाता", "BANK"),
    UPI("UPI", "UPI");

    companion object {
        fun fromKeyOrLabel(value: String?): PaymentAccount {
            if (value.isNullOrBlank()) return CASH
            val normalized = value.trim()
            return entries.firstOrNull {
                it.key.equals(normalized, ignoreCase = true) ||
                    it.labelHi.equals(normalized, ignoreCase = true) ||
                    it.name.equals(normalized, ignoreCase = true)
            } ?: CASH
        }
    }
}

enum class ExpenseCategory(val labelHi: String, val key: String) {
    FOOD("खाना", "FOOD"),
    TRAVEL("यात्रा", "TRAVEL"),
    EDUCATION("पढ़ाई", "EDUCATION"),
    MOBILE("मोबाइल", "MOBILE"),
    HOME("घर", "HOME"),
    SHOPPING("खरीदारी", "SHOPPING"),
    OTHER("अन्य", "OTHER");

    companion object {
        fun fromKeyOrLabel(value: String?): ExpenseCategory {
            if (value.isNullOrBlank()) return OTHER
            val normalized = value.trim()
            return entries.firstOrNull {
                it.key.equals(normalized, ignoreCase = true) ||
                    it.labelHi.equals(normalized, ignoreCase = true) ||
                    it.name.equals(normalized, ignoreCase = true)
            } ?: OTHER
        }
    }
}

enum class IncomeCategory(val labelHi: String, val key: String) {
    SALARY("वेतन", "SALARY"),
    BUSINESS("व्यापार", "BUSINESS"),
    FREELANCE("कमाई/मेहनताना", "FREELANCE"),
    INTEREST("ब्याज/रिटर्न", "INTEREST"),
    GIFT("उपहार/मदद", "GIFT"),
    OTHER("अन्य", "OTHER");

    companion object {
        fun fromKeyOrLabel(value: String?): IncomeCategory {
            if (value.isNullOrBlank()) return OTHER
            val normalized = value.trim()
            return entries.firstOrNull {
                it.key.equals(normalized, ignoreCase = true) ||
                    it.labelHi.equals(normalized, ignoreCase = true) ||
                    it.name.equals(normalized, ignoreCase = true)
            } ?: OTHER
        }
    }
}

data class AccountOpeningBalances(
    val cash: Double = 0.0,
    val bank: Double = 0.0,
    val upi: Double = 0.0
) {
    val total: Double
        get() = cash + bank + upi

    fun forAccount(account: PaymentAccount): Double = when (account) {
        PaymentAccount.CASH -> cash
        PaymentAccount.BANK -> bank
        PaymentAccount.UPI -> upi
    }
}

data class HisabTransaction(
    val id: Long = 0L,
    val type: TransactionType,
    val amount: Double,
    val dateKey: Int, // yyyyMMdd e.g. 20261112
    val partyName: String,
    val category: ExpenseCategory? = null, // Used for EXPENSE
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis(),
    val timeMinutes: Int = 720, // Minutes from midnight (0..1439)
    val account: PaymentAccount = PaymentAccount.CASH,
    val toAccount: PaymentAccount? = null, // Used for TRANSFER
    val incomeCategory: IncomeCategory? = null // Used for INCOME
) {
    val displayCategoryLabel: String
        get() = when (type) {
            TransactionType.INCOME -> (incomeCategory ?: IncomeCategory.OTHER).labelHi
            TransactionType.EXPENSE -> (category ?: ExpenseCategory.OTHER).labelHi
            TransactionType.TRANSFER -> "खाता ट्रांसफ़र"
        }
}

data class LedgerEntry(
    val transaction: HisabTransaction,
    val balanceAfter: Double,
    val accountBalanceAfter: Double = 0.0
)

data class AccountBalanceSummary(
    val account: PaymentAccount,
    val openingBalance: Double,
    val income: Double,
    val expense: Double,
    val transfersIn: Double,
    val transfersOut: Double,
    val currentBalance: Double
)

data class LedgerSummary(
    val openingBalance: Double = 0.0,
    val accountOpenings: AccountOpeningBalances = AccountOpeningBalances(),
    val accountBalances: List<AccountBalanceSummary> = emptyList(),
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val currentBalance: Double = 0.0,
    val todayIncome: Double = 0.0,
    val todayExpense: Double = 0.0,
    val monthIncome: Double = 0.0,
    val monthExpense: Double = 0.0
)

data class MonthlyNotebookSummary(
    val monthKey: Int,
    val broughtForwardBalance: Double,
    val monthIncome: Double,
    val monthExpense: Double,
    val closingBalance: Double
)

enum class ReportPeriod(val labelHi: String) {
    DAILY("दैनिक"),
    WEEKLY("साप्ताहिक"),
    MONTHLY("मासिक"),
    YEARLY("वार्षिक")
}

data class ChartBucket(
    val label: String,
    val income: Double,
    val expense: Double
)

data class CategoryTotal(
    val category: ExpenseCategory,
    val amount: Double,
    val percentage: Float
)

data class ReportSummary(
    val period: ReportPeriod,
    val selectedYear: Int,
    val totalReceived: Double,
    val totalSpent: Double,
    val periodNet: Double,
    val remainingOverallBalance: Double,
    val buckets: List<ChartBucket>,
    val categoryTotals: List<CategoryTotal>
)

// Debt Tracker (strictly separate from Income & Expenses)
enum class DebtType(val labelHi: String) {
    BORROWED("उधार लिया"),
    LENT("उधार दिया")
}

enum class DebtStatus(val labelHi: String) {
    PENDING("बाकी"),
    SETTLED("चुकाया गया")
}

data class DebtEntry(
    val id: Long = 0L,
    val type: DebtType,
    val personName: String,
    val amount: Double,
    val dateKey: Int,
    val dueDateKey: Int? = null,
    val status: DebtStatus = DebtStatus.PENDING,
    val settledDateKey: Int? = null,
    val notes: String = "",
    val createdAtMillis: Long = System.currentTimeMillis()
)

data class DebtSummary(
    val pendingToReceive: Double = 0.0, // Total LENT & PENDING
    val pendingToPay: Double = 0.0,     // Total BORROWED & PENDING
    val settledCount: Int = 0,
    val pendingCount: Int = 0
)

// Recurring Reminders
enum class RecurrenceFrequency(val labelHi: String, val key: String) {
    DAILY("रोज़ाना", "DAILY"),
    WEEKLY("साप्ताहिक", "WEEKLY"),
    MONTHLY("मासिक", "MONTHLY"),
    YEARLY("वार्षिक", "YEARLY");

    companion object {
        fun fromKey(value: String?): RecurrenceFrequency {
            return entries.firstOrNull { it.key.equals(value, ignoreCase = true) } ?: MONTHLY
        }
    }
}

data class RecurringEntry(
    val id: Long = 0L,
    val title: String,
    val type: TransactionType, // INCOME or EXPENSE
    val amount: Double,
    val account: PaymentAccount = PaymentAccount.CASH,
    val expenseCategory: ExpenseCategory? = null,
    val incomeCategory: IncomeCategory? = null,
    val frequency: RecurrenceFrequency = RecurrenceFrequency.MONTHLY,
    val nextDueDateKey: Int,
    val notes: String = "",
    val isActive: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
)
