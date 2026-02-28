package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

class EMA(private val period: Int) : Indicator {
    override val name: String = "EMA($period)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculate(bars.map { it.close }, period)
    }

    companion object {
        fun calculate(values: List<Double>, period: Int): List<Double> {
            if (values.size < period) return List(values.size) { Double.NaN }

            val result = MutableList(values.size) { Double.NaN }
            val multiplier = 2.0 / (period + 1)

            var sum = 0.0
            for (i in 0 until period) {
                sum += values[i]
            }
            result[period - 1] = sum / period

            for (i in period until values.size) {
                result[i] = (values[i] - result[i - 1]) * multiplier + result[i - 1]
            }

            return result
        }
    }
}
