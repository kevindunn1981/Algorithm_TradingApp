package com.algotrader.app.engine.strategy

import com.algotrader.app.data.model.OrderSide
import com.algotrader.app.data.model.OrderType

data class Signal(
    val symbol: String,
    val action: SignalAction,
    val strength: Double = 1.0,
    val orderType: OrderType = OrderType.MARKET,
    val limitPrice: Double? = null,
    val stopPrice: Double? = null,
    val quantity: Double? = null,
    val reason: String = ""
)

enum class SignalAction {
    BUY, SELL, HOLD;

    fun toOrderSide(): OrderSide? = when (this) {
        BUY -> OrderSide.BUY
        SELL -> OrderSide.SELL
        HOLD -> null
    }
}
