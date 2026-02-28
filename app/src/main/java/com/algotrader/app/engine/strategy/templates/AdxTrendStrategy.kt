package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.ADX
import com.algotrader.app.engine.indicators.ADXResult
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class AdxTrendStrategy(
    private val adxPeriod: Int = 14,
    private val adxThreshold: Double = 25.0
) : TradingStrategy {

    override val name = "ADX Trend Following"
    override val description = "DI+/DI- crossover trades filtered by ADX trend strength above $adxThreshold"

    private lateinit var adxResult: ADXResult

    override fun initialize(bars: List<PriceBar>) {
        adxResult = ADX(adxPeriod).calculateFull(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol

        val adx = adxResult.adx[currentIndex]
        val plusDI = adxResult.plusDI[currentIndex]
        val minusDI = adxResult.minusDI[currentIndex]
        val prevPlusDI = adxResult.plusDI[currentIndex - 1]
        val prevMinusDI = adxResult.minusDI[currentIndex - 1]

        if (adx.isNaN() || plusDI.isNaN() || minusDI.isNaN()) return null
        if (prevPlusDI.isNaN() || prevMinusDI.isNaN()) return null

        if (adx < adxThreshold) return null

        return when {
            prevPlusDI <= prevMinusDI && plusDI > minusDI -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = adx / 50.0,
                reason = "DI+ crossed above DI- (ADX: ${"%.1f".format(adx)})"
            )
            prevPlusDI >= prevMinusDI && plusDI < minusDI -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = adx / 50.0,
                reason = "DI- crossed above DI+ (ADX: ${"%.1f".format(adx)})"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "adxPeriod" to adxPeriod,
        "adxThreshold" to adxThreshold
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("ADX Trend Following") {
    describe("Trade DI+/DI- crossovers when ADX confirms strong trend")
    param("period", 14)
    param("threshold", 25.0)

    onBar { index ->
        if (index < 28) return@onBar null
        // Use EMAs as DI proxy for DSL
        val fast = ema(7)
        val slow = ema(21)
        val trend = ema(50)

        val trendStrength = if (!trend[index].isNaN() && trend[index] != 0.0) {
            kotlin.math.abs((close[index] - trend[index]) / trend[index]) * 100
        } else 0.0

        if (trendStrength < 1.0) return@onBar null  // Weak trend filter

        when {
            crossOver(fast, slow, index) -> buy(reason = "Bullish DI cross, strong trend")
            crossUnder(fast, slow, index) -> sell(reason = "Bearish DI cross, strong trend")
            else -> null
        }
    }
}"""
    }
}
