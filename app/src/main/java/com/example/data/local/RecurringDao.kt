package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringDao {
    @Query("SELECT * FROM recurring_entries ORDER BY isActive DESC, nextDueDateKey ASC, id DESC")
    fun observeAllRecurring(): Flow<List<RecurringEntity>>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(entity: RecurringEntity): Long

    @Update
    suspend fun update(entity: RecurringEntity)

    @Query("DELETE FROM recurring_entries WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM recurring_entries")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<RecurringEntity>)

    @Transaction
    suspend fun replaceAll(entities: List<RecurringEntity>) {
        deleteAll()
        if (entities.isNotEmpty()) {
            insertAll(entities)
        }
    }
}
