package com.example.util

import com.example.data.model.AccountEntity
import com.example.data.model.FinancialGoalEntity
import com.example.data.model.MonthSummary
import com.example.data.model.ParsedTransaction
import java.text.NumberFormat
import java.util.*
import java.util.regex.Pattern

object CurrencyFormatter {
    private val format = NumberFormat.getCurrencyInstance(Locale.US)

    fun format(amount: Double): String {
        return format.format(amount)
    }

    fun formatCompact(amount: Double): String {
        return when {
            Math.abs(amount) >= 1_000_000 -> String.format(Locale.US, "$%.1fM", amount / 1_000_000)
            Math.abs(amount) >= 1_000 -> String.format(Locale.US, "$%.1fk", amount / 1_000)
            else -> format.format(amount)
        }
    }
}

object SmartTransactionParser {
    private val AMOUNT_PATTERN = Pattern.compile("""(?:\$|USD\s*|Rs\.?\s*|INR\s*)?([0-9]+(?:,[0-9]{3})*(?:\.[0-9]{1,2})?)""")
    private val SPENT_KEYWORDS = listOf("spent", "debited", "paid", "purchase", "charged", "sent", "withdrawn")
    private val RECEIVED_KEYWORDS = listOf("credited", "received", "deposited", "refund", "salary", "bonus")

    fun parse(text: String): ParsedTransaction? {
        val lower = text.lowercase(Locale.ROOT)
        var isExpense = true
        for (kw in SPENT_KEYWORDS) {
            if (lower.contains(kw)) {
                isExpense = true
                break
            }
        }
        for (kw in RECEIVED_KEYWORDS) {
            if (lower.contains(kw)) {
                isExpense = false
                break
            }
        }

        val matcher = AMOUNT_PATTERN.matcher(text)
        var extractedAmount: Double? = null
        while (matcher.find()) {
            val numStr = matcher.group(1)?.replace(",", "")
            val parsed = numStr?.toDoubleOrNull()
            if (parsed != null && parsed > 0.0) {
                extractedAmount = parsed
                break
            }
        }

        if (extractedAmount == null) return null

        // Detect Merchant
        var merchant = "Merchant"
        val atMatcher = Pattern.compile("""(?i)(?:at|to|from|for|in)\s+([A-Za-z0-9\s&'.]{2,25})""").matcher(text)
        if (atMatcher.find()) {
            val candidate = atMatcher.group(1)?.trim() ?: ""
            if (candidate.isNotEmpty() && !candidate.equals("your", ignoreCase = true) && !candidate.equals("the", ignoreCase = true)) {
                merchant = candidate.split(" ").take(3).joinToString(" ")
            }
        }

        // Categorize
        val category = when {
            lower.contains("grocery") || lower.contains("supermarket") || lower.contains("food") || lower.contains("whole foods") || lower.contains("trader joe") -> "Groceries & Food"
            lower.contains("uber") || lower.contains("lyft") || lower.contains("gas") || lower.contains("chevron") || lower.contains("shell") -> "Transportation"
            lower.contains("restaurant") || lower.contains("cafe") || lower.contains("starbucks") || lower.contains("dine") -> "Dining Out"
            lower.contains("netflix") || lower.contains("spotify") || lower.contains("movie") || lower.contains("entertainment") -> "Entertainment"
            lower.contains("electric") || lower.contains("water") || lower.contains("internet") || lower.contains("wifi") || lower.contains("utility") -> "Utilities & Internet"
            lower.contains("rent") || lower.contains("mortgage") -> "Housing & Mortgage"
            lower.contains("hospital") || lower.contains("pharmacy") || lower.contains("doctor") || lower.contains("cvs") || lower.contains("walgreens") -> "Healthcare"
            lower.contains("salary") || lower.contains("payroll") || lower.contains("deposit") -> "Salary"
            else -> if (isExpense) "Shopping" else "Other Income"
        }

        val nature = when (category) {
            "Housing & Mortgage", "Groceries & Food", "Utilities & Internet", "Healthcare", "Transportation" -> "NEED"
            "Dining Out", "Entertainment", "Shopping" -> "WANT"
            else -> "SAVINGS"
        }

        return ParsedTransaction(
            amount = extractedAmount,
            merchant = merchant,
            category = category,
            type = if (isExpense) "EXPENSE" else "INCOME",
            nature = nature,
            paymentMethod = "CREDIT_CARD",
            rawText = text
        )
    }
}

data class CfoReviewReport(
    val healthScore: Int, // 0 - 100
    val healthGrade: String, // "Excellent", "Healthy", "Attention Required", "Critical"
    val netWorth: Double,
    val monthlySavingsRate: Double,
    val runwayMonths: Double,
    val needsRatio: Double,
    val wantsRatio: Double,
    val savingsRatio: Double,
    val keyInsights: List<String>,
    val actionItems: List<String>
)

object CfoAdvisorEngine {
    fun generateReport(
        summary: MonthSummary,
        accounts: List<AccountEntity>,
        goals: List<FinancialGoalEntity>
    ): CfoReviewReport {
        val totalAssets = accounts.filter { it.balance > 0 }.sumOf { it.balance }
        val totalDebts = accounts.filter { it.balance < 0 }.sumOf { Math.abs(it.balance) }
        val netWorth = totalAssets - totalDebts

        val income = if (summary.totalIncome > 0) summary.totalIncome else 1.0
        val needsRatio = (summary.needsSpend / income) * 100
        val wantsRatio = (summary.wantsSpend / income) * 100
        val savingsRate = ((summary.totalIncome - summary.totalExpense) / income) * 100

        val monthlyBurn = if (summary.totalExpense > 0) summary.totalExpense else 2500.0
        val liquidCash = accounts.filter { it.type == "CHECKING" || it.type == "SAVINGS" || it.type == "CASH" }.sumOf { it.balance }
        val runway = if (monthlyBurn > 0) liquidCash / monthlyBurn else 12.0

        var score = 70
        if (runway >= 6.0) score += 10
        if (savingsRate >= 20.0) score += 15
        if (needsRatio <= 50.0) score += 5
        if (totalDebts > totalAssets * 0.5) score -= 15
        if (score > 100) score = 100
        if (score < 20) score = 20

        val grade = when {
            score >= 85 -> "Excellent"
            score >= 70 -> "Healthy"
            score >= 50 -> "Caution"
            else -> "At Risk"
        }

        val insights = mutableListOf<String>()
        val actions = mutableListOf<String>()

        if (runway >= 6.0) {
            insights.add("Your liquid emergency reserve covers ${String.format(Locale.US, "%.1f", runway)} months of household expenses.")
        } else {
            insights.add("Emergency buffer is currently ${String.format(Locale.US, "%.1f", runway)} months (target: 6.0 months).")
            actions.add("Direct excess monthly cashflow to high-yield savings until runway reaches 6 months.")
        }

        if (savingsRate >= 20.0) {
            insights.add("Strong savings rate of ${String.format(Locale.US, "%.1f", savingsRate)}%, exceeding the classic 50/30/20 target.")
        } else {
            insights.add("Current monthly savings rate is ${String.format(Locale.US, "%.1f", Math.max(0.0, savingsRate))}%.")
            actions.add("Review discretionary want categories (Dining, Entertainment) to reclaim 5-10% in savings.")
        }

        if (needsRatio > 55.0) {
            insights.add("Fixed needs consume ${String.format(Locale.US, "%.1f", needsRatio)}% of income (recommended ceiling: 50%).")
            actions.add("Audit recurring subscription rules and utility providers for lower rate plans.")
        }

        val pendingGoals = goals.filter { !it.isCompleted }
        if (pendingGoals.isNotEmpty()) {
            insights.add("${pendingGoals.size} financial goals actively tracked across the household.")
        }

        return CfoReviewReport(
            healthScore = score,
            healthGrade = grade,
            netWorth = netWorth,
            monthlySavingsRate = Math.max(0.0, savingsRate),
            runwayMonths = runway,
            needsRatio = needsRatio,
            wantsRatio = wantsRatio,
            savingsRatio = Math.max(0.0, savingsRate),
            keyInsights = insights,
            actionItems = actions
        )
    }
}
