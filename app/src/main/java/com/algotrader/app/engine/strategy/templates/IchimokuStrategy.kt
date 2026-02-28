package com.algotrader.app.engine.strategy.templates

import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.IchimokuCloud
import com.algotrader.app.engine.indicators.IchimokuResult
import com.algotrader.app.engine.strategy.Signal
import com.algotrader.app.engine.strategy.SignalAction
import com.algotrader.app.engine.strategy.TradingStrategy
import kotlin.math.max
import kotlin.math.min

class IchimokuStrategy(
    private val tenkanPeriod: Int = 9,
    private val kijunPeriod: Int = 26,
    private val senkouBPeriod: Int = 52
) : TradingStrategy {

    override val name = "Ichimoku Cloud"
    override val description = "Japanese multi-component system: Tenkan/Kijun crossover filtered by Kumo cloud position"

    private lateinit var ichimoku: IchimokuResult

    override fun initialize(bars: List<PriceBar>) {
        ichimoku = IchimokuCloud(tenkanPeriod, kijunPeriod, senkouBPeriod).calculateFull(bars)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (currentIndex < 1) return null
        val symbol = bars[currentIndex].symbol
        val close = bars[currentIndex].close

        val tenkan = ichimoku.tenkanSen[currentIndex]
        val kijun = ichimoku.kijunSen[currentIndex]
        val prevTenkan = ichimoku.tenkanSen[currentIndex - 1]
        val prevKijun = ichimoku.kijunSen[currentIndex - 1]
        val spanA = ichimoku.senkouSpanA[currentIndex]
        val spanB = ichimoku.senkouSpanB[currentIndex]

        if (tenkan.isNaN() || kijun.isNaN() || prevTenkan.isNaN() || prevKijun.isNaN()) return null

        val cloudTop = if (!spanA.isNaN() && !spanB.isNaN()) max(spanA, spanB) else Double.NaN
        val cloudBottom = if (!spanA.isNaN() && !spanB.isNaN()) min(spanA, spanB) else Double.NaN
        val aboveCloud = !cloudTop.isNaN() && close > cloudTop
        val belowCloud = !cloudBottom.isNaN() && close < cloudBottom

        val tenkanCrossAbove = prevTenkan <= prevKijun && tenkan > kijun
        val tenkanCrossBelow = prevTenkan >= prevKijun && tenkan < kijun

        return when {
            tenkanCrossAbove && aboveCloud -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = 1.0,
                reason = "Tenkan crossed above Kijun, price above Kumo cloud"
            )
            tenkanCrossAbove && !belowCloud -> Signal(
                symbol = symbol,
                action = SignalAction.BUY,
                strength = 0.6,
                reason = "Tenkan crossed above Kijun (neutral cloud position)"
            )
            tenkanCrossBelow && belowCloud -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = 1.0,
                reason = "Tenkan crossed below Kijun, price below Kumo cloud"
            )
            tenkanCrossBelow && !aboveCloud -> Signal(
                symbol = symbol,
                action = SignalAction.SELL,
                strength = 0.6,
                reason = "Tenkan crossed below Kijun (neutral cloud position)"
            )
            else -> null
        }
    }

    override fun getParameters() = mapOf(
        "tenkanPeriod" to tenkanPeriod,
        "kijunPeriod" to kijunPeriod,
        "senkouBPeriod" to senkouBPeriod
    )

    companion object {
        const val TEMPLATE_CODE = """strategy("Ichimoku Cloud") {
    describe("Tenkan/Kijun crossover filtered by Kumo cloud position")
    param("tenkan", 9)
    param("kijun", 26)
    param("senkouB", 52)

    onBar { index ->
        if (index < 52) return@onBar null
        val tenkanLine = sma(9)  // Simplified Tenkan approximation
        val kijunLine = sma(26)  // Simplified Kijun approximation
        val longTrend = sma(52)

        val aboveTrend = close[index] > longTrend[index]
        val belowTrend = close[index] < longTrend[index]

        when {
            crossOver(tenkanLine, kijunLine, index) && aboveTrend ->
                buy(reason = "Bullish Ichimoku: TK cross above cloud")
            crossUnder(tenkanLine, kijunLine, index) && belowTrend ->
                sell(reason = "Bearish Ichimoku: TK cross below cloud")
            else -> null
        }
    }
}"""
    }
}
