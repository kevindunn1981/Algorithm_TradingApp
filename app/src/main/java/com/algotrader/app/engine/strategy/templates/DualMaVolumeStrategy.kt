package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.EMA
import com.algotrader.app.engine.indicators.SMA
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class DualMaVolumeStrategy(
    private val fastPeriod: Int = 20,
    private val slowPeriod: Int = 50,
    private val volumeSmaPeriod: Int = 20,
    private val volumeMultiplier: Double = 1.3
) : TradingStrategy {

    override val name = "Dual MA + Volume Filter"
    override val description = "EMA crossover validated by above-average volume, filtering out low-conviction signals"

    private var fastEma = listOf<Double>()
    private var slowEma = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        fastEma = EMA(fastPeriod).calculate(bars)
        slowEma = EMA(slowPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < volumeSmaPeriod + 1) return null
        val symbol = bars[currentIndex].symbol

        val fast = fastEma[currentIndex]
        val slow = slowEma[currentIndex]
        val prevFast = fastEma[currentIndex - 1]
        val prevSlow = slowEma[currentIndex - 1]

        if (fast.isNaN() || slow.isNaN() || prevFast.isNaN() || prevSlow.isNaN()) return null

        val avgVolume = bars.subList(currentIndex - volumeSmaPeriod, currentIndex)
            .map { it.volume }.average()
        val currentVolume = bars[currentIndex].volume
        val volumeRatio = currentVolume / avgVolume

        if (volumeRatio < volumeMultiplier) return null

        return when {
            prevFast <= prevSlow && fast > slow -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = (volumeRatio / volumeMultiplier).coerceAtMost(2.0) / 2.0,
                reason = "EMA golden cross + ${"%.1f".format(volumeRatio)}x average volume"
            )
            prevFast >= prevSlow && fast < slow -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = (volumeRatio / volumeMultiplier).coerceAtMost(2.0) / 2.0,
                reason = "EMA death cross + ${"%.1f".format(volumeRatio)}x average volume"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "fastPeriod" to fastPeriod,
        "slowPeriod" to slowPeriod,
        "volumeSmaPeriod" to volumeSmaPeriod,
        "volumeMultiplier" to volumeMultiplier
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Dual MA + Volume Filter") {
    describe("EMA crossover with above-average volume confirmation")
    param("fast", 20)
    param("slow", 50)
    param("volumeMult", 1.3)

    onBar { index ->
        if (index < 21) return@onBar null
        val fast = ema(20)
        val slow = ema(50)
        if (fast[index].isNaN() || slow[index].isNaN()) return@onBar null

        val avgVol = volume.subList(maxOf(0, index - 20), index).average()
        val volRatio = volume[index].toDouble() / avgVol
        if (volRatio < 1.3) return@onBar null

        when {
            crossOver(fast, slow, index) ->
                buy(reason = "Golden cross + volume surge")
            crossUnder(fast, slow, index) ->
                sell(reason = "Death cross + volume surge")
            else -> null
        }
    }
}"""
    }
}
