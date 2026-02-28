package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

data class DonchianChannelResult(
    val upper: List<Double>,
    val lower: List<Double>,
    val middle: List<Double>
)

class DonchianChannel(private val period: Int = 20) : Indicator {
    override val name: String = "Donchian($period)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculateFull(bars).middle
    }

    fun calculateFull(bars: List<PriceBar>): DonchianChannelResult {
        val upper = MutableList(bars.size) { Double.NaN }
        val lower = MutableList(bars.size) { Double.NaN }
        val middle = MutableList(bars.size) { Double.NaN }

        for (i in period - 1 until bars.size) {
            val window = bars.subList(i - period + 1, i + 1)
            val high = window.maxOf { it.high }
            val low = window.minOf { it.low }
            upper[i] = high
            lower[i] = low
            middle[i] = (high + low) / 2.0
        }

        return DonchianChannelResult(upper, lower, middle)
    }
}
