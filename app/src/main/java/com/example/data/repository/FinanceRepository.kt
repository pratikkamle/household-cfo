package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class FinanceRepository(private val database: AppDatabase) {

    fun getPrimaryHouseholdFlow(): Flow<HouseholdEntity?> =
        database.householdDao().getPrimaryHouseholdFlow()

    suspend fun getPrimaryHousehold(): HouseholdEntity? =
        database.householdDao().getPrimaryHousehold()

    suspend fun getHouseholdByInviteCode(code: String): HouseholdEntity? =
        database.householdDao().getHouseholdByInviteCode(code)

    suspend fun getHouseholdByAdminEmail(email: String): HouseholdEntity? =
        database.householdDao().getHouseholdByAdminEmail(email)

    suspend fun insertHousehold(household: HouseholdEntity): Long =
        database.householdDao().insertHousehold(household)

    suspend fun updateHousehold(household: HouseholdEntity) =
        database.householdDao().updateHousehold(household)

    fun getAllUsers(): Flow<List<UserEntity>> =
        database.userDao().getAllUsersFlow()

    suspend fun getAllUsersSync(): List<UserEntity> =
        database.userDao().getAllUsers()

    fun getUserById(id: Long): Flow<UserEntity?> =
        database.userDao().getUserById(id)

    suspend fun getUserByIdSync(id: Long): UserEntity? =
        database.userDao().getUserByIdSync(id)

    suspend fun getUserByEmail(email: String): UserEntity? =
        database.userDao().getUserByEmail(email)

    suspend fun getUserByName(name: String): UserEntity? =
        database.userDao().getUserByName(name)

    suspend fun insertUser(user: UserEntity): Long =
        database.userDao().insertUser(user)

    suspend fun insertUsers(users: List<UserEntity>) =
        database.userDao().insertUsers(users)

    suspend fun updateUser(user: UserEntity) =
        database.userDao().updateUser(user)

    suspend fun deleteUser(id: Long) =
        database.userDao().deleteUser(id)

    fun getAllAccounts(): Flow<List<AccountEntity>> =
        database.accountDao().getAllAccountsFlow()

    suspend fun getAllAccountsSync(): List<AccountEntity> =
        database.accountDao().getAllAccounts()

    suspend fun getAccountById(id: Long): AccountEntity? =
        database.accountDao().getAccountById(id)

    suspend fun insertAccount(account: AccountEntity): Long =
        database.accountDao().insertAccount(account)

    suspend fun insertAccounts(accounts: List<AccountEntity>) =
        database.accountDao().insertAccounts(accounts)

    suspend fun updateAccount(account: AccountEntity) =
        database.accountDao().updateAccount(account)

    suspend fun deleteAccount(id: Long) =
        database.accountDao().deleteAccount(id)

    fun getBudgets(monthYear: String = ""): Flow<List<CategoryBudgetEntity>> =
        database.budgetDao().getAllBudgetsFlow()

    suspend fun getBudgetsSync(): List<CategoryBudgetEntity> =
        database.budgetDao().getAllBudgets()

    suspend fun insertBudgets(budgets: List<CategoryBudgetEntity>) =
        database.budgetDao().insertBudgets(budgets)

    suspend fun insertBudget(budget: CategoryBudgetEntity): Long =
        database.budgetDao().insertBudget(budget)

    suspend fun deleteBudgetByCategory(category: String) =
        database.budgetDao().deleteBudgetByCategory(category)

    fun getAllTransactions(): Flow<List<TransactionEntity>> =
        database.transactionDao().getAllTransactions()

    fun getTransactionsForMonth(monthYear: String): Flow<List<TransactionEntity>> =
        database.transactionDao().getTransactionsForMonth(monthYear)

    suspend fun insertTransaction(transaction: TransactionEntity): Long =
        database.transactionDao().insertTransaction(transaction)

    suspend fun insertTransactions(transactions: List<TransactionEntity>) =
        database.transactionDao().insertTransactions(transactions)

    suspend fun updateTransaction(transaction: TransactionEntity) =
        database.transactionDao().updateTransaction(transaction)

    suspend fun deleteTransaction(transaction: TransactionEntity) =
        database.transactionDao().deleteTransaction(transaction)

    suspend fun deleteTransactionById(id: Long) =
        database.transactionDao().deleteTransactionById(id)

    fun getAllRecurringRules(): Flow<List<RecurringTransactionEntity>> =
        database.recurringRuleDao().getAllRecurringRules()

    suspend fun getAllRecurringRulesSync(): List<RecurringTransactionEntity> =
        database.recurringRuleDao().getAllRecurringRulesSync()

    suspend fun insertRecurringRule(rule: RecurringTransactionEntity): Long =
        database.recurringRuleDao().insertRecurringRule(rule)

    suspend fun insertRecurringRules(rules: List<RecurringTransactionEntity>) =
        database.recurringRuleDao().insertRecurringRules(rules)

    suspend fun updateRecurringRule(rule: RecurringTransactionEntity) =
        database.recurringRuleDao().updateRecurringRule(rule)

    suspend fun deleteRecurringRule(id: Long) =
        database.recurringRuleDao().deleteRecurringRuleById(id)

    fun getGoals(householdId: Long = 1): Flow<List<FinancialGoalEntity>> =
        database.goalDao().getAllGoalsFlow()

    suspend fun insertGoal(goal: FinancialGoalEntity): Long =
        database.goalDao().insertGoal(goal)

    suspend fun insertGoals(goals: List<FinancialGoalEntity>) =
        database.goalDao().insertGoals(goals)

    suspend fun updateGoal(goal: FinancialGoalEntity) =
        database.goalDao().updateGoal(goal)

    suspend fun deleteGoal(id: Long) =
        database.goalDao().deleteGoal(id)

    fun getAllMemberProfiles(): Flow<List<MemberProfile>> =
        combine(database.userDao().getAllUsersFlow(), database.transactionDao().getAllTransactions()) { users, transactions ->
            users.map { u ->
                val memberTxs = transactions.filter { it.paidBy.equals(u.name, ignoreCase = true) || it.userId == u.id }
                val expense = memberTxs.filter { it.type == "EXPENSE" }.sumOf { it.amount }
                val income = memberTxs.filter { it.type == "INCOME" }.sumOf { it.amount }
                MemberProfile(
                    id = u.id,
                    name = u.name,
                    email = u.email,
                    role = u.role,
                    avatarInitial = u.initial,
                    colorHex = u.colorHex,
                    photoUrl = u.photoUrl,
                    totalExpense = expense,
                    totalIncome = income
                )
            }
        }

    suspend fun clearAllLocalData() {
        database.transactionDao().clearAll()
        database.budgetDao().clearAll()
        database.accountDao().clearAll()
        database.recurringRuleDao().clearAll()
        database.goalDao().clearAll()
        database.userDao().clearAll()
        database.householdDao().clearAll()
    }
}
