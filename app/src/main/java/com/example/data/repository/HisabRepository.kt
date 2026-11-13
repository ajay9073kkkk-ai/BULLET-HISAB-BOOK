package com.example.data.repository

import com.example.data.local.DebtDao
import com.example.data.local.DebtEntity
import com.example.data.local.OpeningBalanceDataStore
import com.example.data.local.PinLockConfig
import com.example.data.local.RecurringDao
import com.example.data.local.RecurringEntity
import com.example.data.local.TransactionDao
import com.example.data.local.TransactionEntity
import com.example.domain.model.AccountOpeningBalances
import com.example.domain.model.DebtEntry
import com.example.domain.model.HisabTransaction
import com.example.domain.model.RecurringEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface HisabDataSource {
    val transactions: Flow<List<HisabTransaction>>
    val openingBalance: Flow<Double>
    val accountOpenings: Flow<AccountOpeningBalances>
    val debts: Flow<List<DebtEntry>>
    val recurringEntries: Flow<List<RecurringEntry>>
    val pinLockConfig: Flow<PinLockConfig>

    suspend fun addTransaction(tx: HisabTransaction): Long
    suspend fun updateTransaction(tx: HisabTransaction)
    suspend fun deleteTransaction(id: Long)
    suspend fun setOpeningBalance(amount: Double)
    suspend fun setAccountOpenings(openings: AccountOpeningBalances)

    suspend fun addDebt(debt: DebtEntry): Long
    suspend fun updateDebt(debt: DebtEntry)
    suspend fun deleteDebt(id: Long)

    suspend fun addRecurring(entry: RecurringEntry): Long
    suspend fun updateRecurring(entry: RecurringEntry)
    suspend fun deleteRecurring(id: Long)

    suspend fun setPin(pin: String)
    suspend fun disablePin()

    suspend fun replaceAllData(
        openingBalance: Double?,
        transactions: List<HisabTransaction>,
        accountOpenings: AccountOpeningBalances? = null,
        debts: List<DebtEntry> = emptyList(),
        recurringEntries: List<RecurringEntry> = emptyList()
    )

    suspend fun mergeTransactions(
        openingBalance: Double?,
        transactions: List<HisabTransaction>,
        accountOpenings: AccountOpeningBalances? = null,
        debts: List<DebtEntry> = emptyList(),
        recurringEntries: List<RecurringEntry> = emptyList()
    )

    suspend fun clearAllData()
}

class HisabRepository(
    private val transactionDao: TransactionDao,
    private val debtDao: DebtDao,
    private val recurringDao: RecurringDao,
    private val openingBalanceStore: OpeningBalanceDataStore
) : HisabDataSource {

    override val transactions: Flow<List<HisabTransaction>> =
        transactionDao.observeAllTransactions().map { entities ->
            entities.map { it.toDomain() }
        }

    override val openingBalance: Flow<Double> = openingBalanceStore.openingBalance
    override val accountOpenings: Flow<AccountOpeningBalances> = openingBalanceStore.accountOpenings

    override val debts: Flow<List<DebtEntry>> =
        debtDao.observeAllDebts().map { entities ->
            entities.map { it.toDomain() }
        }

    override val recurringEntries: Flow<List<RecurringEntry>> =
        recurringDao.observeAllRecurring().map { entities ->
            entities.map { it.toDomain() }
        }

    override val pinLockConfig: Flow<PinLockConfig> = openingBalanceStore.pinLockConfig

    override suspend fun addTransaction(tx: HisabTransaction): Long {
        return transactionDao.insert(TransactionEntity.fromDomain(tx.copy(id = 0L)))
    }

    override suspend fun updateTransaction(tx: HisabTransaction) {
        require(tx.id > 0L) { "Cannot update transaction without valid id" }
        transactionDao.update(TransactionEntity.fromDomain(tx))
    }

    override suspend fun deleteTransaction(id: Long) {
        transactionDao.deleteById(id)
    }

    override suspend fun setOpeningBalance(amount: Double) {
        openingBalanceStore.setOpeningBalance(amount)
    }

    override suspend fun setAccountOpenings(openings: AccountOpeningBalances) {
        openingBalanceStore.setAccountOpenings(openings)
    }

    override suspend fun addDebt(debt: DebtEntry): Long {
        return debtDao.insert(DebtEntity.fromDomain(debt.copy(id = 0L)))
    }

    override suspend fun updateDebt(debt: DebtEntry) {
        require(debt.id > 0L) { "Cannot update debt without valid id" }
        debtDao.update(DebtEntity.fromDomain(debt))
    }

    override suspend fun deleteDebt(id: Long) {
        debtDao.deleteById(id)
    }

    override suspend fun addRecurring(entry: RecurringEntry): Long {
        return recurringDao.insert(RecurringEntity.fromDomain(entry.copy(id = 0L)))
    }

    override suspend fun updateRecurring(entry: RecurringEntry) {
        require(entry.id > 0L) { "Cannot update recurring entry without valid id" }
        recurringDao.update(RecurringEntity.fromDomain(entry))
    }

    override suspend fun deleteRecurring(id: Long) {
        recurringDao.deleteById(id)
    }

    override suspend fun setPin(pin: String) {
        openingBalanceStore.setPin(pin)
    }

    override suspend fun disablePin() {
        openingBalanceStore.disablePin()
    }

    override suspend fun replaceAllData(
        openingBalance: Double?,
        transactions: List<HisabTransaction>,
        accountOpenings: AccountOpeningBalances?,
        debts: List<DebtEntry>,
        recurringEntries: List<RecurringEntry>
    ) {
        val txEntities = transactions.map { TransactionEntity.fromDomain(it.copy(id = 0L)) }
        transactionDao.replaceAll(txEntities)
        debtDao.replaceAll(debts.map { DebtEntity.fromDomain(it.copy(id = 0L)) })
        recurringDao.replaceAll(recurringEntries.map { RecurringEntity.fromDomain(it.copy(id = 0L)) })

        if (accountOpenings != null) {
            openingBalanceStore.setAccountOpenings(accountOpenings)
        } else if (openingBalance != null) {
            openingBalanceStore.setAccountOpenings(AccountOpeningBalances(cash = openingBalance))
        }
    }

    override suspend fun mergeTransactions(
        openingBalance: Double?,
        transactions: List<HisabTransaction>,
        accountOpenings: AccountOpeningBalances?,
        debts: List<DebtEntry>,
        recurringEntries: List<RecurringEntry>
    ) {
        val txEntities = transactions.map { TransactionEntity.fromDomain(it.copy(id = 0L)) }
        if (txEntities.isNotEmpty()) {
            transactionDao.insertAll(txEntities)
        }
        val debtEntities = debts.map { DebtEntity.fromDomain(it.copy(id = 0L)) }
        if (debtEntities.isNotEmpty()) {
            debtDao.insertAll(debtEntities)
        }
        val recEntities = recurringEntries.map { RecurringEntity.fromDomain(it.copy(id = 0L)) }
        if (recEntities.isNotEmpty()) {
            recurringDao.insertAll(recEntities)
        }
        if (accountOpenings != null) {
            openingBalanceStore.setAccountOpenings(accountOpenings)
        } else if (openingBalance != null) {
            openingBalanceStore.setOpeningBalance(openingBalance)
        }
    }

    override suspend fun clearAllData() {
        transactionDao.deleteAll()
        debtDao.deleteAll()
        recurringDao.deleteAll()
        openingBalanceStore.setAccountOpenings(AccountOpeningBalances())
    }
}
