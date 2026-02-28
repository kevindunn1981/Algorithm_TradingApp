package com.algotrader.app.data.remote.moomoo

import com.google.gson.annotations.SerializedName

// ── Connection & Protocol ──────────────────────────────────────────────

data class MoomooRequest(
    @SerializedName("c2s") val request: Map<String, Any>,
    @SerializedName("protoId") val protocolId: Int,
    @SerializedName("serialNo") val serialNo: Int = 0
)

data class MoomooResponse(
    @SerializedName("retType") val retCode: Int = 0,
    @SerializedName("retMsg") val retMsg: String = "",
    @SerializedName("errCode") val errCode: Int = 0,
    @SerializedName("s2c") val data: Map<String, Any>? = null
)

data class MoomooInitConnectRequest(
    @SerializedName("clientVer") val clientVersion: Int = 300,
    @SerializedName("clientID") val clientId: String = "AlgoTrader",
    @SerializedName("recvNotify") val receiveNotify: Boolean = true
)

// ── Account & Trading ──────────────────────────────────────────────────

data class MoomooAccountInfo(
    @SerializedName("accID") val accountId: Long = 0,
    @SerializedName("trdEnv") val tradingEnvironment: Int = 0,
    @SerializedName("trdMarketAuthList") val authorizedMarkets: List<Int> = emptyList(),
    @SerializedName("accType") val accountType: Int = 0
)

data class MoomooAccInfoResponse(
    @SerializedName("totalAssets") val totalAssets: Double = 0.0,
    @SerializedName("cash") val cash: Double = 0.0,
    @SerializedName("marketVal") val marketValue: Double = 0.0,
    @SerializedName("frozenCash") val frozenCash: Double = 0.0,
    @SerializedName("availableFunds") val availableFunds: Double = 0.0,
    @SerializedName("maxFinanceAmount") val maxFinanceAmount: Double = 0.0,
    @SerializedName("totalPl") val totalPnl: Double = 0.0,
    @SerializedName("riskLevel") val riskLevel: Int = 0,
    @SerializedName("power") val buyingPower: Double = 0.0,
    @SerializedName("currency") val currency: Int = 0
)

data class MoomooPositionItem(
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("qty") val quantity: Double = 0.0,
    @SerializedName("canSellQty") val sellableQuantity: Double = 0.0,
    @SerializedName("costPrice") val costPrice: Double = 0.0,
    @SerializedName("price") val marketPrice: Double = 0.0,
    @SerializedName("costPriceValid") val costPriceValid: Boolean = true,
    @SerializedName("marketVal") val marketValue: Double = 0.0,
    @SerializedName("plRatio") val plRatio: Double = 0.0,
    @SerializedName("plRatioValid") val plRatioValid: Boolean = true,
    @SerializedName("plVal") val plValue: Double = 0.0,
    @SerializedName("todayBuyQty") val todayBuyQty: Double = 0.0,
    @SerializedName("todaySellQty") val todaySellQty: Double = 0.0,
    @SerializedName("todayPLVal") val todayPnl: Double = 0.0,
    @SerializedName("secMarket") val market: Int = 0
)

data class MoomooPositionListResponse(
    @SerializedName("positionList") val positions: List<MoomooPositionItem> = emptyList()
)

// ── Orders ─────────────────────────────────────────────────────────────

data class MoomooPlaceOrderRequest(
    @SerializedName("trdSide") val side: Int,
    @SerializedName("orderType") val orderType: Int,
    @SerializedName("code") val code: String,
    @SerializedName("qty") val quantity: Double,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("adjustPrice") val adjustPrice: Boolean = false,
    @SerializedName("trdEnv") val tradingEnvironment: Int = 0,
    @SerializedName("accID") val accountId: Long = 0,
    @SerializedName("secMarket") val market: Int = 2,
    @SerializedName("auxPrice") val auxPrice: Double? = null,
    @SerializedName("trailType") val trailType: Int? = null,
    @SerializedName("trailValue") val trailValue: Double? = null,
    @SerializedName("trailSpread") val trailSpread: Double? = null
)

data class MoomooPlaceOrderResponse(
    @SerializedName("orderID") val orderId: String = "",
    @SerializedName("orderIDEx") val orderIdExternal: String = ""
)

data class MoomooModifyOrderRequest(
    @SerializedName("orderID") val orderId: String,
    @SerializedName("modifyOrderOp") val operation: Int,
    @SerializedName("qty") val quantity: Double? = null,
    @SerializedName("price") val price: Double? = null,
    @SerializedName("trdEnv") val tradingEnvironment: Int = 0,
    @SerializedName("accID") val accountId: Long = 0
)

data class MoomooOrderItem(
    @SerializedName("orderID") val orderId: String = "",
    @SerializedName("orderIDEx") val orderIdExternal: String = "",
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("trdSide") val side: Int = 0,
    @SerializedName("orderType") val orderType: Int = 0,
    @SerializedName("qty") val quantity: Double = 0.0,
    @SerializedName("price") val price: Double = 0.0,
    @SerializedName("fillAvgPrice") val fillAvgPrice: Double = 0.0,
    @SerializedName("fillQty") val filledQuantity: Double = 0.0,
    @SerializedName("orderStatus") val status: Int = 0,
    @SerializedName("createTime") val createTime: String = "",
    @SerializedName("updateTime") val updateTime: String = "",
    @SerializedName("secMarket") val market: Int = 0,
    @SerializedName("auxPrice") val auxPrice: Double = 0.0
)

data class MoomooOrderListResponse(
    @SerializedName("orderList") val orders: List<MoomooOrderItem> = emptyList()
)

// ── Quote / Market Data ────────────────────────────────────────────────

data class MoomooQuoteItem(
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("lastPrice") val lastPrice: Double = 0.0,
    @SerializedName("openPrice") val openPrice: Double = 0.0,
    @SerializedName("highPrice") val highPrice: Double = 0.0,
    @SerializedName("lowPrice") val lowPrice: Double = 0.0,
    @SerializedName("prevClosePrice") val prevClosePrice: Double = 0.0,
    @SerializedName("volume") val volume: Long = 0,
    @SerializedName("turnover") val turnover: Double = 0.0,
    @SerializedName("turnoverRate") val turnoverRate: Double = 0.0,
    @SerializedName("amplitude") val amplitude: Double = 0.0,
    @SerializedName("suspension") val suspension: Boolean = false,
    @SerializedName("listingDate") val listingDate: String = "",
    @SerializedName("priceSpread") val priceSpread: Double = 0.0,
    @SerializedName("dataDate") val dataDate: String = "",
    @SerializedName("dataTime") val dataTime: String = "",
    @SerializedName("secStatus") val securityStatus: Int = 0
)

data class MoomooQuoteResponse(
    @SerializedName("quoteList") val quotes: List<MoomooQuoteItem> = emptyList()
)

data class MoomooSnapshotItem(
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("lastPrice") val lastPrice: Double = 0.0,
    @SerializedName("openPrice") val openPrice: Double = 0.0,
    @SerializedName("highPrice") val highPrice: Double = 0.0,
    @SerializedName("lowPrice") val lowPrice: Double = 0.0,
    @SerializedName("prevClosePrice") val prevClosePrice: Double = 0.0,
    @SerializedName("volume") val volume: Long = 0,
    @SerializedName("turnover") val turnover: Double = 0.0,
    @SerializedName("updateTime") val updateTime: String = "",
    @SerializedName("lotSize") val lotSize: Int = 0,
    @SerializedName("totalMarketVal") val totalMarketValue: Double = 0.0,
    @SerializedName("circulationMarketVal") val circulationMarketValue: Double = 0.0,
    @SerializedName("peRatio") val peRatio: Double = 0.0,
    @SerializedName("pbRatio") val pbRatio: Double = 0.0,
    @SerializedName("peTTMRatio") val peTtmRatio: Double = 0.0,
    @SerializedName("dividendRateTTM") val dividendRateTtm: Double = 0.0,
    @SerializedName("high52WeeksPrice") val high52Weeks: Double = 0.0,
    @SerializedName("low52WeeksPrice") val low52Weeks: Double = 0.0,
    @SerializedName("isStock") val isStock: Boolean = true
)

data class MoomooSnapshotResponse(
    @SerializedName("snapshotList") val snapshots: List<MoomooSnapshotItem> = emptyList()
)

// ── Candlestick / K-Line ───────────────────────────────────────────────

data class MoomooKLineItem(
    @SerializedName("code") val code: String = "",
    @SerializedName("timeKey") val timeKey: String = "",
    @SerializedName("open") val open: Double = 0.0,
    @SerializedName("close") val close: Double = 0.0,
    @SerializedName("high") val high: Double = 0.0,
    @SerializedName("low") val low: Double = 0.0,
    @SerializedName("volume") val volume: Long = 0,
    @SerializedName("turnover") val turnover: Double = 0.0,
    @SerializedName("peRatio") val peRatio: Double = 0.0,
    @SerializedName("changeRate") val changeRate: Double = 0.0,
    @SerializedName("lastClose") val lastClose: Double = 0.0
)

data class MoomooKLineResponse(
    @SerializedName("klList") val klines: List<MoomooKLineItem> = emptyList(),
    @SerializedName("code") val code: String = "",
    @SerializedName("name") val name: String = "",
    @SerializedName("hasNext") val hasNext: Boolean = false,
    @SerializedName("nextPageReqKey") val nextPageKey: String? = null
)

// ── Enums / Constants ──────────────────────────────────────────────────

object MoomooConstants {
    // Trading side
    const val TRD_SIDE_BUY = 1
    const val TRD_SIDE_SELL = 2
    const val TRD_SIDE_SELL_SHORT = 3
    const val TRD_SIDE_BUY_BACK = 4

    // Order type
    const val ORDER_TYPE_NORMAL = 1        // Limit (regular)
    const val ORDER_TYPE_MARKET = 2        // Market (for HK/US)
    const val ORDER_TYPE_ABSOLUTE_LIMIT = 5
    const val ORDER_TYPE_AUCTION = 6
    const val ORDER_TYPE_AUCTION_LIMIT = 7
    const val ORDER_TYPE_SPECIAL_LIMIT = 8
    const val ORDER_TYPE_SPECIAL_LIMIT_ALL = 9

    // Order status
    const val ORDER_STATUS_UNSUBMITTED = 0
    const val ORDER_STATUS_UNKNOWN = -1
    const val ORDER_STATUS_WAITING_SUBMIT = 1
    const val ORDER_STATUS_SUBMITTING = 2
    const val ORDER_STATUS_SUBMITTED = 3
    const val ORDER_STATUS_FILLED_PART = 10
    const val ORDER_STATUS_FILLED_ALL = 11
    const val ORDER_STATUS_CANCELLING = 12
    const val ORDER_STATUS_CANCELLED_PART = 13
    const val ORDER_STATUS_CANCELLED_ALL = 14
    const val ORDER_STATUS_FAILED = 15
    const val ORDER_STATUS_DISABLED = 21
    const val ORDER_STATUS_DELETED = 22
    const val ORDER_STATUS_FILL_CANCELLED = 23

    // Modify order operation
    const val MODIFY_OP_NORMAL = 0
    const val MODIFY_OP_CANCEL = 1
    const val MODIFY_OP_DISABLE = 2
    const val MODIFY_OP_ENABLE = 3
    const val MODIFY_OP_DELETE = 4

    // Trading environment
    const val TRD_ENV_SIMULATE = 0    // Paper trading
    const val TRD_ENV_REAL = 1        // Live trading

    // Market
    const val TRD_MARKET_HK = 1
    const val TRD_MARKET_US = 2
    const val TRD_MARKET_CN = 3
    const val TRD_MARKET_HKCC = 4
    const val TRD_MARKET_FUTURES = 5
    const val TRD_MARKET_SG = 6
    const val TRD_MARKET_JP = 7
    const val TRD_MARKET_AU = 8

    // K-Line type
    const val KLINE_1MIN = 1
    const val KLINE_3MIN = 6
    const val KLINE_5MIN = 2
    const val KLINE_15MIN = 3
    const val KLINE_30MIN = 4
    const val KLINE_60MIN = 5
    const val KLINE_DAY = 7
    const val KLINE_WEEK = 8
    const val KLINE_MONTH = 9
    const val KLINE_YEAR = 11
    const val KLINE_QUARTER = 12

    // Protocol IDs
    const val PROTO_INIT_CONNECT = 1001
    const val PROTO_GET_GLOBAL_STATE = 1002
    const val PROTO_KEEP_ALIVE = 1004

    const val PROTO_QOT_GET_MARKET_SNAPSHOT = 3203
    const val PROTO_QOT_GET_STOCK_QUOTE = 3004
    const val PROTO_QOT_REQUEST_HISTORY_KLINE = 3103
    const val PROTO_QOT_SUB = 3001
    const val PROTO_QOT_GET_SUB_INFO = 3003

    const val PROTO_TRD_GET_ACC_LIST = 2001
    const val PROTO_TRD_UNLOCK_TRADE = 2005
    const val PROTO_TRD_GET_FUNDS = 2101
    const val PROTO_TRD_GET_POSITIONS = 2102
    const val PROTO_TRD_GET_MAX_TRD_QTYS = 2111
    const val PROTO_TRD_GET_ORDER_LIST = 2201
    const val PROTO_TRD_PLACE_ORDER = 2202
    const val PROTO_TRD_MODIFY_ORDER = 2205
    const val PROTO_TRD_GET_ORDER_FILL_LIST = 2211
    const val PROTO_TRD_GET_HISTORY_ORDER_LIST = 2221
    const val PROTO_TRD_GET_HISTORY_ORDER_FILL_LIST = 2222

    fun marketSymbolPrefix(market: Int): String = when (market) {
        TRD_MARKET_US -> "US"
        TRD_MARKET_HK -> "HK"
        TRD_MARKET_CN -> "SH"
        TRD_MARKET_SG -> "SG"
        TRD_MARKET_JP -> "JP"
        TRD_MARKET_AU -> "AU"
        else -> "US"
    }

    fun toMoomooSymbol(symbol: String, market: Int = TRD_MARKET_US): String {
        if (symbol.contains(".")) return symbol
        return "${marketSymbolPrefix(market)}.$symbol"
    }

    fun fromMoomooSymbol(moomooCode: String): String {
        val parts = moomooCode.split(".")
        return if (parts.size >= 2) parts[1] else moomooCode
    }
}
