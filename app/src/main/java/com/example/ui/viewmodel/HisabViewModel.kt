package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
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
import com.example.domain.util.BackupPayload
import com.example.domain.util.BackupSerializer
import com.example.domain.util.CurrencyFormatter
import com.example.domain.util.DateUtils
import com.example.domain.util.HisabCalculator
import com.example.domain.util.PinHashUtil
import com.example.ui.state.DebtSheetState
import com.example.ui.state.FilterState
import com.example.ui.state.HisabUiState
import com.example.ui.state.RecurringSheetState
import com.example.ui.state.TransactionSheetState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class HisabViewModel(
    private val repository: HisabDataSource,
    private val todayProvider: () -> Int = { DateUtils.todayDateKey() },
    private val timeProvider: () -> Int = { DateUtils.currentTimeMinutes() }
) : ViewModel() {

    private val initialToday = todayProvider()
    private val filterState = MutableStateFlow(FilterState())
    private val selectedNotebookMonthKey = MutableStateFlow(DateUtils.monthKeyFromDateKey(initialToday))
    private val showAllMonthsInDiary = MutableStateFlow(false)
    private val reportPeriod = MutableStateFlow(ReportPeriod.DAILY)
    private val reportSelectedYear = MutableStateFlow(DateUtils.yearOf(initialToday))
    private val debtTypeFilter = MutableStateFlow<DebtType?>(null)

    private val pinUnlockedThisSession = MutableStateFlow(false)
    private var latestPinConfig = PinLockConfig()

    private val _sheetState = MutableStateFlow(TransactionSheetState())
    val sheetState: StateFlow<TransactionSheetState> = _sheetState.asStateFlow()

    private val _debtSheetState = MutableStateFlow(DebtSheetState())
    val debtSheetState: StateFlow<DebtSheetState> = _debtSheetState.asStateFlow()

    private val _recurringSheetState = MutableStateFlow(RecurringSheetState())
    val recurringSheetState: StateFlow<RecurringSheetState> = _recurringSheetState.asStateFlow()

    private val _openingBalanceDialogOpen = MutableStateFlow(false)
    val openingBalanceDialogOpen: StateFlow<Boolean> = _openingBalanceDialogOpen.asStateFlow()

    private val _pinDialogOpen = MutableStateFlow(false)
    val pinDialogOpen: StateFlow<Boolean> = _pinDialogOpen.asStateFlow()

    private val _deleteCandidate = MutableStateFlow<HisabTransaction?>(null)
    val deleteCandidate: StateFlow<HisabTransaction?> = _deleteCandidate.asStateFlow()

    private val _deleteDebtCandidate = MutableStateFlow<DebtEntry?>(null)
    val deleteDebtCandidate: StateFlow<DebtEntry?> = _deleteDebtCandidate.asStateFlow()

    private val _deleteRecurringCandidate = MutableStateFlow<RecurringEntry?>(null)
    val deleteRecurringCandidate: StateFlow<RecurringEntry?> = _deleteRecurringCandidate.asStateFlow()

    private val _pendingRestore = MutableStateFlow<BackupPayload?>(null)
    val pendingRestore: StateFlow<BackupPayload?> = _pendingRestore.asStateFlow()

    private val _snackbarMessages = MutableSharedFlow<String>(extraBufferCapacity = 10)
    val snackbarMessages: SharedFlow<String> = _snackbarMessages.asSharedFlow()

    private data class CoreLedgerInputs(
        val transactions: List<HisabTransaction>,
        val accountOpenings: AccountOpeningBalances,
        val debts: List<DebtEntry>,
        val recurring: List<RecurringEntry>,
        val pinConfig: PinLockConfig
    )

    private data class ViewControls(
        val filters: FilterState,
        val notebookMonthKey: Int,
        val showAllMonths: Boolean,
        val period: ReportPeriod,
        val reportYear: Int,
        val debtFilter: DebtType?,
        val pinUnlocked: Boolean
    )

    private val coreInputsFlow = combine(
        repository.transactions,
        repository.accountOpenings,
        repository.debts,
        repository.recurringEntries,
        repository.pinLockConfig
    ) { transactions, openings, debts, recurring, pinConfig ->
        latestPinConfig = pinConfig
        CoreLedgerInputs(transactions, openings, debts, recurring, pinConfig)
    }

    private val viewControlsFlow = combine(
        combine(filterState, selectedNotebookMonthKey, showAllMonthsInDiary) { f, m, all -> Triple(f, m, all) },
        combine(reportPeriod, reportSelectedYear, debtTypeFilter, pinUnlockedThisSession) { p, y, df, unlocked ->
            listOf(p, y, df, unlocked)
        }
    ) { triple, rest ->
        @Suppress("UNCHECKED_CAST")
        ViewControls(
            filters = triple.first,
            notebookMonthKey = triple.second,
            showAllMonths = triple.third,
            period = rest[0] as ReportPeriod,
            reportYear = rest[1] as Int,
            debtFilter = rest[2] as DebtType?,
            pinUnlocked = rest[3] as Boolean
        )
    }

    val uiState: StateFlow<HisabUiState> = combine(
        coreInputsFlow,
        viewControlsFlow
    ) { core, controls ->
        val todayKey = todayProvider()
        val (summary, allEntries) = HisabCalculator.calculateLedger(
            transactions = core.transactions,
            accountOpenings = core.accountOpenings,
            todayDateKey = todayKey
        )

        val effectiveMonthFilter = when {
            controls.filters.monthKeyFilter != null -> controls.filters.monthKeyFilter
            controls.filters.query.isNotBlank() || controls.showAllMonths -> null
            else -> controls.notebookMonthKey
        }

        val filtered = HisabCalculator.filterEntries(
            entries = allEntries,
            query = controls.filters.query,
            typeFilter = controls.filters.typeFilter,
            categoryFilter = controls.filters.categoryFilter,
            monthKeyFilter = effectiveMonthFilter,
            dateKeyFilter = controls.filters.dateKeyFilter,
            accountFilter = controls.filters.accountFilter
        )

        val monthlyNotebookSummary = HisabCalculator.calculateMonthlyNotebook(
            allTransactions = core.transactions,
            totalOpeningBalance = core.accountOpenings.total,
            monthKey = controls.notebookMonthKey
        )

        val availableMonths = (core.transactions.map { DateUtils.monthKeyFromDateKey(it.dateKey) } +
            DateUtils.monthKeyFromDateKey(todayKey))
            .distinct()
            .sortedDescending()

        val report = HisabCalculator.calculateReport(
            transactions = core.transactions,
            openingBalance = core.accountOpenings.total,
            period = controls.period,
            todayDateKey = todayKey,
            selectedYear = controls.reportYear
        )

        val debtSummary = HisabCalculator.calculateDebtSummary(core.debts)
        val dueRecurring = core.recurring.filter { it.isActive && it.nextDueDateKey <= todayKey }
        val pinEnabled = core.pinConfig.isEnabled && core.pinConfig.hash.isNotBlank()
        val isLocked = pinEnabled && !controls.pinUnlocked

        HisabUiState(
            isLoading = false,
            todayDateKey = todayKey,
            summary = summary,
            allEntries = allEntries,
            filteredEntries = filtered,
            availableMonthKeys = availableMonths,
            filterState = controls.filters,
            selectedNotebookMonthKey = controls.notebookMonthKey,
            showAllMonthsInDiary = controls.showAllMonths,
            monthlyNotebookSummary = monthlyNotebookSummary,
            reportPeriod = controls.period,
            reportSelectedYear = controls.reportYear,
            reportSummary = report,
            debts = core.debts,
            debtSummary = debtSummary,
            debtTypeFilter = controls.debtFilter,
            recurringEntries = core.recurring,
            dueRecurringEntries = dueRecurring,
            isPinEnabled = pinEnabled,
            isAppLocked = isLocked
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HisabUiState()
    )

    // --- Transaction CRUD & Validation ---
    fun openAddTransaction(type: TransactionType) {
        _sheetState.value = TransactionSheetState(
            isOpen = true,
            type = type,
            editingTransaction = null
        )
    }

    fun openEditTransaction(transaction: HisabTransaction) {
        _sheetState.value = TransactionSheetState(
            isOpen = true,
            type = transaction.type,
            editingTransaction = transaction
        )
    }

    fun closeTransactionSheet() {
        _sheetState.value = _sheetState.value.copy(isOpen = false, editingTransaction = null)
    }

    fun currentDefaultTimeMinutes(): Int = timeProvider()

    fun saveTransaction(
        type: TransactionType,
        amountText: String,
        dateKey: Int,
        partyName: String,
        category: ExpenseCategory?,
        notes: String,
        timeMinutes: Int = timeProvider(),
        account: PaymentAccount = PaymentAccount.CASH,
        toAccount: PaymentAccount? = null,
        incomeCategory: IncomeCategory? = null
    ): Boolean {
        val validAmount = CurrencyFormatter.parseValidTransactionAmount(amountText)
        if (validAmount == null) {
            _snackbarMessages.tryEmit("कृपया सही रकम दर्ज करें (0 से अधिक)")
            return false
        }
        if (type == TransactionType.TRANSFER && (toAccount == null || toAccount == account)) {
            _snackbarMessages.tryEmit("ट्रांसफ़र के लिए दो अलग-अलग खाते चुनें")
            return false
        }

        val cleanParty = partyName.trim().ifBlank {
            when (type) {
                TransactionType.INCOME -> "अन्य स्रोत"
                TransactionType.EXPENSE -> "अन्य खर्च"
                TransactionType.TRANSFER -> "${account.labelHi} → ${toAccount?.labelHi}"
            }
        }
        val cleanNotes = notes.trim()
        val editing = _sheetState.value.editingTransaction

        val candidate = HisabTransaction(
            id = editing?.id ?: 0L,
            type = type,
            amount = validAmount,
            dateKey = dateKey,
            partyName = cleanParty,
            category = if (type == TransactionType.EXPENSE) (category ?: ExpenseCategory.OTHER) else null,
            notes = cleanNotes,
            createdAtMillis = editing?.createdAtMillis ?: System.currentTimeMillis(),
            timeMinutes = timeMinutes.coerceIn(0, 1439),
            account = account,
            toAccount = if (type == TransactionType.TRANSFER) toAccount else null,
            incomeCategory = if (type == TransactionType.INCOME) (incomeCategory ?: IncomeCategory.OTHER) else null
        )

        val existingTransactions = uiState.value.allEntries.map { it.transaction }
        if (editing == null && HisabCalculator.isDuplicateTransaction(candidate, existingTransactions)) {
            _snackbarMessages.tryEmit("यह एंट्री पहले से मौजूद है (डुप्लीकेट से बचाया गया)")
            return false
        }

        viewModelScope.launch {
            if (editing != null && editing.id > 0L) {
                repository.updateTransaction(candidate)
                _snackbarMessages.tryEmit("एंट्री अपडेट हो गई")
            } else {
                repository.addTransaction(candidate)
                val msg = when (type) {
                    TransactionType.INCOME -> "आमदनी सफलतापूर्वक सेव हुई"
                    TransactionType.EXPENSE -> "खर्च सफलतापूर्वक सेव हुआ"
                    TransactionType.TRANSFER -> "खाता ट्रांसफ़र सेव हुआ"
                }
                _snackbarMessages.tryEmit(msg)
            }
            closeTransactionSheet()
        }
        return true
    }

    fun requestDeleteTransaction(transaction: HisabTransaction) {
        _deleteCandidate.value = transaction
    }

    fun dismissDeleteDialog() {
        _deleteCandidate.value = null
    }

    fun confirmDeleteTransaction() {
        val target = _deleteCandidate.value ?: return
        _deleteCandidate.value = null
        viewModelScope.launch {
            repository.deleteTransaction(target.id)
            _snackbarMessages.tryEmit("एंट्री हटा दी गई")
        }
    }

    // --- Opening Balance (Single & Multi-Account) ---
    fun openOpeningBalanceDialog() {
        _openingBalanceDialogOpen.value = true
    }

    fun dismissOpeningBalanceDialog() {
        _openingBalanceDialogOpen.value = false
    }

    fun saveOpeningBalance(amountText: String): Boolean {
        val valid = CurrencyFormatter.parseValidOpeningBalance(amountText)
        if (valid == null) {
            _snackbarMessages.tryEmit("शुरुआती बैलेंस 0 या उससे अधिक होना चाहिए")
            return false
        }
        viewModelScope.launch {
            val current = uiState.value.summary.accountOpenings
            repository.setAccountOpenings(current.copy(cash = valid))
            _openingBalanceDialogOpen.value = false
            _snackbarMessages.tryEmit("शुरुआती बैलेंस अपडेट हो गया")
        }
        return true
    }

    fun saveAccountOpenings(cashText: String, bankText: String, upiText: String): Boolean {
        val cash = CurrencyFormatter.parseValidOpeningBalance(cashText.ifBlank { "0" })
        val bank = CurrencyFormatter.parseValidOpeningBalance(bankText.ifBlank { "0" })
        val upi = CurrencyFormatter.parseValidOpeningBalance(upiText.ifBlank { "0" })
        if (cash == null || bank == null || upi == null) {
            _snackbarMessages.tryEmit("सभी खातों का शुरुआती बैलेंस 0 या उससे अधिक होना चाहिए")
            return false
        }
        viewModelScope.launch {
            repository.setAccountOpenings(AccountOpeningBalances(cash = cash, bank = bank, upi = upi))
            _openingBalanceDialogOpen.value = false
            _snackbarMessages.tryEmit("तीनों खातों का शुरुआती बैलेंस अपडेट हो गया")
        }
        return true
    }

    // --- Monthly Notebook & Filters ---
    fun previousNotebookMonth() {
        selectedNotebookMonthKey.value = DateUtils.shiftMonths(selectedNotebookMonthKey.value, -1)
        showAllMonthsInDiary.value = false
    }

    fun nextNotebookMonth() {
        selectedNotebookMonthKey.value = DateUtils.shiftMonths(selectedNotebookMonthKey.value, 1)
        showAllMonthsInDiary.value = false
    }

    fun selectNotebookMonth(monthKey: Int) {
        selectedNotebookMonthKey.value = monthKey
        showAllMonthsInDiary.value = false
    }

    fun toggleShowAllMonthsInDiary(showAll: Boolean) {
        showAllMonthsInDiary.value = showAll
    }

    fun updateSearchQuery(query: String) {
        filterState.value = filterState.value.copy(query = query)
    }

    fun setTypeFilter(type: TransactionType?) {
        val nextCat = if (type != TransactionType.EXPENSE) null else filterState.value.categoryFilter
        filterState.value = filterState.value.copy(typeFilter = type, categoryFilter = nextCat)
    }

    fun setAccountFilter(account: PaymentAccount?) {
        filterState.value = filterState.value.copy(accountFilter = account)
    }

    fun setCategoryFilter(category: ExpenseCategory?) {
        filterState.value = filterState.value.copy(
            categoryFilter = category,
            typeFilter = if (category != null) TransactionType.EXPENSE else filterState.value.typeFilter
        )
    }

    fun setMonthFilter(monthKey: Int?) {
        filterState.value = filterState.value.copy(monthKeyFilter = monthKey)
        if (monthKey != null) {
            selectedNotebookMonthKey.value = monthKey
        }
    }

    fun setDateFilter(dateKey: Int?) {
        filterState.value = filterState.value.copy(dateKeyFilter = dateKey)
    }

    fun clearAllFilters() {
        filterState.value = FilterState()
        showAllMonthsInDiary.value = false
    }

    // --- Reports ---
    fun selectReportPeriod(period: ReportPeriod) {
        reportPeriod.value = period
    }

    fun shiftReportYear(delta: Int) {
        reportSelectedYear.value = (reportSelectedYear.value + delta).coerceIn(1970, 2100)
    }

    // --- Debt Tracker (Strictly separated from Income/Expense) ---
    fun setDebtTypeFilter(type: DebtType?) {
        debtTypeFilter.value = type
    }

    fun openAddDebt(type: DebtType) {
        _debtSheetState.value = DebtSheetState(isOpen = true, initialType = type, editingDebt = null)
    }

    fun openEditDebt(debt: DebtEntry) {
        _debtSheetState.value = DebtSheetState(isOpen = true, initialType = debt.type, editingDebt = debt)
    }

    fun closeDebtSheet() {
        _debtSheetState.value = _debtSheetState.value.copy(isOpen = false, editingDebt = null)
    }

    fun saveDebt(
        type: DebtType,
        personName: String,
        amountText: String,
        dateKey: Int,
        dueDateKey: Int?,
        notes: String
    ): Boolean {
        val validAmount = CurrencyFormatter.parseValidTransactionAmount(amountText)
        if (validAmount == null) {
            _snackbarMessages.tryEmit("कृपया सही उधार रकम दर्ज करें (0 से अधिक)")
            return false
        }
        val cleanPerson = personName.trim()
        if (cleanPerson.isBlank()) {
            _snackbarMessages.tryEmit("कृपया व्यक्ति का नाम लिखें")
            return false
        }
        val editing = _debtSheetState.value.editingDebt
        viewModelScope.launch {
            if (editing != null && editing.id > 0L) {
                repository.updateDebt(
                    editing.copy(
                        type = type,
                        personName = cleanPerson,
                        amount = validAmount,
                        dateKey = dateKey,
                        dueDateKey = dueDateKey,
                        notes = notes.trim()
                    )
                )
                _snackbarMessages.tryEmit("उधार रिकॉर्ड अपडेट हो गया")
            } else {
                repository.addDebt(
                    DebtEntry(
                        type = type,
                        personName = cleanPerson,
                        amount = validAmount,
                        dateKey = dateKey,
                        dueDateKey = dueDateKey,
                        status = DebtStatus.PENDING,
                        notes = notes.trim()
                    )
                )
                _snackbarMessages.tryEmit("उधार रिकॉर्ड सेव हो गया")
            }
            closeDebtSheet()
        }
        return true
    }

    fun toggleDebtStatus(debt: DebtEntry) {
        viewModelScope.launch {
            val nextStatus = if (debt.status == DebtStatus.PENDING) DebtStatus.SETTLED else DebtStatus.PENDING
            val settledDate = if (nextStatus == DebtStatus.SETTLED) todayProvider() else null
            repository.updateDebt(debt.copy(status = nextStatus, settledDateKey = settledDate))
            val msg = if (nextStatus == DebtStatus.SETTLED) {
                "उधार चुकाया गया (Settled) मार्क हुआ"
            } else {
                "उधार फिर से बाकी (Pending) मार्क हुआ"
            }
            _snackbarMessages.tryEmit(msg)
        }
    }

    fun requestDeleteDebt(debt: DebtEntry) {
        _deleteDebtCandidate.value = debt
    }

    fun dismissDeleteDebtDialog() {
        _deleteDebtCandidate.value = null
    }

    fun confirmDeleteDebt() {
        val target = _deleteDebtCandidate.value ?: return
        _deleteDebtCandidate.value = null
        viewModelScope.launch {
            repository.deleteDebt(target.id)
            _snackbarMessages.tryEmit("उधार रिकॉर्ड हटा दिया गया")
        }
    }

    // --- Recurring Reminders ---
    fun openAddRecurring() {
        _recurringSheetState.value = RecurringSheetState(isOpen = true, editingRecurring = null)
    }

    fun openEditRecurring(entry: RecurringEntry) {
        _recurringSheetState.value = RecurringSheetState(isOpen = true, editingRecurring = entry)
    }

    fun closeRecurringSheet() {
        _recurringSheetState.value = _recurringSheetState.value.copy(isOpen = false, editingRecurring = null)
    }

    fun saveRecurring(
        title: String,
        type: TransactionType,
        amountText: String,
        account: PaymentAccount,
        expenseCategory: ExpenseCategory?,
        incomeCategory: IncomeCategory?,
        frequency: RecurrenceFrequency,
        nextDueDateKey: Int,
        notes: String
    ): Boolean {
        val validAmount = CurrencyFormatter.parseValidTransactionAmount(amountText)
        if (validAmount == null) {
            _snackbarMessages.tryEmit("कृपया सही रकम दर्ज करें")
            return false
        }
        val cleanTitle = title.trim()
        if (cleanTitle.isBlank()) {
            _snackbarMessages.tryEmit("कृपया याददाश्त का नाम लिखें")
            return false
        }
        val safeType = if (type == TransactionType.INCOME) TransactionType.INCOME else TransactionType.EXPENSE
        val editing = _recurringSheetState.value.editingRecurring

        viewModelScope.launch {
            if (editing != null && editing.id > 0L) {
                repository.updateRecurring(
                    editing.copy(
                        title = cleanTitle,
                        type = safeType,
                        amount = validAmount,
                        account = account,
                        expenseCategory = if (safeType == TransactionType.EXPENSE) (expenseCategory ?: ExpenseCategory.OTHER) else null,
                        incomeCategory = if (safeType == TransactionType.INCOME) (incomeCategory ?: IncomeCategory.OTHER) else null,
                        frequency = frequency,
                        nextDueDateKey = nextDueDateKey,
                        notes = notes.trim()
                    )
                )
                _snackbarMessages.tryEmit("नियमित याददाश्त अपडेट हुई")
            } else {
                repository.addRecurring(
                    RecurringEntry(
                        title = cleanTitle,
                        type = safeType,
                        amount = validAmount,
                        account = account,
                        expenseCategory = if (safeType == TransactionType.EXPENSE) (expenseCategory ?: ExpenseCategory.OTHER) else null,
                        incomeCategory = if (safeType == TransactionType.INCOME) (incomeCategory ?: IncomeCategory.OTHER) else null,
                        frequency = frequency,
                        nextDueDateKey = nextDueDateKey,
                        notes = notes.trim(),
                        isActive = true
                    )
                )
                _snackbarMessages.tryEmit("नियमित याददाश्त सेव हुई")
            }
            closeRecurringSheet()
        }
        return true
    }

    fun recordDueRecurring(entry: RecurringEntry) {
        val todayKey = todayProvider()
        val nowMinutes = timeProvider()
        viewModelScope.launch {
            val tx = HisabTransaction(
                id = 0L,
                type = entry.type,
                amount = entry.amount,
                dateKey = todayKey,
                partyName = entry.title,
                category = entry.expenseCategory,
                notes = entry.notes.ifBlank { "नियमित एंट्री (${entry.frequency.labelHi})" },
                createdAtMillis = System.currentTimeMillis(),
                timeMinutes = nowMinutes,
                account = entry.account,
                incomeCategory = entry.incomeCategory
            )
            repository.addTransaction(tx)
            val nextDate = DateUtils.advanceRecurringDate(
                maxOf(entry.nextDueDateKey, todayKey),
                entry.frequency
            )
            repository.updateRecurring(entry.copy(nextDueDateKey = nextDate))
            _snackbarMessages.tryEmit("\"${entry.title}\" हिसाब में दर्ज हो गया")
        }
    }

    fun skipDueRecurring(entry: RecurringEntry) {
        val todayKey = todayProvider()
        viewModelScope.launch {
            val nextDate = DateUtils.advanceRecurringDate(
                maxOf(entry.nextDueDateKey, todayKey),
                entry.frequency
            )
            repository.updateRecurring(entry.copy(nextDueDateKey = nextDate))
            _snackbarMessages.tryEmit("अगली तारीख ${DateUtils.formatDateHindi(nextDate)} पर बढ़ाई गई")
        }
    }

    fun toggleRecurringActive(entry: RecurringEntry) {
        viewModelScope.launch {
            repository.updateRecurring(entry.copy(isActive = !entry.isActive))
        }
    }

    fun requestDeleteRecurring(entry: RecurringEntry) {
        _deleteRecurringCandidate.value = entry
    }

    fun dismissDeleteRecurringDialog() {
        _deleteRecurringCandidate.value = null
    }

    fun confirmDeleteRecurring() {
        val target = _deleteRecurringCandidate.value ?: return
        _deleteRecurringCandidate.value = null
        viewModelScope.launch {
            repository.deleteRecurring(target.id)
            _snackbarMessages.tryEmit("नियमित याददाश्त हटाई गई")
        }
    }

    // --- PIN Lock ---
    fun openPinDialog() {
        _pinDialogOpen.value = true
    }

    fun dismissPinDialog() {
        _pinDialogOpen.value = false
    }

    fun enablePin(pin: String): Boolean {
        if (!PinHashUtil.isValidPinFormat(pin)) {
            _snackbarMessages.tryEmit("कृपया 4 अंकों का पिन डालें")
            return false
        }
        viewModelScope.launch {
            repository.setPin(pin)
            pinUnlockedThisSession.value = true
            _pinDialogOpen.value = false
            _snackbarMessages.tryEmit("ऐप पिन लॉक चालू हो गया")
        }
        return true
    }

    fun disablePin() {
        viewModelScope.launch {
            repository.disablePin()
            pinUnlockedThisSession.value = false
            _pinDialogOpen.value = false
            _snackbarMessages.tryEmit("ऐप पिन लॉक बंद कर दिया गया")
        }
    }

    fun verifyAndUnlockPin(pin: String): Boolean {
        val config = latestPinConfig
        val ok = PinHashUtil.verifyPin(pin, config.salt, config.hash)
        if (ok) {
            pinUnlockedThisSession.value = true
        }
        return ok
    }

    fun lockAppNow() {
        if (uiState.value.isPinEnabled) {
            pinUnlockedThisSession.value = false
        }
    }

    // --- Backup, Export & Restore ---
    fun buildJsonBackup(): String {
        val state = uiState.value
        val txs = state.allEntries.map { it.transaction }
        return BackupSerializer.toJson(
            accountOpenings = state.summary.accountOpenings,
            transactions = txs,
            debts = state.debts,
            recurringEntries = state.recurringEntries
        )
    }

    fun buildCsvBackup(): String {
        val state = uiState.value
        val txs = state.allEntries.map { it.transaction }
        return BackupSerializer.toCsv(state.summary.accountOpenings, txs)
    }

    fun notifyBackupExported(formatLabel: String) {
        _snackbarMessages.tryEmit("$formatLabel फ़ाइल सफलतापूर्वक सेव हो गई")
    }

    fun notifyBackupError(message: String) {
        _snackbarMessages.tryEmit(message)
    }

    fun parseBackupContent(rawText: String, fileNameHint: String?) {
        try {
            val trimmed = rawText.trimStart()
            val payload = if (
                fileNameHint?.endsWith(".json", ignoreCase = true) == true ||
                trimmed.startsWith("{")
            ) {
                BackupSerializer.fromJson(rawText)
            } else {
                BackupSerializer.fromCsv(rawText)
            }
            if (payload.transactions.isEmpty() &&
                payload.openingBalance == null &&
                payload.debts.isEmpty() &&
                payload.recurringEntries.isEmpty()
            ) {
                _snackbarMessages.tryEmit("फ़ाइल में कोई सही हिसाब डेटा नहीं मिला")
            } else {
                _pendingRestore.value = payload
            }
        } catch (_: Exception) {
            _snackbarMessages.tryEmit("बैकअप फ़ाइल अमान्य है। कृपया सही JSON या CSV चुनें")
        }
    }

    fun dismissRestoreDialog() {
        _pendingRestore.value = null
    }

    fun applyRestore(replaceExisting: Boolean) {
        val payload = _pendingRestore.value ?: return
        _pendingRestore.value = null
        viewModelScope.launch {
            if (replaceExisting) {
                repository.replaceAllData(
                    openingBalance = payload.openingBalance,
                    transactions = payload.transactions,
                    accountOpenings = payload.accountOpenings,
                    debts = payload.debts,
                    recurringEntries = payload.recurringEntries
                )
            } else {
                val existing = uiState.value.allEntries.map { it.transaction }
                val dedupedTxs = payload.transactions.filterNot { candidate ->
                    HisabCalculator.isDuplicateTransaction(candidate, existing)
                }
                repository.mergeTransactions(
                    openingBalance = payload.openingBalance,
                    transactions = dedupedTxs,
                    accountOpenings = payload.accountOpenings,
                    debts = payload.debts,
                    recurringEntries = payload.recurringEntries
                )
            }
            val skippedNote = if (payload.skippedInvalidRows > 0) {
                " (${payload.skippedInvalidRows} गलत पंक्तियाँ छोड़ी गईं)"
            } else ""
            _snackbarMessages.tryEmit("${payload.transactions.size} एंट्री सफलतापूर्वक रीस्टोर हुईं$skippedNote")
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
            _snackbarMessages.tryEmit("सारा डेटा साफ़ कर दिया गया")
        }
    }

    class Factory(private val repository: HisabDataSource) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return HisabViewModel(repository) as T
        }
    }
}
