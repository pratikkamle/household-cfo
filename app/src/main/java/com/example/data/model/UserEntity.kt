package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 1,
    val name: String = "Kamle Household",
    val inviteCode: String = "CFO-8821",
    val adminName: String = "Pratik",
    val adminEmail: String = "pratik.kamle17@gmail.com",
    val monthlyDiscretionaryBudget: Double = 50000.0,
    val securityPin: String = "1234",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val householdId: Long = 1,
    val name: String,
    val email: String = "",
    val role: String = "MEMBER",
    val avatarInitial: String = "",
    val colorHex: String = "#2563EB",
    val photoUrl: String? = null,
    val firebaseUid: String? = null,
    val joinedAt: Long = System.currentTimeMillis()
) {
    val initial: String
        get() = if (avatarInitial.isNotBlank()) avatarInitial else name.take(1).uppercase()
}
