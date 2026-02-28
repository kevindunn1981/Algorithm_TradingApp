package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar
import kotlin.math.abs
import kotlin.math.max

data class ADXResult(
    val adx: List<Double>,
    val plusDI: List<Double>,
    val minusDI: List<Double>
)

class ADX(private val period: Int = 14) : Indicator {
    override val name: String = "ADX($period)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculateFull(bars).adx
    }

    fun calculateFull(bars: List<PriceBar>): ADXResult {
        val n = bars.size
        if (n < period + 1) {
            return ADXResult(
                List(n) { Double.NaN },
                List(n) { Double.NaN },
                List(n) { Double.NaN }
            )
        }

        val plusDM = DoubleArray(n)
        val minusDM = DoubleArray(n)
        val tr = DoubleArray(n)

        for (i in 1 until n) {
            val upMove = bars[i].high - bars[i - 1].high
            val downMove = bars[i - 1].low - bars[i].low

            plusDM[i] = if (upMove > downMove && upMove > 0) upMove else 0.0
            minusDM[i] = if (downMove > upMove && downMove > 0) downMove else 0.0

            val hl = bars[i].high - bars[i].low
            val hpc = abs(bars[i].high - bars[i - 1].close)
            val lpc = abs(bars[i].low - bars[i - 1].close)
            tr[i] = max(hl, max(hpc, lpc))
        }

        val smoothedPlusDM = DoubleArray(n)
        val smoothedMinusDM = DoubleArray(n)
        val smoothedTR = DoubleArray(n)

        var sumPlusDM = 0.0
        var sumMinusDM = 0.0
        var sumTR = 0.0
        for (i in 1..period) {
            sumPlusDM += plusDM[i]
            sumMinusDM += minusDM[i]
            sumTR += tr[i]
        }
        smoothedPlusDM[period] = sumPlusDM
        smoothedMinusDM[period] = sumMinusDM
        smoothedTR[period] = sumTR

        for (i in period + 1 until n) {
            smoothedPlusDM[i] = smoothedPlusDM[i - 1] - (smoothedPlusDM[i - 1] / period) + plusDM[i]
            smoothedMinusDM[i] = smoothedMinusDM[i - 1] - (smoothedMinusDM[i - 1] / period) + minusDM[i]
            smoothedTR[i] = smoothedTR[i - 1] - (smoothedTR[i - 1] / period) + tr[i]
        }

        val plusDIResult = MutableList(n) { Double.NaN }
        val minusDIResult = MutableList(n) { Double.NaN }
        val dx = DoubleArray(n)

        for (i in period until n) {
            plusDIResult[i] = if (smoothedTR[i] != 0.0) (smoothedPlusDM[i] / smoothedTR[i]) * 100 else 0.0
            minusDIResult[i] = if (smoothedTR[i] != 0.0) (smoothedMinusDM[i] / smoothedTR[i]) * 100 else 0.0
            val diSum = plusDIResult[i] + minusDIResult[i]
            dx[i] = if (diSum != 0.0) (abs(plusDIResult[i] - minusDIResult[i]) / diSum) * 100 else 0.0
        }

        val adxResult = MutableList(n) { Double.NaN }
        val adxStart = 2 * period - 1
        if (adxStart < n) {
            var adxSum = 0.0
            for (i in period until adxStart + 1) {
                adxSum += dx[i]
            }
            adxResult[adxStart] = adxSum / period

            for (i in adxStart + 1 until n) {
                adxResult[i] = (adxResult[i - 1] * (period - 1) + dx[i]) / period
            }
        }

        return ADXResult(adxResult, plusDIResult, minusDIResult)
    }
}
