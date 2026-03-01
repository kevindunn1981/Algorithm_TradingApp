package com.algotrader.app.data

import kotlin.random.Random

object MockDataProvider {

    private val random = Random.Default

    val marketIndices = listOf(
        MarketIndex("S&P 500", 5_248.49, 32.14, 0.62),
        MarketIndex("NASDAQ", 16_428.82, -45.23, -0.27),
        MarketIndex("DOW", 39_118.86, 125.08, 0.32)
    )

    val watchlist = listOf(
        Stock("AAPL", "Apple Inc.", 189.30, 2.45, 1.31, "58.2M"),
        Stock("GOOGL", "Alphabet Inc.", 175.82, -1.23, -0.69, "22.1M"),
        Stock("MSFT", "Microsoft Corp.", 415.55, 5.78, 1.41, "21.8M"),
        Stock("AMZN", "Amazon.com Inc.", 186.44, 3.12, 1.70, "44.5M"),
        Stock("TSLA", "Tesla Inc.", 177.58, -8.34, -4.49, "105.3M"),
        Stock("NVDA", "NVIDIA Corp.", 879.44, 21.33, 2.49, "39.7M"),
        Stock("META", "Meta Platforms", 503.31, -2.15, -0.43, "18.9M"),
        Stock("JPM", "JPMorgan Chase", 198.47, 1.06, 0.54, "9.8M")
    )

    val holdings = listOf(
        Holding("AAPL", "Apple Inc.", 50.0, 155.20, 189.30),
        Holding("MSFT", "Microsoft Corp.", 25.0, 380.00, 415.55),
        Holding("NVDA", "NVIDIA Corp.", 10.0, 620.00, 879.44),
        Holding("AMZN", "Amazon.com Inc.", 30.0, 175.50, 186.44),
        Holding("GOOGL", "Alphabet Inc.", 20.0, 165.00, 175.82)
    )

    val algorithms = listOf(
        TradingAlgorithm(
            name = "Moving Average Crossover",
            description = "Buys when 50-day MA crosses above 200-day MA (Golden Cross)",
            isActive = true,
            totalTrades = 142,
            winRate = 61.3,
            totalReturn = 18.7,
            status = "Monitoring"
        ),
        TradingAlgorithm(
            name = "RSI Mean Reversion",
            description = "Buys oversold stocks (RSI < 30), sells overbought (RSI > 70)",
            isActive = true,
            totalTrades = 287,
            winRate = 54.7,
            totalReturn = 12.4,
            status = "Signal Detected"
        ),
        TradingAlgorithm(
            name = "Momentum Strategy",
            description = "Follows stocks with strong upward momentum over 12-month period",
            isActive = false,
            totalTrades = 95,
            winRate = 58.9,
            totalReturn = 22.1,
            status = "Paused"
        ),
        TradingAlgorithm(
            name = "MACD Divergence",
            description = "Identifies divergence between MACD line and signal line",
            isActive = true,
            totalTrades = 183,
            winRate = 56.8,
            totalReturn = 15.3,
            status = "Scanning"
        ),
        TradingAlgorithm(
            name = "Bollinger Band Squeeze",
            description = "Enters positions when volatility contracts and price breaks out",
            isActive = false,
            totalTrades = 61,
            winRate = 63.9,
            totalReturn = 9.8,
            status = "Inactive"
        )
    )

    fun refreshedWatchlist(): List<Stock> = watchlist.map { stock ->
        val delta = (random.nextDouble() - 0.5) * 2.0
        val newPrice = (stock.price + delta).coerceAtLeast(1.0)
        val newChange = stock.change + delta
        val newChangePercent = (newChange / (newPrice - newChange)) * 100
        stock.copy(price = newPrice, change = newChange, changePercent = newChangePercent)
    }

    fun refreshedIndices(): List<MarketIndex> = marketIndices.map { index ->
        val delta = (random.nextDouble() - 0.5) * 10.0
        val newValue = (index.value + delta).coerceAtLeast(1.0)
        val newChange = index.change + delta
        val newChangePercent = if (newValue - newChange != 0.0) (newChange / (newValue - newChange)) * 100 else 0.0
        index.copy(value = newValue, change = newChange, changePercent = newChangePercent)
    }
}
