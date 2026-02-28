package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.Supertrend
import com.algotrader.app.engine.indicators.SupertrendResult
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class SupertrendStrategy(
    private val atrPeriod: Int = 10,
    private val multiplier: Double = 3.0
) : TradingStrategy {

    override val name = "Supertrend"
    override val description = "ATR-based trend following with dynamic support/resistance flip signals"

    private lateinit var stResult: SupertrendResult

    override fun initialize(bars: List<PriceBar>) {
        stResult = Supertrend(atrPeriod, multiplier).calculateFull(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol

        val currDir = stResult.direction[currentIndex]
        val prevDir = stResult.direction[currentIndex - 1]
        val st = stResult.supertrend[currentIndex]

        if (st.isNaN()) return null

        return when {
            prevDir == -1 && currDir == 1 -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "Supertrend flipped bullish (support: ${"%.2f".format(st)})"
            )
            prevDir == 1 && currDir == -1 -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Supertrend flipped bearish (resistance: ${"%.2f".format(st)})"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "atrPeriod" to atrPeriod,
        "multiplier" to multiplier
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Supertrend") {
    describe("ATR-based trend with dynamic support/resistance flips")
    param("atrPeriod", 10)
    param("multiplier", 3.0)

    onBar { index ->
        if (index < 11) return@onBar null
        val atrLine = atr(10)
        val price = close[index]
        val prevPrice = close[index - 1]
        if (atrLine[index].isNaN()) return@onBar null

        val hl2 = (high[index] + low[index]) / 2.0
        val upper = hl2 + 3.0 * atrLine[index]
        val lower = hl2 - 3.0 * atrLine[index]

        when {
            prevPrice <= lower && price > lower ->
                buy(reason = "Supertrend bullish flip")
            prevPrice >= upper && price < upper ->
                sell(reason = "Supertrend bearish flip")
            else -> null
        }
    }
}"""
    }
}
