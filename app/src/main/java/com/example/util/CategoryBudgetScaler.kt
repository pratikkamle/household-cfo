package com.example.util

import com.example.data.model.CategoryBudgetEntity
import kotlin.math.round

object CategoryBudgetScaler {
    const val BASELINE_TOTAL = 76500.0

    val BASELINE_WEIGHTS: List<Pair<String, Double>> = listOf(
        "Food & Dining" to 10000.0,
        "Groceries" to 15000.0,
        "Shopping" to 8000.0,
        "Fuel & Travel" to 6000.0,
        "Entertainment" to 5000.0,
        "Travel & Holidays" to 12500.0,
        "Utilities & Bills" to 6000.0,
        "Medical & Healthcare" to 5000.0,
        "Personal Care" to 4000.0,
        "Miscellaneous" to 5000.0
    )

    /**
     * Calculates the category allowances based on the exact percentage distribution
     * of each category relative to the 76,500 baseline, ensuring the sum of all category
     * allowances equals the admin's chosen monthly discretionary budget exactly.
     */
    fun scaleBudgets(totalBudget: Double): List<CategoryBudgetEntity> {
        val total = if (totalBudget > 0) totalBudget else BASELINE_TOTAL
        var remaining = total
        val list = mutableListOf<CategoryBudgetEntity>()

        for (i in BASELINE_WEIGHTS.indices) {
            val (category, baseAmount) = BASELINE_WEIGHTS[i]
            if (i == BASELINE_WEIGHTS.lastIndex) {
                // Assign remaining balance to ensure the sum matches totalBudget exactly without rounding loss
                list.add(CategoryBudgetEntity(category, remaining))
            } else {
                val percentage = baseAmount / BASELINE_TOTAL
                val allocated = round(total * percentage)
                list.add(CategoryBudgetEntity(category, allocated))
                remaining -= allocated
            }
        }
        return list
    }
}
