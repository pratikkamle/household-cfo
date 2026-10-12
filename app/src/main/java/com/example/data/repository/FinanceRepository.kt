package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.AccountDao
import com.example.data.local.BudgetDao
import com.example.data.local.GoalDao
import com.example.data.local.HouseholdDao
import com.example.data.local.RecurringRuleDao
import com.example.data.local.TransactionDao
import com.example.data.local.UserDao
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FinanceRepository(
    private val database: AppDatabase,
    private val householdDao: HouseholdDao = database.householdDao(),
    private val userDao: UserDao = database.userDao(),
    private val transactionDao: TransactionDao = database.transactionDao(),
    private val recurringRuleDao: RecurringRuleDao = database.recurringRuleDao(),
    private val accountDao: AccountDao = database.accountDao(),
    private val budgetDao: BudgetDao = database.budgetDao(),
    private val goalDao: GoalDao = database.goalDao()
) {
    fun getHousehold(): Flow<HouseholdEntity?> =
        householdDao.getHouseholdById(1L)

    suspend fun getPrimaryHousehold(): HouseholdEntity? =
        householdDao.getPrimaryHousehold()

    suspend fun getHouseholdByName(name: String): HouseholdEntity? =
        householdDao.getHouseholdByName(name)

    suspend fun updateHousehold(household: HouseholdEntity) =
        householdDao.updateHousehold(household)

    suspend fun saveHousehold(household: HouseholdEntity): Long =
        householdDao.insertHousehold(household)

    fun getUsers(): Flow<List<UserEntity>> =
        userDao.getAllUsers()

    suspend fun getUserByHouseholdAndName(householdId: Long, name: String): UserEntity? =
        userDao.getUserByHouseholdAndName(householdId, name)

    suspend fun saveUser(user: UserEntity): Long =
        userDao.insertUser(user)

    suspend fun deleteUser(userId: Long) =
        userDao.deleteUser(userId)

    fun getTransactionsForMonth(monthYear: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByMonth(monthYear)

    fun getTransactionsForMonthAndMember(monthYear: String, member: String): Flow<List<TransactionEntity>> =
        transactionDao.getTransactionsByMonthAndMember(monthYear, member)

    fun getAllRecurringRules(): Flow<List<RecurringTransactionEntity>> =
        recurringRuleDao.getAllRules()

    fun getAllAccounts(): Flow<List<AccountEntity>> =
        accountDao.getAllAccounts()

    fun getBudgets(monthYear: String): Flow<List<CategoryBudgetEntity>> =
        budgetDao.getBudgets(monthYear)

    fun getRecentMerchants(): Flow<List<String>> =
        transactionDao.getRecentMerchants()

    suspend fun getSuggestionForMerchant(merchant: String): TransactionEntity? =
        transactionDao.getLastTransactionForMerchant(merchant)

    suspend fun checkRecurringRulePaid(ruleId: Long, monthYear: String): Boolean =
        transactionDao.getTransactionForRecurringRule(ruleId, monthYear) != null

    suspend fun insertTransaction(transaction: TransactionEntity): Long {
        val id = transactionDao.insertTransaction(transaction)
        // Update account balance
        val accId = transaction.accountId
        if (accId != null && accId > 0) {
            val delta = when (transaction.type) {
                "INCOME" -> transaction.amount
                "EXPENSE" -> -transaction.amount
                "INVESTMENT" -> -transaction.amount
                else -> 0.0
            }
            accountDao.updateBalance(accId, delta)
        }
        return id
    }

    suspend fun updateTransaction(newTx: TransactionEntity) {
        val oldTx = transactionDao.getTransactionById(newTx.id)
        transactionDao.updateTransaction(newTx)

        // Reconcile account balances if account or amount/type changed
        if (oldTx != null) {
            val oldAccId = oldTx.accountId
            if (oldAccId != null && oldAccId > 0) {
                val oldReversal = when (oldTx.type) {
                    "INCOME" -> -oldTx.amount
                    "EXPENSE" -> oldTx.amount
                    "INVESTMENT" -> oldTx.amount
                    else -> 0.0
                }
                accountDao.updateBalance(oldAccId, oldReversal)
            }

            val newAccId = newTx.accountId
            if (newAccId != null && newAccId > 0) {
                val newDelta = when (newTx.type) {
                    "INCOME" -> newTx.amount
                    "EXPENSE" -> -newTx.amount
                    "INVESTMENT" -> -newTx.amount
                    else -> 0.0
                }
                accountDao.updateBalance(newAccId, newDelta)
            }
        }
    }

    suspend fun deleteTransaction(transaction: TransactionEntity) {
        transactionDao.deleteTransactionById(transaction.id)
        val accId = transaction.accountId
        if (accId != null && accId > 0) {
            val reversalDelta = when (transaction.type) {
                "INCOME" -> -transaction.amount
                "EXPENSE" -> transaction.amount
                "INVESTMENT" -> transaction.amount
                else -> 0.0
            }
            accountDao.updateBalance(accId, reversalDelta)
        }
    }

    suspend fun markRecurringAsPaid(rule: RecurringTransactionEntity, monthYear: String): Long {
        val now = System.currentTimeMillis()
        val isIncome = rule.type == "INCOME"
        val paymentMethod = if (isIncome && (rule.paymentMethod.contains("DEBIT", ignoreCase = true) || rule.paymentMethod.isBlank())) {
            "Auto Credit"
        } else {
            rule.paymentMethod
        }
        val transaction = TransactionEntity(
            householdId = rule.householdId,
            userId = rule.userId,
            amount = rule.amount,
            category = rule.category,
            paidBy = rule.paidBy,
            paymentMethod = paymentMethod,
            accountId = rule.accountId,
            accountName = rule.accountName,
            merchant = rule.title,
            notes = "Scheduled recurring payment for $monthYear",
            timestamp = now,
            monthYear = monthYear,
            nature = rule.nature,
            type = rule.type,
            isRecurring = true,
            recurringRuleId = rule.id
        )
        return insertTransaction(transaction)
    }

    suspend fun saveBudget(budget: CategoryBudgetEntity) =
        budgetDao.insertBudget(budget)

    suspend fun saveBudget(category: String, amount: Double) =
        budgetDao.insertBudget(CategoryBudgetEntity(category = category, monthlyBudget = amount))

    suspend fun saveRecurringRule(rule: RecurringTransactionEntity): Long =
        recurringRuleDao.insertRule(rule)

    suspend fun updateRecurringRule(rule: RecurringTransactionEntity) =
        recurringRuleDao.updateRule(rule)

    suspend fun deleteRecurringRule(id: Long) =
        recurringRuleDao.deleteRuleById(id)

    suspend fun saveAccount(account: AccountEntity): Long =
        accountDao.insertAccount(account)

    suspend fun updateAccount(account: AccountEntity) =
        accountDao.updateAccount(account)

    suspend fun deleteAccount(id: Long) =
        accountDao.deleteAccountById(id)

    suspend fun updateUserRole(userId: Long, newRole: String) =
        userDao.updateUserRole(userId, newRole)

    // Financial Goals
    fun getGoals(householdId: Long = 1L): Flow<List<FinancialGoalEntity>> =
        goalDao.getGoalsByHousehold(householdId)

    suspend fun saveGoal(goal: FinancialGoalEntity): Long =
        goalDao.insertGoal(goal)

    suspend fun updateGoal(goal: FinancialGoalEntity) =
        goalDao.updateGoal(goal)

    suspend fun deleteGoal(id: Long) =
        goalDao.deleteGoal(id)

    suspend fun addGoalContribution(id: Long, additionalAmount: Double) {
        val existing = goalDao.getGoalById(id) ?: return
        val newSaved = (existing.currentSaved + additionalAmount).coerceAtLeast(0.0)
        val isCompleted = newSaved >= existing.targetAmount
        goalDao.updateGoalProgress(id, newSaved, isCompleted)
    }

    suspend fun toggleGoalCompleted(id: Long) {
        val existing = goalDao.getGoalById(id) ?: return
        goalDao.updateGoalProgress(id, existing.currentSaved, !existing.isCompleted)
    }

    suspend fun clearAllData() {
        transactionDao.clearAllTransactions()
        recurringRuleDao.clearAllRules()
    }

    suspend fun resetDatabase(reseedBaseline: Boolean = true) {
        database.resetDatabase(reseedBaseline)
    }

    fun exportTransactionsToCsv(transactions: List<TransactionEntity>): String {
        val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val sb = StringBuilder()
        sb.append("ID,Date,Merchant,Category,Amount,Type,Nature,Paid By,Payment Method,Account,Notes\n")
        transactions.forEach { t ->
            val dateStr = dateFormat.format(Date(t.timestamp))
            val safeMerchant = t.merchant.replace("\"", "\"\"")
            val safeNotes = t.notes.replace("\"", "\"\"")
            sb.append("${t.id},\"$dateStr\",\"$safeMerchant\",\"${t.category}\",${t.amount},\"${t.type}\",\"${t.nature}\",\"${t.paidBy}\",\"${t.paymentMethod}\",\"${t.accountName}\",\"$safeNotes\"\n")
        }
        return sb.toString()
    }
}
