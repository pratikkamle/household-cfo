package com.example.data.local

import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.UserEntity

object DatabaseInitializer {

    suspend fun populateInitialData(database: AppDatabase) {
        // Ensure standard category budget envelopes are ready
        val budgetDao = database.budgetDao()
        if (budgetDao.getBudgetCount() == 0) {
            val budgets = listOf(
                CategoryBudgetEntity("Food & Dining", 10000.0),
                CategoryBudgetEntity("Groceries", 15000.0),
                CategoryBudgetEntity("Shopping", 8000.0),
                CategoryBudgetEntity("Fuel & Travel", 6000.0),
                CategoryBudgetEntity("Entertainment", 5000.0),
                CategoryBudgetEntity("Travel & Holidays", 12500.0),
                CategoryBudgetEntity("Utilities & Bills", 6000.0),
                CategoryBudgetEntity("Medical & Healthcare", 5000.0),
                CategoryBudgetEntity("Personal Care", 4000.0),
                CategoryBudgetEntity("Miscellaneous", 5000.0)
            )
            budgetDao.insertBudgets(budgets)
        }
        // Note: No placeholder accounts or hardcoded multiple users are seeded.
        // Users start with 0 accounts and can add their real accounts from scratch.
    }

    suspend fun registerHouseholdAndAdmin(
        database: AppDatabase,
        adminName: String,
        adminEmail: String = "",
        householdName: String = "",
        discretionaryBudget: Double = 50000.0,
        securityPin: String = "1234"
    ): Long {
        val cleanAdmin = adminName.trim().ifBlank { "Admin" }
        val cleanHName = householdName.trim().ifBlank { "$cleanAdmin's Household" }
        val code = "CFO-" + (1000..9999).random()

        val household = HouseholdEntity(
            id = 1L,
            name = cleanHName,
            currency = "INR",
            monthlyDiscretionaryBudget = if (discretionaryBudget > 0) discretionaryBudget else 50000.0,
            inviteCode = code,
            adminName = cleanAdmin,
            adminEmail = adminEmail.trim(),
            securityPin = securityPin.ifBlank { "1234" },
            createdAt = System.currentTimeMillis()
        )
        database.householdDao().insertHousehold(household)

        val adminUser = UserEntity(
            id = 1L,
            householdId = 1L,
            name = cleanAdmin,
            email = adminEmail.trim(),
            role = "ADMIN",
            initial = cleanAdmin.take(1).uppercase(),
            colorHex = "#2563EB",
            createdAt = System.currentTimeMillis()
        )
        database.userDao().insertUser(adminUser)

        return 1L
    }
}
