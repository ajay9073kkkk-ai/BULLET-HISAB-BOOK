package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [
        TransactionEntity::class,
        DebtEntity::class,
        RecurringEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class HisabDatabase : RoomDatabase() {
    abstract fun transactionDao(): TransactionDao
    abstract fun debtDao(): DebtDao
    abstract fun recurringDao(): RecurringDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN timeMinutes INTEGER NOT NULL DEFAULT 720"
                )
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN accountKey TEXT NOT NULL DEFAULT 'CASH'"
                )
                db.execSQL(
                    "ALTER TABLE transactions ADD COLUMN toAccountKey TEXT"
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS debts (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        type TEXT NOT NULL,
                        personName TEXT NOT NULL,
                        amount REAL NOT NULL,
                        dateKey INTEGER NOT NULL,
                        dueDateKey INTEGER,
                        status TEXT NOT NULL,
                        settledDateKey INTEGER,
                        notes TEXT NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS recurring_entries (
                        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        title TEXT NOT NULL,
                        type TEXT NOT NULL,
                        amount REAL NOT NULL,
                        accountKey TEXT NOT NULL,
                        categoryKey TEXT,
                        frequencyKey TEXT NOT NULL,
                        nextDueDateKey INTEGER NOT NULL,
                        notes TEXT NOT NULL,
                        isActive INTEGER NOT NULL,
                        createdAtMillis INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: HisabDatabase? = null

        fun getInstance(context: Context): HisabDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    HisabDatabase::class.java,
                    "bullet_hisab_book.db"
                )
                    .addMigrations(MIGRATION_1_2)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
