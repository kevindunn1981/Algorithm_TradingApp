package com.algotrader.app.engine

import com.algotrader.app.data.model.BacktestResult
import com.algotrader.app.data.model.BacktestTrade
import com.algotrader.app.data.model.EquityPoint
import com.algotrader.app.data.model.OrderSide
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Duration
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

class BacktestEngine(
    private val commissionRate: Double = 0.0,
    private val slippagePct: Double = 0.001
) {
    data class BacktestConfig(
        val initialCapital: Double = 100_000.0,
        val positionSizePercent: Double = 100.0,
        val maxPositionSize: Double = Double.MAX_VALUE,
        val stopLossPercent: Double? = null,
        val takeProfitPercent: Double? = null,
        val symbol: String = "UNKNOWN"
    )

    suspend fun run(
        strategy: TradingStrategy,
        bars: List<PriceBar>,
        config: BacktestConfig = BacktestConfig()
    ): BacktestResult = withContext(Dispatchers.Default) {
        require(bars.isNotEmpty()) { "Price data cannot be empty" }

        strategy.initialize(bars)

        var cash = config.initialCapital
        var shares = 0.0
        var entryPrice = 0.0
        var entryTime = bars.first().timestamp

        val trades = mutableListOf<BacktestTrade>()
        val equityCurve = mutableListOf<EquityPoint>()
        var peakEquity = config.initialCapital

        for (i in bars.indices) {
            val bar = bars[i]
            val equity = cash + shares * bar.close
            equityCurve.add(EquityPoint(bar.timestamp, equity))
            peakEquity = max(peakEquity, equity)

            if (shares > 0 && config.stopLossPercent != null) {
                val loss = (bar.close - entryPrice) / entryPrice
                if (loss <= -config.stopLossPercent / 100.0) {
                    val exitPrice = bar.close * (1 - slippagePct)
                    val commission = shares * exitPrice * commissionRate
                    val pnl = (exitPrice - entryPrice) * shares - commission
                    cash += shares * exitPrice - commission
                    trades.add(BacktestTrade(
                        entryTime = entryTime,
                        exitTime = bar.timestamp,
                        side = OrderSide.BUY,
                        entryPrice = entryPrice,
                        exitPrice = exitPrice,
                        quantity = shares,
                        pnl = pnl,
                        pnlPercent = (exitPrice - entryPrice) / entryPrice * 100
                    ))
                    shares = 0.0
                    continue
                }
            }

            if (shares > 0 && config.takeProfitPercent != null) {
                val gain = (bar.close - entryPrice) / entryPrice
                if (gain >= config.takeProfitPercent / 100.0) {
                    val exitPrice = bar.close * (1 - slippagePct)
                    val commission = shares * exitPrice * commissionRate
                    val pnl = (exitPrice - entryPrice) * shares - commission
                    cash += shares * exitPrice - commission
                    trades.add(BacktestTrade(
                        entryTime = entryTime,
                        exitTime = bar.timestamp,
                        side = OrderSide.BUY,
                        entryPrice = entryPrice,
                        exitPrice = exitPrice,
                        quantity = shares,
                        pnl = pnl,
                        pnlPercent = (exitPrice - entryPrice) / entryPrice * 100
                    ))
                    shares = 0.0
                    continue
                }
            }

            val signal = strategy.onBar(i, bars) ?: continue

            when (signal.action) {
                SignalAction.BUY -> {
                    if (shares <= 0) {
                        val buyPrice = bar.close * (1 + slippagePct)
                        val availableCapital = cash * (config.positionSizePercent / 100.0)
                        val maxShares = min(availableCapital / buyPrice, config.maxPositionSize)
                        if (maxShares > 0) {
                            val commission = maxShares * buyPrice * commissionRate
                            shares = maxShares
                            entryPrice = buyPrice
                            entryTime = bar.timestamp
                            cash -= shares * buyPrice + commission
                        }
                    }
                }
                SignalAction.SELL -> {
                    if (shares > 0) {
                        val sellPrice = bar.close * (1 - slippagePct)
                        val commission = shares * sellPrice * commissionRate
                        val pnl = (sellPrice - entryPrice) * shares - commission
                        cash += shares * sellPrice - commission
                        trades.add(BacktestTrade(
                            entryTime = entryTime,
                            exitTime = bar.timestamp,
                            side = OrderSide.BUY,
                            entryPrice = entryPrice,
                            exitPrice = sellPrice,
                            quantity = shares,
                            pnl = pnl,
                            pnlPercent = (sellPrice - entryPrice) / entryPrice * 100
                        ))
                        shares = 0.0
                    }
                }
                SignalAction.HOLD -> {}
            }
        }

        if (shares > 0) {
            val lastPrice = bars.last().close
            val pnl = (lastPrice - entryPrice) * shares
            cash += shares * lastPrice
            trades.add(BacktestTrade(
                entryTime = entryTime,
                exitTime = bars.last().timestamp,
                side = OrderSide.BUY,
                entryPrice = entryPrice,
                exitPrice = lastPrice,
                quantity = shares,
                pnl = pnl,
                pnlPercent = (lastPrice - entryPrice) / entryPrice * 100
            ))
        }

        val finalCapital = cash
        val totalReturn = finalCapital - config.initialCapital
        val totalReturnPercent = (totalReturn / config.initialCapital) * 100

        val durationDays = if (bars.size > 1) {
            Duration.between(bars.first().timestamp, bars.last().timestamp).toDays().coerceAtLeast(1)
        } else 1L
        val years = durationDays / 365.25
        val annualizedReturn = if (years > 0) {
            ((finalCapital / config.initialCapital).pow(1.0 / years) - 1) * 100
        } else totalReturnPercent

        val winningTrades = trades.filter { it.pnl > 0 }
        val losingTrades = trades.filter { it.pnl <= 0 }
        val winRate = if (trades.isNotEmpty()) winningTrades.size.toDouble() / trades.size * 100 else 0.0

        val avgWin = if (winningTrades.isNotEmpty()) winningTrades.map { it.pnl }.average() else 0.0
        val avgLoss = if (losingTrades.isNotEmpty()) losingTrades.map { it.pnl }.average() else 0.0
        val profitFactor = if (avgLoss != 0.0) abs(avgWin * winningTrades.size / (avgLoss * losingTrades.size)) else Double.MAX_VALUE

        val returns = equityCurve.zipWithNext().map { (a, b) ->
            if (a.equity != 0.0) (b.equity - a.equity) / a.equity else 0.0
        }
        val sharpeRatio = calculateSharpeRatio(returns)
        val sortinoRatio = calculateSortinoRatio(returns)

        var maxDrawdown = 0.0
        var maxDrawdownPercent = 0.0
        var peak = config.initialCapital
        for (point in equityCurve) {
            peak = max(peak, point.equity)
            val drawdown = peak - point.equity
            val drawdownPct = if (peak > 0) drawdown / peak * 100 else 0.0
            maxDrawdown = max(maxDrawdown, drawdown)
            maxDrawdownPercent = max(maxDrawdownPercent, drawdownPct)
        }

        BacktestResult(
            strategyId = 0,
            strategyName = strategy.name,
            symbol = config.symbol,
            startDate = bars.first().timestamp,
            endDate = bars.last().timestamp,
            initialCapital = config.initialCapital,
            finalCapital = finalCapital,
            totalReturn = totalReturn,
            totalReturnPercent = totalReturnPercent,
            annualizedReturn = annualizedReturn,
            sharpeRatio = sharpeRatio,
            sortinoRatio = sortinoRatio,
            maxDrawdown = maxDrawdown,
            maxDrawdownPercent = maxDrawdownPercent,
            totalTrades = trades.size,
            winningTrades = winningTrades.size,
            losingTrades = losingTrades.size,
            winRate = winRate,
            profitFactor = profitFactor,
            averageWin = avgWin,
            averageLoss = avgLoss,
            largestWin = winningTrades.maxOfOrNull { it.pnl } ?: 0.0,
            largestLoss = losingTrades.minOfOrNull { it.pnl } ?: 0.0,
            trades = trades,
            equityCurve = equityCurve
        )
    }

    private fun calculateSharpeRatio(returns: List<Double>, riskFreeRate: Double = 0.0): Double {
        if (returns.size < 2) return 0.0
        val excessReturns = returns.map { it - riskFreeRate / 252 }
        val mean = excessReturns.average()
        val stdDev = sqrt(excessReturns.map { (it - mean) * (it - mean) }.average())
        return if (stdDev != 0.0) (mean / stdDev) * sqrt(252.0) else 0.0
    }

    private fun calculateSortinoRatio(returns: List<Double>, riskFreeRate: Double = 0.0): Double {
        if (returns.size < 2) return 0.0
        val excessReturns = returns.map { it - riskFreeRate / 252 }
        val mean = excessReturns.average()
        val downsideReturns = excessReturns.filter { it < 0 }
        val downsideDev = if (downsideReturns.isNotEmpty()) {
            sqrt(downsideReturns.map { it * it }.average())
        } else 0.0
        return if (downsideDev != 0.0) (mean / downsideDev) * sqrt(252.0) else 0.0
    }
}
