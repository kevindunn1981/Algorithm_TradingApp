package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar
import kotlin.math.abs

class RSI(private val period: Int = 14) : Indicator {
    override val name: String = "RSI($period)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculate(bars.map { it.close }, period)
    }

    companion object {
        fun calculate(values: List<Double>, period: Int = 14): List<Double> {
            if (values.size < period + 1) return List(values.size) { Double.NaN }

            val result = MutableList(values.size) { Double.NaN }
            val gains = mutableListOf<Double>()
            val losses = mutableListOf<Double>()

            for (i in 1 until values.size) {
                val change = values[i] - values[i - 1]
                gains.add(if (change > 0) change else 0.0)
                losses.add(if (change < 0) abs(change) else 0.0)
            }

            var avgGain = gains.subList(0, period).average()
            var avgLoss = losses.subList(0, period).average()

            if (avgLoss == 0.0) {
                result[period] = 100.0
            } else {
                val rs = avgGain / avgLoss
                result[period] = 100.0 - (100.0 / (1.0 + rs))
            }

            for (i in period until gains.size) {
                avgGain = (avgGain * (period - 1) + gains[i]) / period
                avgLoss = (avgLoss * (period - 1) + losses[i]) / period

                if (avgLoss == 0.0) {
                    result[i + 1] = 100.0
                } else {
                    val rs = avgGain / avgLoss
                    result[i + 1] = 100.0 - (100.0 / (1.0 + rs))
                }
            }

            return result
        }
    }
}
