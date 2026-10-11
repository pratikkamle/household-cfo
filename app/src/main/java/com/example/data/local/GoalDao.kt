package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.FinancialGoalEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GoalDao {
    @Query("SELECT * FROM financial_goals WHERE householdId = :householdId ORDER BY isCompleted ASC, CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, id DESC")
    fun getGoalsByHousehold(householdId: Long): Flow<List<FinancialGoalEntity>>

    @Query("SELECT * FROM financial_goals ORDER BY isCompleted ASC, CASE priority WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END, id DESC")
    fun getAllGoals(): Flow<List<FinancialGoalEntity>>

    @Query("SELECT * FROM financial_goals WHERE id = :id LIMIT 1")
    suspend fun getGoalById(id: Long): FinancialGoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoal(goal: FinancialGoalEntity): Long

    @Update
    suspend fun updateGoal(goal: FinancialGoalEntity)

    @Query("DELETE FROM financial_goals WHERE id = :id")
    suspend fun deleteGoal(id: Long)

    @Query("UPDATE financial_goals SET currentSaved = :newSaved, isCompleted = :isCompleted WHERE id = :id")
    suspend fun updateGoalProgress(id: Long, newSaved: Double, isCompleted: Boolean)

    @Query("SELECT COUNT(*) FROM financial_goals")
    suspend fun getGoalCount(): Int
}
