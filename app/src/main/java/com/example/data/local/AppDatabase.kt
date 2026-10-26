package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.*
import com.example.util.CategoryBudgetScaler

@Database(
    entities = [
        HouseholdEntity::class,
        UserEntity::class,
        AccountEntity::class,
        CategoryBudgetEntity::class,
        TransactionEntity::class,
        RecurringTransactionEntity::class,
        FinancialGoalEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun householdDao(): HouseholdDao
    abstract fun userDao(): UserDao
    abstract fun accountDao(): AccountDao
    abstract fun budgetDao(): BudgetDao
    abstract fun transactionDao(): TransactionDao
    abstract fun recurringRuleDao(): RecurringRuleDao
    abstract fun goalDao(): GoalDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "household_cfo.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}

object DatabaseInitializer {
    suspend fun populateInitialData(database: AppDatabase) {
        val household = database.householdDao().getPrimaryHousehold()
        if (household != null && household.monthlyDiscretionaryBudget > 0) {
            val count = database.budgetDao().getBudgetCount()
            if (count == 0) {
                val budgets = CategoryBudgetScaler.scaleBudgets(household.monthlyDiscretionaryBudget)
                database.budgetDao().insertBudgets(budgets)
            }
        }
    }

    suspend fun registerHouseholdAndAdmin(
        database: AppDatabase,
        adminName: String,
        adminEmail: String = "",
        householdName: String = "",
        discretionaryBudget: Double = 50000.0,
        securityPin: String = "1234"
    ): HouseholdEntity {
        val cleanAdmin = adminName.trim().ifBlank { "Pratik" }
        val cleanHName = householdName.trim().ifBlank { "$cleanAdmin's Household" }
        val code = "CFO-" + (1000..9999).random()

        val household = HouseholdEntity(
            id = 1,
            name = cleanHName,
            inviteCode = code,
            adminName = cleanAdmin,
            adminEmail = adminEmail.trim(),
            monthlyDiscretionaryBudget = discretionaryBudget,
            securityPin = securityPin.ifBlank { "1234" },
            createdAt = System.currentTimeMillis()
        )
        database.householdDao().insertHousehold(household)

        val scaledBudgets = CategoryBudgetScaler.scaleBudgets(discretionaryBudget)
        database.budgetDao().insertBudgets(scaledBudgets)

        val adminUser = UserEntity(
            id = 1,
            householdId = 1,
            name = cleanAdmin,
            email = adminEmail.trim(),
            role = "ADMIN",
            avatarInitial = cleanAdmin.take(1).uppercase(),
            colorHex = "#2563EB",
            photoUrl = null,
            firebaseUid = null,
            joinedAt = System.currentTimeMillis()
        )
        database.userDao().insertUser(adminUser)

        val defaultAccounts = listOf(
            AccountEntity(
                householdId = 1,
                name = "Primary Bank (HDFC)",
                type = "BANK",
                balance = 125000.0,
                colorHex = "#2563EB",
                isPrimary = true,
                bankName = "HDFC Bank"
            ),
            AccountEntity(
                householdId = 1,
                name = "Credit Card (Regalia)",
                type = "CREDIT_CARD",
                balance = 18450.0,
                creditLimit = 250000.0,
                billingCycleDay = 15,
                paymentDueDay = 5,
                colorHex = "#7C3AED",
                bankName = "HDFC Bank"
            ),
            AccountEntity(
                householdId = 1,
                name = "Cash in Hand",
                type = "CASH",
                balance = 5000.0,
                colorHex = "#059669"
            )
        )
        database.accountDao().insertAccounts(defaultAccounts)

        return household
    }
}
