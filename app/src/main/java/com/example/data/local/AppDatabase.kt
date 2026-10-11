package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Database(
    entities = [
        HouseholdEntity::class,
        UserEntity::class,
        AccountEntity::class,
        TransactionEntity::class,
        RecurringTransactionEntity::class,
        CategoryBudgetEntity::class,
        FinancialGoalEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun householdDao(): HouseholdDao
    abstract fun userDao(): UserDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun goalDao(): GoalDao

    fun recurringTransactionDao(): RecurringTransactionDao = recurringRuleDao()

    /**
     * Clears all tables in the Room database while preserving the schema.
     * @param reseedBaseline When true, re-creates the essential baseline entities
     * (Household, admin user profile, clean zero-balance account envelopes,
     * and category budget limits) allowing the user to start manual entry right away.
     * When false, all tables are left completely empty.
     */
    suspend fun resetDatabase(reseedBaseline: Boolean = true) {
        withContext(Dispatchers.IO) {
            runInTransaction {
                openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = OFF")
                clearAllTables()
                openHelper.writableDatabase.execSQL("PRAGMA foreign_keys = ON")
            }
            if (reseedBaseline) {
                DatabaseInitializer.registerHouseholdAndAdmin(
                    database = this@AppDatabase,
                    adminName = "Admin",
                    adminEmail = "",
                    householdName = "Family Household",
                    discretionaryBudget = 50000.0,
                    securityPin = "1234"
                )
                DatabaseInitializer.populateInitialData(this@AppDatabase)
            }
        }
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "household_cfo.db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
