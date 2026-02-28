package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy
import kotlin.math.sqrt

class PairsTradingStrategy(
    private val lookbackPeriod: Int = 60,
    private val entryZScore: Double = 2.0,
    private val exitZScore: Double = 0.5
) : TradingStrategy {

    override val name = "Mean Reversion Z-Score"
    override val description = "Statistical arbitrage: trade when price Z-score deviates from rolling mean, exit on reversion"

    override fun initialize(bars: List<PriceBar>) {}

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < lookbackPeriod) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close

        val window = bars.subList(currentIndex - lookbackPeriod, currentIndex)
        val closes = window.map { it.close }
        val mean = closes.average()
        val variance = closes.sumOf { (it - mean) * (it - mean) } / closes.size
        val stdDev = sqrt(variance)

        if (stdDev == 0.0) return null

        val zScore = (close - mean) / stdDev
        val prevClose = bars[currentIndex - 1].close
        val prevZScore = (prevClose - mean) / stdDev

        return when {
            prevZScore > -entryZScore && zScore <= -entryZScore -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = kotlin.math.abs(zScore) / entryZScore,
                reason = "Z-score dropped to ${"%.2f".format(zScore)} (entry threshold: -$entryZScore)"
            )
            prevZScore < entryZScore && zScore >= entryZScore -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = kotlin.math.abs(zScore) / entryZScore,
                reason = "Z-score rose to ${"%.2f".format(zScore)} (entry threshold: +$entryZScore)"
            )
            prevZScore < -exitZScore && zScore >= -exitZScore -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Z-score reverted to ${"%.2f".format(zScore)} (exit long)"
            )
            prevZScore > exitZScore && zScore <= exitZScore -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "Z-score reverted to ${"%.2f".format(zScore)} (exit short)"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "lookbackPeriod" to lookbackPeriod,
        "entryZScore" to entryZScore,
        "exitZScore" to exitZScore
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Mean Reversion Z-Score") {
    describe("Trade Z-score deviations from rolling mean")
    param("lookback", 60)
    param("entryZ", 2.0)
    param("exitZ", 0.5)

    onBar { index ->
        if (index < 60) return@onBar null
        val window = close.subList(index - 60, index)
        val mean = window.average()
        val stdDev = kotlin.math.sqrt(window.sumOf { (it - mean) * (it - mean) } / 60)
        if (stdDev == 0.0) return@onBar null

        val z = (close[index] - mean) / stdDev
        val prevZ = (close[index - 1] - mean) / stdDev

        when {
            prevZ > -2.0 && z <= -2.0 -> buy(reason = "Z-score oversold")
            prevZ < 2.0 && z >= 2.0 -> sell(reason = "Z-score overbought")
            else -> null
        }
    }
}"""
    }
}
