package com.algotrader.app.data.model

import java.time.Instant
import java.time.LocalDateTime

data class PriceBar(
    val symbol: String,
    val timestamp: Instant,
    val open: Double,
    val high: Double,
    val low: Double,
    val close: Double,
    val volume: Long
)

data class Quote(
    val symbol: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: Long,
    val timestamp: Instant
)

data class Position(
    val symbol: String,
    val quantity: Double,
    val averageEntryPrice: Double,
    val currentPrice: Double,
    val marketValue: Double,
    val unrealizedPnl: Double,
    val unrealizedPnlPercent: Double
)

data class Account(
    val id: String,
    val equity: Double,
    val cash: Double,
    val buyingPower: Double,
    val portfolioValue: Double,
    val dayTradeCount: Int,
    val patternDayTrader: Boolean
)

data class Order(
    val id: String,
    val symbol: String,
    val side: OrderSide,
    val type: OrderType,
    val quantity: Double,
    val limitPrice: Double? = null,
    val stopPrice: Double? = null,
    val filledPrice: Double? = null,
    val status: OrderStatus,
    val submittedAt: Instant,
    val filledAt: Instant? = null
)

enum class OrderSide { BUY, SELL }

enum class OrderType { MARKET, LIMIT, STOP, STOP_LIMIT, TRAILING_STOP }

enum class OrderStatus {
    NEW, PARTIALLY_FILLED, FILLED, DONE_FOR_DAY,
    CANCELED, EXPIRED, REPLACED, PENDING_NEW,
    ACCEPTED, PENDING_CANCEL, PENDING_REPLACE, REJECTED
}

data class Strategy(
    val id: Long = 0,
    val name: String,
    val description: String,
    val code: String,
    val language: StrategyLanguage = StrategyLanguage.KOTLIN_DSL,
    val isActive: Boolean = false,
    val createdAt: LocalDateTime = LocalDateTime.now(),
    val updatedAt: LocalDateTime = LocalDateTime.now()
)

enum class StrategyLanguage { KOTLIN_DSL, PYTHON }

enum class TradingMode { BACKTEST, PAPER, LIVE }

data class BacktestResult(
    val strategyId: Long,
    val strategyName: String,
    val symbol: String,
    val startDate: Instant,
    val endDate: Instant,
    val initialCapital: Double,
    val finalCapital: Double,
    val totalReturn: Double,
    val totalReturnPercent: Double,
    val annualizedReturn: Double,
    val sharpeRatio: Double,
    val sortinoRatio: Double,
    val maxDrawdown: Double,
    val maxDrawdownPercent: Double,
    val totalTrades: Int,
    val winningTrades: Int,
    val losingTrades: Int,
    val winRate: Double,
    val profitFactor: Double,
    val averageWin: Double,
    val averageLoss: Double,
    val largestWin: Double,
    val largestLoss: Double,
    val trades: List<BacktestTrade>,
    val equityCurve: List<EquityPoint>
)

data class BacktestTrade(
    val entryTime: Instant,
    val exitTime: Instant,
    val side: OrderSide,
    val entryPrice: Double,
    val exitPrice: Double,
    val quantity: Double,
    val pnl: Double,
    val pnlPercent: Double
)

data class EquityPoint(
    val timestamp: Instant,
    val equity: Double
)

data class WatchlistItem(
    val symbol: String,
    val name: String,
    val price: Double = 0.0,
    val change: Double = 0.0,
    val changePercent: Double = 0.0,
    val addedAt: LocalDateTime = LocalDateTime.now()
)

data class MarketOverview(
    val indices: List<Quote>,
    val topGainers: List<Quote>,
    val topLosers: List<Quote>,
    val mostActive: List<Quote>
)

data class PerformanceMetrics(
    val totalReturn: Double,
    val totalReturnPercent: Double,
    val dayReturn: Double,
    val dayReturnPercent: Double,
    val sharpeRatio: Double,
    val maxDrawdown: Double,
    val winRate: Double,
    val totalTrades: Int
)
