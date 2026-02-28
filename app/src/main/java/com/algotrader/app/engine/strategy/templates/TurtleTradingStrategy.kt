package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.ATR
import com.algotrader.app.engine.indicators.DonchianChannel
import com.algotrader.app.engine.indicators.DonchianChannelResult
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class TurtleTradingStrategy(
    private val entryPeriod: Int = 20,
    private val exitPeriod: Int = 10,
    private val atrPeriod: Int = 20
) : TradingStrategy {

    override val name = "Turtle Trading"
    override val description = "Richard Dennis' famous trend-following system using Donchian Channel breakouts with ATR-based stops"

    private lateinit var entryChannel: DonchianChannelResult
    private lateinit var exitChannel: DonchianChannelResult
    private var atrValues = listOf<Double>()

    override fun initialize(bars: List<PriceBar>) {
        entryChannel = DonchianChannel(entryPeriod).calculateFull(bars)
        exitChannel = DonchianChannel(exitPeriod).calculateFull(bars)
        atrValues = ATR(atrPeriod).calculate(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close
        val prevClose = bars[currentIndex - 1].close

        val upperEntry = entryChannel.upper[currentIndex - 1]
        val lowerEntry = entryChannel.lower[currentIndex - 1]
        val upperExit = exitChannel.upper[currentIndex - 1]
        val lowerExit = exitChannel.lower[currentIndex - 1]

        if (upperEntry.isNaN() || lowerEntry.isNaN()) return null

        return when {
            prevClose <= upperEntry && close > upperEntry -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "Breakout above ${entryPeriod}-day high (Turtle System 1)"
            )
            prevClose >= lowerEntry && close < lowerEntry -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Breakdown below ${entryPeriod}-day low (Turtle System 1)"
            )
            !lowerExit.isNaN() && prevClose >= lowerExit && close < lowerExit -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Exit: ${exitPeriod}-day low broken"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "entryPeriod" to entryPeriod,
        "exitPeriod" to exitPeriod,
        "atrPeriod" to atrPeriod
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Turtle Trading") {
    describe("Donchian Channel breakout with ATR-based position sizing")
    param("entryPeriod", 20)
    param("exitPeriod", 10)

    onBar { index ->
        if (index < 1) return@onBar null
        val entryHigh = high.subList(maxOf(0, index - 20), index).maxOrNull() ?: return@onBar null
        val entryLow = low.subList(maxOf(0, index - 20), index).minOrNull() ?: return@onBar null
        val price = close[index]
        val prevPrice = close[index - 1]

        when {
            prevPrice <= entryHigh && price > entryHigh ->
                buy(reason = "Breakout above 20-day high")
            prevPrice >= entryLow && price < entryLow ->
                sell(reason = "Breakdown below 20-day low")
            else -> null
        }
    }
}"""
    }
}
