package com.example.ui.viewmodel

import android.app.Activity
import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseInitializer
import com.example.data.model.AccountEntity
import com.example.data.model.CategoryBudgetEntity
import com.example.data.model.ExpenseNature
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.HouseholdMember
import com.example.data.model.MemberColorPalette
import com.example.data.model.MemberProfile
import com.example.data.model.PaymentMethod
import com.example.data.model.RecurringRuleEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.data.model.UserRole
import com.example.data.model.toMemberProfile
import com.example.data.repository.FinanceRepository
import com.example.data.sync.CloudSyncManager
import com.example.util.CfoAdvisorEngine
import com.example.util.CfoReviewReport
import com.example.util.GoalFeasibilityInsight
import com.example.util.GoalSimulationResult
import com.example.util.ParsedTransaction
import com.example.util.SmartTransactionParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class MonthSummary(
    val monthYear: String, // "2026-10"
    val displayMonth: String, // "October 2026"
    val totalIncome: Double,
    val scheduledExpenses: Double,
    val randomExpenses: Double,
    val investments: Double,
    val availableBalance: Double,
    val discretionaryBudget: Double,
    val remainingDiscretionary: Double,
    val pratikSpent: Double,
    val manasviSpent: Double,
    val jointSpent: Double,
    val memberSpendBreakdown: Map<String, Double> = emptyMap()
)

class FinanceViewModel(application: Application) : AndroidViewModel(application) {

    private val database = AppDatabase.getDatabase(application)
    val cloudSyncManager = CloudSyncManager(application, database)
    val currentUser = cloudSyncManager.currentUser
    val syncState = cloudSyncManager.syncState

    private val repository = FinanceRepository(database)

    // Current selected month: defaults to October 2026 per brief
    private val _selectedMonth = MutableStateFlow("2026-10")
    val selectedMonth: StateFlow<String> = _selectedMonth.asStateFlow()

    // Household filter: null means All Household, or a specific member name ("PRATIK", "MANASVI", etc.), or "JOINT"
    private val _selectedMemberName = MutableStateFlow<String?>(null)
    val selectedMemberName: StateFlow<String?> = _selectedMemberName.asStateFlow()

    // Backwards-compatible enum representation
    private val _selectedMember = MutableStateFlow<HouseholdMember?>(null)
    val selectedMember: StateFlow<HouseholdMember?> = _selectedMember.asStateFlow()

    // Active bottom navigation tab
    private val _currentTab = MutableStateFlow(0) // 0: Dashboard, 1: Recurring, 2: Budgets, 3: Reports, 4: Accounts
    val currentTab: StateFlow<Int> = _currentTab.asStateFlow()

    // Dialog & Smart detection states
    private val _isAddExpenseOpen = MutableStateFlow(false)
    val isAddExpenseOpen: StateFlow<Boolean> = _isAddExpenseOpen.asStateFlow()

    private val _isAddIncomeOpen = MutableStateFlow(false)
    val isAddIncomeOpen: StateFlow<Boolean> = _isAddIncomeOpen.asStateFlow()

    private val _isSmartDetectOpen = MutableStateFlow(false)
    val isSmartDetectOpen: StateFlow<Boolean> = _isSmartDetectOpen.asStateFlow()

    private val _isCfoAdvisorOpen = MutableStateFlow(false)
    val isCfoAdvisorOpen: StateFlow<Boolean> = _isCfoAdvisorOpen.asStateFlow()

    private val _isHouseholdHubOpen = MutableStateFlow(false)
    val isHouseholdHubOpen: StateFlow<Boolean> = _isHouseholdHubOpen.asStateFlow()

    private val _isEditBudgetPlanOpen = MutableStateFlow(false)
    val isEditBudgetPlanOpen: StateFlow<Boolean> = _isEditBudgetPlanOpen.asStateFlow()

    private val _suggestedTransaction = MutableStateFlow<ParsedTransaction?>(null)
    val suggestedTransaction: StateFlow<ParsedTransaction?> = _suggestedTransaction.asStateFlow()

    // Auto-complete suggestion for merchant entry
    private val _merchantSuggestion = MutableStateFlow<TransactionEntity?>(null)
    val merchantSuggestion: StateFlow<TransactionEntity?> = _merchantSuggestion.asStateFlow()

    // Household and dynamic members
    val household: StateFlow<HouseholdEntity?> = repository.getHousehold()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val users: StateFlow<List<UserEntity>> = repository.getUsers()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val memberProfiles: StateFlow<List<MemberProfile>> = users.map { list ->
        list.map { it.toMemberProfile() }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val goals: StateFlow<List<FinancialGoalEntity>> = repository.getGoals()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Sensitivity State: controls obscuring balances and sensitive details
    private val _isSensitiveRevealed = MutableStateFlow(false)
    val isSensitiveRevealed: StateFlow<Boolean> = _isSensitiveRevealed.asStateFlow()

    fun toggleSensitivity() {
        _isSensitiveRevealed.value = !_isSensitiveRevealed.value
    }

    fun setSensitivityRevealed(revealed: Boolean) {
        _isSensitiveRevealed.value = revealed
    }

    // Active User / Persona (persisted in SharedPreferences)
    private val prefs = application.getSharedPreferences("household_cfo_prefs", android.content.Context.MODE_PRIVATE)
    private val _activeUserId = MutableStateFlow<Long?>(
        prefs.getLong("active_user_id", -1L).takeIf { it > 0 }
    )
    val activeUserId: StateFlow<Long?> = _activeUserId.asStateFlow()

    val activeUser: StateFlow<UserEntity?> = combine(users, _activeUserId) { userList, selectedId ->
        if (selectedId != null) {
            userList.find { it.id == selectedId }
        } else {
            userList.find { it.role.equals("ADMIN", ignoreCase = true) } ?: userList.firstOrNull()
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val activeUserRole: StateFlow<UserRole> = activeUser.map { user ->
        UserRole.fromString(user?.role)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserRole.ADMIN)

    fun setActiveUser(user: UserEntity) {
        prefs.edit().putLong("active_user_id", user.id).apply()
        _activeUserId.value = user.id
    }

    // Active Data Flows from Room
    val allTransactions: StateFlow<List<TransactionEntity>> = combine(
        _selectedMonth,
        _selectedMemberName,
        database.transactionDao().getAllTransactions()
    ) { month, memberName, allTx ->
        allTx.filter { tx ->
            val monthMatches = tx.monthYear == month
            val memberMatches = memberName == null ||
                tx.paidBy.equals(memberName, ignoreCase = true) ||
                tx.paidBy.equals("JOINT", ignoreCase = true)
            monthMatches && memberMatches
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recurringRules: StateFlow<List<RecurringRuleEntity>> = repository.getAllRecurringRules()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val accounts: StateFlow<List<AccountEntity>> = repository.getAllAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val budgets: StateFlow<List<CategoryBudgetEntity>> = repository.getBudgets("")
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Month Summary computation
    val monthSummary: StateFlow<MonthSummary> = combine(
        database.transactionDao().getAllTransactions(),
        recurringRules,
        budgets,
        household,
        _selectedMonth
    ) { allTx, rules, budgetList, h, month ->
        computeMonthSummary(month, allTx, rules, budgetList, h)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        MonthSummary(
            monthYear = "2026-10",
            displayMonth = "October 2026",
            totalIncome = 0.0,
            scheduledExpenses = 0.0,
            randomExpenses = 0.0,
            investments = 0.0,
            availableBalance = 0.0,
            discretionaryBudget = 50000.0,
            remainingDiscretionary = 50000.0,
            pratikSpent = 0.0,
            manasviSpent = 0.0,
            jointSpent = 0.0
        )
    )

    init {
        viewModelScope.launch {
            DatabaseInitializer.populateInitialData(database)
            val h = repository.getPrimaryHousehold()
            val code = h?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            cloudSyncManager.startRealtimeSync(code, viewModelScope)
        }
        cloudSyncManager.attemptSilentSignIn(viewModelScope)
    }

    private fun computeMonthSummary(
        month: String,
        allTx: List<TransactionEntity>,
        rules: List<RecurringRuleEntity>,
        budgetList: List<CategoryBudgetEntity>,
        h: HouseholdEntity? = null
    ): MonthSummary {
        val monthTx = allTx.filter { it.monthYear == month }

        // Expected or actual income for the month
        val txIncomes = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
        val ruleIncomes = rules.filter { it.type == "INCOME" && it.isActive }.sumOf { it.amount }
        val totalIncome = if (txIncomes > 0) maxOf(txIncomes, ruleIncomes) else ruleIncomes

        // Scheduled Commitments
        val ruleCommitments = rules.filter { it.nature == "CORE_COMMITMENT" && it.type == "EXPENSE" && it.isActive }
            .sumOf { rule ->
                if (rule.frequency == "ANNUAL") {
                    val mInt = month.substringAfter("-").toIntOrNull() ?: 10
                    if (rule.dueMonth == mInt) rule.amount else 0.0
                } else rule.amount
            }

        // Random / Discretionary expenses
        val randomExpenses = monthTx.filter { it.type == "EXPENSE" && it.nature == "DISCRETIONARY" }
            .sumOf { it.amount }

        // Investments (SIP + NPS)
        val investments = rules.filter { it.type == "INVESTMENT" && it.isActive }.sumOf { it.amount }

        // Available Cash Flow calculated based on current date of the month:
        // What has actually debited and credited up to the current day
        val calNow = Calendar.getInstance()
        val currentYearMonth = String.format(Locale.US, "%04d-%02d", calNow.get(Calendar.YEAR), calNow.get(Calendar.MONTH) + 1)
        val currentDay = calNow.get(Calendar.DAY_OF_MONTH)
        val isCurrentMonth = month == currentYearMonth
        val isPastMonth = month < currentYearMonth

        val availableBalance = if (isCurrentMonth) {
            // Realized income up to today: recorded income transactions + active income rules due on/before today not yet recorded
            val realizedIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount } +
                rules.filter { rule ->
                    rule.type == "INCOME" && rule.isActive && rule.dueDay <= currentDay &&
                    monthTx.none { it.recurringRuleId == rule.id || it.merchant.equals(rule.title, ignoreCase = true) }
                }.sumOf { it.amount }

            // Scheduled expenses debited up to today: recorded scheduled expense transactions + rules due on/before today not yet recorded
            val debitedScheduled = monthTx.filter { it.type == "EXPENSE" && (it.isRecurring || it.nature == "CORE_COMMITMENT") }.sumOf { it.amount } +
                rules.filter { rule ->
                    rule.nature == "CORE_COMMITMENT" && rule.type == "EXPENSE" && rule.isActive && rule.dueDay <= currentDay &&
                    monthTx.none { it.recurringRuleId == rule.id || it.merchant.equals(rule.title, ignoreCase = true) }
                }.sumOf { rule ->
                    if (rule.frequency == "ANNUAL") {
                        val mInt = month.substringAfter("-").toIntOrNull() ?: 10
                        if (rule.dueMonth == mInt) rule.amount else 0.0
                    } else rule.amount
                }

            // Investments debited up to today
            val debitedInvestments = monthTx.filter { it.type == "INVESTMENT" || (it.nature == "INVESTMENT" && it.type == "EXPENSE") }.sumOf { it.amount } +
                rules.filter { rule ->
                    rule.type == "INVESTMENT" && rule.isActive && rule.dueDay <= currentDay &&
                    monthTx.none { it.recurringRuleId == rule.id || it.merchant.equals(rule.title, ignoreCase = true) }
                }.sumOf { it.amount }

            realizedIncome - (debitedScheduled + debitedInvestments + randomExpenses)
        } else if (isPastMonth) {
            // In past months, all scheduled items and transactions have concluded
            totalIncome - (ruleCommitments + randomExpenses + investments)
        } else {
            // Future month: only explicit pre-recorded transactions
            val futIncome = monthTx.filter { it.type == "INCOME" }.sumOf { it.amount }
            val futExpense = monthTx.filter { it.type == "EXPENSE" }.sumOf { it.amount }
            futIncome - futExpense
        }

        val discretionaryBudget = h?.monthlyDiscretionaryBudget?.takeIf { it > 0 }
            ?: budgetList.sumOf { it.monthlyBudget }.takeIf { it > 0 }
            ?: 50000.0
        val remainingDiscretionary = maxOf(0.0, discretionaryBudget - randomExpenses)

        val spendMap = mutableMapOf<String, Double>()
        monthTx.filter { it.type == "EXPENSE" }.forEach { tx ->
            val key = tx.paidBy.uppercase(Locale.ROOT).trim()
            spendMap[key] = (spendMap[key] ?: 0.0) + tx.amount
        }

        val pratik = spendMap["PRATIK"] ?: 0.0
        val manasvi = spendMap["MANASVI"] ?: 0.0
        val joint = spendMap["JOINT"] ?: 0.0

        val displayMonth = formatDisplayMonth(month)

        return MonthSummary(
            monthYear = month,
            displayMonth = displayMonth,
            totalIncome = totalIncome,
            scheduledExpenses = ruleCommitments,
            randomExpenses = randomExpenses,
            investments = investments,
            availableBalance = availableBalance,
            discretionaryBudget = discretionaryBudget,
            remainingDiscretionary = remainingDiscretionary,
            pratikSpent = pratik,
            manasviSpent = manasvi,
            jointSpent = joint,
            memberSpendBreakdown = spendMap
        )
    }

    private fun formatDisplayMonth(monthYear: String): String {
        return try {
            val parts = monthYear.split("-")
            val year = parts[0]
            val month = parts[1].toInt()
            val monthNames = arrayOf(
                "January", "February", "March", "April", "May", "June",
                "July", "August", "September", "October", "November", "December"
            )
            "${monthNames[month - 1]} $year"
        } catch (e: Exception) {
            monthYear
        }
    }

    fun setMonth(monthYear: String) {
        _selectedMonth.value = monthYear
    }

    fun setPreviousMonth() {
        val cur = _selectedMonth.value.split("-")
        var y = cur[0].toInt()
        var m = cur[1].toInt() - 1
        if (m < 1) {
            m = 12
            y -= 1
        }
        _selectedMonth.value = String.format(Locale.US, "%04d-%02d", y, m)
    }

    fun setNextMonth() {
        val cur = _selectedMonth.value.split("-")
        var y = cur[0].toInt()
        var m = cur[1].toInt() + 1
        if (m > 12) {
            m = 1
            y += 1
        }
        _selectedMonth.value = String.format(Locale.US, "%04d-%02d", y, m)
    }

    fun setMember(member: HouseholdMember?) {
        _selectedMember.value = member
        _selectedMemberName.value = member?.name
    }

    fun setMemberByName(memberName: String?) {
        _selectedMemberName.value = memberName
        _selectedMember.value = HouseholdMember.fromId(memberName)
    }

    fun setTab(index: Int) {
        _currentTab.value = index
    }

    fun openAddExpense() {
        _isAddExpenseOpen.value = true
    }

    fun closeAddExpense() {
        _isAddExpenseOpen.value = false
        _merchantSuggestion.value = null
    }

    fun openAddIncome() {
        _isAddIncomeOpen.value = true
    }

    fun closeAddIncome() {
        _isAddIncomeOpen.value = false
    }

    fun openSmartDetect() {
        _isSmartDetectOpen.value = true
    }

    fun closeSmartDetect() {
        _isSmartDetectOpen.value = false
        _suggestedTransaction.value = null
    }

    fun openCfoAdvisor() {
        _isCfoAdvisorOpen.value = true
    }

    fun closeCfoAdvisor() {
        _isCfoAdvisorOpen.value = false
    }

    fun openHouseholdHub() {
        _isHouseholdHubOpen.value = true
    }

    fun closeHouseholdHub() {
        _isHouseholdHubOpen.value = false
    }

    fun openEditBudgetPlan() {
        _isEditBudgetPlanOpen.value = true
    }

    fun closeEditBudgetPlan() {
        _isEditBudgetPlanOpen.value = false
    }

    fun onMerchantTyped(merchant: String) {
        if (merchant.isBlank()) {
            _merchantSuggestion.value = null
            return
        }
        viewModelScope.launch {
            val history = database.transactionDao().findRecentByMerchant(merchant.trim())
            _merchantSuggestion.value = history
        }
    }

    fun addExpense(
        amount: Double,
        category: String,
        paidBy: String,
        paymentMethod: PaymentMethod,
        accountId: Long?,
        accountName: String,
        merchant: String,
        notes: String,
        timestamp: Long = System.currentTimeMillis(),
        nature: ExpenseNature = ExpenseNature.DISCRETIONARY
    ) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            val monthYear = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

            val matchedUser = users.value.find { it.name.equals(paidBy, ignoreCase = true) }
            val userId = matchedUser?.id

            val validAccountId = if (accountId != null && accountId > 0) accountId else null

            val tx = TransactionEntity(
                householdId = 1L,
                userId = userId,
                amount = amount,
                category = category,
                paidBy = paidBy.trim().uppercase(),
                paymentMethod = paymentMethod.name,
                accountId = validAccountId,
                accountName = accountName,
                merchant = merchant.trim(),
                notes = notes.trim(),
                timestamp = timestamp,
                monthYear = monthYear,
                nature = nature.name,
                type = "EXPENSE",
                isRecurring = false
            )
            val id = repository.insertTransaction(tx)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            cloudSyncManager.pushTransaction(tx.copy(id = id), code, viewModelScope)
            _selectedMonth.value = monthYear
            closeAddExpense()
        }
    }

    // Overload for HouseholdMember backward compatibility
    fun addExpense(
        amount: Double,
        category: String,
        paidBy: HouseholdMember,
        paymentMethod: PaymentMethod,
        accountId: Long?,
        accountName: String,
        merchant: String,
        notes: String,
        timestamp: Long = System.currentTimeMillis(),
        nature: ExpenseNature = ExpenseNature.DISCRETIONARY
    ) {
        addExpense(
            amount = amount,
            category = category,
            paidBy = paidBy.name,
            paymentMethod = paymentMethod,
            accountId = accountId,
            accountName = accountName,
            merchant = merchant,
            notes = notes,
            timestamp = timestamp,
            nature = nature
        )
    }

    fun addIncome(
        amount: Double,
        category: String,
        paidBy: String,
        paymentMethod: PaymentMethod,
        accountId: Long?,
        accountName: String,
        source: String,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            cal.timeInMillis = timestamp
            val monthYear = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)

            val matchedUser = users.value.find { it.name.equals(paidBy, ignoreCase = true) }
            val userId = matchedUser?.id

            val validAccountId = if (accountId != null && accountId > 0) accountId else null

            val tx = TransactionEntity(
                householdId = 1L,
                userId = userId,
                amount = amount,
                category = category,
                paidBy = paidBy.trim().uppercase(),
                paymentMethod = paymentMethod.name,
                accountId = validAccountId,
                accountName = accountName,
                merchant = source.trim(),
                notes = notes.trim(),
                timestamp = timestamp,
                monthYear = monthYear,
                nature = "CORE_COMMITMENT",
                type = "INCOME",
                isRecurring = false
            )
            val id = repository.insertTransaction(tx)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            cloudSyncManager.pushTransaction(tx.copy(id = id), code, viewModelScope)
            closeAddIncome()
        }
    }

    // Overload for HouseholdMember backward compatibility
    fun addIncome(
        amount: Double,
        category: String,
        paidBy: HouseholdMember,
        paymentMethod: PaymentMethod,
        accountId: Long?,
        accountName: String,
        source: String,
        notes: String,
        timestamp: Long = System.currentTimeMillis()
    ) {
        addIncome(
            amount = amount,
            category = category,
            paidBy = paidBy.name,
            paymentMethod = paymentMethod,
            accountId = accountId,
            accountName = accountName,
            source = source,
            notes = notes,
            timestamp = timestamp
        )
    }

    fun deleteTransaction(transaction: TransactionEntity) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            cloudSyncManager.deleteTransaction(transaction.id, code, viewModelScope)
        }
    }

    fun markRecurringAsPaid(rule: RecurringRuleEntity) {
        viewModelScope.launch {
            val id = repository.markRecurringAsPaid(rule, _selectedMonth.value)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            val tx = TransactionEntity(
                id = id,
                householdId = rule.householdId,
                userId = rule.userId,
                amount = rule.amount,
                category = rule.category,
                paidBy = rule.paidBy,
                paymentMethod = rule.paymentMethod,
                accountId = rule.accountId,
                accountName = rule.accountName,
                merchant = rule.title,
                notes = "Auto-recorded scheduled commitment for ${_selectedMonth.value}",
                timestamp = System.currentTimeMillis(),
                monthYear = _selectedMonth.value,
                nature = rule.nature,
                type = rule.type,
                isRecurring = true,
                recurringRuleId = rule.id
            )
            cloudSyncManager.pushTransaction(tx, code, viewModelScope)
        }
    }

    fun saveBudget(category: String, amount: Double) {
        viewModelScope.launch {
            repository.saveBudget(category, amount)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            val allB = repository.getBudgets("").first()
            val total = allB.sumOf { it.monthlyBudget }
            cloudSyncManager.pushBudgetPlan(code, total, allB, viewModelScope)
        }
    }

    /**
     * Add any number of people/members to the household (generalized 1, 2, 3, 4+)
     */
    fun addHouseholdMember(
        name: String,
        email: String = "",
        role: String = "MEMBER",
        colorHex: String = "#4F46E5",
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) {
            onResult(false, "Member name cannot be blank.")
            return
        }
        val isDuplicate = users.value.any { it.name.equals(cleanName, ignoreCase = true) }
        if (isDuplicate) {
            onResult(false, "A member with name '$cleanName' already exists. Member names must be unique.")
            return
        }
        viewModelScope.launch {
            val newMember = UserEntity(
                householdId = 1L,
                name = cleanName,
                email = email.trim(),
                role = role,
                initial = cleanName.take(1).uppercase(),
                colorHex = colorHex
            )
            val userId = repository.saveUser(newMember)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            cloudSyncManager.pushMember(newMember.copy(id = userId), code, viewModelScope)
            onResult(true, "Added $cleanName to household!")
        }
    }

    fun updateMemberRole(userId: Long, newRole: String) {
        viewModelScope.launch {
            repository.updateUserRole(userId, newRole)
            val user = users.value.find { it.id == userId }
            if (user != null) {
                val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
                cloudSyncManager.pushMember(user.copy(role = newRole), code, viewModelScope)
            }
        }
    }

    fun removeMember(userId: Long) {
        viewModelScope.launch {
            repository.deleteUser(userId)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            cloudSyncManager.deleteMember(userId, code, viewModelScope)
        }
    }

    fun addAccount(
        name: String,
        holder: String,
        accountType: String,
        balance: Double,
        accountNumberMask: String = "",
        institution: String = "",
        notes: String = "",
        creditLimit: Double = 0.0,
        userId: Long? = null
    ) {
        viewModelScope.launch {
            val acc = AccountEntity(
                householdId = 1L,
                userId = userId,
                name = name,
                holder = holder,
                accountType = accountType,
                balance = balance,
                accountNumberMask = accountNumberMask,
                institution = institution,
                notes = notes,
                creditLimit = creditLimit
            )
            val newId = repository.saveAccount(acc)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            cloudSyncManager.pushAccount(acc.copy(id = newId), code, viewModelScope)
        }
    }

    fun updateAccount(account: AccountEntity) {
        viewModelScope.launch {
            repository.updateAccount(account)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            cloudSyncManager.pushAccount(account, code, viewModelScope)
        }
    }

    fun deleteAccount(accountId: Long) {
        viewModelScope.launch {
            repository.deleteAccount(accountId)
            val code = household.value?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            cloudSyncManager.deleteAccount(accountId, code, viewModelScope)
        }
    }

    fun registerHousehold(
        adminName: String,
        adminEmail: String,
        householdName: String,
        discretionaryBudget: Double,
        securityPin: String,
        onResult: (Boolean, String) -> Unit = { _, _ -> }
    ) {
        val cleanAdmin = adminName.trim()
        val cleanHName = householdName.trim()
        if (cleanAdmin.isBlank()) {
            onResult(false, "Please enter your name as the Household Admin.")
            return
        }
        if (cleanHName.isBlank()) {
            onResult(false, "Please enter a unique Household Name.")
            return
        }
        viewModelScope.launch {
            val existing = repository.getHouseholdByName(cleanHName)
            if (existing != null) {
                onResult(false, "Household name '$cleanHName' is already registered. Household names must be unique.")
                return@launch
            }
            DatabaseInitializer.registerHouseholdAndAdmin(
                database = database,
                adminName = cleanAdmin,
                adminEmail = adminEmail.trim(),
                householdName = cleanHName,
                discretionaryBudget = discretionaryBudget,
                securityPin = securityPin
            )
            val h = repository.getPrimaryHousehold()
            if (h != null) {
                cloudSyncManager.publishHousehold(h, viewModelScope)
            }
            onResult(true, "Household created successfully!")
        }
    }

    /**
     * Edit the household budget plan (Admin editable)
     */
    fun updateHouseholdBudgetPlan(
        monthlyDiscretionaryBudget: Double,
        categoryBudgets: Map<String, Double>
    ) {
        viewModelScope.launch {
            val h = repository.getPrimaryHousehold()
            if (h != null) {
                val updatedH = h.copy(monthlyDiscretionaryBudget = monthlyDiscretionaryBudget)
                repository.updateHousehold(updatedH)
            }
            categoryBudgets.forEach { (cat, amt) ->
                repository.saveBudget(cat, amt)
            }
            val code = h?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
            val budgetEntities = categoryBudgets.map { CategoryBudgetEntity(it.key, it.value) }
            cloudSyncManager.pushBudgetPlan(code, monthlyDiscretionaryBudget, budgetEntities, viewModelScope)
            closeEditBudgetPlan()
        }
    }

    /**
     * Update household display name
     */
    fun updateHouseholdName(newName: String) {
        val clean = newName.trim()
        if (clean.isBlank()) return
        viewModelScope.launch {
            val h = repository.getPrimaryHousehold() ?: return@launch
            val updated = h.copy(name = clean)
            repository.updateHousehold(updated)
            cloudSyncManager.publishHousehold(updated, viewModelScope)
        }
    }

    /**
     * Join an existing household using an Invite Code
     */
    fun joinHousehold(inviteCode: String, memberName: String, onResult: (Boolean, String) -> Unit) {
        cloudSyncManager.joinHouseholdByCode(
            inviteCode = inviteCode,
            memberName = memberName,
            scope = viewModelScope,
            onResult = { success, msg ->
                if (success) {
                    viewModelScope.launch {
                        val joinedUser = database.userDao().getUserByName(memberName.trim())
                        if (joinedUser != null) {
                            setActiveUser(joinedUser)
                        }
                    }
                }
                onResult(success, msg)
            }
        )
    }

    /**
     * Shareable link and invite code generator via Android Share Sheet
     */
    fun shareHouseholdInvite(context: Context) {
        val h = household.value
        val name = h?.name ?: "Family Household"
        val code = h?.inviteCode?.ifBlank { "CFO-8821" } ?: "CFO-8821"
        cloudSyncManager.shareHouseholdInvite(context, name, code)
    }

    fun parseSmsTransaction(text: String) {
        val parsed = SmartTransactionParser.parse(text)
        _suggestedTransaction.value = parsed
    }

    fun confirmSuggestedTransaction() {
        val st = _suggestedTransaction.value ?: return
        val user = activeUser.value ?: users.value.firstOrNull()
        val memberName = user?.name ?: "Primary"
        val acc = accounts.value.find { it.holder.equals(memberName, ignoreCase = true) }
        addExpense(
            amount = st.amount,
            category = st.category,
            paidBy = memberName,
            paymentMethod = st.paymentMethod,
            accountId = acc?.id,
            accountName = acc?.name ?: "Primary Account",
            merchant = st.merchant,
            notes = "Smart parsed: ${st.rawText.take(40)}..."
        )
        closeSmartDetect()
    }

    fun captureNotificationTransaction(item: com.example.service.CapturedNotification) {
        val parsed = item.parsedTransaction ?: SmartTransactionParser.parse(item.body)
        if (parsed != null) {
            val user = activeUser.value ?: users.value.firstOrNull()
            val memberName = user?.name ?: "Primary"
            val acc = accounts.value.find { it.holder.equals(memberName, ignoreCase = true) }
            addExpense(
                amount = parsed.amount,
                category = parsed.category,
                paidBy = memberName,
                paymentMethod = parsed.paymentMethod,
                accountId = acc?.id,
                accountName = acc?.name ?: "Primary Account",
                merchant = parsed.merchant,
                notes = "Auto-captured from ${item.appName}: ${item.body.take(60)}"
            )
            com.example.service.SmartNotificationManager.markNotificationSaved(item.id)
        }
    }

    fun generateCfoReport(): CfoReviewReport {
        val summary = monthSummary.value
        val txs = allTransactions.value
        val rules = recurringRules.value
        val bList = budgets.value
        val gList = goals.value
        return CfoAdvisorEngine.generateReport(
            summary = summary,
            transactions = txs,
            recurringRules = rules,
            budgets = bList,
            goals = gList
        )
    }

    fun simulateGoal(
        title: String,
        targetAmount: Double,
        currentSaved: Double = 0.0,
        targetMonths: Int = 12
    ): GoalSimulationResult {
        val summary = monthSummary.value
        val surplus = maxOf(0.0, summary.totalIncome - (summary.scheduledExpenses + summary.investments + summary.randomExpenses))
        return CfoAdvisorEngine.simulateCustomGoal(
            title = title,
            targetAmount = targetAmount,
            currentSaved = currentSaved,
            targetMonths = targetMonths,
            monthlySurplus = maxOf(10000.0, surplus),
            discretionaryBuffer = maxOf(0.0, summary.discretionaryBudget - summary.randomExpenses)
        )
    }

    fun addGoal(
        title: String,
        category: String = "CUSTOM",
        targetAmount: Double,
        currentSaved: Double = 0.0,
        targetMonths: Int = 12,
        priority: String = "MEDIUM",
        notes: String = ""
    ) {
        viewModelScope.launch {
            val hid = household.value?.id ?: 1L
            val entity = FinancialGoalEntity(
                householdId = hid,
                title = title.trim(),
                category = category,
                targetAmount = targetAmount,
                currentSaved = currentSaved,
                targetMonths = maxOf(1, targetMonths),
                priority = priority,
                notes = notes.trim(),
                createdAt = System.currentTimeMillis(),
                isCompleted = currentSaved >= targetAmount && targetAmount > 0
            )
            repository.saveGoal(entity)
        }
    }

    fun updateGoal(goal: FinancialGoalEntity) {
        viewModelScope.launch {
            repository.updateGoal(goal)
        }
    }

    fun deleteGoal(goalId: Long) {
        viewModelScope.launch {
            repository.deleteGoal(goalId)
        }
    }

    fun addGoalContribution(goalId: Long, additionalAmount: Double) {
        viewModelScope.launch {
            repository.addGoalContribution(goalId, additionalAmount)
        }
    }

    fun toggleGoalCompleted(goalId: Long) {
        viewModelScope.launch {
            repository.toggleGoalCompleted(goalId)
        }
    }

    fun exportCsvData(): String {
        return repository.exportTransactionsToCsv(allTransactions.value)
    }

    fun addRecurringRule(rule: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.saveRecurringRule(rule)
            autoProcessScheduledTransactions()
        }
    }

    fun updateRecurringRule(rule: RecurringTransactionEntity) {
        viewModelScope.launch {
            repository.updateRecurringRule(rule)
            autoProcessScheduledTransactions()
        }
    }

    fun deleteRecurringRule(ruleId: Long) {
        viewModelScope.launch {
            repository.deleteRecurringRule(ruleId)
        }
    }

    fun autoProcessScheduledTransactions() {
        viewModelScope.launch {
            val cal = Calendar.getInstance()
            val currentDay = cal.get(Calendar.DAY_OF_MONTH)
            val currentMonthStr = String.format(Locale.US, "%04d-%02d", cal.get(Calendar.YEAR), cal.get(Calendar.MONTH) + 1)
            val activeMonth = _selectedMonth.value

            val rules = recurringRules.value
            rules.filter { it.isActive }.forEach { rule ->
                // Check if rule's due day has arrived for current month or if viewing a past month
                val shouldAutoRecord = when {
                    activeMonth < currentMonthStr -> true
                    activeMonth == currentMonthStr -> {
                        // If dueDay is 31 or > 28, consider it end of month (or if day passed)
                        rule.dueDay <= currentDay || (rule.dueDay >= 28 && currentDay >= 28)
                    }
                    else -> false // Future months remain scheduled but not yet debited/credited
                }

                if (shouldAutoRecord) {
                    val alreadyRecorded = repository.checkRecurringRulePaid(rule.id, activeMonth)
                    if (!alreadyRecorded) {
                        markRecurringAsPaid(rule)
                    }
                }
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.clearAllData()
        }
    }

    fun resetDatabase(reseedBaseline: Boolean = true) {
        viewModelScope.launch {
            repository.resetDatabase(reseedBaseline)
        }
    }

    fun syncToCloud() {
        cloudSyncManager.syncToCloud(viewModelScope)
    }

    fun signInWithGoogle(activity: Activity) {
        cloudSyncManager.signInWithGoogle(activity, viewModelScope)
    }

    fun signOut() {
        cloudSyncManager.signOut(viewModelScope)
    }
}
