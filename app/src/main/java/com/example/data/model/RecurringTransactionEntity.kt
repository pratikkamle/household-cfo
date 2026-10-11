package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "recurring_transactions",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["householdId"]),
        Index(value = ["userId"])
    ]
)
data class RecurringTransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val householdId: Long = 1L,
    val userId: Long? = null,
    val title: String,
    val amount: Double,
    val category: String,
    val paidBy: String, // PRATIK, MANASVI, JOINT
    val type: String = "EXPENSE", // EXPENSE, INCOME, INVESTMENT
    val nature: String = "CORE_COMMITMENT", // CORE_COMMITMENT, INVESTMENT
    val dueDay: Int = 1, // 1..31
    val frequency: String = "MONTHLY", // "MONTHLY", "ANNUAL"
    val dueMonth: Int? = null, // 1..12 (for annual rules like LIC)
    val accountId: Long? = null,
    val accountName: String = "",
    val paymentMethod: String = "AUTO_DEBIT",
    val isActive: Boolean = true
)
