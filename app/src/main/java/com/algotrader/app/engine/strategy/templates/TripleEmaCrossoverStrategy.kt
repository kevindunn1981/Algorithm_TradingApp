package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.EMA
import com.algotrader.app.engine.indicators.RSI
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class TripleEmaCrossoverStrategy(
    private val fastPeriod: Int = 8,
    private val mediumPeriod: Int = 21,
    private val slowPeriod: Int = 55,
    private val rsiPeriod: Int = 14,
    private val volumeMultiplier: Double = 1.3
) : TradingStrategy {

    override val name = "Triple EMA Crossover"
    override val description = "Three EMA ribbon with RSI momentum and volume confirmation for high-conviction entries"

    private var fastEma = listOf<Double>()
    private var mediumEma = listOf<Double>()
    private var slowEma = listOf<Double>()
    private var rsiValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        fastEma = EMA(fastPeriod).calculate(bars)
        mediumEma = EMA(mediumPeriod).calculate(bars)
        slowEma = EMA(slowPeriod).calculate(bars)
        rsiValues = RSI(rsiPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 2) return null
        val symbol = bars[currentIndex].symbol

        val fast = fastEma[currentIndex]
        val medium = mediumEma[currentIndex]
        val slow = slowEma[currentIndex]
        val prevFast = fastEma[currentIndex - 1]
        val prevMedium = mediumEma[currentIndex - 1]
        val rsi = rsiValues[currentIndex]

        if (fast.isNaN() || medium.isNaN() || slow.isNaN()) return null

        val avgVolume = bars.subList(maxOf(0, currentIndex - 20), currentIndex)
            .map { it.volume }.average()
        val volumeConfirm = bars[currentIndex].volume > avgVolume * volumeMultiplier

        val bullishAlignment = fast > medium && medium > slow
        val bearishAlignment = fast < medium && medium < slow
        val fastCrossedMedium = prevFast <= prevMedium && fast > medium
        val fastCrossedUnderMedium = prevFast >= prevMedium && fast < medium

        return when {
            fastCrossedMedium && bullishAlignment && (!rsi.isNaN() && rsi > 50) && volumeConfirm -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = 1.0,
                reason = "Triple EMA bullish alignment + RSI ${"%.0f".format(rsi)} + volume confirmed"
            )
            fastCrossedMedium && medium > slow && (!rsi.isNaN() && rsi > 45) -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = 0.7,
                reason = "EMA fast/medium cross + uptrend (RSI: ${"%.0f".format(rsi)})"
            )
            fastCrossedUnderMedium && bearishAlignment && (!rsi.isNaN() && rsi < 50) && volumeConfirm -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = 1.0,
                reason = "Triple EMA bearish alignment + RSI ${"%.0f".format(rsi)} + volume confirmed"
            )
            fastCrossedUnderMedium && medium < slow && (!rsi.isNaN() && rsi < 55) -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = 0.7,
                reason = "EMA fast/medium cross + downtrend (RSI: ${"%.0f".format(rsi)})"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "fastPeriod" to fastPeriod,
        "mediumPeriod" to mediumPeriod,
        "slowPeriod" to slowPeriod,
        "rsiPeriod" to rsiPeriod
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Triple EMA Crossover") {
    describe("Three EMA ribbon + RSI momentum + volume confirmation")
    param("fast", 8)
    param("medium", 21)
    param("slow", 55)

    onBar { index ->
        if (index < 2) return@onBar null
        val fast = ema(8)
        val medium = ema(21)
        val slow = ema(55)
        val rsiLine = rsi(14)

        if (fast[index].isNaN() || slow[index].isNaN()) return@onBar null
        val bullish = fast[index] > medium[index] && medium[index] > slow[index]
        val bearish = fast[index] < medium[index] && medium[index] < slow[index]

        when {
            crossOver(fast, medium, index) && bullish && rsiLine[index] > 50 ->
                buy(reason = "Triple EMA bullish + RSI momentum")
            crossUnder(fast, medium, index) && bearish && rsiLine[index] < 50 ->
                sell(reason = "Triple EMA bearish + RSI weakness")
            else -> null
        }
    }
}"""
    }
}
