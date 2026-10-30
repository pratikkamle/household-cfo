package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "accounts",
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
data class AccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val householdId: Long = 1L,
    val userId: Long? = null,
    val name: String,
    val holder: String, // Dynamic household member name or JOINT
    val accountType: String, // "BANK", "CREDIT_CARD", "DEBIT_CARD", "INVESTMENT", "NPS", "LIC_INSURANCE", "EPF_PPF", "FIXED_DEPOSIT", "GOLD", "CASH", "OTHER"
    val balance: Double = 0.0,
    val accountNumberMask: String = "",
    val institution: String = "", // e.g. "HDFC Bank", "LIC of India", "SBI Mutual Fund", "NSDL"
    val notes: String = "", // e.g. Policy details, Folio, Maturity date
    val creditLimit: Double = 0.0, // For credit cards
    val currency: String = "INR",
    val isActive: Boolean = true
)
