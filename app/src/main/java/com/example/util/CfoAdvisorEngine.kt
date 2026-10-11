package com.example.util

import com.example.data.model.FinancialGoalEntity
import com.example.data.model.RecurringRuleEntity
import com.example.data.model.TransactionEntity
import java.util.Locale
import kotlin.math.ceil

enum class GoalFeasibilityStatus {
    ON_TRACK,            // Required savings <= 35% of monthly surplus
    MODERATE_STRETCH,    // Required savings <= 70% of monthly surplus
    TIGHT_BUFFER,        // Required savings <= 100% of monthly surplus
    DEFICIT_STRETCH      // Required savings > monthly surplus
}

data class GoalFeasibilityInsight(
    val goalId: Long,
    val title: String,
    val category: String,
    val targetAmount: Double,
    val currentSaved: Double,
    val remainingAmount: Double,
    val targetMonths: Int,
    val requiredMonthlySavings: Double,
    val currentMonthlySurplus: Double,
    val surplusSharePercent: Double,
    val feasibilityStatus: GoalFeasibilityStatus,
    val statusLabel: String,
    val projectedMonthsAtCurrentSurplus: Int,
    val cfoVerdict: String,
    val cfoRecommendation: String,
    val progressPercent: Float,
    val isCompleted: Boolean
)

data class GoalSimulationResult(
    val title: String,
    val targetAmount: Double,
    val currentSaved: Double,
    val targetMonths: Int,
    val requiredMonthlySavings: Double,
    val availableMonthlySurplus: Double,
    val surplusSharePercent: Double,
    val surplusRemainingAfterGoal: Double,
    val isFeasible: Boolean,
    val feasibilityStatus: GoalFeasibilityStatus,
    val verdict: String,
    val detailedAdvice: String,
    val recommendedTimelineMonths: Int
)

data class CfoReviewReport(
    val healthScore: Int, // 0..100
    val healthGrade: String, // "Outstanding Household Health", etc.
    val totalIncome: Double,
    val coreCommitments: Double,
    val investments: Double,
    val discretionarySpent: Double,
    val discretionaryBudget: Double,
    val remainingDiscretionary: Double,
    val netSurplus: Double,
    val savingsRatePercent: Double,
    val topSpendingCategory: String,
    val upcomingDueCount: Int,
    val upcomingDueTotal: Double,
    val recommendations: List<String>,
    val warnings: List<String>,
    val goalInsights: List<GoalFeasibilityInsight> = emptyList(),
    val totalGoalsTarget: Double = 0.0,
    val totalGoalsSaved: Double = 0.0,
    val totalGoalsMonthlyDemand: Double = 0.0,
    val aggregateGoalsFeasible: Boolean = true
)

object CfoAdvisorEngine {

    fun generateReport(
        summary: com.example.ui.viewmodel.MonthSummary,
        transactions: List<TransactionEntity>,
        recurringRules: List<RecurringRuleEntity>,
        budgets: List<com.example.data.model.CategoryBudgetEntity> = emptyList(),
        goals: List<FinancialGoalEntity> = emptyList()
    ): CfoReviewReport {
        return generateMonthlyReview(
            income = summary.totalIncome,
            scheduledExpenses = summary.scheduledExpenses,
            investments = summary.investments,
            discretionarySpent = summary.randomExpenses,
            discretionaryBudget = summary.discretionaryBudget,
            transactions = transactions,
            recurringRules = recurringRules,
            goals = goals
        )
    }

    fun generateMonthlyReview(
        income: Double,
        scheduledExpenses: Double,
        investments: Double,
        discretionarySpent: Double,
        discretionaryBudget: Double,
        transactions: List<TransactionEntity>,
        recurringRules: List<RecurringRuleEntity>,
        goals: List<FinancialGoalEntity> = emptyList()
    ): CfoReviewReport {
        val totalOutflow = scheduledExpenses + investments + discretionarySpent
        val netSurplus = maxOf(0.0, income - totalOutflow)
        val totalSavedInvested = investments + netSurplus
        val savingsRate = if (income > 0) (totalSavedInvested / income) * 100.0 else 0.0

        val remainingDiscretionary = maxOf(0.0, discretionaryBudget - discretionarySpent)

        // Find top spending category
        val categoryTotals = transactions
            .filter { it.type == "EXPENSE" }
            .groupBy { it.category }
            .mapValues { entry -> entry.value.sumOf { it.amount } }
        val topCategory = categoryTotals.maxByOrNull { it.value }?.key ?: "Groceries"

        // Health Score calculation
        var score = 70
        if (savingsRate >= 30.0) score += 15
        else if (savingsRate >= 15.0) score += 5
        else score -= 10

        if (discretionarySpent <= discretionaryBudget) score += 10
        else score -= 15

        if (netSurplus > 50000.0) score += 5

        score = score.coerceIn(20, 98)

        val grade = when {
            score >= 85 -> "Outstanding Household Financial Health"
            score >= 70 -> "Solid Household Health & Cashflow"
            else -> "Budget Attention Needed"
        }

        val recs = mutableListOf<String>()
        val warns = mutableListOf<String>()

        recs.add("Household savings rate is strong at ${String.format(Locale.US, "%.1f", savingsRate)}% (target benchmark is 30%+).")
        if (investments > 0) {
            recs.add("Household maintains disciplined wealth creation investments totalling ${CurrencyFormatter.formatInr(investments)}/month.")
        } else {
            recs.add("No active SIP/investments logged yet. Consider deploying surplus into wealth funds.")
        }

        if (discretionarySpent > discretionaryBudget) {
            warns.add("Discretionary spending exceeded the ${CurrencyFormatter.formatInr(discretionaryBudget)} limit by ${CurrencyFormatter.formatInr(discretionarySpent - discretionaryBudget)}.")
        } else {
            recs.add("Discretionary spending is well-contained with ${CurrencyFormatter.formatInr(remainingDiscretionary)} buffer remaining.")
        }

        if (income > 0) {
            val commitmentRatio = (scheduledExpenses / income) * 100.0
            recs.add("Core scheduled commitments account for ~${String.format(Locale.US, "%.1f", commitmentRatio)}% of monthly income (${if (commitmentRatio <= 35.0) "well within safe 40% debt-to-income threshold" else "watch commitment load"}).")
        }

        // Live Goals Evaluation
        val goalInsights = goals.map { goal ->
            evaluateGoal(goal, monthlySurplus = netSurplus, discretionaryBuffer = remainingDiscretionary)
        }

        val activeGoals = goalInsights.filter { !it.isCompleted }
        val totalGoalsTarget = goals.sumOf { it.targetAmount }
        val totalGoalsSaved = goals.sumOf { it.currentSaved }
        val totalMonthlyDemand = activeGoals.sumOf { it.requiredMonthlySavings }
        val aggregateFeasible = totalMonthlyDemand <= (netSurplus + remainingDiscretionary)

        if (activeGoals.isNotEmpty()) {
            if (aggregateFeasible) {
                recs.add("Active household goals require ${CurrencyFormatter.formatInr(totalMonthlyDemand)}/month, fully supported by your available monthly surplus of ${CurrencyFormatter.formatInr(netSurplus)}.")
            } else {
                warns.add("Combined goal savings target of ${CurrencyFormatter.formatInr(totalMonthlyDemand)}/month exceeds current net monthly surplus (${CurrencyFormatter.formatInr(netSurplus)}). Prioritize high-priority goals or stretch timelines.")
            }
        } else {
            recs.add("No financial goals tracked yet. Set customized goals below (e.g. Emergency Fund, Vacation, Home Down Payment) to unlock CFO tracking.")
        }

        return CfoReviewReport(
            healthScore = score,
            healthGrade = grade,
            totalIncome = income,
            coreCommitments = scheduledExpenses,
            investments = investments,
            discretionarySpent = discretionarySpent,
            discretionaryBudget = discretionaryBudget,
            remainingDiscretionary = remainingDiscretionary,
            netSurplus = netSurplus,
            savingsRatePercent = savingsRate,
            topSpendingCategory = topCategory,
            upcomingDueCount = recurringRules.size,
            upcomingDueTotal = recurringRules.sumOf { it.amount },
            recommendations = recs,
            warnings = warns,
            goalInsights = goalInsights,
            totalGoalsTarget = totalGoalsTarget,
            totalGoalsSaved = totalGoalsSaved,
            totalGoalsMonthlyDemand = totalMonthlyDemand,
            aggregateGoalsFeasible = aggregateFeasible
        )
    }

    /**
     * Evaluates a user-defined goal against actual financial surplus.
     */
    fun evaluateGoal(
        goal: FinancialGoalEntity,
        monthlySurplus: Double,
        discretionaryBuffer: Double = 0.0
    ): GoalFeasibilityInsight {
        val remaining = maxOf(0.0, goal.targetAmount - goal.currentSaved)
        val months = maxOf(1, goal.targetMonths)
        val requiredMonthly = if (remaining > 0) remaining / months else 0.0
        val effectiveSurplus = maxOf(1000.0, monthlySurplus)
        val sharePercent = if (effectiveSurplus > 0) (requiredMonthly / effectiveSurplus) * 100.0 else 100.0
        val progress = if (goal.targetAmount > 0) ((goal.currentSaved / goal.targetAmount) * 100.0).toFloat().coerceIn(0f, 100f) else 100f

        val projectedMonths = if (monthlySurplus > 0 && remaining > 0) {
            ceil(remaining / monthlySurplus).toInt()
        } else months

        val isCompleted = goal.isCompleted || (goal.targetAmount > 0 && goal.currentSaved >= goal.targetAmount)

        val (status, statusLabel, verdict, recommendation) = when {
            isCompleted -> {
                Quad(
                    GoalFeasibilityStatus.ON_TRACK,
                    "Goal Achieved! 🎉",
                    "Target of ${CurrencyFormatter.formatInr(goal.targetAmount)} is 100% fulfilled.",
                    "Great discipline! You can now redeploy surplus savings toward your next family goal."
                )
            }
            sharePercent <= 35.0 -> {
                Quad(
                    GoalFeasibilityStatus.ON_TRACK,
                    "Easily Achievable",
                    "Highly feasible! Requires ${CurrencyFormatter.formatInr(requiredMonthly)}/month (${String.format(Locale.US, "%.0f", sharePercent)}% of net surplus).",
                    "With ${CurrencyFormatter.formatInr(monthlySurplus - requiredMonthly)} surplus headroom remaining each month, this goal can be met smoothly without impacting lifestyle or emergency buffers."
                )
            }
            sharePercent <= 70.0 -> {
                Quad(
                    GoalFeasibilityStatus.MODERATE_STRETCH,
                    "Feasible & Healthy",
                    "Moderate allocation: ${CurrencyFormatter.formatInr(requiredMonthly)}/month (${String.format(Locale.US, "%.0f", sharePercent)}% of net surplus).",
                    "Leaves ${CurrencyFormatter.formatInr(monthlySurplus - requiredMonthly)} monthly cushion. Set up an automated recurring transfer on payday to keep this on autopilot."
                )
            }
            sharePercent <= 100.0 -> {
                Quad(
                    GoalFeasibilityStatus.TIGHT_BUFFER,
                    "Achievable with Tight Buffer",
                    "Consumes ${String.format(Locale.US, "%.0f", sharePercent)}% of surplus (${CurrencyFormatter.formatInr(requiredMonthly)}/mo).",
                    "Tight cashflow buffer remaining (${CurrencyFormatter.formatInr(maxOf(0.0, monthlySurplus - requiredMonthly))}). Consider extending horizon to ${months + 3} months to retain a healthier monthly liquidity buffer."
                )
            }
            else -> {
                val extendedMonths = if (monthlySurplus > 0) ceil(remaining / (monthlySurplus * 0.7)).toInt() else months * 2
                Quad(
                    GoalFeasibilityStatus.DEFICIT_STRETCH,
                    "Timeline Extension Recommended",
                    "Requires ${CurrencyFormatter.formatInr(requiredMonthly)}/mo, which exceeds available surplus of ${CurrencyFormatter.formatInr(monthlySurplus)}.",
                    "At 70% of current monthly surplus, you can realistically achieve this in $extendedMonths months. Alternatively, trim discretionary spending by ${CurrencyFormatter.formatInr(requiredMonthly - monthlySurplus)}/mo."
                )
            }
        }

        return GoalFeasibilityInsight(
            goalId = goal.id,
            title = goal.title,
            category = goal.category,
            targetAmount = goal.targetAmount,
            currentSaved = goal.currentSaved,
            remainingAmount = remaining,
            targetMonths = months,
            requiredMonthlySavings = requiredMonthly,
            currentMonthlySurplus = monthlySurplus,
            surplusSharePercent = sharePercent,
            feasibilityStatus = status,
            statusLabel = statusLabel,
            projectedMonthsAtCurrentSurplus = projectedMonths,
            cfoVerdict = verdict,
            cfoRecommendation = recommendation,
            progressPercent = progress,
            isCompleted = isCompleted
        )
    }

    /**
     * Simulates any custom goal asked by the user in real time based on their actual financial surplus.
     */
    fun simulateCustomGoal(
        title: String,
        targetAmount: Double,
        currentSaved: Double = 0.0,
        targetMonths: Int = 12,
        monthlySurplus: Double = 50000.0,
        discretionaryBuffer: Double = 20000.0
    ): GoalSimulationResult {
        val remaining = maxOf(0.0, targetAmount - currentSaved)
        val months = maxOf(1, targetMonths)
        val requiredMonthly = if (remaining > 0) remaining / months else 0.0
        val surplusRemaining = monthlySurplus - requiredMonthly
        val sharePercent = if (monthlySurplus > 0) (requiredMonthly / monthlySurplus) * 100.0 else 100.0
        val isFeasible = monthlySurplus >= requiredMonthly

        val status = when {
            sharePercent <= 35.0 -> GoalFeasibilityStatus.ON_TRACK
            sharePercent <= 70.0 -> GoalFeasibilityStatus.MODERATE_STRETCH
            sharePercent <= 100.0 -> GoalFeasibilityStatus.TIGHT_BUFFER
            else -> GoalFeasibilityStatus.DEFICIT_STRETCH
        }

        val recommendedTimeline = if (monthlySurplus > 0 && remaining > 0) {
            maxOf(months, ceil(remaining / (monthlySurplus * 0.6)).toInt())
        } else months

        val verdict = when (status) {
            GoalFeasibilityStatus.ON_TRACK ->
                "Comfortably Achievable! Saving ${CurrencyFormatter.formatInr(requiredMonthly)}/mo takes only ${String.format(Locale.US, "%.0f", sharePercent)}% of your surplus."
            GoalFeasibilityStatus.MODERATE_STRETCH ->
                "Very Realistic! Requires ${CurrencyFormatter.formatInr(requiredMonthly)}/mo, leaving ${CurrencyFormatter.formatInr(surplusRemaining)}/mo buffer."
            GoalFeasibilityStatus.TIGHT_BUFFER ->
                "Feasible with disciplined spending. Consumes almost all free cash flow (${String.format(Locale.US, "%.0f", sharePercent)}%)."
            GoalFeasibilityStatus.DEFICIT_STRETCH ->
                "Target requires ${CurrencyFormatter.formatInr(requiredMonthly)}/mo vs your ${CurrencyFormatter.formatInr(monthlySurplus)}/mo surplus. Timeline adjustment advised."
        }

        val detailedAdvice = when (status) {
            GoalFeasibilityStatus.ON_TRACK ->
                "With your household's monthly surplus of ${CurrencyFormatter.formatInr(monthlySurplus)}, setting aside ${CurrencyFormatter.formatInr(requiredMonthly)}/month for \"$title\" leaves a robust ₹${CurrencyFormatter.formatInr(surplusRemaining, false)} cushion for emergencies and investments."
            GoalFeasibilityStatus.MODERATE_STRETCH ->
                "Dedicating ${CurrencyFormatter.formatInr(requiredMonthly)} each month allows you to hit ${CurrencyFormatter.formatInr(targetAmount)} in $months months while keeping ${CurrencyFormatter.formatInr(surplusRemaining)}/mo emergency buffer."
            GoalFeasibilityStatus.TIGHT_BUFFER ->
                "This leaves only ${CurrencyFormatter.formatInr(surplusRemaining)}/month unallocated. If unplanned expenses occur, you may need to tap into discretionary savings. We recommend stretching to $recommendedTimeline months for safety."
            GoalFeasibilityStatus.DEFICIT_STRETCH ->
                "At your current financial surplus of ${CurrencyFormatter.formatInr(monthlySurplus)}/mo, saving 60% of surplus (${CurrencyFormatter.formatInr(monthlySurplus * 0.6)}/mo) achieves ${CurrencyFormatter.formatInr(targetAmount)} in $recommendedTimeline months sustainably."
        }

        return GoalSimulationResult(
            title = title.ifBlank { "Custom Goal" },
            targetAmount = targetAmount,
            currentSaved = currentSaved,
            targetMonths = months,
            requiredMonthlySavings = requiredMonthly,
            availableMonthlySurplus = monthlySurplus,
            surplusSharePercent = sharePercent,
            surplusRemainingAfterGoal = surplusRemaining,
            isFeasible = isFeasible,
            feasibilityStatus = status,
            verdict = verdict,
            detailedAdvice = detailedAdvice,
            recommendedTimelineMonths = recommendedTimeline
        )
    }

    private data class Quad<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
}
