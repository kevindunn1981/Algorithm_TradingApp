package com.algotrader.app.data.remote.broker

import com.algotrader.app.data.model.Account
import com.algotrader.app.data.model.Order
import com.algotrader.app.data.model.OrderSide
import com.algotrader.app.data.model.OrderStatus
import com.algotrader.app.data.model.OrderType
import com.algotrader.app.data.model.Position
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.data.model.Quote
import com.algotrader.app.data.remote.moomoo.MoomooConstants
import com.algotrader.app.data.remote.moomoo.MoomooOpenDClient
import com.algotrader.app.data.remote.moomoo.MoomooPlaceOrderRequest
import com.algotrader.app.data.remote.moomoo.MoomooModifyOrderRequest
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MoomooBrokerProvider @Inject constructor(
    private val openDClient: MoomooOpenDClient
) : BrokerProvider, MarketDataProvider {

    override val brokerType = BrokerType.MOOMOO
    override val displayName = "Moomoo"
    override val providerType = BrokerType.MOOMOO

    private val gson = Gson()
    private var accountId: Long = 0
    private var tradingEnv: Int = MoomooConstants.TRD_ENV_SIMULATE
    private var market: Int = MoomooConstants.TRD_MARKET_US

    fun configure(
        accountId: Long = 0,
        paperTrading: Boolean = true,
        market: Int = MoomooConstants.TRD_MARKET_US
    ) {
        this.accountId = accountId
        this.tradingEnv = if (paperTrading) MoomooConstants.TRD_ENV_SIMULATE else MoomooConstants.TRD_ENV_REAL
        this.market = market
    }

    override suspend fun connect(): Boolean {
        val connected = openDClient.connect()
        if (connected && accountId == 0L) {
            discoverAccountId()
        }
        return connected
    }

    override suspend fun disconnect() {
        openDClient.disconnect()
    }

    override fun isConnected(): Boolean = openDClient.isConnected()

    @Suppress("UNCHECKED_CAST")
    private suspend fun discoverAccountId() {
        try {
            val response = openDClient.getAccountList(tradingEnv)
            val accList = response.data?.get("accList") as? List<Map<String, Any>> ?: return
            if (accList.isNotEmpty()) {
                accountId = (accList[0]["accID"] as? Number)?.toLong() ?: 0
            }
        } catch (_: Exception) {}
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getAccount(): Account? {
        return try {
            val response = openDClient.getAccountFunds(accountId, tradingEnv)
            val data = response.data ?: return null

            Account(
                id = accountId.toString(),
                equity = (data["totalAssets"] as? Number)?.toDouble() ?: 0.0,
                cash = (data["cash"] as? Number)?.toDouble() ?: 0.0,
                buyingPower = (data["power"] as? Number)?.toDouble()
                    ?: (data["availableFunds"] as? Number)?.toDouble() ?: 0.0,
                portfolioValue = (data["marketVal"] as? Number)?.toDouble()
                    ?: (data["totalAssets"] as? Number)?.toDouble() ?: 0.0,
                dayTradeCount = 0,
                patternDayTrader = false
            )
        } catch (e: Exception) {
            null
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getPositions(): List<Position> {
        return try {
            val response = openDClient.getPositions(accountId, tradingEnv, market)
            val data = response.data ?: return emptyList()
            val positionList = data["positionList"] as? List<Map<String, Any>> ?: return emptyList()

            positionList.map { pos ->
                val code = pos["code"] as? String ?: ""
                val symbol = MoomooConstants.fromMoomooSymbol(code)
                val qty = (pos["qty"] as? Number)?.toDouble() ?: 0.0
                val costPrice = (pos["costPrice"] as? Number)?.toDouble() ?: 0.0
                val marketPrice = (pos["price"] as? Number)?.toDouble() ?: 0.0
                val marketVal = (pos["marketVal"] as? Number)?.toDouble() ?: (qty * marketPrice)
                val plVal = (pos["plVal"] as? Number)?.toDouble() ?: ((marketPrice - costPrice) * qty)
                val plRatio = (pos["plRatio"] as? Number)?.toDouble() ?: 0.0

                Position(
                    symbol = symbol,
                    quantity = qty,
                    averageEntryPrice = costPrice,
                    currentPrice = marketPrice,
                    marketValue = marketVal,
                    unrealizedPnl = plVal,
                    unrealizedPnlPercent = plRatio
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getOrders(): List<Order> {
        return try {
            val response = openDClient.getOrderList(accountId, tradingEnv, market)
            val data = response.data ?: return emptyList()
            val orderList = data["orderList"] as? List<Map<String, Any>> ?: return emptyList()

            orderList.map { ord ->
                val code = ord["code"] as? String ?: ""
                val symbol = MoomooConstants.fromMoomooSymbol(code)
                val side = (ord["trdSide"] as? Number)?.toInt() ?: 0
                val orderType = (ord["orderType"] as? Number)?.toInt() ?: 1
                val status = (ord["orderStatus"] as? Number)?.toInt() ?: 0

                Order(
                    id = (ord["orderID"] as? String) ?: "",
                    symbol = symbol,
                    side = mapMoomooSide(side),
                    type = mapMoomooOrderType(orderType),
                    quantity = (ord["qty"] as? Number)?.toDouble() ?: 0.0,
                    limitPrice = (ord["price"] as? Number)?.toDouble(),
                    filledPrice = (ord["fillAvgPrice"] as? Number)?.toDouble(),
                    status = mapMoomooOrderStatus(status),
                    submittedAt = parseTimestamp(ord["createTime"] as? String),
                    filledAt = null
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
            val moomooCode = MoomooConstants.toMoomooSymbol(symbol, market)
            val moomooSide = when (side) {
                OrderSide.BUY -> MoomooConstants.TRD_SIDE_BUY
                OrderSide.SELL -> MoomooConstants.TRD_SIDE_SELL
            }
            val moomooOrderType = when (type) {
                OrderType.MARKET -> MoomooConstants.ORDER_TYPE_MARKET
                OrderType.LIMIT -> MoomooConstants.ORDER_TYPE_NORMAL
                else -> MoomooConstants.ORDER_TYPE_NORMAL
            }
            val price = when (type) {
                OrderType.MARKET -> 0.0
                OrderType.LIMIT -> limitPrice ?: 0.0
                else -> limitPrice ?: 0.0
            }

            val request = MoomooPlaceOrderRequest(
                side = moomooSide,
                orderType = moomooOrderType,
                code = moomooCode,
                quantity = quantity,
                price = price,
                tradingEnvironment = tradingEnv,
                accountId = accountId,
                market = market,
                auxPrice = stopPrice
            )

            val response = openDClient.placeOrder(request)
            val data = response.data ?: return null
            val orderId = data["orderID"] as? String ?: ""

            Order(
                id = orderId,
                symbol = symbol,
                side = side,
                type = type,
                quantity = quantity,
                limitPrice = limitPrice,
                stopPrice = stopPrice,
                status = OrderStatus.NEW,
                submittedAt = Instant.now()
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun cancelOrder(orderId: String) {
        try {
            openDClient.modifyOrder(MoomooModifyOrderRequest(
                orderId = orderId,
                operation = MoomooConstants.MODIFY_OP_CANCEL,
                tradingEnvironment = tradingEnv,
                accountId = accountId
            ))
        } catch (_: Exception) {}
    }

    override suspend fun closePosition(symbol: String) {
        try {
            val positions = getPositions()
            val position = positions.find { it.symbol == symbol } ?: return
            val side = if (position.quantity > 0) OrderSide.SELL else OrderSide.BUY
            submitOrder(
                symbol = symbol,
                side = side,
                type = OrderType.MARKET,
                quantity = kotlin.math.abs(position.quantity)
            )
        } catch (_: Exception) {}
    }

    override suspend fun closeAllPositions() {
        try {
            val positions = getPositions()
            positions.forEach { position ->
                closePosition(position.symbol)
            }
        } catch (_: Exception) {}
    }

    // ── MarketDataProvider ──────────────────────────────────────────────

    @Suppress("UNCHECKED_CAST")
    override suspend fun getQuote(symbol: String): Quote? {
        return try {
            val moomooCode = MoomooConstants.toMoomooSymbol(symbol, market)
            val response = openDClient.getMarketSnapshot(listOf(moomooCode))
            val data = response.data ?: return null
            val snapshots = data["snapshotList"] as? List<Map<String, Any>> ?: return null
            if (snapshots.isEmpty()) return null

            val snap = snapshots[0]
            val price = (snap["lastPrice"] as? Number)?.toDouble() ?: 0.0
            val prevClose = (snap["prevClosePrice"] as? Number)?.toDouble() ?: price
            val change = price - prevClose
            val changePercent = if (prevClose != 0.0) (change / prevClose) * 100 else 0.0
            val volume = (snap["volume"] as? Number)?.toLong() ?: 0

            Quote(
                symbol = symbol,
                price = price,
                change = change,
                changePercent = changePercent,
                volume = volume,
                timestamp = Instant.now()
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun getQuotes(symbols: List<String>): Map<String, Quote> {
        return try {
            val moomooCodes = symbols.map { MoomooConstants.toMoomooSymbol(it, market) }
            val response = openDClient.getMarketSnapshot(moomooCodes)
            val data = response.data ?: return emptyMap()

            @Suppress("UNCHECKED_CAST")
            val snapshots = data["snapshotList"] as? List<Map<String, Any>> ?: return emptyMap()

            snapshots.mapNotNull { snap ->
                val code = snap["code"] as? String ?: return@mapNotNull null
                val symbol = MoomooConstants.fromMoomooSymbol(code)
                val price = (snap["lastPrice"] as? Number)?.toDouble() ?: return@mapNotNull null
                val prevClose = (snap["prevClosePrice"] as? Number)?.toDouble() ?: price
                val change = price - prevClose
                val changePercent = if (prevClose != 0.0) (change / prevClose) * 100 else 0.0
                val volume = (snap["volume"] as? Number)?.toLong() ?: 0

                symbol to Quote(symbol, price, change, changePercent, volume, Instant.now())
            }.toMap()
        } catch (e: Exception) {
            emptyMap()
        }
    }

    @Suppress("UNCHECKED_CAST")
    override suspend fun getHistoricalBars(
        symbol: String,
        timeframe: String,
        start: String,
        end: String?,
        limit: Int
    ): List<PriceBar> {
        return try {
            val moomooCode = MoomooConstants.toMoomooSymbol(symbol, market)
            val klType = mapTimeframeToKLineType(timeframe)

            val startFormatted = formatDateForMoomoo(start)
            val endFormatted = end?.let { formatDateForMoomoo(it) }

            val response = openDClient.requestHistoryKLine(
                code = moomooCode,
                klineType = klType,
                start = startFormatted,
                end = endFormatted,
                maxCount = limit
            )

            val data = response.data ?: return emptyList()
            val klList = data["klList"] as? List<Map<String, Any>> ?: return emptyList()

            klList.map { kl ->
                PriceBar(
                    symbol = symbol,
                    timestamp = parseTimestamp(kl["timeKey"] as? String),
                    open = (kl["open"] as? Number)?.toDouble() ?: 0.0,
                    high = (kl["high"] as? Number)?.toDouble() ?: 0.0,
                    low = (kl["low"] as? Number)?.toDouble() ?: 0.0,
                    close = (kl["close"] as? Number)?.toDouble() ?: 0.0,
                    volume = (kl["volume"] as? Number)?.toLong() ?: 0
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ── Mapping helpers ─────────────────────────────────────────────────

    private fun mapMoomooSide(side: Int): OrderSide = when (side) {
        MoomooConstants.TRD_SIDE_BUY, MoomooConstants.TRD_SIDE_BUY_BACK -> OrderSide.BUY
        else -> OrderSide.SELL
    }

    private fun mapMoomooOrderType(type: Int): OrderType = when (type) {
        MoomooConstants.ORDER_TYPE_MARKET -> OrderType.MARKET
        MoomooConstants.ORDER_TYPE_NORMAL -> OrderType.LIMIT
        else -> OrderType.LIMIT
    }

    private fun mapMoomooOrderStatus(status: Int): OrderStatus = when (status) {
        MoomooConstants.ORDER_STATUS_UNSUBMITTED,
        MoomooConstants.ORDER_STATUS_WAITING_SUBMIT -> OrderStatus.PENDING_NEW
        MoomooConstants.ORDER_STATUS_SUBMITTING,
        MoomooConstants.ORDER_STATUS_SUBMITTED -> OrderStatus.NEW
        MoomooConstants.ORDER_STATUS_FILLED_PART -> OrderStatus.PARTIALLY_FILLED
        MoomooConstants.ORDER_STATUS_FILLED_ALL -> OrderStatus.FILLED
        MoomooConstants.ORDER_STATUS_CANCELLING -> OrderStatus.PENDING_CANCEL
        MoomooConstants.ORDER_STATUS_CANCELLED_PART,
        MoomooConstants.ORDER_STATUS_CANCELLED_ALL -> OrderStatus.CANCELED
        MoomooConstants.ORDER_STATUS_FAILED -> OrderStatus.REJECTED
        MoomooConstants.ORDER_STATUS_DELETED -> OrderStatus.CANCELED
        else -> OrderStatus.NEW
    }

    private fun mapTimeframeToKLineType(timeframe: String): Int = when (timeframe.uppercase()) {
        "1MIN" -> MoomooConstants.KLINE_1MIN
        "3MIN" -> MoomooConstants.KLINE_3MIN
        "5MIN" -> MoomooConstants.KLINE_5MIN
        "15MIN" -> MoomooConstants.KLINE_15MIN
        "30MIN" -> MoomooConstants.KLINE_30MIN
        "1HOUR", "60MIN" -> MoomooConstants.KLINE_60MIN
        "1DAY", "DAY" -> MoomooConstants.KLINE_DAY
        "1WEEK", "WEEK" -> MoomooConstants.KLINE_WEEK
        "1MONTH", "MONTH" -> MoomooConstants.KLINE_MONTH
        "QUARTER" -> MoomooConstants.KLINE_QUARTER
        "YEAR" -> MoomooConstants.KLINE_YEAR
        else -> MoomooConstants.KLINE_DAY
    }

    private fun parseTimestamp(timeStr: String?): Instant {
        if (timeStr.isNullOrBlank()) return Instant.now()
        return try {
            val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
            val ldt = LocalDateTime.parse(timeStr, formatter)
            ldt.atZone(ZoneId.of("America/New_York")).toInstant()
        } catch (e: Exception) {
            try {
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                val ldt = LocalDateTime.parse("$timeStr 00:00:00",
                    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                ldt.atZone(ZoneId.of("America/New_York")).toInstant()
            } catch (e2: Exception) {
                Instant.now()
            }
        }
    }

    private fun formatDateForMoomoo(isoDate: String): String {
        return try {
            val instant = Instant.parse(isoDate)
            val ldt = LocalDateTime.ofInstant(instant, ZoneId.of("America/New_York"))
            ldt.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        } catch (e: Exception) {
            isoDate.take(10)
        }
    }
}
