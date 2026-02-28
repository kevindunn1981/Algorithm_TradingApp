package com.algotrader.app.data.remote.broker

import com.algotrader.app.data.model.Account
import com.algotrader.app.data.model.Order
import com.algotrader.app.data.model.OrderSide
import com.algotrader.app.data.model.OrderType
import com.algotrader.app.data.model.Position
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.data.model.Quote
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BrokerManager @Inject constructor(
    private val alpacaProvider: AlpacaBrokerProvider,
    private val moomooProvider: MoomooBrokerProvider
) : BrokerProvider, MarketDataProvider {

    private val _activeBrokerType = MutableStateFlow(BrokerType.ALPACA)
    val activeBrokerType: StateFlow<BrokerType> = _activeBrokerType.asStateFlow()

    private val _connectionState = MutableStateFlow(false)
    val connectionState: StateFlow<Boolean> = _connectionState.asStateFlow()

    override val brokerType: BrokerType get() = _activeBrokerType.value
    override val displayName: String get() = activeBroker.displayName
    override val providerType: BrokerType get() = _activeBrokerType.value

    val activeBroker: BrokerProvider
        get() = when (_activeBrokerType.value) {
            BrokerType.ALPACA -> alpacaProvider
            BrokerType.MOOMOO -> moomooProvider
        }

    val activeMarketData: MarketDataProvider
        get() = when (_activeBrokerType.value) {
            BrokerType.ALPACA -> alpacaProvider
            BrokerType.MOOMOO -> moomooProvider
        }

    val availableBrokers: List<BrokerInfo>
        get() = listOf(
            BrokerInfo(
                type = BrokerType.ALPACA,
                name = "Alpaca",
                description = "Commission-free US stock & ETF trading via REST API",
                features = listOf(
                    "Commission-free trading",
                    "Fractional shares",
                    "Paper trading",
                    "REST API (no gateway needed)",
                    "US stocks & ETFs"
                ),
                setupUrl = "https://alpaca.markets"
            ),
            BrokerInfo(
                type = BrokerType.MOOMOO,
                name = "Moomoo (Futu)",
                description = "Multi-market trading via OpenD gateway with real-time data",
                features = listOf(
                    "US, HK, CN, SG, JP, AU markets",
                    "Options & futures support",
                    "Level 2 market depth",
                    "Up to 20 years historical data",
                    "Paper trading",
                    "Real-time push notifications",
                    "Requires OpenD gateway"
                ),
                setupUrl = "https://openapi.moomoo.com"
            )
        )

    fun switchBroker(type: BrokerType) {
        _activeBrokerType.value = type
        _connectionState.value = activeBroker.isConnected()
    }

    override suspend fun connect(): Boolean {
        val result = activeBroker.connect()
        _connectionState.value = result
        return result
    }

    override suspend fun disconnect() {
        activeBroker.disconnect()
        _connectionState.value = false
    }

    override fun isConnected(): Boolean = activeBroker.isConnected()

    override suspend fun getAccount(): Account? = activeBroker.getAccount()

    override suspend fun getPositions(): List<Position> = activeBroker.getPositions()

    override suspend fun getOrders(): List<Order> = activeBroker.getOrders()

    override suspend fun submitOrder(
        symbol: String,
        side: OrderSide,
        type: OrderType,
        quantity: Double,
        limitPrice: Double?,
        stopPrice: Double?
    ): Order? = activeBroker.submitOrder(symbol, side, type, quantity, limitPrice, stopPrice)

    override suspend fun cancelOrder(orderId: String) = activeBroker.cancelOrder(orderId)

    override suspend fun closePosition(symbol: String) = activeBroker.closePosition(symbol)

    override suspend fun closeAllPositions() = activeBroker.closeAllPositions()

    override suspend fun getQuote(symbol: String): Quote? = activeMarketData.getQuote(symbol)

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> =
        activeMarketData.getQuotes(symbols)

    override suspend fun getHistoricalBars(
        symbol: String,
        timeframe: String,
        start: String,
        end: String?,
        limit: Int
    ): List<PriceBar> = activeMarketData.getHistoricalBars(symbol, timeframe, start, end, limit)
}

data class BrokerInfo(
    val type: BrokerType,
    val name: String,
    val description: String,
    val features: List<String>,
    val setupUrl: String
)
