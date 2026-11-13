package com.example

import com.example.data.local.PinLockConfig
import com.example.data.repository.HisabDataSource
import com.example.domain.model.AccountOpeningBalances
import com.example.domain.model.DebtEntry
import com.example.domain.model.DebtStatus
import com.example.domain.model.DebtType
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.HisabTransaction
import com.example.domain.model.IncomeCategory
import com.example.domain.model.PaymentAccount
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.RecurringEntry
import com.example.domain.model.ReportPeriod
import com.example.domain.model.TransactionType
import com.example.domain.util.PinHashUtil
import com.example.ui.viewmodel.HisabViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HisabViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `viewmodel handles multi-account openings transfers duplicate guard monthly notebook yearly reports debts recurring and pin`() = runTest {
        val fakeRepo = FakeHisabDataSource()
        val vm = HisabViewModel(fakeRepo, todayProvider = { 20261112 }, timeProvider = { 600 })
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.uiState.collect()
        }

        // 1. Multi-account opening balances & validation
        assertFalse(vm.saveAccountOpenings("-100", "0", "0"))
        assertTrue(vm.saveAccountOpenings("1000", "2000", "500"))

        // 2. Reject invalid / self-transfer / duplicate entries
        assertFalse(vm.saveTransaction(TransactionType.INCOME, "-50", 20261112, "वेतन", null, ""))
        assertFalse(vm.saveTransaction(TransactionType.TRANSFER, "500", 20261112, "", null, "", 600, PaymentAccount.BANK, PaymentAccount.BANK))

        assertTrue(vm.saveTransaction(TransactionType.INCOME, "500", 20261112, "सैलरी", null, "नवंबर", 600, PaymentAccount.BANK, null, IncomeCategory.SALARY))
        // Exact duplicate blocked
        assertFalse(vm.saveTransaction(TransactionType.INCOME, "500", 20261112, "सैलरी", null, "नवंबर", 600, PaymentAccount.BANK, null, IncomeCategory.SALARY))

        assertTrue(vm.saveTransaction(TransactionType.EXPENSE, "200", 20261112, "रमेश होटल", ExpenseCategory.FOOD, "खाना", 700, PaymentAccount.UPI))
        assertTrue(vm.saveTransaction(TransactionType.EXPENSE, "100", 20261015, "बस", ExpenseCategory.TRAVEL, "टिकट", 800, PaymentAccount.CASH))
        assertTrue(vm.saveTransaction(TransactionType.TRANSFER, "400", 20261112, "बैंक से UPI", null, "", 900, PaymentAccount.BANK, PaymentAccount.UPI))

        val state1 = vm.uiState.value
        assertEquals(4, state1.allEntries.size)
        assertEquals(3500.0, state1.summary.openingBalance, 0.001)
        assertEquals(500.0, state1.summary.totalIncome, 0.001)
        assertEquals(300.0, state1.summary.totalExpense, 0.001)
        assertEquals(3700.0, state1.summary.currentBalance, 0.001) // Transfer did NOT alter wealth!

        val upiBal = state1.summary.accountBalances.first { it.account == PaymentAccount.UPI }.currentBalance
        assertEquals(700.0, upiBal, 0.001) // 500 + 400 - 200

        // 3. Monthly notebook navigation (November 2026 -> October 2026)
        assertEquals(202611, state1.selectedNotebookMonthKey)
        assertEquals(3, state1.filteredEntries.size) // 3 entries in Nov 2026
        assertEquals(3400.0, state1.monthlyNotebookSummary.broughtForwardBalance, 0.001) // 3500 - 100 (Oct expense)
        vm.previousNotebookMonth()
        assertEquals(202610, vm.uiState.value.selectedNotebookMonthKey)
        assertEquals(1, vm.uiState.value.filteredEntries.size)
        vm.nextNotebookMonth()
        assertEquals(202611, vm.uiState.value.selectedNotebookMonthKey)

        // 4. Edit & Delete recalculate without double-counting
        val foodTx = vm.uiState.value.allEntries.first { it.transaction.partyName == "रमेश होटल" }.transaction
        vm.openEditTransaction(foodTx)
        assertTrue(vm.saveTransaction(TransactionType.EXPENSE, "300", 20261112, "रमेश होटल", ExpenseCategory.FOOD, "खाना", 700, PaymentAccount.UPI))
        assertEquals(3600.0, vm.uiState.value.summary.currentBalance, 0.001)

        vm.requestDeleteTransaction(foodTx)
        vm.confirmDeleteTransaction()
        assertEquals(3900.0, vm.uiState.value.summary.currentBalance, 0.001)

        // 5. Debt Tracker stays strictly separated from total wealth & income/expense
        assertTrue(vm.saveDebt(DebtType.LENT, "महेश", "1000", 20261112, null, "मदद"))
        assertTrue(vm.saveDebt(DebtType.BORROWED, "दीपक", "400", 20261112, null, ""))
        val debtState = vm.uiState.value
        assertEquals(1000.0, debtState.debtSummary.pendingToReceive, 0.001)
        assertEquals(400.0, debtState.debtSummary.pendingToPay, 0.001)
        assertEquals(3900.0, debtState.summary.currentBalance, 0.001) // Unchanged!
        vm.toggleDebtStatus(debtState.debts.first { it.personName == "महेश" })
        assertEquals(0.0, vm.uiState.value.debtSummary.pendingToReceive, 0.001)
        assertEquals(DebtStatus.SETTLED, vm.uiState.value.debts.first { it.personName == "महेश" }.status)

        // 6. Recurring reminders due detection and 1-tap recording
        assertTrue(vm.saveRecurring("किराया", TransactionType.EXPENSE, "500", PaymentAccount.BANK, ExpenseCategory.HOME, null, RecurrenceFrequency.MONTHLY, 20261112, ""))
        assertEquals(1, vm.uiState.value.dueRecurringEntries.size)
        vm.recordDueRecurring(vm.uiState.value.dueRecurringEntries.first())
        assertEquals(0, vm.uiState.value.dueRecurringEntries.size)
        assertEquals(20261212, vm.uiState.value.recurringEntries.first().nextDueDateKey)
        assertEquals(3400.0, vm.uiState.value.summary.currentBalance, 0.001) // 3900 - 500

        // 7. Yearly Report
        vm.selectReportPeriod(ReportPeriod.YEARLY)
        assertEquals(12, vm.uiState.value.reportSummary.buckets.size)
        assertEquals(500.0, vm.uiState.value.reportSummary.totalReceived, 0.001)
        assertEquals(600.0, vm.uiState.value.reportSummary.totalSpent, 0.001) // 100 Oct + 500 recurring Nov

        // 8. PIN lock enable, lock, wrong PIN rejection, unlock, disable
        assertFalse(vm.enablePin("12"))
        assertTrue(vm.enablePin("2580"))
        assertTrue(vm.uiState.value.isPinEnabled)
        assertFalse(vm.uiState.value.isAppLocked)
        vm.lockAppNow()
        assertTrue(vm.uiState.value.isAppLocked)
        assertFalse(vm.verifyAndUnlockPin("9999"))
        assertTrue(vm.uiState.value.isAppLocked)
        assertTrue(vm.verifyAndUnlockPin("2580"))
        assertFalse(vm.uiState.value.isAppLocked)
        vm.disablePin()
        assertFalse(vm.uiState.value.isPinEnabled)

        collectJob.cancel()
    }
}

private class FakeHisabDataSource : HisabDataSource {
    private var nextId = 1L
    private var nextDebtId = 1L
    private var nextRecId = 1L

    private val _transactions = MutableStateFlow<List<HisabTransaction>>(emptyList())
    private val _openings = MutableStateFlow(AccountOpeningBalances())
    private val _openingBalance = MutableStateFlow(0.0)
    private val _debts = MutableStateFlow<List<DebtEntry>>(emptyList())
    private val _recurring = MutableStateFlow<List<RecurringEntry>>(emptyList())
    private val _pin = MutableStateFlow(PinLockConfig())

    override val transactions: Flow<List<HisabTransaction>> = _transactions
    override val openingBalance: Flow<Double> = _openingBalance
    override val accountOpenings: Flow<AccountOpeningBalances> = _openings
    override val debts: Flow<List<DebtEntry>> = _debts
    override val recurringEntries: Flow<List<RecurringEntry>> = _recurring
    override val pinLockConfig: Flow<PinLockConfig> = _pin

    override suspend fun addTransaction(tx: HisabTransaction): Long {
        val id = nextId++
        _transactions.value = _transactions.value + tx.copy(id = id)
        return id
    }

    override suspend fun updateTransaction(tx: HisabTransaction) {
        _transactions.value = _transactions.value.map { if (it.id == tx.id) tx else it }
    }

    override suspend fun deleteTransaction(id: Long) {
        _transactions.value = _transactions.value.filterNot { it.id == id }
    }

    override suspend fun setOpeningBalance(amount: Double) {
        _openingBalance.value = amount
        _openings.value = _openings.value.copy(cash = amount)
    }

    override suspend fun setAccountOpenings(openings: AccountOpeningBalances) {
        _openings.value = openings
        _openingBalance.value = openings.total
    }

    override suspend fun addDebt(debt: DebtEntry): Long {
        val id = nextDebtId++
        _debts.value = _debts.value + debt.copy(id = id)
        return id
    }

    override suspend fun updateDebt(debt: DebtEntry) {
        _debts.value = _debts.value.map { if (it.id == debt.id) debt else it }
    }

    override suspend fun deleteDebt(id: Long) {
        _debts.value = _debts.value.filterNot { it.id == id }
    }

    override suspend fun addRecurring(entry: RecurringEntry): Long {
        val id = nextRecId++
        _recurring.value = _recurring.value + entry.copy(id = id)
        return id
    }

    override suspend fun updateRecurring(entry: RecurringEntry) {
        _recurring.value = _recurring.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun deleteRecurring(id: Long) {
        _recurring.value = _recurring.value.filterNot { it.id == id }
    }

    override suspend fun setPin(pin: String) {
        val salt = PinHashUtil.generateSalt()
        _pin.value = PinLockConfig(isEnabled = true, salt = salt, hash = PinHashUtil.hashPin(pin, salt))
    }

    override suspend fun disablePin() {
        _pin.value = PinLockConfig()
    }

    override suspend fun replaceAllData(
        openingBalance: Double?,
        transactions: List<HisabTransaction>,
        accountOpenings: AccountOpeningBalances?,
        debts: List<DebtEntry>,
        recurringEntries: List<RecurringEntry>
    ) {
        _transactions.value = transactions.map { it.copy(id = nextId++) }
        _debts.value = debts.map { it.copy(id = nextDebtId++) }
        _recurring.value = recurringEntries.map { it.copy(id = nextRecId++) }
        if (accountOpenings != null) {
            setAccountOpenings(accountOpenings)
        } else if (openingBalance != null) {
            setAccountOpenings(AccountOpeningBalances(cash = openingBalance))
        }
    }

    override suspend fun mergeTransactions(
        openingBalance: Double?,
        transactions: List<HisabTransaction>,
        accountOpenings: AccountOpeningBalances?,
        debts: List<DebtEntry>,
        recurringEntries: List<RecurringEntry>
    ) {
        _transactions.value = _transactions.value + transactions.map { it.copy(id = nextId++) }
        _debts.value = _debts.value + debts.map { it.copy(id = nextDebtId++) }
        _recurring.value = _recurring.value + recurringEntries.map { it.copy(id = nextRecId++) }
        if (accountOpenings != null) {
            setAccountOpenings(accountOpenings)
        } else if (openingBalance != null) {
            setOpeningBalance(openingBalance)
        }
    }

    override suspend fun clearAllData() {
        _transactions.value = emptyList()
        _debts.value = emptyList()
        _recurring.value = emptyList()
        setAccountOpenings(AccountOpeningBalances())
    }
}
