package com.algotrader.app.engine.strategy

import com.algotrader.app.data.model.OrderType
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.*

fun strategy(name: String, block: StrategyBuilder.() -> Unit): TradingStrategy {
    val builder = StrategyBuilder(name)
    builder.block()
    return builder.build()
}

class StrategyBuilder(private val strategyName: String) {
    private var description: String = ""
    private var initBlock: (StrategyContext.() -> Unit)? = null
    private var onBarBlock: (StrategyContext.(Int) -> Signal?)? = null
    private val parameters = mutableMapOf<String, Any>()

    fun describe(desc: String) {
        description = desc
    }

    fun param(name: String, value: Any) {
        parameters[name] = value
    }

    fun initialize(block: StrategyContext.() -> Unit) {
        initBlock = block
    }

    fun onBar(block: StrategyContext.(Int) -> Signal?) {
        onBarBlock = block
    }

    fun build(): TradingStrategy = DslStrategy(
        name = strategyName,
        description = description,
        parameters = parameters.toMap(),
        initBlock = initBlock,
        onBarBlock = onBarBlock
    )
}

class StrategyContext(val bars: List<PriceBar>) {
    private val indicatorCache = mutableMapOf<String, List<Double>>()
    var position: Int = 0

    val close: List<Double> get() = bars.map { it.close }
    val open: List<Double> get() = bars.map { it.open }
    val high: List<Double> get() = bars.map { it.high }
    val low: List<Double> get() = bars.map { it.low }
    val volume: List<Long> get() = bars.map { it.volume }

    fun sma(period: Int): List<Double> =
        indicatorCache.getOrPut("SMA_$period") { SMA(period).calculate(bars) }

    fun ema(period: Int): List<Double> =
        indicatorCache.getOrPut("EMA_$period") { EMA(period).calculate(bars) }

    fun rsi(period: Int = 14): List<Double> =
        indicatorCache.getOrPut("RSI_$period") { RSI(period).calculate(bars) }

    fun macd(fast: Int = 12, slow: Int = 26, signal: Int = 9): MacdResult {
        val key = "MACD_${fast}_${slow}_$signal"
        val macdLine = indicatorCache.getOrPut("${key}_macd") {
            MACD.calculateFull(close, fast, slow, signal).macdLine
        }
        val signalLine = indicatorCache.getOrPut("${key}_signal") {
            MACD.calculateFull(close, fast, slow, signal).signalLine
        }
        val histogram = indicatorCache.getOrPut("${key}_hist") {
            MACD.calculateFull(close, fast, slow, signal).histogram
        }
        return MacdResult(macdLine, signalLine, histogram)
    }

    fun bollingerBands(period: Int = 20, stdDev: Double = 2.0): BollingerBandsResult {
        val key = "BB_${period}_$stdDev"
        val upper = indicatorCache.getOrPut("${key}_upper") {
            BollingerBands.calculateFull(close, period, stdDev).upper
        }
        val middle = indicatorCache.getOrPut("${key}_middle") {
            BollingerBands.calculateFull(close, period, stdDev).middle
        }
        val lower = indicatorCache.getOrPut("${key}_lower") {
            BollingerBands.calculateFull(close, period, stdDev).lower
        }
        val bandwidth = indicatorCache.getOrPut("${key}_bw") {
            BollingerBands.calculateFull(close, period, stdDev).bandwidth
        }
        return BollingerBandsResult(upper, middle, lower, bandwidth)
    }

    fun atr(period: Int = 14): List<Double> =
        indicatorCache.getOrPut("ATR_$period") { ATR(period).calculate(bars) }

    fun vwap(): List<Double> =
        indicatorCache.getOrPut("VWAP") { VWAP().calculate(bars) }

    fun crossOver(series1: List<Double>, series2: List<Double>, index: Int): Boolean {
        if (index < 1) return false
        val prev1 = series1[index - 1]
        val prev2 = series2[index - 1]
        val curr1 = series1[index]
        val curr2 = series2[index]
        if (prev1.isNaN() || prev2.isNaN() || curr1.isNaN() || curr2.isNaN()) return false
        return prev1 <= prev2 && curr1 > curr2
    }

    fun crossUnder(series1: List<Double>, series2: List<Double>, index: Int): Boolean {
        if (index < 1) return false
        val prev1 = series1[index - 1]
        val prev2 = series2[index - 1]
        val curr1 = series1[index]
        val curr2 = series2[index]
        if (prev1.isNaN() || prev2.isNaN() || curr1.isNaN() || curr2.isNaN()) return false
        return prev1 >= prev2 && curr1 < curr2
    }

    fun buy(
        symbol: String = bars.firstOrNull()?.symbol ?: "",
        strength: Double = 1.0,
        reason: String = "",
        orderType: OrderType = OrderType.MARKET,
        limitPrice: Double? = null
    ) = Signal(
        symbol = symbol,
        action = SignalAction.BUY,
        strength = strength,
        orderType = orderType,
        limitPrice = limitPrice,
        reason = reason
    )

    fun sell(
        symbol: String = bars.firstOrNull()?.symbol ?: "",
        strength: Double = 1.0,
        reason: String = "",
        orderType: OrderType = OrderType.MARKET,
        limitPrice: Double? = null
    ) = Signal(
        symbol = symbol,
        action = SignalAction.SELL,
        strength = strength,
        orderType = orderType,
        limitPrice = limitPrice,
        reason = reason
    )

    fun hold() = Signal(
        symbol = bars.firstOrNull()?.symbol ?: "",
        action = SignalAction.HOLD,
        reason = "No signal"
    )
}

private class DslStrategy(
    override val name: String,
    override val description: String,
    private val parameters: Map<String, Any>,
    private val initBlock: (StrategyContext.() -> Unit)?,
    private val onBarBlock: (StrategyContext.(Int) -> Signal?)?
) : TradingStrategy {
    private lateinit var context: StrategyContext

    override fun initialize(bars: List<PriceBar>) {
        context = StrategyContext(bars)
        initBlock?.invoke(context)
    }

    override fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal? {
        if (!::context.isInitialized) {
            context = StrategyContext(bars)
        }
        return onBarBlock?.invoke(context, currentIndex)
    }

    override fun getParameters(): Map<String, Any> = parameters
}
