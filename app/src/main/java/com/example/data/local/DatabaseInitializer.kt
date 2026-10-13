package com.example.data.local

import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.UserEntity
import com.example.util.CategoryBudgetScaler

object DatabaseInitializer {

    suspend fun populateInitialData(database: AppDatabase) {
        val budgetDao = database.budgetDao()
        val household = database.householdDao().getPrimaryHousehold()
        if (household != null && household.monthlyDiscretionaryBudget > 0) {
            if (budgetDao.getBudgetCount() == 0) {
                val budgets = CategoryBudgetScaler.scaleBudgets(household.monthlyDiscretionaryBudget)
                budgetDao.insertBudgets(budgets)
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
    ): Long {
        val cleanAdmin = adminName.trim().ifBlank { "Admin" }
        val cleanHName = householdName.trim().ifBlank { "$cleanAdmin's Household" }
        val code = "CFO-" + (1000..9999).random()

        val finalBudget = if (discretionaryBudget > 0) discretionaryBudget else 50000.0

        val household = HouseholdEntity(
            id = 1L,
            name = cleanHName,
            currency = "INR",
            monthlyDiscretionaryBudget = finalBudget,
            inviteCode = code,
            adminName = cleanAdmin,
            adminEmail = adminEmail.trim(),
            securityPin = securityPin.ifBlank { "1234" },
            createdAt = System.currentTimeMillis()
        )
        database.householdDao().insertHousehold(household)

        // Scale category allowances based on the percentage distribution to the baseline of 76500
        val scaledBudgets = CategoryBudgetScaler.scaleBudgets(finalBudget)
        database.budgetDao().insertBudgets(scaledBudgets)

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
