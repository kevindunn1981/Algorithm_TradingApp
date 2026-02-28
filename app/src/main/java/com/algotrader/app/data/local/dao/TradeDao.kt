package com.algotrader.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.algotrader.app.data.local.entity.TradeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TradeDao {

    @Query("SELECT * FROM trades ORDER BY timestamp DESC")
    fun getAllTrades(): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE strategyId = :strategyId ORDER BY timestamp DESC")
    fun getTradesByStrategy(strategyId: Long): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades WHERE mode = :mode ORDER BY timestamp DESC")
    fun getTradesByMode(mode: String): Flow<List<TradeEntity>>

    @Query("SELECT * FROM trades ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentTrades(limit: Int): Flow<List<TradeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrade(trade: TradeEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTrades(trades: List<TradeEntity>)

    @Query("SELECT SUM(pnl) FROM trades WHERE mode = :mode")
    suspend fun getTotalPnl(mode: String): Double?

    @Query("SELECT COUNT(*) FROM trades WHERE pnl > 0 AND mode = :mode")
    suspend fun getWinningTradeCount(mode: String): Int

    @Query("SELECT COUNT(*) FROM trades WHERE mode = :mode")
    suspend fun getTotalTradeCount(mode: String): Int

    @Query("DELETE FROM trades WHERE mode = :mode")
    suspend fun deleteTradesByMode(mode: String)
}
