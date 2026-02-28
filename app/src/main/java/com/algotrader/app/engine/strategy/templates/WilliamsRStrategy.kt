package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.EMA
import com.algotrader.app.engine.indicators.WilliamsR
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class WilliamsRStrategy(
    private val wrPeriod: Int = 14,
    private val oversold: Double = -80.0,
    private val overbought: Double = -20.0,
    private val trendEmaPeriod: Int = 50
) : TradingStrategy {

    override val name = "Williams %R"
    override val description = "Williams Percent Range reversals with EMA trend filter for aligned entries"

    private var wrValues = listOf<Double>()
    private var emaValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        wrValues = WilliamsR(wrPeriod).calculate(bars)
        emaValues = EMA(trendEmaPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close

        val wr = wrValues[currentIndex]
        val prevWr = wrValues[currentIndex - 1]
        val ema = emaValues[currentIndex]

        if (wr.isNaN() || prevWr.isNaN()) return null

        val bullishTrend = !ema.isNaN() && close > ema
        val bearishTrend = !ema.isNaN() && close < ema

        return when {
            prevWr <= oversold && wr > oversold && bullishTrend -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = kotlin.math.abs(wr - oversold) / 20.0,
                reason = "Williams %R bounced from oversold (${"%.0f".format(wr)}), uptrend"
            )
            prevWr >= overbought && wr < overbought && bearishTrend -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = kotlin.math.abs(wr - overbought) / 20.0,
                reason = "Williams %R dropped from overbought (${"%.0f".format(wr)}), downtrend"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "wrPeriod" to wrPeriod,
        "oversold" to oversold,
        "overbought" to overbought
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Williams %R") {
    describe("Williams Percent Range reversals with EMA trend filter")
    param("period", 14)
    param("oversold", -80.0)
    param("overbought", -20.0)

    onBar { index ->
        if (index < 14) return@onBar null
        val lookback = bars.subList(index - 14 + 1, index + 1)
        val hh = lookback.maxOf { it.high }
        val ll = lookback.minOf { it.low }
        val range = hh - ll
        val wr = if (range != 0.0) ((hh - close[index]) / range) * -100 else -50.0

        val trendEma = ema(50)
        val uptrend = !trendEma[index].isNaN() && close[index] > trendEma[index]
        val downtrend = !trendEma[index].isNaN() && close[index] < trendEma[index]

        when {
            wr < -80.0 && uptrend -> buy(reason = "Williams %R oversold in uptrend")
            wr > -20.0 && downtrend -> sell(reason = "Williams %R overbought in downtrend")
            else -> null
        }
    }
}"""
    }
}
