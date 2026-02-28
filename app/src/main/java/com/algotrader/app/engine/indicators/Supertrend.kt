package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

data class SupertrendResult(
    val supertrend: List<Double>,
    val direction: List<Int>,
    val upperBand: List<Double>,
    val lowerBand: List<Double>
)

class Supertrend(
    private val atrPeriod: Int = 10,
    private val multiplier: Double = 3.0
) : Indicator {
    override val name: String = "Supertrend($atrPeriod,$multiplier)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculateFull(bars).supertrend
    }

    fun calculateFull(bars: List<PriceBar>): SupertrendResult {
        val n = bars.size
        val atrValues = ATR(atrPeriod).calculate(bars)
        val supertrend = MutableList(n) { Double.NaN }
        val direction = MutableList(n) { 1 }
        val upperBand = MutableList(n) { Double.NaN }
        val lowerBand = MutableList(n) { Double.NaN }

        for (i in bars.indices) {
            if (atrValues[i].isNaN()) continue

            val hl2 = (bars[i].high + bars[i].low) / 2.0
            var basicUpper = hl2 + multiplier * atrValues[i]
            var basicLower = hl2 - multiplier * atrValues[i]

            if (i > 0 && !upperBand[i - 1].isNaN()) {
                basicUpper = if (basicUpper < upperBand[i - 1] || bars[i - 1].close > upperBand[i - 1]) {
                    basicUpper
                } else {
                    upperBand[i - 1]
                }
            }

            if (i > 0 && !lowerBand[i - 1].isNaN()) {
                basicLower = if (basicLower > lowerBand[i - 1] || bars[i - 1].close < lowerBand[i - 1]) {
                    basicLower
                } else {
                    lowerBand[i - 1]
                }
            }

            upperBand[i] = basicUpper
            lowerBand[i] = basicLower

            if (i == 0 || supertrend[i - 1].isNaN()) {
                direction[i] = if (bars[i].close > basicUpper) 1 else -1
            } else {
                direction[i] = when {
                    supertrend[i - 1] == upperBand[i - 1] && bars[i].close <= upperBand[i] -> -1
                    supertrend[i - 1] == upperBand[i - 1] && bars[i].close > upperBand[i] -> 1
                    supertrend[i - 1] == lowerBand[i - 1] && bars[i].close >= lowerBand[i] -> 1
                    supertrend[i - 1] == lowerBand[i - 1] && bars[i].close < lowerBand[i] -> -1
                    else -> direction[i - 1]
                }
            }

            supertrend[i] = if (direction[i] == 1) lowerBand[i] else upperBand[i]
        }

        return SupertrendResult(supertrend, direction, upperBand, lowerBand)
    }
}
