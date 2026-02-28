package com.algotrader.app.data.repository

import com.algotrader.app.data.local.dao.PriceBarDao
import com.algotrader.app.data.local.entity.PriceBarEntity
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.data.model.Quote
import com.algotrader.app.data.remote.api.AlpacaMarketDataApi
import com.algotrader.app.data.remote.dto.AlpacaBarDto
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarketDataRepository @Inject constructor(
    private val marketDataApi: AlpacaMarketDataApi,
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
            val response = marketDataApi.getBars(symbol, timeframe, start, end, limit)
            val bars = response.bars?.get(symbol) ?: emptyList()
            val priceBars = bars.map { it.toDomain(symbol) }

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
            priceBarDao.insertBars(entities)
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
        return try {
            val snapshot = marketDataApi.getSnapshot(symbol)
            val price = snapshot.latestTrade?.price ?: return null
            val prevClose = snapshot.prevDailyBar?.close ?: price
            val change = price - prevClose
            val changePercent = if (prevClose != 0.0) (change / prevClose) * 100 else 0.0

            Quote(
                symbol = symbol,
                price = price,
                change = change,
                changePercent = changePercent,
                volume = snapshot.dailyBar?.volume ?: 0,
                timestamp = Instant.now()
            )
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getMultipleSnapshots(symbols: List<String>): Map<String, Quote> {
        return try {
            val symbolsStr = symbols.joinToString(",")
            val snapshots = marketDataApi.getSnapshots(symbolsStr)
            snapshots.mapNotNull { (symbol, snapshot) ->
                val price = snapshot.latestTrade?.price ?: return@mapNotNull null
                val prevClose = snapshot.prevDailyBar?.close ?: price
                val change = price - prevClose
                val changePercent = if (prevClose != 0.0) (change / prevClose) * 100 else 0.0
                symbol to Quote(
                    symbol = symbol,
                    price = price,
                    change = change,
                    changePercent = changePercent,
                    volume = snapshot.dailyBar?.volume ?: 0,
                    timestamp = Instant.now()
                )
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun AlpacaBarDto.toDomain(symbol: String): PriceBar {
        val instant = try {
            ZonedDateTime.parse(timestamp, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant()
        } catch (e: Exception) {
            Instant.parse(timestamp)
        }
        return PriceBar(
            symbol = symbol,
            timestamp = instant,
            open = open,
            high = high,
            low = low,
            close = close,
            volume = volume
        )
    }
}
