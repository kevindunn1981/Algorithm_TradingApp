package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.BollingerBands
import com.algotrader.app.engine.indicators.BollingerBandsResult
import com.algotrader.app.engine.indicators.MACD
import com.algotrader.app.engine.indicators.MacdResult
import com.algotrader.app.engine.indicators.RSI
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class MeanReversionConfluenceStrategy(
    private val bbPeriod: Int = 20,
    private val rsiPeriod: Int = 14,
    private val rsiOversold: Double = 30.0,
    private val rsiOverbought: Double = 70.0
) : TradingStrategy {

    override val name = "Mean Reversion Confluence"
    override val description = "Multi-indicator confluence: RSI + Bollinger Bands + MACD histogram for high-probability reversals"

    private lateinit var bbResult: BollingerBandsResult
    private var rsiValues = listOf<Double>()
    private lateinit var macdResult: MacdResult

    override fun initialize(bars: List<PriceBar>) {
        bbResult = BollingerBands(bbPeriod).calculateFull(bars)
        rsiValues = RSI(rsiPeriod).calculate(bars)
        macdResult = MACD().calculateFull(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 2) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close

        val bbLower = bbResult.lower[currentIndex]
        val bbUpper = bbResult.upper[currentIndex]
        val rsi = rsiValues[currentIndex]
        val prevRsi = rsiValues[currentIndex - 1]
        val histogram = macdResult.histogram[currentIndex]
        val prevHistogram = macdResult.histogram[currentIndex - 1]

        if (bbLower.isNaN() || rsi.isNaN() || histogram.isNaN()) return null

        val oversold = rsi < rsiOversold || prevRsi < rsiOversold
        val overbought = rsi > rsiOverbought || prevRsi > rsiOverbought
        val belowLowerBB = close <= bbLower
        val aboveUpperBB = close >= bbUpper
        val macdTurningUp = !prevHistogram.isNaN() && histogram > prevHistogram
        val macdTurningDown = !prevHistogram.isNaN() && histogram < prevHistogram

        var bullSignals = 0
        if (oversold) bullSignals++
        if (belowLowerBB) bullSignals++
        if (macdTurningUp) bullSignals++

        var bearSignals = 0
        if (overbought) bearSignals++
        if (aboveUpperBB) bearSignals++
        if (macdTurningDown) bearSignals++

        return when {
            bullSignals >= 2 && rsi > prevRsi -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = bullSignals / 3.0,
                reason = "Confluence buy: ${bullSignals}/3 indicators (RSI: ${"%.0f".format(rsi)})"
            )
            bearSignals >= 2 && rsi < prevRsi -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = bearSignals / 3.0,
                reason = "Confluence sell: ${bearSignals}/3 indicators (RSI: ${"%.0f".format(rsi)})"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "bbPeriod" to bbPeriod,
        "rsiPeriod" to rsiPeriod,
        "rsiOversold" to rsiOversold,
        "rsiOverbought" to rsiOverbought
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Mean Reversion Confluence") {
    describe("Multi-indicator: RSI + Bollinger + MACD for reversals")
    param("rsiOversold", 30.0)
    param("rsiOverbought", 70.0)

    onBar { index ->
        if (index < 2) return@onBar null
        val bb = bollingerBands(20, 2.0)
        val rsiLine = rsi(14)
        val macdData = macd(12, 26, 9)

        val rsiVal = rsiLine[index]
        val hist = macdData.histogram[index]
        val prevHist = macdData.histogram[index - 1]
        if (rsiVal.isNaN() || hist.isNaN()) return@onBar null

        var bullScore = 0
        if (rsiVal < 30) bullScore++
        if (close[index] <= bb.lower[index]) bullScore++
        if (hist > prevHist) bullScore++

        var bearScore = 0
        if (rsiVal > 70) bearScore++
        if (close[index] >= bb.upper[index]) bearScore++
        if (hist < prevHist) bearScore++

        when {
            bullScore >= 2 -> buy(reason = "Confluence: $bullScore/3 bullish")
            bearScore >= 2 -> sell(reason = "Confluence: $bearScore/3 bearish")
            else -> null
        }
    }
}"""
    }
}
