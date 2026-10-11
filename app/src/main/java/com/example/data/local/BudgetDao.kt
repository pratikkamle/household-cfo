package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.CategoryBudgetEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("SELECT * FROM category_budgets WHERE monthYear = :monthYear OR monthYear = ''")
    fun getBudgets(monthYear: String): Flow<List<CategoryBudgetEntity>>

    @Query("SELECT COUNT(*) FROM category_budgets")
    suspend fun getBudgetCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: CategoryBudgetEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudgets(budgets: List<CategoryBudgetEntity>)

    @Update
    suspend fun updateBudget(budget: CategoryBudgetEntity)

    @Query("DELETE FROM category_budgets WHERE category = :category AND monthYear = :monthYear")
    suspend fun deleteBudget(category: String, monthYear: String)

    @Query("DELETE FROM category_budgets")
    suspend fun clearAllBudgets()
}
