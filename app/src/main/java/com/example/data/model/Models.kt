package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class UserRole {
    ADMIN,
    MEMBER,
    VIEWER
}

enum class ExpenseNature {
    NEED,
    WANT,
    SAVINGS,
    INVESTMENT
}

enum class TransactionType {
    EXPENSE,
    INCOME,
    TRANSFER
}

enum class PaymentMethod {
    CASH,
    DEBIT_CARD,
    CREDIT_CARD,
    BANK_TRANSFER,
    UPI,
    OTHER
}

enum class GoalFeasibilityStatus {
    ON_TRACK,
    AT_RISK,
    OFF_TRACK,
    ACHIEVED
}

@Entity(tableName = "transactions")
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val householdId: Long = 1L,
    val userId: Long? = null,
    val amount: Double,
    val category: String,
    val paidBy: String = "",
    val paymentMethod: String = "CREDIT_CARD",
    val accountId: Long? = null,
    val toAccountId: Long? = null,
    val accountName: String = "",
    val merchant: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val monthYear: String = "",
    val nature: String = "NEED",
    val type: String = "EXPENSE",
    val isRecurring: Boolean = false,
    val recurringRuleId: Long? = null
)

@Entity(tableName = "accounts")
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val householdId: Long = 1L,
    val name: String,
    val type: String, // CHECKING, SAVINGS, CREDIT_CARD, INVESTMENT, CASH
    val balance: Double,
    val currency: String = "USD",
    val institution: String = "",
    val lastFour: String = "",
    val isSensitive: Boolean = false,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "category_budgets")
data class CategoryBudgetEntity(
    @PrimaryKey
    val category: String,
    val householdId: Long = 1L,
    val budgetAmount: Double,
    val nature: String = "NEED",
    val monthYear: String = ""
)

@Entity(tableName = "financial_goals")
data class FinancialGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val householdId: Long = 1L,
    val title: String,
    val targetAmount: Double,
    val currentAmount: Double = 0.0,
    val targetDate: Long = 0L,
    val isCompleted: Boolean = false,
    val priority: Int = 1,
    val category: String = "General",
    val notes: String = ""
)

@Entity(tableName = "recurring_transactions")
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val householdId: Long = 1L,
    val title: String,
    val amount: Double,
    val category: String,
    val frequency: String = "MONTHLY", // DAILY, WEEKLY, MONTHLY, YEARLY
    val nextDueDate: Long = 0L,
    val accountId: Long? = null,
    val accountName: String = "",
    val nature: String = "NEED",
    val type: String = "EXPENSE",
    val isActive: Boolean = true,
    val lastPaidDate: Long? = null
)

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    val name: String = "Our Household",
    val securityPin: String = "",
    val budgetPlanType: String = "50_30_20",
    val currency: String = "USD",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    val householdId: Long = 1L,
    val name: String,
    val email: String,
    val role: String = "ADMIN",
    val isLocalAdmin: Boolean = true
)

data class MemberProfile(
    val id: Long,
    val name: String,
    val role: String,
    val email: String
)

data class MonthSummary(
    val totalIncome: Double = 0.0,
    val totalExpense: Double = 0.0,
    val netSavings: Double = 0.0,
    val needsSpend: Double = 0.0,
    val wantsSpend: Double = 0.0,
    val savingsSpend: Double = 0.0,
    val totalBudget: Double = 0.0,
    val percentOfBudgetUsed: Double = 0.0
)

data class ParsedTransaction(
    val amount: Double,
    val merchant: String,
    val category: String,
    val type: String,
    val nature: String,
    val paymentMethod: String,
    val rawText: String
)
