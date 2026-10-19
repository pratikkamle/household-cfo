package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.*

class FinanceRepository(private val database: AppDatabase) {
    val allTransactions: Flow<List<TransactionEntity>> = database.transactionDao().getAllTransactionsFlow()
    val allAccounts: Flow<List<AccountEntity>> = database.accountDao().getAllAccountsFlow()
    val allBudgets: Flow<List<CategoryBudgetEntity>> = database.budgetDao().getAllBudgetsFlow()
    val allGoals: Flow<List<FinancialGoalEntity>> = database.goalDao().getAllGoalsFlow()
    val allRecurringRules: Flow<List<RecurringTransactionEntity>> = database.recurringRuleDao().getAllRulesFlow()
    val household: Flow<HouseholdEntity?> = database.householdDao().getHouseholdFlow()
    val users: Flow<List<UserEntity>> = database.userDao().getUsersFlow()

    fun getMonthTransactions(monthYear: String): Flow<List<TransactionEntity>> {
        return database.transactionDao().getTransactionsForMonthFlow(monthYear)
    }

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = database.transactionDao().insertTransaction(transaction)
        // Update account balance
        transaction.accountId?.let { accountId ->
            val account = database.accountDao().getAccountById(accountId)
            if (account != null) {
                val newBalance = if (transaction.type == "EXPENSE") {
                    account.balance - transaction.amount
                } else if (transaction.type == "INCOME") {
                    account.balance + transaction.amount
                } else {
                    account.balance
                }
                database.accountDao().updateAccount(account.copy(balance = newBalance, updatedAt = System.currentTimeMillis()))
            }
        }
        return id
    }

    suspend fun updateTransaction(transaction: TransactionEntity) {
        database.transactionDao().updateTransaction(transaction)
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        database.transactionDao().deleteTransaction(transaction)
        // Reverse account balance
        transaction.accountId?.let { accountId ->
            val account = database.accountDao().getAccountById(accountId)
            if (account != null) {
                val restoredBalance = if (transaction.type == "EXPENSE") {
                    account.balance + transaction.amount
                } else if (transaction.type == "INCOME") {
                    account.balance - transaction.amount
                } else {
                    account.balance
                }
                database.accountDao().updateAccount(account.copy(balance = restoredBalance, updatedAt = System.currentTimeMillis()))
            }
        }
    }

    suspend fun insertAccount(account: AccountEntity): Long {
        return database.accountDao().insertAccount(account)
    }

    suspend fun updateAccount(account: AccountEntity) {
        database.accountDao().updateAccount(account)
    }

    suspend fun deleteAccount(account: AccountEntity) {
        database.accountDao().deleteAccount(account)
    }

    suspend fun saveBudget(budget: CategoryBudgetEntity) {
        database.budgetDao().insertOrUpdateBudget(budget)
    }

    suspend fun insertGoal(goal: FinancialGoalEntity): Long {
        return database.goalDao().insertGoal(goal)
    }

    suspend fun updateGoal(goal: FinancialGoalEntity) {
        database.goalDao().updateGoal(goal)
    }

    suspend fun deleteGoal(goal: FinancialGoalEntity) {
        database.goalDao().deleteGoal(goal)
    }

    suspend fun addGoalContribution(goalId: Long, amount: Double) {
        // Find and increment goal
    }

    suspend fun insertRecurringRule(rule: RecurringTransactionEntity): Long {
        return database.recurringRuleDao().insertRule(rule)
    }

    suspend fun updateRecurringRule(rule: RecurringTransactionEntity) {
        database.recurringRuleDao().updateRule(rule)
    }

    suspend fun deleteRecurringRule(rule: RecurringTransactionEntity) {
        database.recurringRuleDao().deleteRule(rule)
    }

    suspend fun updateHousehold(household: HouseholdEntity) {
        database.householdDao().insertOrUpdateHousehold(household)
    }

    suspend fun insertUser(user: UserEntity): Long {
        return database.userDao().insertUser(user)
    }

    suspend fun clearAllData() {
        database.transactionDao().deleteAllTransactions()
        database.accountDao().deleteAllAccounts()
        database.budgetDao().deleteAllBudgets()
        database.goalDao().deleteAllGoals()
        database.recurringRuleDao().deleteAllRules()
    }

    suspend fun seedInitialDataIfEmpty() {
        val currentHousehold = database.householdDao().getHousehold()
        if (currentHousehold == null) {
            database.householdDao().insertOrUpdateHousehold(
                HouseholdEntity(
                    id = 1L,
                    name = "The Smith Household",
                    securityPin = "1234",
                    budgetPlanType = "50_30_20"
                )
            )
            database.userDao().insertUser(
                UserEntity(
                    id = 1L,
                    householdId = 1L,
                    name = "Household CFO Admin",
                    email = "cfo@household.local",
                    role = "ADMIN"
                )
            )
            // Seed sample accounts
            val checkingId = database.accountDao().insertAccount(
                AccountEntity(
                    name = "Primary Checking",
                    type = "CHECKING",
                    balance = 4850.75,
                    institution = "Chase",
                    lastFour = "4129"
                )
            )
            val savingsId = database.accountDao().insertAccount(
                AccountEntity(
                    name = "High-Yield Savings",
                    type = "SAVINGS",
                    balance = 24500.00,
                    institution = "Ally Bank",
                    lastFour = "8832",
                    isSensitive = true
                )
            )
            val ccId = database.accountDao().insertAccount(
                AccountEntity(
                    name = "Sapphire Reserve",
                    type = "CREDIT_CARD",
                    balance = -1230.40,
                    institution = "Chase",
                    lastFour = "9940"
                )
            )

            val monthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
            val currentMonth = monthFormat.format(Date())

            // Seed sample budgets
            database.budgetDao().insertAll(
                listOf(
                    CategoryBudgetEntity("Housing & Mortgage", 1L, 2200.0, "NEED", currentMonth),
                    CategoryBudgetEntity("Groceries & Food", 1L, 850.0, "NEED", currentMonth),
                    CategoryBudgetEntity("Utilities & Internet", 1L, 350.0, "NEED", currentMonth),
                    CategoryBudgetEntity("Dining Out", 1L, 400.0, "WANT", currentMonth),
                    CategoryBudgetEntity("Entertainment", 1L, 250.0, "WANT", currentMonth),
                    CategoryBudgetEntity("Emergency Fund", 1L, 1000.0, "SAVINGS", currentMonth)
                )
            )

            // Seed sample transactions
            val now = System.currentTimeMillis()
            database.transactionDao().insertTransaction(
                TransactionEntity(
                    householdId = 1L,
                    amount = 5800.00,
                    category = "Salary",
                    paidBy = "Household CFO Admin",
                    paymentMethod = "BANK_TRANSFER",
                    accountId = checkingId,
                    accountName = "Primary Checking",
                    merchant = "Employer Direct Deposit",
                    notes = "Bi-weekly paycheck",
                    timestamp = now - 86400000L * 2,
                    monthYear = currentMonth,
                    nature = "SAVINGS",
                    type = "INCOME"
                )
            )
            database.transactionDao().insertTransaction(
                TransactionEntity(
                    householdId = 1L,
                    amount = 142.50,
                    category = "Groceries & Food",
                    paidBy = "Household CFO Admin",
                    paymentMethod = "CREDIT_CARD",
                    accountId = ccId,
                    accountName = "Sapphire Reserve",
                    merchant = "Whole Foods Market",
                    notes = "Weekly grocery restock",
                    timestamp = now - 86400000L,
                    monthYear = currentMonth,
                    nature = "NEED",
                    type = "EXPENSE"
                )
            )
            database.transactionDao().insertTransaction(
                TransactionEntity(
                    householdId = 1L,
                    amount = 68.20,
                    category = "Dining Out",
                    paidBy = "Household CFO Admin",
                    paymentMethod = "CREDIT_CARD",
                    accountId = ccId,
                    accountName = "Sapphire Reserve",
                    merchant = "Olive Garden",
                    notes = "Family dinner",
                    timestamp = now - 3600000L * 5,
                    monthYear = currentMonth,
                    nature = "WANT",
                    type = "EXPENSE"
                )
            )

            // Seed goals
            database.goalDao().insertGoal(
                FinancialGoalEntity(
                    title = "6-Month Emergency Cushion",
                    targetAmount = 30000.0,
                    currentAmount = 24500.0,
                    targetDate = now + 86400000L * 180,
                    priority = 1,
                    category = "Safety"
                )
            )
            database.goalDao().insertGoal(
                FinancialGoalEntity(
                    title = "Family Summer Vacation",
                    targetAmount = 4500.0,
                    currentAmount = 2100.0,
                    targetDate = now + 86400000L * 120,
                    priority = 2,
                    category = "Travel"
                )
            )

            // Seed recurring rules
            database.recurringRuleDao().insertRule(
                RecurringTransactionEntity(
                    title = "Mortgage Payment",
                    amount = 2150.0,
                    category = "Housing & Mortgage",
                    frequency = "MONTHLY",
                    nextDueDate = now + 86400000L * 12,
                    accountId = checkingId,
                    accountName = "Primary Checking",
                    nature = "NEED",
                    type = "EXPENSE"
                )
            )
            database.recurringRuleDao().insertRule(
                RecurringTransactionEntity(
                    title = "Fiber Internet (AT&T)",
                    amount = 80.0,
                    category = "Utilities & Internet",
                    frequency = "MONTHLY",
                    nextDueDate = now + 86400000L * 6,
                    accountId = checkingId,
                    accountName = "Primary Checking",
                    nature = "NEED",
                    type = "EXPENSE"
                )
            )
        }
    }
}
