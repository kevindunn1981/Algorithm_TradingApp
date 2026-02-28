package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar
import kotlin.math.abs

class CCI(private val period: Int = 20) : Indicator {
    override val name: String = "CCI($period)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        if (bars.size < period) return List(bars.size) { Double.NaN }

        val typicalPrices = bars.map { (it.high + it.low + it.close) / 3.0 }
        val result = MutableList(bars.size) { Double.NaN }

        for (i in period - 1 until bars.size) {
            val window = typicalPrices.subList(i - period + 1, i + 1)
            val mean = window.average()
            val meanDeviation = window.sumOf { abs(it - mean) } / period

            result[i] = if (meanDeviation != 0.0) {
                (typicalPrices[i] - mean) / (0.015 * meanDeviation)
            } else {
                0.0
            }
        }
        return result
    }
}
