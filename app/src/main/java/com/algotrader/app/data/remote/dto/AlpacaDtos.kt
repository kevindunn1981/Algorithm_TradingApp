package com.algotrader.app.data.remote.dto

import com.google.gson.annotations.SerializedName

data class AlpacaAccountDto(
    val id: String,
    val equity: String,
    val cash: String,
    @SerializedName("buying_power") val buyingPower: String,
    @SerializedName("portfolio_value") val portfolioValue: String,
    @SerializedName("daytrade_count") val dayTradeCount: Int,
    @SerializedName("pattern_day_trader") val patternDayTrader: Boolean,
    val status: String
)

data class AlpacaPositionDto(
    @SerializedName("asset_id") val assetId: String,
    val symbol: String,
    val qty: String,
    @SerializedName("avg_entry_price") val avgEntryPrice: String,
    @SerializedName("current_price") val currentPrice: String,
    @SerializedName("market_value") val marketValue: String,
    @SerializedName("unrealized_pl") val unrealizedPl: String,
    @SerializedName("unrealized_plpc") val unrealizedPlpc: String,
    val side: String
)

data class AlpacaOrderDto(
    val id: String,
    val symbol: String,
    val side: String,
    val type: String,
    val qty: String?,
    @SerializedName("limit_price") val limitPrice: String?,
    @SerializedName("stop_price") val stopPrice: String?,
    @SerializedName("filled_avg_price") val filledAvgPrice: String?,
    val status: String,
    @SerializedName("submitted_at") val submittedAt: String,
    @SerializedName("filled_at") val filledAt: String?
)

data class AlpacaOrderRequest(
    val symbol: String,
    val qty: String? = null,
    val notional: String? = null,
    val side: String,
    val type: String,
    @SerializedName("time_in_force") val timeInForce: String = "day",
    @SerializedName("limit_price") val limitPrice: String? = null,
    @SerializedName("stop_price") val stopPrice: String? = null,
    @SerializedName("trail_percent") val trailPercent: String? = null
)

data class AlpacaBarDto(
    @SerializedName("t") val timestamp: String,
    @SerializedName("o") val open: Double,
    @SerializedName("h") val high: Double,
    @SerializedName("l") val low: Double,
    @SerializedName("c") val close: Double,
    @SerializedName("v") val volume: Long
)

data class AlpacaBarsResponse(
    val bars: Map<String, List<AlpacaBarDto>>?,
    @SerializedName("next_page_token") val nextPageToken: String?
)

data class AlpacaSnapshotDto(
    val latestTrade: AlpacaTradeDto?,
    val latestQuote: AlpacaQuoteDto?,
    val minuteBar: AlpacaBarDto?,
    val dailyBar: AlpacaBarDto?,
    val prevDailyBar: AlpacaBarDto?
)

data class AlpacaTradeDto(
    @SerializedName("t") val timestamp: String,
    @SerializedName("p") val price: Double,
    @SerializedName("s") val size: Int
)

data class AlpacaQuoteDto(
    @SerializedName("t") val timestamp: String,
    @SerializedName("bp") val bidPrice: Double,
    @SerializedName("bs") val bidSize: Int,
    @SerializedName("ap") val askPrice: Double,
    @SerializedName("as") val askSize: Int
)

data class AlpacaAssetDto(
    val id: String,
    val symbol: String,
    val name: String,
    val exchange: String,
    val status: String,
    val tradable: Boolean,
    val fractionable: Boolean
)
