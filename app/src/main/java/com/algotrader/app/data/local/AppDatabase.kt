package com.algotrader.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.algotrader.app.data.local.dao.BacktestResultDao
import com.algotrader.app.data.local.dao.PriceBarDao
import com.algotrader.app.data.local.dao.StrategyDao
import com.algotrader.app.data.local.dao.TradeDao
import com.algotrader.app.data.local.dao.WatchlistDao
import com.algotrader.app.data.local.entity.BacktestResultEntity
import com.algotrader.app.data.local.entity.PriceBarEntity
import com.algotrader.app.data.local.entity.StrategyEntity
import com.algotrader.app.data.local.entity.TradeEntity
import com.algotrader.app.data.local.entity.WatchlistItemEntity

@Database(
    entities = [
        StrategyEntity::class,
        TradeEntity::class,
        WatchlistItemEntity::class,
        PriceBarEntity::class,
        BacktestResultEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun strategyDao(): StrategyDao
    abstract fun tradeDao(): TradeDao
    abstract fun watchlistDao(): WatchlistDao
    abstract fun priceBarDao(): PriceBarDao
    abstract fun backtestResultDao(): BacktestResultDao
}
