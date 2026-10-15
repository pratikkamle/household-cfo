package com.example.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "users",
    foreignKeys = [
        ForeignKey(
            entity = HouseholdEntity::class,
            parentColumns = ["id"],
            childColumns = ["householdId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["householdId"])
    ]
)
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val householdId: Long = 1L,
    val name: String,
    val email: String = "",
    val role: String = "MEMBER", // "ADMIN", "MEMBER"
    val initial: String = "",
    val colorHex: String = "#0284C7",
    val firebaseUid: String = "",
    val photoUrl: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)
