package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.MACD
import com.algotrader.app.engine.indicators.MacdResult
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class MacdStrategy(
    private val fastPeriod: Int = 12,
    private val slowPeriod: Int = 26,
    private val signalPeriod: Int = 9
) : TradingStrategy {

    override val name = "MACD Crossover"
    override val description = "Trades MACD line crossovers with signal line, confirmed by histogram"

    private lateinit var macdResult: MacdResult

    override fun initialize(bars: List<PriceBar>) {
        macdResult = MACD(fastPeriod, slowPeriod, signalPeriod).calculateFull(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol

        val currMacd = macdResult.macdLine[currentIndex]
        val prevMacd = macdResult.macdLine[currentIndex - 1]
        val currSignal = macdResult.signalLine[currentIndex]
        val prevSignal = macdResult.signalLine[currentIndex - 1]
        val histogram = macdResult.histogram[currentIndex]

        if (currMacd.isNaN() || prevMacd.isNaN() || currSignal.isNaN() || prevSignal.isNaN()) {
            return null
        }

        return when {
            prevMacd <= prevSignal && currMacd > currSignal && histogram > 0 -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "MACD crossed above signal (histogram: ${"%.4f".format(histogram)})"
            )
            prevMacd >= prevSignal && currMacd < currSignal && histogram < 0 -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "MACD crossed below signal (histogram: ${"%.4f".format(histogram)})"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "fastPeriod" to fastPeriod,
        "slowPeriod" to slowPeriod,
        "signalPeriod" to signalPeriod
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("MACD Crossover") {
    describe("Trade MACD/signal crossovers confirmed by histogram")
    param("fastPeriod", 12)
    param("slowPeriod", 26)
    param("signalPeriod", 9)

    onBar { index ->
        val macdData = macd(12, 26, 9)
        if (index < 1) return@onBar null

        val currMacd = macdData.macdLine[index]
        val prevMacd = macdData.macdLine[index - 1]
        val currSig = macdData.signalLine[index]
        val prevSig = macdData.signalLine[index - 1]
        val hist = macdData.histogram[index]

        if (currMacd.isNaN() || currSig.isNaN()) return@onBar null

        when {
            prevMacd <= prevSig && currMacd > currSig && hist > 0 ->
                buy(reason = "Bullish MACD crossover")
            prevMacd >= prevSig && currMacd < currSig && hist < 0 ->
                sell(reason = "Bearish MACD crossover")
            else -> null
        }
    }
}"""
    }
}
