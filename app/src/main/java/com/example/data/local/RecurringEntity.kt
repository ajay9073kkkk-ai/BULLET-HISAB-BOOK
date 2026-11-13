package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.ExpenseCategory
import com.example.domain.model.IncomeCategory
import com.example.domain.model.PaymentAccount
import com.example.domain.model.RecurrenceFrequency
import com.example.domain.model.RecurringEntry
import com.example.domain.model.TransactionType

@Entity(tableName = "recurring_entries")
data class RecurringEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val type: String, // "INCOME" or "EXPENSE"
    val amount: Double,
    val accountKey: String,
    val categoryKey: String?,
    val frequencyKey: String,
    val nextDueDateKey: Int,
    val notes: String,
    val isActive: Boolean,
    val createdAtMillis: Long
) {
    fun toDomain(): RecurringEntry {
        val txType = if (type == TransactionType.INCOME.name) {
            TransactionType.INCOME
        } else {
            TransactionType.EXPENSE
        }
        return RecurringEntry(
            id = id,
            title = title,
            type = txType,
            amount = amount,
            account = PaymentAccount.fromKeyOrLabel(accountKey),
            expenseCategory = if (txType == TransactionType.EXPENSE) ExpenseCategory.fromKeyOrLabel(categoryKey) else null,
            incomeCategory = if (txType == TransactionType.INCOME) IncomeCategory.fromKeyOrLabel(categoryKey) else null,
            frequency = RecurrenceFrequency.fromKey(frequencyKey),
            nextDueDateKey = nextDueDateKey,
            notes = notes,
            isActive = isActive,
            createdAtMillis = createdAtMillis
        )
    }

    companion object {
        fun fromDomain(entry: RecurringEntry): RecurringEntity {
            val catKey = if (entry.type == TransactionType.INCOME) {
                (entry.incomeCategory ?: IncomeCategory.OTHER).key
            } else {
                (entry.expenseCategory ?: ExpenseCategory.OTHER).key
            }
            return RecurringEntity(
                id = entry.id,
                title = entry.title,
                type = entry.type.name,
                amount = entry.amount,
                accountKey = entry.account.key,
                categoryKey = catKey,
                frequencyKey = entry.frequency.key,
                nextDueDateKey = entry.nextDueDateKey,
                notes = entry.notes,
                isActive = entry.isActive,
                createdAtMillis = entry.createdAtMillis
            )
        }
    }
}
