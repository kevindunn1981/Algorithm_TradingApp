package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.RSI
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class RsiStrategy(
    private val period: Int = 14,
    private val oversoldThreshold: Double = 30.0,
    private val overboughtThreshold: Double = 70.0
) : TradingStrategy {

    override val name = "RSI Mean Reversion"
    override val description = "Buys when RSI is oversold, sells when overbought"

    private var rsiValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        rsiValues = RSI(period).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol
        val rsi = rsiValues[currentIndex]
        val prevRsi = rsiValues[currentIndex - 1]

        if (rsi.isNaN() || prevRsi.isNaN()) return null

        return when {
            prevRsi <= oversoldThreshold && rsi > oversoldThreshold -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = (oversoldThreshold - rsi + 10) / 10,
                reason = "RSI bounced from oversold (${"%.1f".format(rsi)})"
            )
            prevRsi >= overboughtThreshold && rsi < overboughtThreshold -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = (rsi - overboughtThreshold + 10) / 10,
                reason = "RSI dropped from overbought (${"%.1f".format(rsi)})"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "period" to period,
        "oversoldThreshold" to oversoldThreshold,
        "overboughtThreshold" to overboughtThreshold
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("RSI Mean Reversion") {
    describe("Buy on RSI oversold bounce, sell on overbought drop")
    param("period", 14)
    param("oversold", 30.0)
    param("overbought", 70.0)

    onBar { index ->
        val rsiValues = rsi(14)
        if (index < 1) return@onBar null

        val current = rsiValues[index]
        val previous = rsiValues[index - 1]
        if (current.isNaN() || previous.isNaN()) return@onBar null

        when {
            previous <= 30.0 && current > 30.0 -> buy(reason = "RSI oversold bounce")
            previous >= 70.0 && current < 70.0 -> sell(reason = "RSI overbought drop")
            else -> null
        }
    }
}"""
    }
}
