package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.EMA
import com.algotrader.app.engine.indicators.RSI
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class MomentumStrategy(
    private val emaPeriod: Int = 20,
    private val rsiPeriod: Int = 14,
    private val rsiOversold: Double = 40.0,
    private val rsiOverbought: Double = 60.0
) : TradingStrategy {

    override val name = "Multi-Indicator Momentum"
    override val description = "Combines EMA trend + RSI momentum for confirmed signals"

    private var emaValues = listOf<Double>()
    private var rsiValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        emaValues = EMA(emaPeriod).calculate(bars)
        rsiValues = RSI(rsiPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 2) return null
        val symbol = bars[currentIndex].symbol

        val price = bars[currentIndex].close
        val ema = emaValues[currentIndex]
        val rsi = rsiValues[currentIndex]
        val prevPrice = bars[currentIndex - 1].close
        val prevEma = emaValues[currentIndex - 1]

        if (ema.isNaN() || rsi.isNaN() || prevEma.isNaN()) return null

        val bullishTrend = price > ema && prevPrice <= prevEma
        val bearishTrend = price < ema && prevPrice >= prevEma

        return when {
            bullishTrend && rsi > rsiOversold && rsi < rsiOverbought -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = (rsi - rsiOversold) / (rsiOverbought - rsiOversold),
                reason = "Bullish EMA crossover + RSI momentum (${"%.1f".format(rsi)})"
            )
            bearishTrend && rsi > rsiOverbought -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = (rsi - rsiOverbought) / (100 - rsiOverbought),
                reason = "Bearish EMA crossover + RSI overbought (${"%.1f".format(rsi)})"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "emaPeriod" to emaPeriod,
        "rsiPeriod" to rsiPeriod,
        "rsiOversold" to rsiOversold,
        "rsiOverbought" to rsiOverbought
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Multi-Indicator Momentum") {
    describe("EMA trend + RSI momentum confirmation")
    param("emaPeriod", 20)
    param("rsiPeriod", 14)

    onBar { index ->
        if (index < 2) return@onBar null

        val emaVals = ema(20)
        val rsiVals = rsi(14)
        val price = close[index]
        val ema = emaVals[index]
        val rsi = rsiVals[index]

        if (ema.isNaN() || rsi.isNaN()) return@onBar null

        val bullish = price > ema && close[index - 1] <= emaVals[index - 1]
        val bearish = price < ema && close[index - 1] >= emaVals[index - 1]

        when {
            bullish && rsi in 40.0..60.0 ->
                buy(reason = "Bullish crossover + RSI momentum")
            bearish && rsi > 60.0 ->
                sell(reason = "Bearish crossover + overbought")
            else -> null
        }
    }
}"""
    }
}
