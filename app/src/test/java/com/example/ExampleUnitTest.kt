package com.example

import com.example.data.model.AccountEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.HouseholdEntity
import com.example.data.model.RecurringTransactionEntity
import com.example.data.model.TransactionEntity
import com.example.data.model.UserEntity
import com.example.util.CfoAdvisorEngine
import com.example.util.CurrencyFormatter
import com.example.util.GoalFeasibilityStatus
import com.example.util.SmartTransactionParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun testCurrencyFormatting() {
        val formatted = CurrencyFormatter.formatInr(366612.0)
        assertEquals("₹3,66,612", formatted)

        val formattedNegative = CurrencyFormatter.formatInr(-14200.0)
        assertEquals("-₹14,200", formattedNegative)
    }

    @Test
    fun testSmartSmsParsing() {
        val sms = "Rs. 480 spent on HDFC Card xx8812 at STARBUCKS on 05-OCT-26 via UPI"
        val parsed = SmartTransactionParser.parse(sms)
        assertNotNull(parsed)
        assertEquals(480.0, parsed!!.amount, 0.01)
        assertEquals("Food & Dining", parsed.category)
        assertEquals("JOINT", parsed.paidBy.name)
    }

    @Test
    fun testLiveGoalEvaluationAndSimulation() {
        val goal = FinancialGoalEntity(
            id = 1L,
            householdId = 1L,
            title = "Emergency Reserve Fund",
            category = "EMERGENCY",
            targetAmount = 300000.0,
            currentSaved = 60000.0,
            targetMonths = 12
        )
        // With surplus of 80,000/mo, required is (300k - 60k)/12 = 20,000/mo (25% share)
        val insight = CfoAdvisorEngine.evaluateGoal(
            goal = goal,
            monthlySurplus = 80000.0
        )
        assertNotNull(insight)
        assertEquals(20000.0, insight.requiredMonthlySavings, 0.1)
        assertEquals(GoalFeasibilityStatus.ON_TRACK, insight.feasibilityStatus)
        assertTrue(insight.cfoVerdict.contains("Highly feasible"))

        // Test custom simulator
        val sim = CfoAdvisorEngine.simulateCustomGoal(
            title = "Japan Vacation",
            targetAmount = 240000.0,
            currentSaved = 40000.0,
            targetMonths = 8,
            monthlySurplus = 50000.0
        )
        assertNotNull(sim)
        assertEquals(25000.0, sim.requiredMonthlySavings, 0.1)
        assertTrue(sim.isFeasible)
        assertEquals(GoalFeasibilityStatus.MODERATE_STRETCH, sim.feasibilityStatus)
    }

    @Test
    fun testRoomEntitiesSchema() {
        val household = HouseholdEntity(id = 1L, name = "Family Household")
        val userPrimary = UserEntity(id = 1L, householdId = household.id, name = "Primary", email = "primary@example.com")
        val userPartner = UserEntity(id = 2L, householdId = household.id, name = "Partner", email = "partner@example.com")

        val account = AccountEntity(
            id = 1L,
            householdId = household.id,
            userId = userPrimary.id,
            name = "HDFC Salary A/C",
            holder = "PRIMARY",
            accountType = "BANK",
            balance = 142500.0
        )

        val recurring = RecurringTransactionEntity(
            id = 1L,
            householdId = household.id,
            userId = userPrimary.id,
            title = "Home Loan EMI",
            amount = 44726.0,
            category = "Home Loan / EMI",
            paidBy = "PRIMARY",
            dueDay = 5
        )

        val transaction = TransactionEntity(
            id = 1L,
            householdId = household.id,
            userId = userPrimary.id,
            accountId = account.id,
            amount = 44726.0,
            category = "Home Loan / EMI",
            paidBy = "PRIMARY",
            paymentMethod = "AUTO_DEBIT",
            monthYear = "2026-10",
            recurringRuleId = recurring.id
        )

        assertEquals(1L, household.id)
        assertEquals(1L, userPrimary.householdId)
        assertEquals(1L, account.userId)
        assertEquals(1L, transaction.userId)
        assertEquals(44726.0, recurring.amount, 0.01)
        assertEquals(44726.0, transaction.amount, 0.01)
    }
}
