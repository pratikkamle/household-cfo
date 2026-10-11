package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.RecurringTransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecurringRuleDao {
    @Query("SELECT * FROM recurring_transactions ORDER BY dueDay ASC")
    fun getAllRules(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE isActive = 1 ORDER BY dueDay ASC")
    fun getActiveRules(): Flow<List<RecurringTransactionEntity>>

    @Query("SELECT * FROM recurring_transactions WHERE householdId = :householdId ORDER BY dueDay ASC")
    fun getRulesByHousehold(householdId: Long = 1L): Flow<List<RecurringTransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRule(rule: RecurringTransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRules(rules: List<RecurringTransactionEntity>)

    @Update
    suspend fun updateRule(rule: RecurringTransactionEntity)

    @Query("DELETE FROM recurring_transactions WHERE id = :id")
    suspend fun deleteRuleById(id: Long)

    @Query("DELETE FROM recurring_transactions")
    suspend fun clearAllRules()
}

typealias RecurringTransactionDao = RecurringRuleDao
