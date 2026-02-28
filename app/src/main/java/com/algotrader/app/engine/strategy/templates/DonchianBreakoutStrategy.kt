package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.DonchianChannel
import com.algotrader.app.engine.indicators.DonchianChannelResult
import com.algotrader.app.engine.indicators.EMA
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class DonchianBreakoutStrategy(
    private val channelPeriod: Int = 20,
    private val trendPeriod: Int = 50
) : TradingStrategy {

    override val name = "Donchian Channel Breakout"
    override val description = "Pure channel breakout strategy with EMA trend filter to avoid choppy markets"

    private lateinit var donchian: DonchianChannelResult
    private var trendEma = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        donchian = DonchianChannel(channelPeriod).calculateFull(bars)
        trendEma = EMA(trendPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close
        val prevClose = bars[currentIndex - 1].close

        val upper = donchian.upper[currentIndex - 1]
        val lower = donchian.lower[currentIndex - 1]
        val ema = trendEma[currentIndex]

        if (upper.isNaN() || lower.isNaN()) return null

        val aboveTrend = ema.isNaN() || close > ema
        val belowTrend = ema.isNaN() || close < ema

        return when {
            prevClose <= upper && close > upper && aboveTrend -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "Donchian $channelPeriod-bar breakout above ${"%.2f".format(upper)}"
            )
            prevClose >= lower && close < lower && belowTrend -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Donchian $channelPeriod-bar breakdown below ${"%.2f".format(lower)}"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "channelPeriod" to channelPeriod,
        "trendPeriod" to trendPeriod
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Donchian Channel Breakout") {
    describe("Channel breakout with EMA trend filter")
    param("channel", 20)
    param("trend", 50)

    onBar { index ->
        if (index < 21) return@onBar null
        val windowHigh = high.subList(index - 20, index).max()
        val windowLow = low.subList(index - 20, index).min()
        val trendLine = ema(50)

        val aboveTrend = trendLine[index].isNaN() || close[index] > trendLine[index]
        val belowTrend = trendLine[index].isNaN() || close[index] < trendLine[index]

        when {
            close[index] > windowHigh && aboveTrend ->
                buy(reason = "Donchian breakout above channel")
            close[index] < windowLow && belowTrend ->
                sell(reason = "Donchian breakdown below channel")
            else -> null
        }
    }
}"""
    }
}
