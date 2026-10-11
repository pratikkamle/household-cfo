package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.HouseholdEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface HouseholdDao {
    @Query("SELECT * FROM households WHERE id = :id LIMIT 1")
    fun getHouseholdById(id: Long): Flow<HouseholdEntity?>

    @Query("SELECT * FROM households LIMIT 1")
    suspend fun getPrimaryHousehold(): HouseholdEntity?

    @Query("SELECT * FROM households WHERE LOWER(name) = LOWER(:name) LIMIT 1")
    suspend fun getHouseholdByName(name: String): HouseholdEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHousehold(household: HouseholdEntity): Long

    @Update
    suspend fun updateHousehold(household: HouseholdEntity)

    @Query("DELETE FROM households WHERE id = :id")
    suspend fun deleteHousehold(id: Long)
}
