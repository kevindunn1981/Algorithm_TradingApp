package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.ATR
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class OpeningRangeBreakoutStrategy(
    private val rangeBars: Int = 5,
    private val atrPeriod: Int = 14,
    private val confirmVolumeMult: Double = 1.2
) : TradingStrategy {

    override val name = "Opening Range Breakout"
    override val description = "Trades breakouts from the first N-bar range with volume confirmation and ATR stops"

    private var atrValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        atrValues = ATR(atrPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < rangeBars + atrPeriod) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close

        val rangeWindow = bars.subList(currentIndex - rangeBars, currentIndex)
        val rangeHigh = rangeWindow.maxOf { it.high }
        val rangeLow = rangeWindow.minOf { it.low }

        val avgVolume = bars.subList(maxOf(0, currentIndex - 20), currentIndex)
            .map { it.volume }.average()
        val currentVolume = bars[currentIndex].volume
        val volumeConfirm = currentVolume > avgVolume * confirmVolumeMult

        if (!volumeConfirm) return null

        val prevClose = bars[currentIndex - 1].close

        return when {
            prevClose <= rangeHigh && close > rangeHigh -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "Breakout above $rangeBars-bar range high (${"%.2f".format(rangeHigh)}) with volume"
            )
            prevClose >= rangeLow && close < rangeLow -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Breakdown below $rangeBars-bar range low (${"%.2f".format(rangeLow)}) with volume"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "rangeBars" to rangeBars,
        "atrPeriod" to atrPeriod,
        "confirmVolumeMult" to confirmVolumeMult
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Opening Range Breakout") {
    describe("Trade breakouts from N-bar range with volume confirmation")
    param("rangeBars", 5)
    param("volumeMult", 1.2)

    onBar { index ->
        if (index < 20) return@onBar null
        val rangeHigh = high.subList(index - 5, index).max()
        val rangeLow = low.subList(index - 5, index).min()
        val avgVol = volume.subList(maxOf(0, index - 20), index).average()
        val volConfirm = volume[index] > avgVol * 1.2

        if (!volConfirm) return@onBar null

        when {
            close[index - 1] <= rangeHigh && close[index] > rangeHigh ->
                buy(reason = "Range breakout with volume")
            close[index - 1] >= rangeLow && close[index] < rangeLow ->
                sell(reason = "Range breakdown with volume")
            else -> null
        }
    }
}"""
    }
}
