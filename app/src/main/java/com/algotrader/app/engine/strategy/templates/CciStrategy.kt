package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.CCI
import com.algotrader.app.engine.indicators.SMA
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class CciStrategy(
    private val cciPeriod: Int = 20,
    private val overbought: Double = 100.0,
    private val oversold: Double = -100.0,
    private val smaPeriod: Int = 50
) : TradingStrategy {

    override val name = "CCI Trend & Reversal"
    override val description = "Commodity Channel Index: buy on CCI crossing above -100, sell on dropping below +100, filtered by SMA trend"

    private var cciValues = listOf<Double>()
    private var smaValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        cciValues = CCI(cciPeriod).calculate(bars)
        smaValues = SMA(smaPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close

        val cci = cciValues[currentIndex]
        val prevCci = cciValues[currentIndex - 1]
        val sma = smaValues[currentIndex]

        if (cci.isNaN() || prevCci.isNaN()) return null

        val bullishTrend = sma.isNaN() || close > sma
        val bearishTrend = sma.isNaN() || close < sma

        return when {
            prevCci <= oversold && cci > oversold && bullishTrend -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "CCI crossed above ${"%.0f".format(oversold)} (${"%.0f".format(cci)})"
            )
            prevCci >= overbought && cci < overbought && bearishTrend -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "CCI dropped below ${"%.0f".format(overbought)} (${"%.0f".format(cci)})"
            )
            cci > 200 -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = 0.5,
                reason = "CCI extreme overbought (${"%.0f".format(cci)})"
            )
            cci < -200 -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = 0.5,
                reason = "CCI extreme oversold (${"%.0f".format(cci)})"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "cciPeriod" to cciPeriod,
        "overbought" to overbought,
        "oversold" to oversold
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("CCI Trend & Reversal") {
    describe("CCI zero-line and extreme level signals with trend filter")
    param("period", 20)
    param("overbought", 100.0)
    param("oversold", -100.0)

    onBar { index ->
        if (index < 20) return@onBar null
        val window = bars.subList(index - 20 + 1, index + 1)
        val tp = window.map { (it.high + it.low + it.close) / 3.0 }
        val mean = tp.average()
        val md = tp.sumOf { kotlin.math.abs(it - mean) } / 20
        val cci = if (md != 0.0) (tp.last() - mean) / (0.015 * md) else 0.0

        val trendFilter = sma(50)
        val uptrend = trendFilter[index].isNaN() || close[index] > trendFilter[index]

        when {
            cci < -100 && uptrend -> buy(reason = "CCI oversold in uptrend")
            cci > 100 && !uptrend -> sell(reason = "CCI overbought in downtrend")
            else -> null
        }
    }
}"""
    }
}
