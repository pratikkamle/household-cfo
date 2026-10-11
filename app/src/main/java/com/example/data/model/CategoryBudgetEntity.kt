package com.example.data.model

import androidx.room.Entity

@Entity(
    tableName = "category_budgets",
    primaryKeys = ["category", "monthYear"]
)
data class CategoryBudgetEntity(
    val category: String,
    val monthlyBudget: Double,
    val monthYear: String = "" // Empty string represents household default baseline
)
