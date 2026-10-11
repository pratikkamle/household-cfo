package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.local.DatabaseInitializer
import com.example.data.model.AccountEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    private lateinit var db: AppDatabase
    private lateinit var context: Context

    @Before
    fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        DatabaseInitializer.registerHouseholdAndAdmin(
            database = db,
            adminName = "Primary Admin",
            adminEmail = "admin@example.com",
            householdName = "Family Household",
            discretionaryBudget = 50000.0,
            securityPin = "1234"
        )
        db.accountDao().insertAccount(
            AccountEntity(id = 1L, householdId = 1L, name = "HDFC Salary A/C", holder = "PRIMARY", accountType = "BANK", balance = 0.0)
        )
        DatabaseInitializer.populateInitialData(db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `read string from context`() {
        val appName = context.getString(R.string.app_name)
        assertEquals("Household CFO", appName)
    }

    @Test
    fun `test launch MainActivity`() {
        org.robolectric.Robolectric.buildActivity(MainActivity::class.java).setup()
    }

    @Test
    fun `test account replacement does not fail transactions accountId not-null constraint`() = runBlocking {
        // Insert a transaction pointing to account 1
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = 999L,
                householdId = 1L,
                amount = 250.0,
                category = "Food & Dining",
                paidBy = "PRATIK",
                accountId = 1L,
                monthYear = "2026-10"
            )
        )

        // Re-inserting / replacing account 1 (triggers onDelete = SET_NULL)
        db.accountDao().insertAccounts(
            listOf(
                AccountEntity(id = 1L, householdId = 1L, name = "HDFC Salary A/C", holder = "PRATIK", accountType = "BANK", balance = 0.0)
            )
        )
    }

    @Test
    fun `resetDatabase clears tables while preserving schema for manual data entry`() = runBlocking {
        // Insert a transaction using the pre-existing baseline account
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = 101L,
                householdId = 1L,
                amount = 1200.0,
                category = "Food & Dining",
                paidBy = "PRATIK",
                paymentMethod = "UPI",
                accountId = 1L,
                accountName = "HDFC Salary A/C",
                merchant = "Swiggy",
                monthYear = "2026-10"
            )
        )

        // Verify insertion
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)

        // Execute Reset Database with reseed baseline
        db.resetDatabase(reseedBaseline = true)

        // Transactions and recurring rules must be completely empty for manual entry
        val transactionsAfterReset = db.transactionDao().getAllTransactions().first()
        val rulesAfterReset = db.recurringRuleDao().getAllRules().first()
        assertTrue("Transactions must be empty after reset", transactionsAfterReset.isEmpty())
        assertTrue("Recurring rules must be empty after reset", rulesAfterReset.isEmpty())

        // Baseline profiles exist
        val household = db.householdDao().getPrimaryHousehold()
        assertTrue(household != null)
        assertEquals("Family Household", household?.name)

        // Schema is preserved - can immediately insert account and manual entry
        db.accountDao().insertAccount(
            AccountEntity(id = 1L, householdId = 1L, name = "HDFC Salary A/C", holder = "PRATIK", accountType = "BANK", balance = 0.0)
        )
        val newTxId = db.transactionDao().insertTransaction(
            TransactionEntity(
                id = 1L,
                householdId = 1L,
                amount = 450.0,
                category = "Groceries",
                paidBy = "MANASVI",
                paymentMethod = "UPI",
                accountId = 1L,
                accountName = "HDFC Salary A/C",
                merchant = "Zepto",
                monthYear = "2026-10"
            )
        )
        assertTrue(newTxId > 0)
        assertEquals(1, db.transactionDao().getAllTransactions().first().size)

        // Execute Wipe without reseeding
        db.resetDatabase(reseedBaseline = false)
        assertTrue(db.transactionDao().getAllTransactions().first().isEmpty())
        assertTrue(db.householdDao().getHouseholdById(1L).first() == null)

        // Schema and foreign keys are preserved even after 100% wipe
        val householdId = db.householdDao().insertHousehold(
            HouseholdEntity(id = 1L, name = "Custom New Household", currency = "INR")
        )
        val manualAccount = db.accountDao().insertAccount(
            AccountEntity(id = 1L, householdId = householdId, name = "Fresh Cash", holder = "PRATIK", accountType = "CASH", balance = 100.0)
        )
        assertTrue(manualAccount > 0)
    }

    @Test
    fun `test generalized household with dynamic members and budget editing`() = runBlocking {
        // Reset to clean baseline
        db.resetDatabase(reseedBaseline = true)
        db.accountDao().insertAccount(
            AccountEntity(id = 1L, householdId = 1L, name = "HDFC Salary A/C", holder = "PRATIK", accountType = "BANK", balance = 0.0)
        )

        // 1. Household starts with default admin
        val initialUsers = db.userDao().getAllUsers().first()
        assertTrue(initialUsers.isNotEmpty())
        assertEquals("Admin", initialUsers.first().name)

        // 2. Household Admin adds additional members
        db.userDao().insertUser(
            com.example.data.model.UserEntity(
                householdId = 1L,
                name = "Partner",
                email = "partner@example.com",
                role = "MEMBER",
                initial = "P",
                colorHex = "#E11D48"
            )
        )
        val rohanId = db.userDao().insertUser(
            com.example.data.model.UserEntity(
                householdId = 1L,
                name = "Rohan",
                email = "rohan@example.com",
                role = "MEMBER",
                initial = "R",
                colorHex = "#4F46E5"
            )
        )
        val snehaId = db.userDao().insertUser(
            com.example.data.model.UserEntity(
                householdId = 1L,
                name = "Sneha",
                email = "sneha@example.com",
                role = "MEMBER",
                initial = "S",
                colorHex = "#D946EF"
            )
        )
        val allUsers = db.userDao().getAllUsers().first()
        assertEquals(4, allUsers.size)
        assertTrue(allUsers.any { it.name == "Rohan" })
        assertTrue(allUsers.any { it.name == "Sneha" })

        // 3. Any member can log an expense
        db.transactionDao().insertTransaction(
            TransactionEntity(
                id = 201L,
                householdId = 1L,
                userId = rohanId,
                amount = 1800.0,
                category = "Food & Dining",
                paidBy = "Rohan",
                paymentMethod = "UPI",
                accountId = 1L,
                monthYear = "2026-10"
            )
        )
        val txs = db.transactionDao().getAllTransactions().first()
        assertTrue(txs.any { it.paidBy == "Rohan" && it.amount == 1800.0 })

        // 4. Admin edits household budget plan
        val h = db.householdDao().getPrimaryHousehold()!!
        db.householdDao().updateHousehold(h.copy(monthlyDiscretionaryBudget = 85000.0))
        assertEquals(85000.0, db.householdDao().getPrimaryHousehold()?.monthlyDiscretionaryBudget)

        // 5. Update category budget limits
        db.budgetDao().insertBudget(
            com.example.data.model.CategoryBudgetEntity("Food & Dining", 20000.0)
        )
        val updatedBudgets = db.budgetDao().getBudgets("").first()
        val foodBudget = updatedBudgets.find { it.category == "Food & Dining" }
        assertEquals(20000.0, foodBudget?.monthlyBudget)
    }

    @Test
    fun `test live financial goal creation progress update and completion`() = runBlocking {
        val goalId = db.goalDao().insertGoal(
            com.example.data.model.FinancialGoalEntity(
                id = 1L,
                householdId = 1L,
                title = "Emergency Reserve Fund",
                category = "EMERGENCY",
                targetAmount = 200000.0,
                currentSaved = 50000.0,
                targetMonths = 10,
                priority = "HIGH"
            )
        )
        assertTrue(goalId > 0)

        val goals = db.goalDao().getAllGoals().first()
        assertEquals(1, goals.size)
        assertEquals("Emergency Reserve Fund", goals[0].title)
        assertEquals(50000.0, goals[0].currentSaved, 0.01)

        // Add savings contribution to goal
        db.goalDao().updateGoalProgress(1L, 200000.0, true)
        val updatedGoal = db.goalDao().getGoalById(1L)
        assertTrue(updatedGoal != null)
        assertEquals(200000.0, updatedGoal!!.currentSaved, 0.01)
        assertTrue(updatedGoal.isCompleted)
    }
}
