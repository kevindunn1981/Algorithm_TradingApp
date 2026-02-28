package com.algotrader.app.engine.strategy

import com.algotrader.app.data.model.PriceBar

interface TradingStrategy {
    val name: String
    val description: String

    fun initialize(bars: List<PriceBar>)

    fun onBar(currentIndex: Int, bars: List<PriceBar>): Signal?

    fun getParameters(): Map<String, Any> = emptyMap()
}
