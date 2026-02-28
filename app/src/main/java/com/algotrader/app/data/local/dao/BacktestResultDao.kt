package com.algotrader.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.algotrader.app.data.local.entity.BacktestResultEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BacktestResultDao {

    @Query("SELECT * FROM backtest_results ORDER BY createdAt DESC")
    fun getAllResults(): Flow<List<BacktestResultEntity>>

    @Query("SELECT * FROM backtest_results WHERE strategyId = :strategyId ORDER BY createdAt DESC")
    fun getResultsByStrategy(strategyId: Long): Flow<List<BacktestResultEntity>>

    @Query("SELECT * FROM backtest_results WHERE id = :id")
    suspend fun getResultById(id: Long): BacktestResultEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: BacktestResultEntity): Long

    @Query("DELETE FROM backtest_results WHERE id = :id")
    suspend fun deleteResult(id: Long)

    @Query("DELETE FROM backtest_results WHERE strategyId = :strategyId")
    suspend fun deleteResultsByStrategy(strategyId: Long)
}
