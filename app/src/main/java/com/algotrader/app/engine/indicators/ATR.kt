package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar
import kotlin.math.abs
import kotlin.math.max

class ATR(private val period: Int = 14) : Indicator {
    override val name: String = "ATR($period)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        if (bars.size < 2) return List(bars.size) { Double.NaN }

        val trueRanges = MutableList(bars.size) { Double.NaN }
        trueRanges[0] = bars[0].high - bars[0].low

        for (i in 1 until bars.size) {
            val highLow = bars[i].high - bars[i].low
            val highPrevClose = abs(bars[i].high - bars[i - 1].close)
            val lowPrevClose = abs(bars[i].low - bars[i - 1].close)
            trueRanges[i] = max(highLow, max(highPrevClose, lowPrevClose))
        }

        val result = MutableList(bars.size) { Double.NaN }
        if (bars.size < period) return result

        var sum = 0.0
        for (i in 0 until period) {
            sum += trueRanges[i]
        }
        result[period - 1] = sum / period

        for (i in period until bars.size) {
            result[i] = (result[i - 1] * (period - 1) + trueRanges[i]) / period
        }

        return result
    }
}
