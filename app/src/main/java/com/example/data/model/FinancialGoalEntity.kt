package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "financial_goals")
data class FinancialGoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val householdId: Long = 1L,
    val title: String,
    val category: String = "CUSTOM", // EMERGENCY, VACATION, HOME, VEHICLE, EDUCATION, RETIREMENT, WEALTH, CUSTOM
    val targetAmount: Double,
    val currentSaved: Double = 0.0,
    val targetMonths: Int = 12,
    val priority: String = "MEDIUM", // HIGH, MEDIUM, LOW
    val notes: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val isCompleted: Boolean = false
)
