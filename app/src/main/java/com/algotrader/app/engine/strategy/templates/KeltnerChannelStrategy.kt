package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.KeltnerChannel
import com.algotrader.app.engine.indicators.KeltnerChannelResult
import com.algotrader.app.engine.indicators.RSI
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class KeltnerChannelStrategy(
    private val emaPeriod: Int = 20,
    private val atrMultiplier: Double = 2.0,
    private val rsiPeriod: Int = 14,
    private val rsiOversold: Double = 30.0,
    private val rsiOverbought: Double = 70.0
) : TradingStrategy {

    override val name = "Keltner Channel Mean Reversion"
    override val description = "Mean reversion at Keltner Channel bands with RSI confirmation"

    private lateinit var keltner: KeltnerChannelResult
    private var rsiValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        keltner = KeltnerChannel(emaPeriod, 14, atrMultiplier).calculateFull(bars)
        rsiValues = RSI(rsiPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close
        val prevClose = bars[currentIndex - 1].close

        val upper = keltner.upper[currentIndex]
        val lower = keltner.lower[currentIndex]
        val rsi = rsiValues[currentIndex]

        if (upper.isNaN() || lower.isNaN()) return null

        return when {
            prevClose <= lower && close > lower && (!rsi.isNaN() && rsi < rsiOversold + 10) -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "Price bounced off lower Keltner band (RSI: ${"%.1f".format(rsi)})"
            )
            prevClose >= upper && close < upper && (!rsi.isNaN() && rsi > rsiOverbought - 10) -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Price rejected at upper Keltner band (RSI: ${"%.1f".format(rsi)})"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "emaPeriod" to emaPeriod,
        "atrMultiplier" to atrMultiplier,
        "rsiPeriod" to rsiPeriod
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Keltner Channel Mean Reversion") {
    describe("Fade Keltner Channel extremes with RSI confirmation")
    param("emaPeriod", 20)
    param("atrMultiplier", 2.0)

    onBar { index ->
        if (index < 1) return@onBar null
        val emaLine = ema(20)
        val atrLine = atr(14)
        val rsiLine = rsi(14)
        val price = close[index]
        val prevPrice = close[index - 1]

        if (emaLine[index].isNaN() || atrLine[index].isNaN()) return@onBar null

        val upper = emaLine[index] + 2.0 * atrLine[index]
        val lower = emaLine[index] - 2.0 * atrLine[index]
        val rsiVal = rsiLine[index]

        when {
            prevPrice <= lower && price > lower && rsiVal < 40 ->
                buy(reason = "Keltner lower band bounce + oversold")
            prevPrice >= upper && price < upper && rsiVal > 60 ->
                sell(reason = "Keltner upper band rejection + overbought")
            else -> null
        }
    }
}"""
    }
}
