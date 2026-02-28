package com.algotrader.app.data.repository

import com.algotrader.app.data.local.dao.TradeDao
import com.algotrader.app.data.local.entity.TradeEntity
import com.algotrader.app.data.model.Account
import com.algotrader.app.data.model.Order
import com.algotrader.app.data.model.OrderSide
import com.algotrader.app.data.model.OrderType
import com.algotrader.app.data.model.Position
import com.algotrader.app.data.remote.broker.BrokerManager
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TradingRepository @Inject constructor(
    private val brokerManager: BrokerManager,
    private val tradeDao: TradeDao
) {
    suspend fun getAccount(): Account? = brokerManager.getAccount()

    suspend fun getPositions(): List<Position> = brokerManager.getPositions()

    suspend fun getOrders(): List<Order> = brokerManager.getOrders()

    suspend fun submitOrder(
        symbol: String,
        side: OrderSide,
        type: OrderType,
        quantity: Double,
        limitPrice: Double? = null,
        stopPrice: Double? = null
    ): Order? = brokerManager.submitOrder(symbol, side, type, quantity, limitPrice, stopPrice)

    suspend fun cancelOrder(orderId: String) = brokerManager.cancelOrder(orderId)

    suspend fun closePosition(symbol: String) = brokerManager.closePosition(symbol)

    suspend fun testConnection(): Boolean = brokerManager.connect()

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
}
