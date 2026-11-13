package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.domain.model.DebtEntry
import com.example.domain.model.DebtStatus
import com.example.domain.model.DebtType

@Entity(tableName = "debts")
data class DebtEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val type: String, // "BORROWED" or "LENT"
    val personName: String,
    val amount: Double,
    val dateKey: Int,
    val dueDateKey: Int? = null,
    val status: String, // "PENDING" or "SETTLED"
    val settledDateKey: Int? = null,
    val notes: String,
    val createdAtMillis: Long
) {
    fun toDomain(): DebtEntry {
        return DebtEntry(
            id = id,
            type = if (type == DebtType.LENT.name) DebtType.LENT else DebtType.BORROWED,
            personName = personName,
            amount = amount,
            dateKey = dateKey,
            dueDateKey = dueDateKey,
            status = if (status == DebtStatus.SETTLED.name) DebtStatus.SETTLED else DebtStatus.PENDING,
            settledDateKey = settledDateKey,
            notes = notes,
            createdAtMillis = createdAtMillis
        )
    }

    companion object {
        fun fromDomain(entry: DebtEntry): DebtEntity {
            return DebtEntity(
                id = entry.id,
                type = entry.type.name,
                personName = entry.personName,
                amount = entry.amount,
                dateKey = entry.dateKey,
                dueDateKey = entry.dueDateKey,
                status = entry.status.name,
                settledDateKey = entry.settledDateKey,
                notes = entry.notes,
                createdAtMillis = entry.createdAtMillis
            )
        }
    }
}
