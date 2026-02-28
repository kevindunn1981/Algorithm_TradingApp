package com.algotrader.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.algotrader.app.data.local.entity.WatchlistItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WatchlistDao {

    @Query("SELECT * FROM watchlist ORDER BY addedAt DESC")
    fun getAllItems(): Flow<List<WatchlistItemEntity>>

    @Query("SELECT * FROM watchlist WHERE symbol = :symbol")
    suspend fun getItem(symbol: String): WatchlistItemEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: WatchlistItemEntity)

    @Delete
    suspend fun deleteItem(item: WatchlistItemEntity)

    @Query("DELETE FROM watchlist WHERE symbol = :symbol")
    suspend fun deleteBySymbol(symbol: String)

    @Query("SELECT EXISTS(SELECT 1 FROM watchlist WHERE symbol = :symbol)")
    suspend fun isInWatchlist(symbol: String): Boolean
}
