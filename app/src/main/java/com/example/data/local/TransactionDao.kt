package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.TransactionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE monthYear = :monthYear ORDER BY timestamp DESC")
    fun getTransactionsByMonth(monthYear: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE monthYear = :monthYear AND paidBy = :paidBy ORDER BY timestamp DESC")
    fun getTransactionsByMonthAndMember(monthYear: String, paidBy: String): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE recurringRuleId = :ruleId AND monthYear = :monthYear LIMIT 1")
    suspend fun getTransactionForRecurringRule(ruleId: Long, monthYear: String): TransactionEntity?

    @Query("SELECT DISTINCT merchant FROM transactions WHERE merchant != '' ORDER BY timestamp DESC LIMIT 20")
    fun getRecentMerchants(): Flow<List<String>>

    @Query("SELECT * FROM transactions WHERE merchant = :merchant ORDER BY timestamp DESC LIMIT 1")
    suspend fun getLastTransactionForMerchant(merchant: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE merchant LIKE '%' || :merchant || '%' ORDER BY timestamp DESC LIMIT 1")
    suspend fun findRecentByMerchant(merchant: String): TransactionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransactions(transactions: List<TransactionEntity>)

    @Update
    suspend fun updateTransaction(transaction: TransactionEntity)

    @Query("DELETE FROM transactions WHERE id = :id")
    suspend fun deleteTransactionById(id: Long)

    @Query("DELETE FROM transactions")
    suspend fun clearAllTransactions()
}
