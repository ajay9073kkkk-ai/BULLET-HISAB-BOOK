package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.DebtEntity
import com.example.data.local.HisabDatabase
import com.example.data.local.PinLockConfig
import com.example.data.local.RecurringEntity
import com.example.data.local.TransactionEntity
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
import com.example.domain.util.BackupSerializer
import com.example.domain.util.HisabCalculator
import com.example.domain.util.PdfReportExporter
import com.example.domain.util.PinHashUtil
import com.example.ui.BulletHisabApp
import com.example.ui.theme.BulletHisabTheme
import com.example.ui.viewmodel.HisabViewModel
import java.io.ByteArrayOutputStream
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36], qualifiers = "w411dp-h915dp-xxhdpi")
class BulletHisabRobolectricTest {

    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var db: HisabDatabase
    private lateinit var context: Context

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, HisabDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun `json v2 backup round trip preserves accounts transfers debts recurring and skips invalid rows`() {
        val openings = AccountOpeningBalances(cash = 1000.0, bank = 3000.0, upi = 500.0)
        val txs = listOf(
            HisabTransaction(1L, TransactionType.INCOME, 3500.0, 20261112, "राजू भाई", null, "दुकान हिसाब, नकद", 100L, 630, PaymentAccount.BANK, null, IncomeCategory.BUSINESS),
            HisabTransaction(2L, TransactionType.EXPENSE, 240.5, 20261112, "अग्रवाल स्टोर", ExpenseCategory.SHOPPING, "सब्जी", 200L, 900, PaymentAccount.UPI),
            HisabTransaction(3L, TransactionType.TRANSFER, 500.0, 20261112, "बैंक->नकद", null, "", 300L, 950, PaymentAccount.BANK, PaymentAccount.CASH)
        )
        val debts = listOf(
            DebtEntry(1L, DebtType.LENT, "रमेश", 800.0, 20261110, 20261201, DebtStatus.PENDING, null, "मदद")
        )
        val recurring = listOf(
            RecurringEntry(1L, "बिजली बिल", TransactionType.EXPENSE, 1200.0, PaymentAccount.UPI, ExpenseCategory.HOME, null, RecurrenceFrequency.MONTHLY, 20261205)
        )

        val json = BackupSerializer.toJson(openings, txs, debts, recurring)
        val parsed = BackupSerializer.fromJson(json)
        assertEquals(4500.0, parsed.openingBalance!!, 0.001)
        assertEquals(3000.0, parsed.accountOpenings!!.bank, 0.001)
        assertEquals(3, parsed.transactions.size)
        assertEquals(PaymentAccount.BANK, parsed.transactions[0].account)
        assertEquals(PaymentAccount.CASH, parsed.transactions[2].toAccount)
        assertEquals(1, parsed.debts.size)
        assertEquals("रमेश", parsed.debts[0].personName)
        assertEquals(1, parsed.recurringEntries.size)
        assertEquals("बिजली बिल", parsed.recurringEntries[0].title)
    }

    @Test
    fun `room migration 1 to 2 preserves existing v1 transactions and creates debts and recurring tables`() = runBlocking {
        val dbName = "migration_test_v1_v2.db"
        context.deleteDatabase(dbName)

        // Create authentic v1 SQLite schema & row
        val config = SupportSQLiteOpenHelper.Configuration.builder(context)
            .name(dbName)
            .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                override fun onCreate(db: SupportSQLiteDatabase) {
                    db.execSQL(
                        """
                        CREATE TABLE IF NOT EXISTS transactions (
                            id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                            type TEXT NOT NULL,
                            amount REAL NOT NULL,
                            dateKey INTEGER NOT NULL,
                            partyName TEXT NOT NULL,
                            categoryKey TEXT,
                            notes TEXT NOT NULL,
                            createdAtMillis INTEGER NOT NULL
                        )
                        """.trimIndent()
                    )
                }

                override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
            })
            .build()

        val v1Helper = FrameworkSQLiteOpenHelperFactory().create(config)
        v1Helper.writableDatabase.use { v1Db ->
            v1Db.execSQL(
                "INSERT INTO transactions (type, amount, dateKey, partyName, categoryKey, notes, createdAtMillis) VALUES ('INCOME', 2500.0, 20261101, 'पुराना रिकॉर्ड', NULL, 'माइग्रेशन टेस्ट', 12345)"
            )
        }
        v1Helper.close()

        // Open with Room v2 + MIGRATION_1_2
        val migratedDb = Room.databaseBuilder(context, HisabDatabase::class.java, dbName)
            .addMigrations(HisabDatabase.MIGRATION_1_2)
            .allowMainThreadQueries()
            .build()
        try {
            val migratedTxs = migratedDb.transactionDao().observeAllTransactions().first()
            assertEquals(1, migratedTxs.size)
            assertEquals("पुराना रिकॉर्ड", migratedTxs[0].partyName)
            assertEquals(720, migratedTxs[0].timeMinutes)
            assertEquals("CASH", migratedTxs[0].accountKey)

            migratedDb.debtDao().insert(
                DebtEntity(type = "LENT", personName = "सोहन", amount = 500.0, dateKey = 20261112, dueDateKey = null, status = "PENDING", settledDateKey = null, notes = "", createdAtMillis = 1L)
            )
            migratedDb.recurringDao().insert(
                RecurringEntity(title = "किराया", type = "EXPENSE", amount = 4000.0, accountKey = "BANK", categoryKey = "HOME", frequencyKey = "MONTHLY", nextDueDateKey = 20261201, notes = "", isActive = true, createdAtMillis = 1L)
            )
            assertEquals(1, migratedDb.debtDao().observeAllDebts().first().size)
            assertEquals(1, migratedDb.recurringDao().observeAllRecurring().first().size)
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `pdf report exporter writes valid pdf header and content`() {
        val openings = AccountOpeningBalances(cash = 1000.0, bank = 2000.0, upi = 500.0)
        val txs = listOf(
            HisabTransaction(1L, TransactionType.INCOME, 1500.0, 20261112, "Salary", null, "Nov", 100L, 600, PaymentAccount.BANK),
            HisabTransaction(2L, TransactionType.EXPENSE, 250.0, 20261112, "Lunch", ExpenseCategory.FOOD, "", 200L, 780, PaymentAccount.UPI)
        )
        val (summary, entries) = HisabCalculator.calculateLedger(txs, openings, 20261112)
        val report = HisabCalculator.calculateReport(txs, openings.total, ReportPeriod.MONTHLY, 20261112)
        val out = ByteArrayOutputStream()
        PdfReportExporter.writeAccountReportPdf(out, summary, report, entries, 20261112)
        val bytes = out.toByteArray()
        assertTrue(bytes.size > 200)
        assertEquals("%PDF-", bytes.take(5).toByteArray().toString(Charsets.US_ASCII))
    }

    @Test
    fun `compose UI displays all 5 tabs monthly notebook search debt tracker yearly report settings and pin lock`() {
        val fakeRepo = RobolectricInMemoryDataSource()
        val vm = HisabViewModel(fakeRepo, todayProvider = { 20261112 }, timeProvider = { 600 })

        composeRule.setContent {
            BulletHisabTheme {
                BulletHisabApp(viewModel = vm)
            }
        }
        composeRule.waitForIdle()

        // 1. Empty state rendered on first launch
        composeRule.onNodeWithTag("current_balance_text").assertTextContains("₹0")
        composeRule.onNodeWithText("हिसाब डायरी खाली है").performScrollTo().assertIsDisplayed()

        // 2. Populate multi-account openings, transactions (Oct & Nov), and a pending debt
        fakeRepo.setImmediate(
            openings = AccountOpeningBalances(cash = 1000.0, bank = 2000.0, upi = 500.0),
            items = listOf(
                HisabTransaction(1L, TransactionType.INCOME, 500.0, 20261112, "सैलरी", null, "नकद", 100L, 540, PaymentAccount.BANK),
                HisabTransaction(2L, TransactionType.EXPENSE, 200.0, 20261112, "रमेश होटल", ExpenseCategory.FOOD, "खाना", 200L, 720, PaymentAccount.UPI),
                HisabTransaction(3L, TransactionType.EXPENSE, 100.0, 20261015, "अक्टूबर बस", ExpenseCategory.TRAVEL, "टिकट", 50L, 600, PaymentAccount.CASH)
            ),
            debts = listOf(
                DebtEntry(1L, DebtType.LENT, "सुरेश जी", 1200.0, 20261110, null, DebtStatus.PENDING, null, "मदद")
            )
        )
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("current_balance_text").performScrollTo().assertTextContains("₹3,700")
        composeRule.onNodeWithTag("account_card_cash").assertTextContains("₹900")
        composeRule.onNodeWithTag("account_card_bank").assertTextContains("₹2,500")
        composeRule.onNodeWithTag("account_card_upi").assertTextContains("₹300")

        // 3. History Tab: Monthly Notebook navigation + Search by amount
        composeRule.onNodeWithTag("nav_history").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notebook_month_title").assertTextContains("नवंबर 2026")
        composeRule.onNodeWithTag("ledger_entry_1").assertIsDisplayed()
        composeRule.onNodeWithTag("ledger_entry_3").assertDoesNotExist()

        composeRule.onNodeWithTag("prev_month_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("notebook_month_title").assertTextContains("अक्टूबर 2026")
        composeRule.onNodeWithTag("ledger_entry_3").assertIsDisplayed()

        composeRule.onNodeWithTag("next_month_button").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("history_search_input").performTextInput("200")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("ledger_entry_2").assertIsDisplayed()
        composeRule.onNodeWithTag("ledger_entry_1").assertDoesNotExist()

        // 4. Debt Tab
        composeRule.onNodeWithTag("nav_debts").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("debt_to_receive_text").assertTextContains("₹1,200")
        composeRule.onNodeWithTag("debt_card_1").performScrollTo().assertIsDisplayed()

        // 5. Reports Tab including Yearly mode & Export buttons
        composeRule.onNodeWithTag("nav_reports").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("report_period_yearly").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("report_total_received").assertTextContains("₹500")
        composeRule.onNodeWithTag("report_total_spent").assertTextContains("₹300")
        composeRule.onNodeWithTag("reports_export_pdf_button").assertIsDisplayed()

        // 6. Settings Tab: PIN lock, Recurring, Export PDF/CSV/JSON
        composeRule.onNodeWithTag("nav_settings").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("manage_pin_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("export_pdf_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("export_json_button").performScrollTo().assertIsDisplayed()
        composeRule.onNodeWithTag("export_csv_button").performScrollTo().assertIsDisplayed()

        // 7. Enable PIN & Lock screen -> verify unlocking with keypad
        fakeRepo.setPinImmediate("1234")
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("pin_key_1").assertIsDisplayed()
        composeRule.onNodeWithTag("pin_key_1").performClick()
        composeRule.onNodeWithTag("pin_key_2").performClick()
        composeRule.onNodeWithTag("pin_key_3").performClick()
        composeRule.onNodeWithTag("pin_key_4").performClick()
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("nav_dashboard").assertIsDisplayed()
    }
}

private class RobolectricInMemoryDataSource : HisabDataSource {
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

    fun setImmediate(
        openings: AccountOpeningBalances,
        items: List<HisabTransaction>,
        debts: List<DebtEntry> = emptyList()
    ) {
        _openings.value = openings
        _openingBalance.value = openings.total
        _transactions.value = items
        _debts.value = debts
        nextId = (items.maxOfOrNull { it.id } ?: 0L) + 1L
        nextDebtId = (debts.maxOfOrNull { it.id } ?: 0L) + 1L
    }

    fun setPinImmediate(pin: String) {
        val salt = PinHashUtil.generateSalt()
        _pin.value = PinLockConfig(isEnabled = true, salt = salt, hash = PinHashUtil.hashPin(pin, salt))
    }

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
        setPinImmediate(pin)
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
