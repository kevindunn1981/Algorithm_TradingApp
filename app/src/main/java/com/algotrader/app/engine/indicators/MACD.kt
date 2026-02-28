package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

class MACD(
    private val fastPeriod: Int = 12,
    private val slowPeriod: Int = 26,
    private val signalPeriod: Int = 9
) : Indicator {
    override val name: String = "MACD($fastPeriod,$slowPeriod,$signalPeriod)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculateFull(bars.map { it.close }).macdLine
    }

    fun calculateFull(bars: List<PriceBar>): MacdResult {
        return calculateFull(bars.map { it.close })
    }

    companion object {
        fun calculateFull(
            values: List<Double>,
            fastPeriod: Int = 12,
            slowPeriod: Int = 26,
            signalPeriod: Int = 9
        ): MacdResult {
            val fastEma = EMA.calculate(values, fastPeriod)
            val slowEma = EMA.calculate(values, slowPeriod)

            val macdLine = fastEma.zip(slowEma).map { (fast, slow) ->
                if (fast.isNaN() || slow.isNaN()) Double.NaN else fast - slow
            }

            val validMacd = macdLine.filter { !it.isNaN() }
            val signalEma = EMA.calculate(validMacd, signalPeriod)

            val signalLine = MutableList(macdLine.size) { Double.NaN }
            val firstValidIndex = macdLine.indexOfFirst { !it.isNaN() }
            if (firstValidIndex >= 0) {
                for (i in signalEma.indices) {
                    signalLine[firstValidIndex + i] = signalEma[i]
                }
            }

            val histogram = macdLine.zip(signalLine).map { (macd, signal) ->
                if (macd.isNaN() || signal.isNaN()) Double.NaN else macd - signal
            }

            return MacdResult(macdLine, signalLine, histogram)
        }
    }
}
