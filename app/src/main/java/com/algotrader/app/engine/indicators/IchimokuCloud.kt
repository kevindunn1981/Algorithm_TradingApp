package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

data class IchimokuResult(
    val tenkanSen: List<Double>,
    val kijunSen: List<Double>,
    val senkouSpanA: List<Double>,
    val senkouSpanB: List<Double>,
    val chikouSpan: List<Double>
)

class IchimokuCloud(
    private val tenkanPeriod: Int = 9,
    private val kijunPeriod: Int = 26,
    private val senkouBPeriod: Int = 52,
    private val displacement: Int = 26
) : Indicator {
    override val name: String = "Ichimoku($tenkanPeriod,$kijunPeriod,$senkouBPeriod)"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        return calculateFull(bars).tenkanSen
    }

    fun calculateFull(bars: List<PriceBar>): IchimokuResult {
        val n = bars.size
        val tenkan = MutableList(n) { Double.NaN }
        val kijun = MutableList(n) { Double.NaN }
        val senkouA = MutableList(n + displacement) { Double.NaN }
        val senkouB = MutableList(n + displacement) { Double.NaN }
        val chikou = MutableList(n) { Double.NaN }

        for (i in bars.indices) {
            if (i >= tenkanPeriod - 1) {
                val window = bars.subList(i - tenkanPeriod + 1, i + 1)
                tenkan[i] = (window.maxOf { it.high } + window.minOf { it.low }) / 2.0
            }
            if (i >= kijunPeriod - 1) {
                val window = bars.subList(i - kijunPeriod + 1, i + 1)
                kijun[i] = (window.maxOf { it.high } + window.minOf { it.low }) / 2.0
            }
            if (i >= kijunPeriod - 1 && !tenkan[i].isNaN() && !kijun[i].isNaN()) {
                val futureIdx = i + displacement
                if (futureIdx < senkouA.size) {
                    senkouA[futureIdx] = (tenkan[i] + kijun[i]) / 2.0
                }
            }
            if (i >= senkouBPeriod - 1) {
                val window = bars.subList(i - senkouBPeriod + 1, i + 1)
                val futureIdx = i + displacement
                if (futureIdx < senkouB.size) {
                    senkouB[futureIdx] = (window.maxOf { it.high } + window.minOf { it.low }) / 2.0
                }
            }
            val pastIdx = i - displacement
            if (pastIdx >= 0) {
                chikou[pastIdx] = bars[i].close
            }
        }

        val trimmedSenkouA = if (senkouA.size > n) senkouA.subList(0, n) else senkouA
        val trimmedSenkouB = if (senkouB.size > n) senkouB.subList(0, n) else senkouB

        return IchimokuResult(tenkan, kijun, trimmedSenkouA, trimmedSenkouB, chikou)
    }
}
