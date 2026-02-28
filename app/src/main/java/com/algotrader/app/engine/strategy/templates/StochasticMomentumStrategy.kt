package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.EMA
import com.algotrader.app.engine.indicators.StochasticOscillator
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class StochasticMomentumStrategy(
    private val kPeriod: Int = 14,
    private val dPeriod: Int = 3,
    private val oversold: Double = 20.0,
    private val overbought: Double = 80.0,
    private val trendEmaPeriod: Int = 200
) : TradingStrategy {

    override val name = "Stochastic Momentum"
    override val description = "Stochastic %K/%D crossover at extremes, filtered by 200 EMA trend direction"

    private var stochK = listOf<Double>()
    private var stochD = listOf<Double>()
    private var trendEma = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        val stoch = StochasticOscillator(kPeriod, dPeriod)
        stochK = stoch.calculateK(bars)
        stochD = stoch.calculateD(bars)
        trendEma = EMA(trendEmaPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close

        val k = stochK[currentIndex]
        val d = stochD[currentIndex]
        val prevK = stochK[currentIndex - 1]
        val prevD = stochD[currentIndex - 1]
        val ema = trendEma[currentIndex]

        if (k.isNaN() || d.isNaN() || prevK.isNaN() || prevD.isNaN()) return null

        val uptrend = ema.isNaN() || close > ema
        val downtrend = ema.isNaN() || close < ema

        val kCrossAboveD = prevK <= prevD && k > d
        val kCrossBelowD = prevK >= prevD && k < d

        return when {
            kCrossAboveD && k < oversold + 10 && uptrend -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "Stochastic bullish cross at ${"%.0f".format(k)} in uptrend"
            )
            kCrossBelowD && k > overbought - 10 && downtrend -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Stochastic bearish cross at ${"%.0f".format(k)} in downtrend"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "kPeriod" to kPeriod,
        "dPeriod" to dPeriod,
        "oversold" to oversold,
        "overbought" to overbought
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Stochastic Momentum") {
    describe("Stochastic %K/%D cross at extremes + 200 EMA trend")
    param("kPeriod", 14)
    param("dPeriod", 3)

    onBar { index ->
        if (index < 14) return@onBar null
        val window = bars.subList(index - 14 + 1, index + 1)
        val hh = window.maxOf { it.high }
        val ll = window.minOf { it.low }
        val range = hh - ll
        val k = if (range != 0.0) ((close[index] - ll) / range) * 100 else 50.0

        val trend = ema(200)
        val uptrend = trend[index].isNaN() || close[index] > trend[index]

        when {
            k < 20 && uptrend -> buy(reason = "Stochastic oversold in uptrend")
            k > 80 && !uptrend -> sell(reason = "Stochastic overbought in downtrend")
            else -> null
        }
    }
}"""
    }
}
