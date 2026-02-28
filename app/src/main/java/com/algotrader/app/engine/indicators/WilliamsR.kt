package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

class WilliamsR(private val period: Int = 14) : Indicator {
    override val name: String = "Williams%R($period)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        if (bars.size < period) return List(bars.size) { Double.NaN }

        val result = MutableList(bars.size) { Double.NaN }

        for (i in period - 1 until bars.size) {
            val window = bars.subList(i - period + 1, i + 1)
            val highestHigh = window.maxOf { it.high }
            val lowestLow = window.minOf { it.low }
            val range = highestHigh - lowestLow

            result[i] = if (range != 0.0) {
                ((highestHigh - bars[i].close) / range) * -100.0
            } else {
                -50.0
            }
        }
        return result
    }
}
