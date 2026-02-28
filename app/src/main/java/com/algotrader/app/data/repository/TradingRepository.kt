package com.algotrader.app.data.repository

import com.algotrader.app.data.local.dao.TradeDao
import com.algotrader.app.data.local.entity.TradeEntity
import com.algotrader.app.data.model.Account
import com.algotrader.app.data.model.Order
import com.algotrader.app.data.model.OrderSide
import com.algotrader.app.data.model.OrderStatus
import com.algotrader.app.data.model.OrderType
import com.algotrader.app.data.model.Position
import com.algotrader.app.data.remote.api.AlpacaTradingApi
import com.algotrader.app.data.remote.dto.AlpacaOrderRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TradingRepository @Inject constructor(
    private val tradingApi: AlpacaTradingApi,
    private val tradeDao: TradeDao
) {
    suspend fun getAccount(): Account? {
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

    suspend fun getPositions(): List<Position> {
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

    suspend fun getOrders(): List<Order> {
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

    suspend fun submitOrder(
        symbol: String,
        side: OrderSide,
        type: OrderType,
        quantity: Double,
        limitPrice: Double? = null,
        stopPrice: Double? = null
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

    suspend fun cancelOrder(orderId: String) {
        try {
            tradingApi.cancelOrder(orderId)
        } catch (_: Exception) {}
    }

    suspend fun closePosition(symbol: String) {
        try {
            tradingApi.closePosition(symbol)
        } catch (_: Exception) {}
    }

    fun getRecentTrades(limit: Int = 50): Flow<List<TradeEntity>> =
        tradeDao.getRecentTrades(limit)

    fun getTradesByMode(mode: String): Flow<List<TradeEntity>> =
        tradeDao.getTradesByMode(mode)

    suspend fun recordTrade(trade: TradeEntity): Long =
        tradeDao.insertTrade(trade)

    suspend fun getTotalPnl(mode: String): Double =
        tradeDao.getTotalPnl(mode) ?: 0.0

    suspend fun getWinRate(mode: String): Double {
        val total = tradeDao.getTotalTradeCount(mode)
        if (total == 0) return 0.0
        val wins = tradeDao.getWinningTradeCount(mode)
        return wins.toDouble() / total
    }

    private fun parseOrderStatus(status: String): OrderStatus {
        return try {
            OrderStatus.valueOf(status.uppercase().replace(" ", "_"))
        } catch (e: Exception) {
            OrderStatus.NEW
        }
    }
}
