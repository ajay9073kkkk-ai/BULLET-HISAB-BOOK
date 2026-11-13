package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface DebtDao {
    @Query("SELECT * FROM debts ORDER BY CASE WHEN status = 'PENDING' THEN 0 ELSE 1 END, dateKey DESC, createdAtMillis DESC, id DESC")
    fun observeAllDebts(): Flow<List<DebtEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: DebtEntity): Long

    @Update
    suspend fun update(entity: DebtEntity)

    @Query("DELETE FROM debts WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM debts")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<DebtEntity>)

    @Transaction
    suspend fun replaceAll(entities: List<DebtEntity>) {
        deleteAll()
        if (entities.isNotEmpty()) {
            insertAll(entities)
        }
    }
}
