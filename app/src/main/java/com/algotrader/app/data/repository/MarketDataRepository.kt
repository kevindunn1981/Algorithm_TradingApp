package com.algotrader.app.data.repository

import com.algotrader.app.data.local.dao.PriceBarDao
import com.algotrader.app.data.local.entity.PriceBarEntity
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.data.model.Quote
import com.algotrader.app.data.remote.broker.BrokerManager
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketDataRepository @Inject constructor(
    private val brokerManager: BrokerManager,
    private val priceBarDao: PriceBarDao
) {
    suspend fun getBars(
        symbol: String,
        timeframe: String,
        start: String,
        end: String? = null,
        limit: Int = 1000
    ): List<PriceBar> {
        return try {
            val priceBars = brokerManager.getHistoricalBars(symbol, timeframe, start, end, limit)

            val entities = priceBars.map { bar ->
                PriceBarEntity(
                    symbol = bar.symbol,
                    timestamp = bar.timestamp.toEpochMilli(),
                    timeframe = timeframe,
                    open = bar.open,
                    high = bar.high,
                    low = bar.low,
                    close = bar.close,
                    volume = bar.volume
                )
            }
            if (entities.isNotEmpty()) {
                priceBarDao.insertBars(entities)
            }
            priceBars
        } catch (e: Exception) {
            getCachedBars(symbol, timeframe)
        }
    }

    suspend fun getCachedBars(symbol: String, timeframe: String, limit: Int = 1000): List<PriceBar> {
        return priceBarDao.getRecentBars(symbol, timeframe, limit)
            .sortedBy { it.timestamp }
            .map { entity ->
                PriceBar(
                    symbol = entity.symbol,
                    timestamp = Instant.ofEpochMilli(entity.timestamp),
                    open = entity.open,
                    high = entity.high,
                    low = entity.low,
                    close = entity.close,
                    volume = entity.volume
                )
            }
    }

    suspend fun getSnapshot(symbol: String): Quote? {
        return brokerManager.getQuote(symbol)
    }

    suspend fun getMultipleSnapshots(symbols: List<String>): Map<String, Quote> {
        return brokerManager.getQuotes(symbols)
    }
}
