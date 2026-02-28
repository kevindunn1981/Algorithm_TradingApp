package com.algotrader.app.data.remote.broker

import com.algotrader.app.data.model.Account
import com.algotrader.app.data.model.Order
import com.algotrader.app.data.model.OrderSide
import com.algotrader.app.data.model.OrderType
import com.algotrader.app.data.model.Position
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.data.model.Quote

enum class BrokerType {
    ALPACA,
    MOOMOO
}

interface BrokerProvider {
    val brokerType: BrokerType
    val displayName: String

    suspend fun connect(): Boolean
    suspend fun disconnect()
    fun isConnected(): Boolean

    suspend fun getAccount(): Account?
    suspend fun getPositions(): List<Position>
    suspend fun getOrders(): List<Order>

    suspend fun submitOrder(
        symbol: String,
        side: OrderSide,
        type: OrderType,
        quantity: Double,
        limitPrice: Double? = null,
        stopPrice: Double? = null
    ): Order?

    suspend fun cancelOrder(orderId: String)
    suspend fun closePosition(symbol: String)
    suspend fun closeAllPositions()
}

interface MarketDataProvider {
    val providerType: BrokerType

    suspend fun getQuote(symbol: String): Quote?
    suspend fun getQuotes(symbols: List<String>): Map<String, Quote>

    suspend fun getHistoricalBars(
        symbol: String,
        timeframe: String,
        start: String,
        end: String? = null,
        limit: Int = 1000
    ): List<PriceBar>
}
