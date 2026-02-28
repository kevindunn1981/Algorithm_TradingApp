package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

class SMA(private val period: Int) : Indicator {
    override val name: String = "SMA($period)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        if (bars.size < period) return List(bars.size) { Double.NaN }

        val result = MutableList(bars.size) { Double.NaN }
        var sum = 0.0

        for (i in bars.indices) {
            sum += bars[i].close
            if (i >= period) {
                sum -= bars[i - period].close
            }
            if (i >= period - 1) {
                result[i] = sum / period
            }
        }
        return result
    }

    companion object {
        fun calculate(values: List<Double>, period: Int): List<Double> {
            if (values.size < period) return List(values.size) { Double.NaN }

            val result = MutableList(values.size) { Double.NaN }
            var sum = 0.0

            for (i in values.indices) {
                sum += values[i]
                if (i >= period) {
                    sum -= values[i - period]
                }
                if (i >= period - 1) {
                    result[i] = sum / period
                }
            }
            return result
        }
    }
}
