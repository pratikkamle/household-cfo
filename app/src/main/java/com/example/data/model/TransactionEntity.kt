package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "transactions",
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
        ),
        ForeignKey(
            entity = AccountEntity::class,
            parentColumns = ["id"],
            childColumns = ["accountId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [
        Index(value = ["householdId"]),
        Index(value = ["userId"]),
        Index(value = ["accountId"]),
        Index(value = ["monthYear"]),
        Index(value = ["paidBy"]),
        Index(value = ["category"])
    ]
)
data class TransactionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val householdId: Long = 1L,
    val userId: Long? = null,
    val amount: Double,
    val category: String,
    val paidBy: String, // PRATIK, MANASVI, JOINT
    val paymentMethod: String = "UPI", // UPI, CREDIT_CARD, etc.
    val accountId: Long? = null,
    val toAccountId: Long? = null, // for TRANSFER transactions between accounts
    val accountName: String = "",
    val merchant: String = "",
    val notes: String = "",
    val timestamp: Long = System.currentTimeMillis(),
    val monthYear: String, // "2026-10"
    val nature: String = "DISCRETIONARY", // DISCRETIONARY, CORE_COMMITMENT, INVESTMENT
    val type: String = "EXPENSE", // EXPENSE, INCOME, INVESTMENT, TRANSFER
    val isRecurring: Boolean = false,
    val recurringRuleId: Long? = null
)
