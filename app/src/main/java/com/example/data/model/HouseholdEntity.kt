package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "households")
data class HouseholdEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 1L,
    val name: String,
    val currency: String = "INR",
    val monthlyDiscretionaryBudget: Double = 66500.0,
    val inviteCode: String = "CFO-8821",
    val adminName: String = "Admin",
    val adminEmail: String = "",
    val adminUserId: String = "",
    val securityPin: String = "1234",
    val isCloudSynced: Boolean = false,
    val createdAt: Long = System.currentTimeMillis()
)
