package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.EMA
import com.algotrader.app.engine.indicators.MACD
import com.algotrader.app.engine.indicators.MacdResult
import com.algotrader.app.engine.indicators.StochasticOscillator
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class ElderTripleScreenStrategy(
    private val trendEmaPeriod: Int = 50,
    private val stochPeriod: Int = 14,
    private val stochOversold: Double = 20.0,
    private val stochOverbought: Double = 80.0
) : TradingStrategy {

    override val name = "Elder Triple Screen"
    override val description = "Alexander Elder's multi-timeframe: MACD trend (tide) + Stochastic entry (wave) + price confirmation (ripple)"

    private var trendEma = listOf<Double>()
    private lateinit var macdResult: MacdResult
    private var stochK = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        trendEma = EMA(trendEmaPeriod).calculate(bars)
        macdResult = MACD().calculateFull(bars)
        stochK = StochasticOscillator(stochPeriod).calculateK(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 2) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close

        val ema = trendEma[currentIndex]
        val histogram = macdResult.histogram[currentIndex]
        val prevHistogram = macdResult.histogram[currentIndex - 1]
        val stoch = stochK[currentIndex]
        val prevStoch = stochK[currentIndex - 1]

        if (ema.isNaN() || histogram.isNaN() || stoch.isNaN()) return null

        // Screen 1: Trend direction (tide)
        val bullishTrend = close > ema && (!prevHistogram.isNaN() && histogram > prevHistogram)
        val bearishTrend = close < ema && (!prevHistogram.isNaN() && histogram < prevHistogram)

        // Screen 2: Counter-trend pullback (wave)
        val stochOversoldBounce = !prevStoch.isNaN() && prevStoch <= stochOversold && stoch > stochOversold
        val stochOverboughtDrop = !prevStoch.isNaN() && prevStoch >= stochOverbought && stoch < stochOverbought

        // Screen 3: Price confirmation (ripple)
        val priceBreakAbove = close > bars[currentIndex - 1].high
        val priceBreakBelow = close < bars[currentIndex - 1].low

        return when {
            bullishTrend && stochOversoldBounce && priceBreakAbove -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = 1.0,
                reason = "Elder Triple Screen: uptrend + stochastic bounce + price breakout"
            )
            bullishTrend && stoch < 40 && priceBreakAbove -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = 0.6,
                reason = "Elder: uptrend + stochastic low + price confirmation"
            )
            bearishTrend && stochOverboughtDrop && priceBreakBelow -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = 1.0,
                reason = "Elder Triple Screen: downtrend + stochastic drop + price breakdown"
            )
            bearishTrend && stoch > 60 && priceBreakBelow -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = 0.6,
                reason = "Elder: downtrend + stochastic high + price confirmation"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "trendEmaPeriod" to trendEmaPeriod,
        "stochPeriod" to stochPeriod,
        "stochOversold" to stochOversold,
        "stochOverbought" to stochOverbought
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Elder Triple Screen") {
    describe("Multi-timeframe: MACD trend + Stochastic pullback + price trigger")
    param("trendEma", 50)
    param("stochPeriod", 14)

    onBar { index ->
        if (index < 50) return@onBar null
        val trend = ema(50)
        val macdData = macd(12, 26, 9)
        val rsiLine = rsi(14)

        if (trend[index].isNaN() || macdData.histogram[index].isNaN()) return@onBar null
        val hist = macdData.histogram[index]
        val prevHist = macdData.histogram[index - 1]

        // Screen 1: Trend
        val uptrend = close[index] > trend[index] && hist > prevHist
        val downtrend = close[index] < trend[index] && hist < prevHist

        // Screen 2: Pullback (RSI as stochastic proxy)
        val pullbackBuy = rsiLine[index] < 40
        val pullbackSell = rsiLine[index] > 60

        // Screen 3: Trigger
        val breakUp = close[index] > high[index - 1]
        val breakDown = close[index] < low[index - 1]

        when {
            uptrend && pullbackBuy && breakUp ->
                buy(reason = "Elder: trend + pullback + trigger")
            downtrend && pullbackSell && breakDown ->
                sell(reason = "Elder: trend + pullback + trigger")
            else -> null
        }
    }
}"""
    }
}
