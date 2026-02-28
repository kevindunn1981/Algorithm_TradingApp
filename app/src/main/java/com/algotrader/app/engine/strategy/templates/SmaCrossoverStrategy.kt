package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.SMA
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class SmaCrossoverStrategy(
    private val fastPeriod: Int = 10,
    private val slowPeriod: Int = 30
) : TradingStrategy {

    override val name = "SMA Crossover"
    override val description = "Buys when fast SMA crosses above slow SMA, sells on cross below"

    private var fastSma = listOf<Double>()
    private var slowSma = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        fastSma = SMA(fastPeriod).calculate(bars)
        slowSma = SMA(slowPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol

        val prevFast = fastSma[currentIndex - 1]
        val prevSlow = slowSma[currentIndex - 1]
        val currFast = fastSma[currentIndex]
        val currSlow = slowSma[currentIndex]

        if (prevFast.isNaN() || prevSlow.isNaN() || currFast.isNaN() || currSlow.isNaN()) {
            return null
        }

        return when {
            prevFast <= prevSlow && currFast > currSlow -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "Fast SMA crossed above Slow SMA"
            )
            prevFast >= prevSlow && currFast < currSlow -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Fast SMA crossed below Slow SMA"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "fastPeriod" to fastPeriod,
        "slowPeriod" to slowPeriod
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("SMA Crossover") {
    describe("Buy on golden cross, sell on death cross")
    param("fastPeriod", 10)
    param("slowPeriod", 30)

    onBar { index ->
        val fast = sma(10)
        val slow = sma(30)

        when {
            crossOver(fast, slow, index) -> buy(reason = "Golden cross")
            crossUnder(fast, slow, index) -> sell(reason = "Death cross")
            else -> null
        }
    }
}"""
    }
}
