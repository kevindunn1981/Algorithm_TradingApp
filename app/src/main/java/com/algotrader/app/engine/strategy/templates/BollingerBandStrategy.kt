package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.BollingerBands
import com.algotrader.app.engine.indicators.BollingerBandsResult
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy

class BollingerBandStrategy(
    private val period: Int = 20,
    private val stdDev: Double = 2.0
) : TradingStrategy {

    override val name = "Bollinger Band Bounce"
    override val description = "Buys at lower band, sells at upper band (mean reversion)"

    private lateinit var bbResult: BollingerBandsResult

    override fun initialize(bars: List<PriceBar>) {
        bbResult = BollingerBands(period, stdDev).calculateFull(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol

        val upper = bbResult.upper[currentIndex]
        val lower = bbResult.lower[currentIndex]
        val prevClose = bars[currentIndex - 1].close
        val currClose = bars[currentIndex].close

        if (upper.isNaN() || lower.isNaN()) return null

        return when {
            prevClose <= lower && currClose > lower -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                reason = "Price bounced off lower Bollinger Band"
            )
            prevClose >= upper && currClose < upper -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                reason = "Price rejected at upper Bollinger Band"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "period" to period,
        "stdDev" to stdDev
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Bollinger Band Bounce") {
    describe("Mean reversion at Bollinger Bands boundaries")
    param("period", 20)
    param("stdDev", 2.0)

    onBar { index ->
        val bb = bollingerBands(20, 2.0)
        if (index < 1) return@onBar null

        val upper = bb.upper[index]
        val lower = bb.lower[index]
        val prevClose = close[index - 1]
        val currClose = close[index]

        if (upper.isNaN() || lower.isNaN()) return@onBar null

        when {
            prevClose <= lower && currClose > lower ->
                buy(reason = "Bounce off lower band")
            prevClose >= upper && currClose < upper ->
                sell(reason = "Rejection at upper band")
            else -> null
        }
    }
}"""
    }
}
