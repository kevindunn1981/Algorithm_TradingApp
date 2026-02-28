package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

data class KeltnerChannelResult(
    val upper: List<Double>,
    val middle: List<Double>,
    val lower: List<Double>
)

class KeltnerChannel(
    private val emaPeriod: Int = 20,
    private val atrPeriod: Int = 14,
    private val atrMultiplier: Double = 2.0
) : Indicator {
    override val name: String = "Keltner($emaPeriod,$atrMultiplier)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculateFull(bars).middle
    }

    fun calculateFull(bars: List<PriceBar>): KeltnerChannelResult {
        val emaValues = EMA(emaPeriod).calculate(bars)
        val atrValues = ATR(atrPeriod).calculate(bars)

        val upper = MutableList(bars.size) { Double.NaN }
        val middle = emaValues.toMutableList()
        val lower = MutableList(bars.size) { Double.NaN }

        for (i in bars.indices) {
            if (!emaValues[i].isNaN() && !atrValues[i].isNaN()) {
                upper[i] = emaValues[i] + atrMultiplier * atrValues[i]
                lower[i] = emaValues[i] - atrMultiplier * atrValues[i]
            }
        }

        return KeltnerChannelResult(upper, middle, lower)
    }
}
