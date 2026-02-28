package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

interface Indicator {
    val name: String
    fun calculate(bars: List<PriceBar>): List<Double>
}

data class IndicatorValue(
    val name: String,
    val values: List<Double>
)

data class MacdResult(
    val macdLine: List<Double>,
    val signalLine: List<Double>,
    val histogram: List<Double>
)

data class BollingerBandsResult(
    val upper: List<Double>,
    val middle: List<Double>,
    val lower: List<Double>,
    val bandwidth: List<Double>
)
