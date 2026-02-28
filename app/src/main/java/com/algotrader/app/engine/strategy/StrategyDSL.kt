package com.algotrader.app.engine.strategy

import com.algotrader.app.data.model.OrderType
import com.algotrader.app.data.model.PriceBar
import com.algotrader.app.engine.indicators.ATR
import com.algotrader.app.engine.indicators.BollingerBands
import com.algotrader.app.engine.indicators.BollingerBandsResult
import com.algotrader.app.engine.indicators.CCI
import com.algotrader.app.engine.indicators.DonchianChannel
import com.algotrader.app.engine.indicators.DonchianChannelResult
import com.algotrader.app.engine.indicators.EMA
import com.algotrader.app.engine.indicators.KeltnerChannel
import com.algotrader.app.engine.indicators.KeltnerChannelResult
import com.algotrader.app.engine.indicators.MACD
import com.algotrader.app.engine.indicators.MacdResult
import com.algotrader.app.engine.indicators.RSI
import com.algotrader.app.engine.indicators.SMA
import com.algotrader.app.engine.indicators.Supertrend
import com.algotrader.app.engine.indicators.SupertrendResult
import com.algotrader.app.engine.indicators.VWAP
import com.algotrader.app.engine.indicators.WilliamsR

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

    fun cci(period: Int = 20): List<Double> =
        indicatorCache.getOrPut("CCI_$period") { CCI(period).calculate(bars) }

    fun williamsR(period: Int = 14): List<Double> =
        indicatorCache.getOrPut("WR_$period") { WilliamsR(period).calculate(bars) }

    fun donchianChannel(period: Int = 20): DonchianChannelResult {
        val key = "DC_$period"
        val upper = indicatorCache.getOrPut("${key}_upper") { DonchianChannel(period).calculateFull(bars).upper }
        val lower = indicatorCache.getOrPut("${key}_lower") { DonchianChannel(period).calculateFull(bars).lower }
        val middle = indicatorCache.getOrPut("${key}_middle") { DonchianChannel(period).calculateFull(bars).middle }
        return DonchianChannelResult(upper, lower, middle)
    }

    fun keltnerChannel(emaPeriod: Int = 20, atrMult: Double = 2.0): KeltnerChannelResult {
        val key = "KC_${emaPeriod}_$atrMult"
        val upper = indicatorCache.getOrPut("${key}_upper") { KeltnerChannel(emaPeriod, 14, atrMult).calculateFull(bars).upper }
        val middle = indicatorCache.getOrPut("${key}_middle") { KeltnerChannel(emaPeriod, 14, atrMult).calculateFull(bars).middle }
        val lower = indicatorCache.getOrPut("${key}_lower") { KeltnerChannel(emaPeriod, 14, atrMult).calculateFull(bars).lower }
        return KeltnerChannelResult(upper, middle, lower)
    }

    fun supertrend(atrPeriod: Int = 10, multiplier: Double = 3.0): SupertrendResult {
        val key = "ST_${atrPeriod}_$multiplier"
        val st = indicatorCache.getOrPut("${key}_st") { Supertrend(atrPeriod, multiplier).calculateFull(bars).supertrend }
        val dir = indicatorCache.getOrPut("${key}_dir") { Supertrend(atrPeriod, multiplier).calculateFull(bars).direction.map { it.toDouble() } }
        val ub = indicatorCache.getOrPut("${key}_ub") { Supertrend(atrPeriod, multiplier).calculateFull(bars).upperBand }
        val lb = indicatorCache.getOrPut("${key}_lb") { Supertrend(atrPeriod, multiplier).calculateFull(bars).lowerBand }
        return SupertrendResult(st, dir.map { it.toInt() }, ub, lb)
    }

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
