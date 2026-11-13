package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.HisabTransaction
import com.example.domain.model.IncomeCategory
import com.example.domain.model.PaymentAccount
import com.example.domain.model.TransactionType

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // "INCOME", "EXPENSE", or "TRANSFER"
    val amount: Double,
    val dateKey: Int, // yyyyMMdd
    val partyName: String,
    val categoryKey: String?, // ExpenseCategory.key or IncomeCategory.key
    val notes: String,
    val createdAtMillis: Long,
    val timeMinutes: Int = 720,
    val accountKey: String = PaymentAccount.CASH.key,
    val toAccountKey: String? = null
) {
    fun toDomain(): HisabTransaction {
        val txType = when (type) {
            TransactionType.INCOME.name -> TransactionType.INCOME
            TransactionType.TRANSFER.name -> TransactionType.TRANSFER
            else -> TransactionType.EXPENSE
        }
        val expenseCategory = if (txType == TransactionType.EXPENSE) {
            ExpenseCategory.fromKeyOrLabel(categoryKey)
        } else null
        val incomeCategory = if (txType == TransactionType.INCOME) {
            IncomeCategory.fromKeyOrLabel(categoryKey)
        } else null
        val acct = PaymentAccount.fromKeyOrLabel(accountKey)
        val destAcct = if (txType == TransactionType.TRANSFER && !toAccountKey.isNullOrBlank()) {
            PaymentAccount.fromKeyOrLabel(toAccountKey)
        } else null

        return HisabTransaction(
            id = id,
            type = txType,
            amount = amount,
            dateKey = dateKey,
            partyName = partyName,
            category = expenseCategory,
            notes = notes,
            createdAtMillis = createdAtMillis,
            timeMinutes = timeMinutes.coerceIn(0, 1439),
            account = acct,
            toAccount = destAcct,
            incomeCategory = incomeCategory
        )
    }

    companion object {
        fun fromDomain(tx: HisabTransaction): TransactionEntity {
            val catKey = when (tx.type) {
                TransactionType.EXPENSE -> (tx.category ?: ExpenseCategory.OTHER).key
                TransactionType.INCOME -> (tx.incomeCategory ?: IncomeCategory.OTHER).key
                TransactionType.TRANSFER -> null
            }
            return TransactionEntity(
                id = tx.id,
                type = tx.type.name,
                amount = tx.amount,
                dateKey = tx.dateKey,
                partyName = tx.partyName,
                categoryKey = catKey,
                notes = tx.notes,
                createdAtMillis = tx.createdAtMillis,
                timeMinutes = tx.timeMinutes.coerceIn(0, 1439),
                accountKey = tx.account.key,
                toAccountKey = if (tx.type == TransactionType.TRANSFER) tx.toAccount?.key else null
            )
        }
    }
}
