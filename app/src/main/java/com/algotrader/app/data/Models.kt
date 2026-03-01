package com.algotrader.app.data

data class Stock(
    val ticker: String,
    val name: String,
    val price: Double,
    val change: Double,
    val changePercent: Double,
    val volume: String
)

data class Holding(
    val ticker: String,
    val name: String,
    val shares: Double,
    val avgCost: Double,
    val currentPrice: Double
) {
    val totalValue: Double get() = shares * currentPrice
    val totalCost: Double get() = shares * avgCost
    val gainLoss: Double get() = totalValue - totalCost
    val gainLossPercent: Double get() = if (totalCost > 0) (gainLoss / totalCost) * 100 else 0.0
}

data class TradingAlgorithm(
    val name: String,
    val description: String,
    val isActive: Boolean,
    val totalTrades: Int,
    val winRate: Double,
    val totalReturn: Double,
    val status: String
)

data class MarketIndex(
    val name: String,
    val value: Double,
    val change: Double,
    val changePercent: Double
)
