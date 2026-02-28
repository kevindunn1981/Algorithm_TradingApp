package com.algotrader.app.engine.indicators

import com.algotrader.app.data.model.PriceBar

class VWAP : Indicator {
    override val name: String = "VWAP"

    override fun calculate(bars: List<PriceBar>): List<Double> {
        if (bars.isEmpty()) return emptyList()

        val result = MutableList(bars.size) { Double.NaN }
        var cumulativeTPV = 0.0
        var cumulativeVolume = 0L

        for (i in bars.indices) {
            val typicalPrice = (bars[i].high + bars[i].low + bars[i].close) / 3.0
            cumulativeTPV += typicalPrice * bars[i].volume
            cumulativeVolume += bars[i].volume

            if (cumulativeVolume > 0) {
                result[i] = cumulativeTPV / cumulativeVolume
            }
        }
        return result
    }
}
