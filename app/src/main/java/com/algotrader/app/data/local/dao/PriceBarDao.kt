package com.algotrader.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.algotrader.app.data.local.entity.PriceBarEntity

@Dao
interface PriceBarDao {

    @Query("""
        SELECT * FROM price_bars 
        WHERE symbol = :symbol AND timeframe = :timeframe 
        AND timestamp BETWEEN :startTime AND :endTime 
        ORDER BY timestamp ASC
    """)
    suspend fun getBars(
        symbol: String,
        timeframe: String,
        startTime: Long,
        endTime: Long
    ): List<PriceBarEntity>

    @Query("""
        SELECT * FROM price_bars 
        WHERE symbol = :symbol AND timeframe = :timeframe 
        ORDER BY timestamp DESC LIMIT :limit
    """)
    suspend fun getRecentBars(
        symbol: String,
        timeframe: String,
        limit: Int
    ): List<PriceBarEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBars(bars: List<PriceBarEntity>)

    @Query("DELETE FROM price_bars WHERE symbol = :symbol AND timeframe = :timeframe")
    suspend fun deleteBars(symbol: String, timeframe: String)

    @Query("SELECT MAX(timestamp) FROM price_bars WHERE symbol = :symbol AND timeframe = :timeframe")
    suspend fun getLatestTimestamp(symbol: String, timeframe: String): Long?
}
