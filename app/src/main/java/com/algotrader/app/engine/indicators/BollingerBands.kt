package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar
import kotlin.math.sqrt

class BollingerBands(
    private val period: Int = 20,
    private val stdDevMultiplier: Double = 2.0
) : Indicator {
    override val name: String = "BB($period,$stdDevMultiplier)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculateFull(bars).middle
    }

    fun calculateFull(bars: List<PriceBar>): BollingerBandsResult {
        return calculateFull(bars.map { it.close }, period, stdDevMultiplier)
    }

    companion object {
        fun calculateFull(
            values: List<Double>,
            period: Int = 20,
            stdDevMultiplier: Double = 2.0
        ): BollingerBandsResult {
            val middle = SMA.calculate(values, period)
            val upper = MutableList(values.size) { Double.NaN }
            val lower = MutableList(values.size) { Double.NaN }
            val bandwidth = MutableList(values.size) { Double.NaN }

            for (i in period - 1 until values.size) {
                val slice = values.subList(i - period + 1, i + 1)
                val mean = middle[i]
                val variance = slice.sumOf { (it - mean) * (it - mean) } / period
                val stdDev = sqrt(variance)

                upper[i] = mean + stdDevMultiplier * stdDev
                lower[i] = mean - stdDevMultiplier * stdDev
                bandwidth[i] = if (mean != 0.0) (upper[i] - lower[i]) / mean else 0.0
            }

            return BollingerBandsResult(upper, middle, lower, bandwidth)
        }
    }
}
