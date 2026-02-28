package com.algotrader.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.algotrader.app.data.local.entity.StrategyEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StrategyDao {

    @Query("SELECT * FROM strategies ORDER BY updatedAt DESC")
    fun getAllStrategies(): Flow<List<StrategyEntity>>

    @Query("SELECT * FROM strategies WHERE id = :id")
    suspend fun getStrategyById(id: Long): StrategyEntity?

    @Query("SELECT * FROM strategies WHERE isActive = 1")
    fun getActiveStrategies(): Flow<List<StrategyEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStrategy(strategy: StrategyEntity): Long

    @Update
    suspend fun updateStrategy(strategy: StrategyEntity)

    @Delete
    suspend fun deleteStrategy(strategy: StrategyEntity)

    @Query("UPDATE strategies SET isActive = :isActive, updatedAt = :timestamp WHERE id = :id")
    suspend fun setStrategyActive(id: Long, isActive: Boolean, timestamp: Long = System.currentTimeMillis())

    @Query("SELECT COUNT(*) FROM strategies")
    suspend fun getStrategyCount(): Int
}
