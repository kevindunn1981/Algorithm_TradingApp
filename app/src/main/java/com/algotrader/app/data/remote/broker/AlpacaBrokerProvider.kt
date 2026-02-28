package com.algotrader.app.data.remote.broker

import com.algotrader.app.data.model.Account
import com.algotrader.app.data.model.Order
import com.algotrader.app.data.model.OrderSide
import com.algotrader.app.data.model.OrderStatus
import com.algotrader.app.data.model.OrderType
import com.algotrader.app.data.model.Position
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.data.model.Quote
import com.algotrader.app.data.remote.api.AlpacaMarketDataApi
import com.algotrader.app.data.remote.api.AlpacaTradingApi
import com.algotrader.app.data.remote.dto.AlpacaOrderRequest
import java.time.Instant
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AlpacaBrokerProvider @Inject constructor(
    private val tradingApi: AlpacaTradingApi,
    private val marketDataApi: AlpacaMarketDataApi
) : BrokerProvider, MarketDataProvider {

    override val brokerType = BrokerType.ALPACA
    override val displayName = "Alpaca"
    override val providerType = BrokerType.ALPACA

    private var connected = false

    override suspend fun connect(): Boolean {
        return try {
            tradingApi.getAccount()
            connected = true
            true
        } catch (e: Exception) {
            connected = false
            false
        }
    }

    override suspend fun disconnect() {
        connected = false
    }

    override fun isConnected(): Boolean = connected

    override suspend fun getAccount(): Account? {
        return try {
            val dto = tradingApi.getAccount()
            Account(
                id = dto.id,
                equity = dto.equity.toDouble(),
                cash = dto.cash.toDouble(),
                buyingPower = dto.buyingPower.toDouble(),
                portfolioValue = dto.portfolioValue.toDouble(),
                dayTradeCount = dto.dayTradeCount,
                patternDayTrader = dto.patternDayTrader
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getPositions(): List<Position> {
        return try {
            tradingApi.getPositions().map { dto ->
                Position(
                    symbol = dto.symbol,
                    quantity = dto.qty.toDouble(),
                    averageEntryPrice = dto.avgEntryPrice.toDouble(),
                    currentPrice = dto.currentPrice.toDouble(),
                    marketValue = dto.marketValue.toDouble(),
                    unrealizedPnl = dto.unrealizedPl.toDouble(),
                    unrealizedPnlPercent = dto.unrealizedPlpc.toDouble() * 100
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun getOrders(): List<Order> {
        return try {
            tradingApi.getOrders().map { dto ->
                Order(
                    id = dto.id,
                    symbol = dto.symbol,
                    side = OrderSide.valueOf(dto.side.uppercase()),
                    type = OrderType.valueOf(dto.type.uppercase()),
                    quantity = dto.qty?.toDouble() ?: 0.0,
                    limitPrice = dto.limitPrice?.toDouble(),
                    stopPrice = dto.stopPrice?.toDouble(),
                    filledPrice = dto.filledAvgPrice?.toDouble(),
                    status = parseOrderStatus(dto.status),
                    submittedAt = Instant.parse(dto.submittedAt),
                    filledAt = dto.filledAt?.let { Instant.parse(it) }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    override suspend fun submitOrder(
        symbol: String,
        side: OrderSide,
        type: OrderType,
        quantity: Double,
        limitPrice: Double?,
        stopPrice: Double?
    ): Order? {
        return try {
            val request = AlpacaOrderRequest(
                symbol = symbol,
                qty = quantity.toString(),
                side = side.name.lowercase(),
                type = type.name.lowercase(),
                limitPrice = limitPrice?.toString(),
                stopPrice = stopPrice?.toString()
            )
            val dto = tradingApi.submitOrder(request)
            Order(
                id = dto.id,
                symbol = dto.symbol,
                side = OrderSide.valueOf(dto.side.uppercase()),
                type = OrderType.valueOf(dto.type.uppercase()),
                quantity = dto.qty?.toDouble() ?: quantity,
                status = parseOrderStatus(dto.status),
                submittedAt = Instant.now()
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun cancelOrder(orderId: String) {
        try { tradingApi.cancelOrder(orderId) } catch (_: Exception) {}
    }

    override suspend fun closePosition(symbol: String) {
        try { tradingApi.closePosition(symbol) } catch (_: Exception) {}
    }

    override suspend fun closeAllPositions() {
        try { tradingApi.closeAllPositions() } catch (_: Exception) {}
    }

    override suspend fun getQuote(symbol: String): Quote? {
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

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        return try {
            val snapshots = marketDataApi.getSnapshots(symbols.joinToString(","))
            snapshots.mapNotNull { (symbol, snapshot) ->
                val price = snapshot.latestTrade?.price ?: return@mapNotNull null
                val prevClose = snapshot.prevDailyBar?.close ?: price
                val change = price - prevClose
                val changePercent = if (prevClose != 0.0) (change / prevClose) * 100 else 0.0
                symbol to Quote(symbol, price, change, changePercent, snapshot.dailyBar?.volume ?: 0, Instant.now())
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    override suspend fun getHistoricalBars(
        symbol: String,
        timeframe: String,
        start: String,
        end: String?,
        limit: Int
    ): List<PriceBar> {
        return try {
            val response = marketDataApi.getBars(symbol, timeframe, start, end, limit)
            val bars = response.bars?.get(symbol) ?: emptyList()
            bars.map { bar ->
                val instant = try {
                    ZonedDateTime.parse(bar.timestamp, DateTimeFormatter.ISO_ZONED_DATE_TIME).toInstant()
                } catch (e: Exception) {
                    Instant.parse(bar.timestamp)
                }
                PriceBar(symbol, instant, bar.open, bar.high, bar.low, bar.close, bar.volume)
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parseOrderStatus(status: String): OrderStatus {
        return try {
            OrderStatus.valueOf(status.uppercase().replace(" ", "_"))
        } catch (e: Exception) {
            OrderStatus.NEW
        }
    }
}
