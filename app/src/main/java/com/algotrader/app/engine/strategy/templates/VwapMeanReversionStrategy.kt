package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.VWAP
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy
import kotlin.math.abs

class VwapMeanReversionStrategy(
    private val deviationThreshold: Double = 1.5
) : TradingStrategy {

    override val name = "VWAP Mean Reversion"
    override val description = "Fades price deviations from VWAP, expecting reversion to volume-weighted average"

    private var vwapValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        vwapValues = VWAP().calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 2) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close
        val prevClose = bars[currentIndex - 1].close
        val vwap = vwapValues[currentIndex]
        val prevVwap = vwapValues[currentIndex - 1]

        if (vwap.isNaN() || vwap == 0.0) return null

        val deviationPct = ((close - vwap) / vwap) * 100
        val prevDeviationPct = ((prevClose - prevVwap) / prevVwap) * 100

        return when {
            prevDeviationPct < -deviationThreshold && deviationPct > -deviationThreshold -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = abs(deviationPct) / deviationThreshold,
                reason = "Price reverting up to VWAP (deviation: ${"%.2f".format(deviationPct)}%)"
            )
            prevDeviationPct > deviationThreshold && deviationPct < deviationThreshold -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = abs(deviationPct) / deviationThreshold,
                reason = "Price reverting down to VWAP (deviation: ${"%.2f".format(deviationPct)}%)"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "deviationThreshold" to deviationThreshold
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("VWAP Mean Reversion") {
    describe("Fade price deviations from VWAP")
    param("deviationPct", 1.5)

    onBar { index ->
        if (index < 2) return@onBar null
        val vwapLine = vwap()
        val price = close[index]
        val vwapVal = vwapLine[index]
        if (vwapVal.isNaN() || vwapVal == 0.0) return@onBar null

        val deviation = ((price - vwapVal) / vwapVal) * 100
        val prevDev = ((close[index-1] - vwapLine[index-1]) / vwapLine[index-1]) * 100

        when {
            prevDev < -1.5 && deviation > -1.5 ->
                buy(reason = "Reverting up to VWAP")
            prevDev > 1.5 && deviation < 1.5 ->
                sell(reason = "Reverting down to VWAP")
            else -> null
        }
    }
}"""
    }
}
