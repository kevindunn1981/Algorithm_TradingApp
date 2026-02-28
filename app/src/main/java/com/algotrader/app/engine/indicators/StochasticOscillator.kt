package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

class StochasticOscillator(
    private val kPeriod: Int = 14,
    private val dPeriod: Int = 3
) : Indicator {
    override val name: String = "STOCH($kPeriod,$dPeriod)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculateK(bars)
    }

    fun calculateK(bars: List<PriceBar>): List<Double> {
        if (bars.size < kPeriod) return List(bars.size) { Double.NaN }

        val result = MutableList(bars.size) { Double.NaN }

        for (i in kPeriod - 1 until bars.size) {
            val window = bars.subList(i - kPeriod + 1, i + 1)
            val lowestLow = window.minOf { it.low }
            val highestHigh = window.maxOf { it.high }

            val range = highestHigh - lowestLow
            result[i] = if (range != 0.0) {
                ((bars[i].close - lowestLow) / range) * 100.0
            } else {
                50.0
            }
        }

        return result
    }

    fun calculateD(bars: List<PriceBar>): List<Double> {
        val kValues = calculateK(bars)
        return SMA.calculate(kValues.filter { !it.isNaN() }, dPeriod).let { smaValues ->
            val result = MutableList(bars.size) { Double.NaN }
            val firstValid = kValues.indexOfFirst { !it.isNaN() }
            if (firstValid >= 0) {
                for (i in smaValues.indices) {
                    if (!smaValues[i].isNaN()) {
                        result[firstValid + i] = smaValues[i]
                    }
                }
            }
            result
        }
    }
}
