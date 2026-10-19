package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.model.*
import com.example.data.repository.FinanceRepository
import com.example.service.SmartNotificationManager
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class FinanceViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: FinanceRepository

    init {
        val database = AppDatabase.getDatabase(application)
        repository = FinanceRepository(database)
        viewModelScope.launch {
            repository.seedInitialDataIfEmpty()
        }
    }

    private val currentMonthFormat = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val currentMonthYear: String = currentMonthFormat.format(Date())

    val transactions: StateFlow<List<TransactionEntity>> = repository.allTransactions
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentMonthTransactions: StateFlow<List<TransactionEntity>> = repository.getMonthTransactions(currentMonthYear)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.allAccounts
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<CategoryBudgetEntity>> = repository.allBudgets
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<FinancialGoalEntity>> = repository.allGoals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringRules: StateFlow<List<RecurringTransactionEntity>> = repository.allRecurringRules
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val household: StateFlow<HouseholdEntity?> = repository.household
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val users: StateFlow<List<UserEntity>> = repository.users
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val monthSummary: StateFlow<MonthSummary> = combine(
        currentMonthTransactions,
        budgets
    ) { txList, budgetList ->
        var income = 0.0
        var expense = 0.0
        var needs = 0.0
        var wants = 0.0
        var savings = 0.0

        for (tx in txList) {
            when (tx.type) {
                "INCOME" -> income += tx.amount
                "EXPENSE" -> {
                    expense += tx.amount
                    when (tx.nature) {
                        "NEED" -> needs += tx.amount
                        "WANT" -> wants += tx.amount
                        else -> savings += tx.amount
                    }
                }
            }
        }

        val totalBudget = budgetList.sumOf { it.budgetAmount }
        val pctUsed = if (totalBudget > 0) (expense / totalBudget) * 100 else 0.0

        MonthSummary(
            totalIncome = income,
            totalExpense = expense,
            netSavings = income - expense,
            needsSpend = needs,
            wantsSpend = wants,
            savingsSpend = savings,
            totalBudget = totalBudget,
            percentOfBudgetUsed = pctUsed
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MonthSummary())

    val latestDetectedTx: StateFlow<ParsedTransaction?> = SmartNotificationManager.latestDetectedTx

    private val _isUserAuthenticated = MutableStateFlow(true)
    val isUserAuthenticated: StateFlow<Boolean> = _isUserAuthenticated.asStateFlow()

    private val _isSensitiveRevealed = MutableStateFlow(false)
    val isSensitiveRevealed: StateFlow<Boolean> = _isSensitiveRevealed.asStateFlow()

    private val _activeTab = MutableStateFlow(0)
    val activeTab: StateFlow<Int> = _activeTab.asStateFlow()

    fun selectTab(tab: Int) {
        _activeTab.value = tab
    }

    fun toggleSensitivityRevealed() {
        _isSensitiveRevealed.value = !_isSensitiveRevealed.value
    }

    fun dismissDetectedTx() {
        SmartNotificationManager.clearLatestDetected()
    }

    fun addExpense(
        amount: Double,
        category: String,
        merchant: String,
        accountId: Long?,
        accountName: String,
        nature: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    amount = amount,
                    category = category,
                    merchant = merchant,
                    accountId = accountId,
                    accountName = accountName,
                    nature = nature,
                    notes = notes,
                    type = "EXPENSE",
                    monthYear = currentMonthYear
                )
            )
        }
    }

    fun addIncome(
        amount: Double,
        category: String,
        merchant: String,
        accountId: Long?,
        accountName: String,
        notes: String
    ) {
        viewModelScope.launch {
            repository.insertTransaction(
                TransactionEntity(
                    amount = amount,
                    category = category,
                    merchant = merchant,
                    accountId = accountId,
                    accountName = accountName,
                    nature = "SAVINGS",
                    notes = notes,
                    type = "INCOME",
                    monthYear = currentMonthYear
                )
            )
        }
    }

    fun updateTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.updateTransaction(transaction)
        }
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
        }
    }

    fun addAccount(
        name: String,
        type: String,
        balance: Double,
        institution: String,
        lastFour: String,
        isSensitive: Boolean
    ) {
        viewModelScope.launch {
            repository.insertAccount(
                AccountEntity(
                    name = name,
                    type = type,
                    balance = balance,
                    institution = institution,
                    lastFour = lastFour,
                    isSensitive = isSensitive
                )
            )
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
        }
    }

    fun deleteAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.deleteAccount(account)
        }
    }

    fun saveBudget(category: String, amount: Double, nature: String) {
        viewModelScope.launch {
            repository.saveBudget(
                CategoryBudgetEntity(
                    category = category,
                    budgetAmount = amount,
                    nature = nature,
                    monthYear = currentMonthYear
                )
            )
        }
    }

    fun addGoal(title: String, targetAmount: Double, targetDate: Long, category: String) {
        viewModelScope.launch {
            repository.insertGoal(
                FinancialGoalEntity(
                    title = title,
                    targetAmount = targetAmount,
                    targetDate = targetDate,
                    category = category
                )
            )
        }
    }

    fun addGoalContribution(goal: FinancialGoalEntity, amount: Double) {
        viewModelScope.launch {
            val updated = goal.copy(
                currentAmount = goal.currentAmount + amount,
                isCompleted = (goal.currentAmount + amount) >= goal.targetAmount
            )
            repository.updateGoal(updated)
        }
    }

    fun deleteGoal(goal: FinancialGoalEntity) {
        viewModelScope.launch {
            repository.deleteGoal(goal)
        }
    }

    fun addRecurringRule(
        title: String,
        amount: Double,
        category: String,
        frequency: String,
        nextDueDate: Long,
        accountId: Long?,
        accountName: String,
        nature: String
    ) {
        viewModelScope.launch {
            repository.insertRecurringRule(
                RecurringTransactionEntity(
                    title = title,
                    amount = amount,
                    category = category,
                    frequency = frequency,
                    nextDueDate = nextDueDate,
                    accountId = accountId,
                    accountName = accountName,
                    nature = nature
                )
            )
        }
    }

    fun markRecurringAsPaid(rule: RecurringTransactionEntity) {
        viewModelScope.launch {
            // 1. Record expense transaction
            repository.insertTransaction(
                TransactionEntity(
                    amount = rule.amount,
                    category = rule.category,
                    merchant = rule.title,
                    accountId = rule.accountId,
                    accountName = rule.accountName,
                    nature = rule.nature,
                    notes = "Recurring Bill Payment",
                    type = "EXPENSE",
                    isRecurring = true,
                    recurringRuleId = rule.id,
                    monthYear = currentMonthYear
                )
            )
            // 2. Advance next due date by 30 days
            val nextDate = rule.nextDueDate + 86400000L * 30
            repository.updateRecurringRule(
                rule.copy(
                    nextDueDate = nextDate,
                    lastPaidDate = System.currentTimeMillis()
                )
            )
        }
    }

    fun deleteRecurringRule(rule: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.deleteRecurringRule(rule)
        }
    }

    fun signInWithEmail(email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        try {
            FirebaseAuth.getInstance().signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    _isUserAuthenticated.value = true
                    onResult(true, null)
                }
                .addOnFailureListener {
                    onResult(false, it.message)
                }
        } catch (e: Exception) {
            onResult(false, e.message)
        }
    }

    fun registerWithEmail(name: String, email: String, pass: String, onResult: (Boolean, String?) -> Unit) {
        try {
            FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, pass)
                .addOnSuccessListener {
                    _isUserAuthenticated.value = true
                    onResult(true, null)
                }
                .addOnFailureListener {
                    onResult(false, it.message)
                }
        } catch (e: Exception) {
            onResult(false, e.message)
        }
    }

    fun signInWithGoogle(activity: Activity) {
        _isUserAuthenticated.value = true
    }

    fun continueAsOfflineAdmin(name: String, email: String) {
        _isUserAuthenticated.value = true
    }

    fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // ignore
        }
        _isUserAuthenticated.value = false
    }

    fun resetDatabase() {
        viewModelScope.launch {
            repository.clearAllData()
            repository.seedInitialDataIfEmpty()
        }
    }
}
