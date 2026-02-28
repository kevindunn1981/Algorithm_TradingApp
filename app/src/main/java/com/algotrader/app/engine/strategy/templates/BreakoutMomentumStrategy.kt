package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.ATR
import com.algotrader.app.engine.indicators.EMA
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class BreakoutMomentumStrategy(
    private val lookbackPeriod: Int = 20,
    private val emaPeriod: Int = 50,
    private val volumeMultiplier: Double = 1.5,
    private val atrPeriod: Int = 14
) : TradingStrategy {

    override val name = "Breakout Momentum"
    override val description = "High/Low breakouts confirmed by volume expansion and EMA trend alignment"

    private var emaValues = listOf<Double>()
    private var atrValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        emaValues = EMA(emaPeriod).calculate(bars)
        atrValues = ATR(atrPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < lookbackPeriod + 1) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close
        val prevClose = bars[currentIndex - 1].close
        val ema = emaValues[currentIndex]

        if (ema.isNaN()) return null

        val window = bars.subList(currentIndex - lookbackPeriod, currentIndex)
        val periodHigh = window.maxOf { it.high }
        val periodLow = window.minOf { it.low }

        val avgVolume = window.map { it.volume }.average()
        val currentVolume = bars[currentIndex].volume
        val volumeConfirm = currentVolume > avgVolume * volumeMultiplier

        val bullishTrend = close > ema
        val bearishTrend = close < ema

        return when {
            prevClose <= periodHigh && close > periodHigh && volumeConfirm && bullishTrend -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = 1.0,
                reason = "Breakout above ${lookbackPeriod}-bar high with ${String.format("%.1f", currentVolume / avgVolume)}x volume"
            )
            prevClose >= periodLow && close < periodLow && volumeConfirm && bearishTrend -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = 1.0,
                reason = "Breakdown below ${lookbackPeriod}-bar low with ${String.format("%.1f", currentVolume / avgVolume)}x volume"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "lookbackPeriod" to lookbackPeriod,
        "emaPeriod" to emaPeriod,
        "volumeMultiplier" to volumeMultiplier
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Breakout Momentum") {
    describe("Volume-confirmed breakouts with EMA trend alignment")
    param("lookback", 20)
    param("volumeMult", 1.5)

    onBar { index ->
        if (index < 21) return@onBar null
        val trendEma = ema(50)
        if (trendEma[index].isNaN()) return@onBar null

        val periodHigh = high.subList(index - 20, index).max()
        val periodLow = low.subList(index - 20, index).min()
        val avgVol = volume.subList(index - 20, index).average()
        val volConfirm = volume[index] > avgVol * 1.5

        when {
            close[index] > periodHigh && volConfirm && close[index] > trendEma[index] ->
                buy(reason = "Bullish breakout + volume + trend")
            close[index] < periodLow && volConfirm && close[index] < trendEma[index] ->
                sell(reason = "Bearish breakdown + volume + trend")
            else -> null
        }
    }
}"""
    }
}
